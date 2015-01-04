package org.accela.file.record.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.accela.file.common.DataFormatException;
import org.accela.file.common.PersistanceDelegate;

public class BufferFactory implements PersistanceDelegate<Buffer>
{
	private int slotSize = 0;

	private int blockSize = 0;

	public BufferFactory(int slotSize)
	{
		if (slotSize < 0)
		{
			throw new IllegalArgumentException("slotSize should not be null");
		}

		this.slotSize = slotSize;
		this.blockSize = Integer.SIZE / Byte.SIZE + slotSize;
	}

	public int getSlotSize()
	{
		return slotSize;
	}

	public int getBlockSize()
	{
		return blockSize;
	}

	public Buffer createBuffer(byte[] data)
	{
		return new Buffer(data, getSlotSize());
	}

	@Override
	public Buffer read(DataInput in) throws IOException, DataFormatException
	{
		int dataLength = in.readInt();
		byte[] slot = new byte[getSlotSize()];
		in.readFully(slot, 0, Math.max(0, Math.min(dataLength, slot.length)));

		try
		{
			return new Buffer(dataLength, slot);
		}
		catch (IllegalArgumentException ex)
		{
			throw new DataFormatException(ex);
		}
	}

	@Override
	public void write(DataOutput out, Buffer buffer) throws IOException
	{
		if (buffer.getSlotSize() != getSlotSize())
		{
			throw new IllegalArgumentException("slotSize not matched");
		}

		out.writeInt(buffer.getDataLength());
		out.write(buffer.getSlot(), 0, buffer.getDataLength());
	}

}
