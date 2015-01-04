package org.accela.file.record.impl;

import java.io.IOException;

import org.accela.common.Assertion;
import org.accela.file.record.RecordArray;
import org.accela.file.record.util.Cache;

public class CachedRecordArray implements RecordArray
{
	private RecordArray accesser = null;

	private Cache<Long, byte[], IOException> cache = null;

	public CachedRecordArray(RecordArray accesser, int capacity)
	{
		if (null == accesser)
		{
			throw new IllegalArgumentException("accesser should not be null");
		}

		this.accesser = accesser;
		this.cache = new Cache<Long, byte[], IOException>(capacity);
		this.cache.setFlushHandler(new RecordArrayFlushHandler());
	}

	public RecordArray getAccesser()
	{
		return accesser;
	}
	
	public int getCapacity()
	{
		return this.cache.getCapacity();
	}

	@Override
	public int getSlotSize()
	{
		return this.accesser.getSlotSize();
	}

	@Override
	public synchronized byte[] get(long idx) throws IOException
	{
		byte[] data = cache.get(idx);
		if (data != null)
		{
			return data;
		}

		data = accesser.get(idx);
		cache.put(idx, data);

		return data;
	}

	@Override
	public synchronized void set(long idx, byte[] data) throws IOException
	{
		if (cache.contains(idx))
		{
			cache.put(idx, data);
			cache.markDirty(idx);
		}
		else
		{
			accesser.set(idx, data);
			cache.put(idx, data);
		}
	}

	@Override
	public synchronized void close() throws IOException
	{
		this.flush();
		accesser.close();
	}

	@Override
	public synchronized void flush() throws IOException
	{
		cache.flush();
		accesser.flush();
	}

	private class RecordArrayFlushHandler implements
			Cache.FlushHandler<Long, byte[], IOException>
	{
		@Override
		public void flush(Long key, byte[] value) throws IOException
		{
			assert (key != null) : Assertion.declare();
			assert (value != null) : Assertion.declare();

			accesser.set(key, value);
		}
	}

}
