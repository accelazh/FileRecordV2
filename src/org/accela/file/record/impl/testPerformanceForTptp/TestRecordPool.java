package org.accela.file.record.impl.testPerformanceForTptp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.accela.file.common.DataFormatException;
import org.accela.file.record.RecordArray;
import org.accela.file.record.RecordArrayFactory;
import org.accela.file.record.RecordPool;
import org.accela.file.record.RecordPoolFactory;
import org.accela.file.record.RecordPoolKeyIterator;
import org.accela.file.record.impl.CachedRecordArray;
import org.accela.file.record.impl.ObjectRecordPool;
import org.accela.file.record.impl.PlainRecordArray;
import org.accela.file.record.impl.PlainRecordPool;
import org.accela.file.record.impl.RandomFileAccesser;
import org.accela.file.record.impl.VarRecordPool;

public abstract class TestRecordPool extends TestRecordFile
{
	protected boolean posSlotSize;

	protected boolean varRecord;

	protected ObjectRecordPool accesser = null;

	public TestRecordPool(boolean posSlotSize, boolean varRecord)
	{
		super(new File("testRecordPool.txt"), new File("sina.txt"));

		this.posSlotSize = posSlotSize;
		this.varRecord = varRecord;
	}

	protected class PlainRecordArrayFactory implements RecordArrayFactory
	{

		@Override
		public RecordArray create(int slotSize) throws IOException
		{
			return new PlainRecordArray(new RandomFileAccesser(testFile), slotSize);
		}

	}

	protected class CachedPlainRecordArrayFactory implements RecordArrayFactory
	{

		@Override
		public RecordArray create(int slotSize) throws IOException
		{
			return new CachedRecordArray(new PlainRecordArrayFactory().create(slotSize), 128);
		}

	}

	protected class CachedCachedPlainRecordArrayFactory implements
			RecordArrayFactory
	{

		@Override
		public RecordArray create(int slotSize) throws IOException
		{
			return new CachedRecordArray(new CachedPlainRecordArrayFactory().create(slotSize), 128);
		}

	}

