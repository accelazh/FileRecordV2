package org.accela.file.record.util;

import java.util.Arrays;

public class Buffer
{
	private int dataLength = 0;

	private byte[] slot = null;

	protected Buffer(Buffer buffer)
	{
		if (null == buffer)
		{
			throw new IllegalArgumentException("buffer should not be null");
		}

		this.dataLength = buffer.getDataLength();
		this.slot = Arrays.copyOf(buffer.getSlot(), buffer.getSlot().length);
	}

	protected Buffer(int dataLength, byte[] slot)
	{
		if (dataLength < 0)
		{
			throw new IllegalArgumentException("length should not be negative");
		}
		if (null == slot)
		{
			throw new IllegalArgumentException("buf should not be null");
		}
		if (dataLength > slot.length)
		{
			throw new IllegalArgumentException(
					"length should not be larger than buf.length");
		}

		this.dataLength = dataLength;
		this.slot = Arrays.copyOf(slot, slot.length);
	}

	protected Buffer(byte[] data, int slotSize)
	{
		if (null == data)
		{
			throw new IllegalArgumentException("data should not be null");
		}
		if (slotSize < 0)
		{
			throw new IllegalArgumentException(
					"slotSize should not be negative");
		}
		if (data.length > slotSize)
		{
			throw new IllegalArgumentException(
					"data.length should not be larger than slotSize");
		}

		this.dataLength = data.length;
		this.slot = Arrays.copyOf(data, slotSize);
	}

	protected int getDataLength()
	{
		return this.dataLength;
	}

	protected byte[] getSlot()
	{
		return this.slot;
	}

	public byte[] getData()
	{
		return Arrays.copyOf(slot, dataLength);
	}

	protected int getSlotSize()
	{
		return this.slot.length;
	}
}
