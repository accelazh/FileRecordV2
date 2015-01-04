package org.accela.file.record.impl;

import java.io.IOException;

import org.accela.common.Assertion;
import org.accela.file.common.DataFormatException;
import org.accela.file.record.RecordArray;
import org.accela.file.record.RecordArrayFactory;
import org.accela.file.record.RecordPool;
import org.accela.file.record.RecordPoolKeyIterator;
import org.accela.file.record.util.LinkedBuffer;
import org.accela.file.record.util.LinkedBufferFactory;

public class PlainRecordPool implements RecordPool
{
	private ObjectRecordArray accesser = null;

	private int slotSize = 0;

	private LinkedBufferFactory delegate = null;

	private long poolSize = 0;

	private long size = 0;

	public PlainRecordPool(RecordArrayFactory factory,
			int slotSize,
			long poolSize) throws IOException
	{
		if (null == factory)
		{
			throw new IllegalArgumentException("factory should not be null");
		}
		if (slotSize < 0)
		{
			throw new IllegalArgumentException(
					"slotSize should not be negative: " + slotSize);
		}
		if (poolSize < 0)
		{
			throw new IllegalArgumentException(
					"poolSize should not be negative");
		}

		this.slotSize = slotSize;
		this.delegate = new LinkedBufferFactory(getSlotSize());

		RecordArray recordArray = factory.create(this.getBlockSize());
		if (null == recordArray)
		{
			throw new IllegalArgumentException("factory should not create null");
		}
		this.accesser = new ObjectRecordArray(recordArray);

		init(poolSize);
	}

	public RecordArray getAccesser()
	{
		return accesser.getAccesser();
	}

	@Override
	public int getSlotSize()
	{
		return this.slotSize;
	}

	private int getBlockSize()
	{
		return delegate.getBlockSize();
	}

	// used to init size and poolSize, and construct free list
	private void init(long initPoolSize) throws IOException
	{
		if (initPoolSize < 1)
		{
			initPoolSize = 1;
		}

		// shrink poolSize
		long realPoolSize = 1;
		for (long idx = 1; idx < initPoolSize; idx++)
		{
			try
			{
				LinkedBuffer buffer = unsafeGet(idx);
				if (buffer.isUsable())
				{
					realPoolSize = idx + 1;
				}
			}
			catch (DataFormatException ex)
			{
				// skip
			}
		}
		poolSize = realPoolSize;
		assert (poolSize >= 1) : Assertion.declare();

		// format free block list header
		set(0, delegate.createBuffer(false, 0, new byte[0]));

		// construct free list
		long freeSize = 0;
		for (long idx = 1; idx < poolSize; idx++)
		{
			LinkedBuffer buffer = safeGet(idx, delegate.createBuffer(false,
					0,
					new byte[0]));
			if (buffer.isUsable())
			{
				continue;
			}

			free(idx);
			freeSize++;
		}

		// figure out size
		this.size = poolSize - 1 - freeSize;
	}

	private LinkedBuffer safeGet(long idx, LinkedBuffer pitch)
			throws IOException
	{
		LinkedBuffer buffer = null;
		while (null == buffer)
		{
			try
			{
				buffer = unsafeGet(idx);
			}
			catch (DataFormatException ex)
			{
				repair(idx, pitch);
			}
		}

		return buffer;
	}

	private LinkedBuffer unsafeGet(long idx) throws IOException,
			DataFormatException
	{
		return accesser.get(idx, delegate);
	}

	private void repair(long idx, LinkedBuffer pitch) throws IOException
	{
		assert (pitch != null) : Assertion.declare();

		set(idx, pitch);
	}

	private void set(long idx, LinkedBuffer buffer) throws IOException
	{
		assert (buffer != null) : Assertion.declare();

		accesser.set(idx, buffer, delegate);
	}