	protected class PlainRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new PlainRecordPool(new PlainRecordArrayFactory(), slotSize, poolSize);
		}

	}

	protected class CachedPlainRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new PlainRecordPool(new CachedPlainRecordArrayFactory(), slotSize, poolSize);
		}

	}

	protected class CachedCachedPlainRecordPoolFactory implements
			RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new PlainRecordPool(new CachedCachedPlainRecordArrayFactory(), slotSize, poolSize);
		}

	}

	protected class VarRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new PlainRecordPoolFactory(), slotSize, poolSize);
		}

	}

	protected class VarVarRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new VarRecordPoolFactory(), slotSize, poolSize);
		}

	}

	protected class CachedVarRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new CachedPlainRecordPoolFactory(), slotSize, poolSize);
		}

	}

	protected class CachedCachedVarRecordPoolFactory implements
			RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new CachedCachedPlainRecordPoolFactory(), slotSize, poolSize);
		}

	}

	protected class CachedVarVarRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new CachedVarRecordPoolFactory(), slotSize, poolSize);
		}

	}

	protected class CachedCachedVarVarRecordPoolFactory implements
			RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new CachedCachedVarRecordPoolFactory(), slotSize, poolSize);
		}

	}

	@Override
	protected void close() throws IOException
	{
		if (this.accesser != null)
		{
			this.accesser.close();
		}
	}

	@Override
	protected void open(long poolSize) throws IOException
	{
		this.accesser = new ObjectRecordPool(createRecordPool(128, poolSize));
	}

	protected abstract RecordPool createRecordPool(int slotSize, long poolSize)
			throws IOException;

	protected Map<Long, byte[]> fillBytes(long count, int length)
			throws IOException
	{
		if (!varRecord)
		{
			length = Math.min(accesser.getSlotSize(), length);
		}

		long orignSize = accesser.size();
		Map<Long, byte[]> map = new HashMap<Long, byte[]>();
		while (map.size() < count)
		{
			long oldSize = accesser.size();
			byte[] bytes = genBytes(length);
			long key = accesser.put(bytes);
			map.put(key, bytes);
			assert (accesser.size() == oldSize + 1);
			assert (accesser.contains(key));
			assert (Arrays.equals(accesser.get(key), bytes));

			if (rand.nextDouble() < 0.4)
			{
				oldSize = accesser.size();
				key = map.keySet().toArray(new Long[0])[rand.nextInt(map.size())];
				assert (accesser.contains(key));
				byte[] bs1 = map.remove(key);
				byte[] bs2 = accesser.get(key);
				assert (Arrays.equals(bs1, bs2));
				accesser.remove(key);
				assert (!accesser.contains(key));
				assert (accesser.size() == oldSize - 1);
			}
		}

		assert (accesser.size() == orignSize + map.size());
		containsBytes(map);
		return map;
	}

	protected Map<Long, String> fillStr(long count, int length)
			throws IOException
	{
		if (!varRecord)
		{
			length = Math.min(accesser.getSlotSize(), length);
		}

		long orignSize = accesser.size();
		Map<Long, String> map = new HashMap<Long, String>();
		while (map.size() < count)
		{
			long oldSize = accesser.size();
			String str = genStr((length - 2) / 3);
			long key = accesser.put(str, delegate);
			map.put(key, str);
			assert (accesser.size() == oldSize + 1);
			assert (accesser.contains(key));
			try
			{
				assert (accesser.get(key, delegate).equals(str));
			}
			catch (DataFormatException ex)
			{
				ex.printStackTrace();
				assert (false);
			}

			if (rand.nextDouble() < 0.4)
			{
				oldSize = accesser.size();
				key = map.keySet().toArray(new Long[0])[rand.nextInt(map.size())];
				assert (accesser.contains(key));
				String str1 = map.remove(key);
				String str2 = null;
				try
				{
					str2 = accesser.get(key, delegate);
				}
				catch (DataFormatException ex)
				{
					ex.printStackTrace();
					assert (false);
				}
				assert (str1.equals(str2));
				accesser.remove(key);
				assert (!accesser.contains(key));
				assert (accesser.size() == oldSize - 1);
			}
		}

		assert (accesser.size() == orignSize + map.size());
		containsStr(map);
		return map;
	}

	protected Map<Long, byte[]> fillBytesRnd(long count, int length)
			throws IOException
	{
		if (!varRecord)
		{
			length = Math.min(accesser.getSlotSize(), length);
		}

		long orignSize = accesser.size();
		Map<Long, byte[]> map = new HashMap<Long, byte[]>();
		while (map.size() < count)
		{
			long oldSize = accesser.size();
			byte[] bytes = genBytesRnd(length);
			long key = accesser.put(bytes);
			map.put(key, bytes);
			assert (accesser.size() == oldSize + 1);
			assert (accesser.contains(key));
			assert (Arrays.equals(accesser.get(key), bytes));

			if (rand.nextDouble() < 0.4)
			{
				oldSize = accesser.size();
				key = map.keySet().toArray(new Long[0])[rand.nextInt(map.size())];
				assert (accesser.contains(key));
				byte[] bs1 = map.remove(key);
				byte[] bs2 = accesser.get(key);
				assert (Arrays.equals(bs1, bs2));
				accesser.remove(key);
				assert (!accesser.contains(key));
				assert (accesser.size() == oldSize - 1);
			}
		}

		assert (accesser.size() == orignSize + map.size());
		containsBytes(map);
		return map;
	}

	protected Map<Long, String> fillStrRnd(long count, int length)
			throws IOException
	{
		if (!varRecord)
		{
			length = Math.min(accesser.getSlotSize(), length);
		}

		long orignSize = accesser.size();
		Map<Long, String> map = new HashMap<Long, String>();
		while (map.size() < count)
		{
			long oldSize = accesser.size();
			String str = genStrRnd((length - 2) / 3);
			long key = accesser.put(str, delegate);
			map.put(key, str);
			assert (accesser.size() == oldSize + 1);
			assert (accesser.contains(key));
			try
			{
				assert (accesser.get(key, delegate).equals(str));
			}
			catch (DataFormatException ex)
			{
				ex.printStackTrace();
				assert (false);
			}

			if (rand.nextDouble() < 0.4)
			{
				oldSize = accesser.size();
				key = map.keySet().toArray(new Long[0])[rand.nextInt(map.size())];
				assert (accesser.contains(key));
				String str1 = map.remove(key);
				String str2 = null;
				try
				{
					str2 = accesser.get(key, delegate);
				}
				catch (DataFormatException ex)
				{
					ex.printStackTrace();
					assert (false);
				}
				assert (str1.equals(str2));
				accesser.remove(key);
				assert (!accesser.contains(key));
				assert (accesser.size() == oldSize - 1);
			}
		}

		assert (accesser.size() == orignSize + map.size());
		containsStr(map);
		return map;
	}

	protected void containsBytes(Map<Long, byte[]> map) throws IOException
	{
		long orignSize = accesser.size();
		for (long key : map.keySet())
		{
			assert (accesser.contains(key));
			assert (Arrays.equals(accesser.get(key), map.get(key)));
			assert (accesser.size() >= map.size());
		}
		assert (accesser.size() == orignSize);
	}

	protected void containsStr(Map<Long, String> map) throws IOException
	{
		long orignSize = accesser.size();
		for (long key : map.keySet())
		{
			assert (accesser.contains(key));
			try
			{
				assert (accesser.get(key, delegate).equals(map.get(key)));
			}
			catch (DataFormatException ex)
			{
				ex.printStackTrace();
				assert (false);
			}
			assert (accesser.size() >= map.size());
		}
		assert (accesser.size() == orignSize);
	}

	protected void contained(Map<Long, byte[]> bmap, Map<Long, String> smap)
			throws IOException
	{
		for (long idx = 0; idx < accesser.poolSize(); idx++)
		{
			if (!accesser.contains(idx))
			{
				continue;
			}

			final long key = idx;
			if (bmap.containsKey(key) && Arrays.equals(bmap.get(key), accesser.get(key)))
			{
				continue;
			}
			try
			{
				if (smap.containsKey(key) && accesser.get(key, delegate).equals(smap.get(key)))
				{
					continue;
				}
			}
			catch (DataFormatException ex)
			{
				assert (false);
			}

			assert (false);
		}

		iterator();

	}

	protected void removeBytes(Map<Long, byte[]> map) throws IOException
	{
		long orignSize = accesser.size();
		containsBytes(map);
		for (long key : map.keySet())
		{
			long oldSize = accesser.size();
			assert (Arrays.equals(accesser.get(key), map.get(key)));
			accesser.remove(key);
			assert (!accesser.contains(key));
			assert (accesser.size() == oldSize - 1);
		}
		assert (accesser.size() == orignSize - map.size());

		for (long key : map.keySet())
		{
			assert (!accesser.contains(key));
		}
	}

	protected void removeStr(Map<Long, String> map) throws IOException
	{
		long orignSize = accesser.size();
		containsStr(map);
		for (long key : map.keySet())
		{
			long oldSize = accesser.size();
			try
			{
				assert (accesser.get(key, delegate).equals(map.get(key)));
			}
			catch (DataFormatException ex)
			{
				ex.printStackTrace();
				assert (false);
			}
			accesser.remove(key);
			assert (!accesser.contains(key));
			assert (accesser.size() == oldSize - 1);
		}
		assert (accesser.size() == orignSize - map.size());

		for (long key : map.keySet())
		{
			assert (!accesser.contains(key));
		}
	}

	protected Map<Long, String> setBytesRnd(Map<Long, byte[]> map, int length)
			throws IOException
	{
		if (!varRecord)
		{
			length = Math.min(accesser.getSlotSize(), length);
		}

		Map<Long, String> newMap = new HashMap<Long, String>();

		long orignSize = accesser.size();
		containsBytes(map);
		for (long key : map.keySet())
		{
			long oldSize = accesser.size();
			assert (Arrays.equals(accesser.get(key), map.get(key)));

			String newStr = genStrRnd((length - 2) / 3);
			accesser.set(key, newStr, delegate);
			newMap.put(key, newStr);

			assert (accesser.contains(key));
			try
			{
				assert (accesser.get(key, delegate).equals(newStr));
			}
			catch (DataFormatException ex)
			{
				ex.printStackTrace();
				assert (false);
			}

			assert (accesser.size() == oldSize);
		}
		assert (accesser.size() == orignSize);

		containsStr(newMap);

		return newMap;
	}

	protected Map<Long, byte[]> setStrRnd(Map<Long, String> map, int length)
			throws IOException
	{
		if (!varRecord)
		{
			length = Math.min(accesser.getSlotSize(), length);
		}

		Map<Long, byte[]> newMap = new HashMap<Long, byte[]>();

		long orignSize = accesser.size();
		containsStr(map);
		for (long key : map.keySet())
		{
			long oldSize = accesser.size();
			try
			{
				assert (accesser.get(key, delegate).equals(map.get(key)));
			}
			catch (DataFormatException ex)
			{
				ex.printStackTrace();
				assert (false);
			}

			byte[] newBytes = genBytesRnd(length);
			accesser.set(key, newBytes);
			newMap.put(key, newBytes);

			assert (accesser.contains(key));
			assert (Arrays.equals(accesser.get(key), newBytes));
			assert (accesser.size() == oldSize);
		}
		assert (accesser.size() == orignSize);

		containsBytes(newMap);

		return newMap;
	}

	protected void testClear(Map<Long, byte[]> byteMap, Map<Long, String> strMap)
			throws IOException
	{
		assert (accesser.size() == 0);
		assert (accesser.poolSize() == 1);

		for (Long key : byteMap.keySet())
		{
			assert (!accesser.contains(key));
		}
		for (Long key : strMap.keySet())
		{
			assert (!accesser.contains(key));
		}
	}

	protected void clear(Map<Long, byte[]> byteMap, Map<Long, String> strMap)
			throws IOException
	{
		accesser.clear();

		testClear(byteMap, strMap);
	}

	protected void iterator() throws IOException
	{
		RecordPoolKeyIterator itr = accesser.iterator();
		Set<Long> itrKeys = new HashSet<Long>();
		while (itr.hasNext())
		{
			long key = itr.next();
			assert (!itrKeys.contains(key));
			assert (accesser.contains(key));
			itrKeys.add(key);
		}
		assert (itrKeys.size() == accesser.size());

		for (long idx = 0; idx < accesser.poolSize(); idx++)
		{
			if (!accesser.contains(idx))
			{
				continue;
			}

			assert (itrKeys.contains(idx));
		}
	}

	protected void putStr(String str) throws IOException
	{
		long key = accesser.put(str, delegate);
		try
		{
			assert (accesser.contains(key));
			String value = accesser.get(key, delegate);
			assert (value.equals(str));
		}
		catch (DataFormatException ex)
		{
			ex.printStackTrace();
		}
	}

	protected Map<Long, byte[]> compondFillBytes() throws IOException
	{
		Map<Long, byte[]> byteMap = new HashMap<Long, byte[]>();
		byteMap.putAll(fillBytes(10, 0));
		byteMap.putAll(fillBytes(10, 1));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize() / 2));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize() - 1));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize()));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize() + 1));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize() + accesser.getSlotSize() / 2));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize() + accesser.getSlotSize() - 1));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize() + accesser.getSlotSize()));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize() + accesser.getSlotSize() + 1));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize() * 10));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize() * 20 + accesser.getSlotSize() / 2));
		byteMap.putAll(fillBytesRnd(40, accesser.getSlotSize() * 40));

		return byteMap;
	}

	protected Map<Long, String> compondFillStr() throws IOException
	{
		Map<Long, String> strMap = new HashMap<Long, String>();
		strMap.putAll(fillStr(10, 0));
		strMap.putAll(fillStr(10, 1));
		strMap.putAll(fillStr(10, accesser.getSlotSize() / 2));
		strMap.putAll(fillStr(10, accesser.getSlotSize() - 1));
		strMap.putAll(fillStr(10, accesser.getSlotSize()));
		strMap.putAll(fillStr(10, accesser.getSlotSize() + 1));
		strMap.putAll(fillStr(10, accesser.getSlotSize() + accesser.getSlotSize() / 2));
		strMap.putAll(fillStr(10, accesser.getSlotSize() + accesser.getSlotSize() - 1));
		strMap.putAll(fillStr(10, accesser.getSlotSize() + accesser.getSlotSize()));
		strMap.putAll(fillStr(10, accesser.getSlotSize() + accesser.getSlotSize() + 1));
		strMap.putAll(fillStr(10, accesser.getSlotSize() * 10));
		strMap.putAll(fillStr(10, accesser.getSlotSize() * 20 + accesser.getSlotSize() / 2));
		strMap.putAll(fillStrRnd(40, accesser.getSlotSize() * 40));

		return strMap;
	}

	public void testPerformance() throws IOException
	{
		Map<Long, byte[]> bmap=fillBytes(100, accesser.getSlotSize());
		setBytesRnd(bmap, accesser.getSlotSize());
	}
	
}
