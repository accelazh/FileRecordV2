package org.accela.file.record.impl;

import java.io.IOException;

import org.accela.common.Assertion;
import org.accela.file.common.DataFormatException;
import org.accela.file.record.FileAccesser;
import org.accela.file.record.RecordArray;
import org.accela.file.record.util.Buffer;
import org.accela.file.record.util.BufferFactory;

public class PlainRecordArray implements RecordArray
{
	private FileAccesser accesser = null;

	private int slotSize = 0;

	private int blockSizePow = 0;

	private long maxIdx = -1;

	private BufferFactory delegate = null;

	public PlainRecordArray(FileAccesser accesser, int slotSize)
	{
		if (null == accesser)
		{
			throw new IllegalArgumentException("accesser should not be null");
		}
		if (slotSize < 0)
		{
			throw new IllegalArgumentException(
					"slotSize should not be negative: " + slotSize);
		}

		this.slotSize = slotSize;
		this.delegate = new BufferFactory(getSlotSize());
		this.accesser = accesser;

		this.blockSizePow = initBlockSizePow();
		this.maxIdx = -1;
	}

	private int initBlockSizePow()
	{
		int orignBlockSize = delegate.getBlockSize();
		int newBlockSize = 1;
		int newBlockSizePow = 0;
		while (newBlockSize < orignBlockSize)
		{
			newBlockSize <<= 1;
			newBlockSizePow++;
		}

		assert ((1 << newBlockSizePow) >= orignBlockSize) : Assertion.declare();
		return newBlockSizePow;
	}

	public FileAccesser getAccesser()
	{
		return accesser;
	}

	@Override
	public int getSlotSize()
	{
		return slotSize;
	}

	private int getBlockSizePow()
	{
		return blockSizePow;
	}

	private int getBlockSize()
	{
		return 1 << getBlockSizePow();
	}

	private long getStartPos(long idx)
	{
		assert (idx >= 0) : Assertion.declare();
		return idx << getBlockSizePow();
	}

	private long getEndPos(long idx)
	{
		return getStartPos(idx) + getBlockSize();
	}

	private void extendTo(long idx) throws IOException
	{
		if (idx <= maxIdx)
		{
			return;
		}

		long endPos = getEndPos(idx * 2);
		if (accesser.length() < endPos)
		{
			accesser.setLength(endPos);
		}
		this.maxIdx = idx * 2;
	}

	@Override
	public synchronized void close() throws IOException
	{
		accesser.close();
	}

	@Override
	public synchronized void flush()
	{
		// do nothing
	}

	private void repair(long idx) throws IOException
	{
		assert (idx >= 0);
		set(idx, delegate.createBuffer(new byte[0]));
	}

	@Override
	public synchronized byte[] get(long idx) throws IOException
	{
		if (idx < 0)
		{
			throw new IllegalArgumentException("idx should not be negative");
		}

		Buffer buffer = safeGet(idx);
		return buffer.getData();
	}

	private Buffer safeGet(long idx) throws IOException
	{
		Buffer buffer = null;
		while (null == buffer)
		{
			try
			{
				buffer = unsafeGet(idx);
			}
			catch (DataFormatException ex)
			{
				repair(idx);
			}
		}

		return buffer;
	}

	private Buffer unsafeGet(long idx) throws IOException, DataFormatException
	{
		assert (idx >= 0);

		extendTo(idx);

		accesser.seek(getStartPos(idx));
		Buffer buffer = delegate.read(accesser);
		return buffer;
	}

	@Override
	public synchronized void set(long idx, byte[] data) throws IOException
	{
		if (idx < 0)
		{
			throw new IllegalArgumentException("idx should not be negative");
		}
		if (null == data)
		{
			throw new IllegalArgumentException("data should not be null");
		}
		if (data.length > getSlotSize())
		{
			throw new IllegalArgumentException(
					"data.length should not be greater than slotSize: "
							+ "data.length="
							+ data.length
							+ ", slotSize="
							+ getSlotSize());
		}

		Buffer buffer = delegate.createBuffer(data);
		set(idx, buffer);
	}

	private void set(long idx, Buffer buffer) throws IOException
	{
		assert (idx >= 0) : Assertion.declare();
		assert (buffer != null) : Assertion.declare();

		accesser.seek(getStartPos(idx));
		delegate.write(accesser, buffer);
	}

}