	private LinkedBuffer[] getFreeListHeadAndNext() throws IOException
	{
		LinkedBuffer head = safeGet(0, delegate.createBuffer(false,
				0,
				new byte[0]));

		// check head pointer points to a legal block
		boolean error = false;
		if (head.isUsable())
		{
			error = true;
			head.setUsable(false);
		}
		if (head.getPointer() < 0 || head.getPointer() >= poolSize)
		{
			error = true;
			head.setPointer(0);
		}
		LinkedBuffer next = null;
		if (0 == head.getPointer())
		{
			next = null;
		}
		else
		{
			try
			{
				next = unsafeGet(head.getPointer());
				if (next.isUsable())
				{
					error = true;
					head.setPointer(0);
					next = null;
				}
			}
			catch (DataFormatException ex)
			{
				error = true;
				head.setPointer(0);
			}
		}

		if (error)
		{
			set(0, head);
		}

		return new LinkedBuffer[] { head, next };
	}

	// don't rely on this.size, or change this.size
	// any block freed will be set to be unusable
	private void free(long idx) throws IOException
	{
		assert (idx > 0) : Assertion.declare();
		assert (idx < poolSize()) : Assertion.declare();

		LinkedBuffer buffer = delegate.createBuffer(false, 0, new byte[0]);

		// free list
		LinkedBuffer head = getFreeListHeadAndNext()[0];

		buffer.setPointer(head.getPointer());
		head.setPointer(idx);

		set(idx, buffer);
		set(0, head);
	}

	// alloc don't change size or rely on size
	// ensure that the block returned is usable
	// allocated block will be set with data
	private long alloc(byte[] data) throws IOException
	{
		assert (data != null);
		assert (data.length <= getSlotSize());

		long idx = listAlloc(data);
		if (idx <= 0)
		{
			assert (0 == idx) : Assertion.declare();
			idx = expandAlloc(data);
		}

		assert (idx > 0 && idx < poolSize) : Assertion.declare();
		return idx;
	}

	// return 0 if no free block
	private long listAlloc(byte[] data) throws IOException
	{
		LinkedBuffer[] headAndNext = getFreeListHeadAndNext();
		LinkedBuffer head = headAndNext[0];
		LinkedBuffer next = headAndNext[1];

		if (0 == head.getPointer() || null == next)
		{
			assert (null == next) : Assertion.declare();
			return 0;
		}

		long idx = head.getPointer();

		head.setPointer(next.getPointer());
		next = delegate.createBuffer(true, 0, data);

		set(0, head);
		set(idx, next);

		return idx;
	}

	// will change pool size
	private long expandAlloc(byte[] data) throws IOException
	{
		long idx = poolSize;
		set(idx, delegate.createBuffer(true, 0, data));
		poolSize++;

		return idx;
	}

	@Override
	public synchronized void close() throws IOException
	{
		this.accesser.close();
	}

	@Override
	public synchronized void flush() throws IOException
	{
		this.accesser.flush();
	}

	@Override
	public synchronized long put(byte[] data) throws IOException
	{
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

		long idx = alloc(data);
		size++;

		return idx;
	}

	@Override
	public synchronized boolean setIfContains(long key, byte[] data)
			throws IOException
	{
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
		if (!contains(key))
		{
			return false;
		}

		set(key, delegate.createBuffer(true, 0, data));
		return true;
	}

	@Override
	public synchronized byte[] getIfContains(long key) throws IOException
	{
		if (key <= 0 || key >= poolSize)
		{
			return null;
		}

		try
		{
			LinkedBuffer buffer = unsafeGet(key);
			if (buffer.isUsable())
			{
				return buffer.getData();
			}
			else
			{
				return null;
			}
		}
		catch (DataFormatException ex)
		{
			return null;
		}
	}

	@Override
	public synchronized boolean contains(long key) throws IOException
	{
		return getIfContains(key) != null;
	}

	@Override
	public synchronized boolean removeIfContains(long key) throws IOException
	{
		if (!contains(key))
		{
			return false;
		}

		free(key);
		size--;

		assert (!contains(key)) : Assertion.declare();
		return true;
	}

	@Override
	public synchronized void clear() throws IOException
	{
		init(0);
	}

	@Override
	public synchronized RecordPoolKeyIterator iterator()
	{
		return new RecordPoolKeyIterator(this);
	}

	@Override
	public synchronized long size()
	{
		assert (this.size >= 0) : Assertion.declare();
		assert (this.size < poolSize) : Assertion.declare();
		return this.size;
	}

	@Override
	public synchronized long poolSize()
	{
		return this.poolSize;
	}
}
