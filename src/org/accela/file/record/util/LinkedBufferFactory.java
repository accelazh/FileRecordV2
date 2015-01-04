package org.accela.file.record.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.accela.file.common.DataFormatException;
import org.accela.file.common.PersistanceDelegate;

public class LinkedBufferFactory implements PersistanceDelegate<LinkedBuffer>
{
	private int blockSize = 0;

	private BufferFactory factory = null;

	public LinkedBufferFactory(int slotSize)
	{
		this.factory = new BufferFactory(slotSize);
		this.blockSize = Byte.SIZE
				/ Byte.SIZE
				+ Long.SIZE
				/ Byte.SIZE
				+ factory.getBlockSize();
	}

	public int getSlotSize()
	{
		return factory.getSlotSize();
	}

	public int getBlockSize()
	{
		return blockSize;
	}

	public LinkedBuffer createBuffer(boolean usable, long pointer, byte[] data)
	{
		return new LinkedBuffer(usable, pointer, data, getSlotSize());
	}

	@Override
	public LinkedBuffer read(DataInput in) throws IOException,
			DataFormatException
	{
		boolean usable = in.readBoolean();
		long pointer = in.readLong();
		Buffer buffer = factory.read(in);

		try
		{
			return new LinkedBuffer(usable, pointer, buffer);
		}
		catch (IllegalArgumentException ex)
		{
			throw new DataFormatException(ex);
		}
	}

	@Override
	public void write(DataOutput out, LinkedBuffer buffer) throws IOException
	{
		if (buffer.getSlotSize() != this.getSlotSize())
		{
			throw new IllegalArgumentException("slotSize not matched");
		}

		out.writeBoolean(buffer.isUsable());
		out.writeLong(buffer.getPointer());
		factory.write(out, buffer.getBuffer());
	}

}
