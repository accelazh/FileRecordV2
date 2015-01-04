package org.accela.file.record.util;

public class LinkedBuffer
{
	private boolean usable = false;

	private long pointer = 0;

	private Buffer buffer = null;

	protected LinkedBuffer(boolean usable,
			long pointer,
			byte[] data,
			int slotSize)
	{
		if (pointer < 0)
		{
			throw new IllegalArgumentException("pointer should not be negative");
		}

		this.usable = usable;
		this.pointer = pointer;
		this.buffer = new Buffer(data, slotSize);
	}

	protected LinkedBuffer(boolean usable, long pointer, Buffer buffer)
	{
		if (pointer < 0)
		{
			throw new IllegalArgumentException("pointer should not be negative");
		}
		if (null == buffer)
		{
			throw new IllegalArgumentException("buffer should not be null");
		}

		this.usable = usable;
		this.pointer = pointer;
		this.buffer = new Buffer(buffer);
	}

	public boolean isUsable()
	{
		return this.usable;
	}

	public void setUsable(boolean usable)
	{
		this.usable = usable;
	}

	public long getPointer()
	{
		return this.pointer;
	}

	public void setPointer(long pointer)
	{
		if (pointer < 0)
		{
			throw new IllegalArgumentException("pointer should not be negative");
		}

		this.pointer = pointer;
	}

	public byte[] getData()
	{
		return buffer.getData();
	}

	protected Buffer getBuffer()
	{
		return this.buffer;
	}

	protected int getSlotSize()
	{
		return this.buffer.getSlotSize();
	}

}
