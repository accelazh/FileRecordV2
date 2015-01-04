package org.accela.file.record.impl;

import java.io.IOException;

import org.accela.file.common.BytePersistanceDelegate;
import org.accela.file.common.DataFormatException;
import org.accela.file.common.PersistanceDelegate;
import org.accela.file.record.RecordArray;

public class ObjectRecordArray implements RecordArray
{
	private RecordArray accesser = null;

	public ObjectRecordArray(RecordArray accesser)
	{
		if (null == accesser)
		{
			throw new IllegalArgumentException("accesser should not be null");
		}

		this.accesser = accesser;
	}

	public RecordArray getAccesser()
	{
		return accesser;
	}

	@Override
	public void close() throws IOException
	{
		accesser.close();
	}

	@Override
	public void flush() throws IOException
	{
		accesser.flush();
	}

	@Override
	public byte[] get(long idx) throws IOException
	{
		return accesser.get(idx);
	}

	@Override
	public int getSlotSize()
	{
		return accesser.getSlotSize();
	}

	@Override
	public void set(long idx, byte[] data) throws IOException
	{
		accesser.set(idx, data);
	}

	public <T> T get(long idx, PersistanceDelegate<T> delegate)
			throws IOException, DataFormatException
	{
		if (null == delegate)
		{
			throw new IllegalArgumentException("delegate should not be null");
		}

		return new BytePersistanceDelegate<T>(delegate).readBytes(get(idx));
	}

	public <T> void set(long idx, T object, PersistanceDelegate<T> delegate)
			throws IOException
	{
		if (null == object)
		{
			throw new IllegalArgumentException("object should not be null");
		}
		if (null == delegate)
		{
			throw new IllegalArgumentException("delegate should not be null");
		}

		set(idx, new BytePersistanceDelegate<T>(delegate).writeBytes(object));
	}
}
