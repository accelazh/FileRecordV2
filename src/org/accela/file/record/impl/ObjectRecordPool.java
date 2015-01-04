package org.accela.file.record.impl;

import java.io.IOException;

import org.accela.file.common.BytePersistanceDelegate;
import org.accela.file.common.DataFormatException;
import org.accela.file.common.PersistanceDelegate;
import org.accela.file.record.RecordPool;
import org.accela.file.record.RecordPoolKeyIterator;

public class ObjectRecordPool implements RecordPool
{
	private RecordPool accesser = null;

	public ObjectRecordPool(RecordPool accesser)
	{
		if (null == accesser)
		{
			throw new IllegalArgumentException("accesser should not be null");
		}

		this.accesser = accesser;
	}

	public RecordPool getAccesser()
	{
		return accesser;
	}

	@Override
	public int getSlotSize()
	{
		return accesser.getSlotSize();
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
	public RecordPoolKeyIterator iterator()
	{
		return accesser.iterator();
	}

	@Override
	public void clear() throws IOException
	{
		accesser.clear();
	}

	@Override
	public long poolSize()
	{
		return accesser.poolSize();
	}

	@Override
	public long size()
	{
		return accesser.size();
	}

	@Override
	public long put(byte[] data) throws IOException
	{
		return accesser.put(data);
	}

	@Override
	public boolean contains(long key) throws IOException
	{
		return accesser.contains(key);
	}

	public byte[] get(long key) throws IOException
	{
		byte[] data = getIfContains(key);
		if (null == data)
		{
			throw new IllegalArgumentException("key not found: " + key);
		}

		return data;
	}

	@Override
	public byte[] getIfContains(long key) throws IOException
	{
		return accesser.getIfContains(key);
	}

	public void remove(long key) throws IOException
	{
		boolean ret = removeIfContains(key);
		if (!ret)
		{
			throw new IllegalArgumentException("key not found: " + key);
		}
	}

	@Override
	public boolean removeIfContains(long key) throws IOException
	{
		return accesser.removeIfContains(key);
	}

	public void set(long key, byte[] data) throws IOException
	{
		boolean ret = setIfContains(key, data);
		if (!ret)
		{
			throw new IllegalArgumentException("key not found: " + key);
		}
	}

	@Override
	public boolean setIfContains(long key, byte[] data) throws IOException
	{
		return accesser.setIfContains(key, data);
	}

	public <T> long put(T object, PersistanceDelegate<T> delegate)
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

		return put(new BytePersistanceDelegate<T>(delegate).writeBytes(object));
	}

	public <T> void set(long key, T object, PersistanceDelegate<T> delegate)
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

		set(key, new BytePersistanceDelegate<T>(delegate).writeBytes(object));
	}

	public <T> T get(long key, PersistanceDelegate<T> delegate)
			throws IOException, DataFormatException
	{
		if (null == delegate)
		{
			throw new IllegalArgumentException("delegate should not be null");
		}

		return new BytePersistanceDelegate<T>(delegate).readBytes(get(key));
	}

	public <T> T getIfContains(long key, PersistanceDelegate<T> delegate)
			throws IOException, DataFormatException
	{
		if (null == delegate)
		{
			throw new IllegalArgumentException("delegate should not be null");
		}

		byte[] data = getIfContains(key);
		if (null == data)
		{
			return null;
		}
		else
		{
			return new BytePersistanceDelegate<T>(delegate).readBytes(data);
		}
	}

	public <T> boolean setIfContains(long key,
			T object,
			PersistanceDelegate<T> delegate) throws IOException
	{
		if (null == object)
		{
			throw new IllegalArgumentException("object should not be null");
		}
		if (null == delegate)
		{
			throw new IllegalArgumentException("delegate should not be null");
		}

		return setIfContains(key, new BytePersistanceDelegate<T>(delegate)
				.writeBytes(object));
	}
}
