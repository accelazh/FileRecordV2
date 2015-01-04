package org.accela.file.record.impl.test;

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
	private boolean posSlotSize;

	private boolean varRecord;

	private ObjectRecordPool accesser = null;

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
			return new PlainRecordArray(new RandomFileAccesser(testFile),
					slotSize);
		}

	}

	protected class CachedPlainRecordArrayFactory implements RecordArrayFactory
	{

		@Override
		public RecordArray create(int slotSize) throws IOException
		{
			return new CachedRecordArray(new PlainRecordArrayFactory()
					.create(slotSize), 128);
		}

	}

	protected class CachedCachedPlainRecordArrayFactory implements
			RecordArrayFactory
	{

		@Override
		public RecordArray create(int slotSize) throws IOException
		{
			return new CachedRecordArray(new CachedPlainRecordArrayFactory()
					.create(slotSize), 128);
		}

	}

	protected class PlainRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new PlainRecordPool(new PlainRecordArrayFactory(), slotSize,
					poolSize);
		}

	}

	protected class CachedPlainRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new PlainRecordPool(new CachedPlainRecordArrayFactory(),
					slotSize, poolSize);
		}

	}

	protected class CachedCachedPlainRecordPoolFactory implements
			RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new PlainRecordPool(
					new CachedCachedPlainRecordArrayFactory(), slotSize,
					poolSize);
		}

	}

	protected class VarRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new PlainRecordPoolFactory(), slotSize,
					poolSize);
		}

	}

	protected class VarVarRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new VarRecordPoolFactory(), slotSize,
					poolSize);
		}

	}

	protected class CachedVarRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new CachedPlainRecordPoolFactory(),
					slotSize, poolSize);
		}

	}

	protected class CachedCachedVarRecordPoolFactory implements
			RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new CachedCachedPlainRecordPoolFactory(),
					slotSize, poolSize);
		}

	}

	protected class CachedVarVarRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new CachedVarRecordPoolFactory(),
					slotSize, poolSize);
		}

	}

	protected class CachedCachedVarVarRecordPoolFactory implements
			RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new CachedCachedVarRecordPoolFactory(),
					slotSize, poolSize);
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

	private Map<Long, byte[]> fillBytes(long count, int length)
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
				key = map.keySet().toArray(new Long[0])[rand
						.nextInt(map.size())];
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

	private Map<Long, String> fillStr(long count, int length)
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
				key = map.keySet().toArray(new Long[0])[rand
						.nextInt(map.size())];
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

	private Map<Long, byte[]> fillBytesRnd(long count, int length)
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
				key = map.keySet().toArray(new Long[0])[rand
						.nextInt(map.size())];
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

	private Map<Long, String> fillStrRnd(long count, int length)
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
				key = map.keySet().toArray(new Long[0])[rand
						.nextInt(map.size())];
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

	private void containsBytes(Map<Long, byte[]> map) throws IOException
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

	private void containsStr(Map<Long, String> map) throws IOException
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

	private void contained(Map<Long, byte[]> bmap, Map<Long, String> smap)
			throws IOException
	{
		for (long idx = 0; idx < accesser.poolSize(); idx++)
		{
			if (!accesser.contains(idx))
			{
				continue;
			}

			final long key = idx;
			if (bmap.containsKey(key)
					&& Arrays.equals(bmap.get(key), accesser.get(key)))
			{
				continue;
			}
			try
			{
				if (smap.containsKey(key)
						&& accesser.get(key, delegate).equals(smap.get(key)))
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

	private void getIfContains(Map<Long, byte[]> bmap, Map<Long, String> smap)
			throws IOException
	{
		try
		{
			long maxKey = accesser.poolSize() + 1;
			for (long i = 0; i < maxKey; i++)
			{
				if (bmap.containsKey(i))
				{
					assert (Arrays.equals(accesser.get(i), accesser
							.getIfContains(i)));
				}
				else if (smap.containsKey(i))
				{
					assert (smap.get(i).equals(accesser.getIfContains(i,
							delegate)));
				}
				else
				{
					assert (accesser.getIfContains(i) == null);
					assert (accesser.getIfContains(i, delegate) == null);
				}
			}
		}
		catch (DataFormatException ex)
		{
			ex.printStackTrace();
			assert (false);
		}
	}

	private void removeBytes(Map<Long, byte[]> map) throws IOException
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

	private void removeStr(Map<Long, String> map) throws IOException
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

	private Map<Long, String> setBytesRnd(Map<Long, byte[]> map, int length)
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

	private Map<Long, byte[]> setStrRnd(Map<Long, String> map, int length)
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

	private void testClear(Map<Long, byte[]> byteMap, Map<Long, String> strMap)
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

	private void clear(Map<Long, byte[]> byteMap, Map<Long, String> strMap)
			throws IOException
	{
		accesser.clear();

		testClear(byteMap, strMap);
	}

	private void iterator() throws IOException
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

	private void putStr(String str) throws IOException
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

	public void testExceedSize() throws IOException
	{
		File orignTestFile = this.testFile;
		this.testFile = new File("testRecordPool2.txt"); // 避免文件无法关闭

		long key = accesser.put(new byte[0]);

		if (!varRecord)
		{
			for (int i = 0; i < 100; i++)
			{
				try
				{
					accesser.set(key, new byte[accesser.getSlotSize()
							+ 1
							+ rand.nextInt(100)]);
					assert (false);
				}
				catch (Exception ex)
				{
					assert (ex instanceof IllegalArgumentException);
				}
				try
				{
					accesser.put(new byte[accesser.getSlotSize()
							+ 1
							+ rand.nextInt(100)]);
					assert (false);
				}
				catch (Exception ex)
				{
					assert (ex instanceof IllegalArgumentException);
				}
			}
		}
		else
		{
			for (int i = 0; i < 100; i++)
			{
				try
				{
					accesser.set(key, new byte[accesser.getSlotSize()
							+ 1
							+ rand.nextInt(100)
							+ i
							/ 2
							* accesser.getSlotSize()
							/ 4]);
					accesser.put(new byte[accesser.getSlotSize()
							+ 1
							+ rand.nextInt(100)
							+ i
							/ 2
							* accesser.getSlotSize()
							/ 4]);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					assert (false);
				}
			}
		}

		if (posSlotSize)
		{
			try
			{
				createRecordPool(0, 0);
				assert (false);
			}
			catch (Exception ex)
			{
				assert (ex instanceof IllegalArgumentException);
			}
		}
		else
		{
			try
			{
				createRecordPool(0, 0);
			}
			catch (Exception ex)
			{
				assert (false);
			}
		}

		this.testFile = orignTestFile;
	}

	public void testReadEmpty()
	{
		for (int i = 0; i < 1000; i++)
		{
			try
			{
				accesser.get(i);
				assert (false);
			}
			catch (Exception ex)
			{
				assert (ex instanceof IllegalArgumentException);
			}
		}
		for (int i = 0; i < 1000; i++)
		{
			try
			{
				assert (accesser.getIfContains(i) == null);
			}
			catch (Exception ex)
			{
				assert (false);
			}
		}
	}

	public void testSetEmpty()
	{
		for (int i = 0; i < 1000; i++)
		{
			try
			{
				accesser.set(i, new byte[0]);
				assert (false);
			}
			catch (Exception ex)
			{
				assert (ex instanceof IllegalArgumentException);
			}
		}
		for (int i = 0; i < 1000; i++)
		{
			try
			{
				assert (!accesser.setIfContains(i, new byte[0]));
			}
			catch (Exception ex)
			{
				assert (false);
			}
		}
	}

	public void testRemoveEmpty()
	{
		for (int i = 0; i < 1000; i++)
		{
			try
			{
				accesser.remove(i);
				assert (false);
			}
			catch (Exception ex)
			{
				assert (ex instanceof IllegalArgumentException);
			}
		}
		for (int i = 0; i < 1000; i++)
		{
			try
			{
				assert (!accesser.removeIfContains(i));
			}
			catch (Exception ex)
			{
				assert (false);
			}
		}
	}

	private Map<Long, byte[]> compondFillBytes() throws IOException
	{
		Map<Long, byte[]> byteMap = new HashMap<Long, byte[]>();
		byteMap.putAll(fillBytes(10, 0));
		byteMap.putAll(fillBytes(10, 1));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize() / 2));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize() - 1));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize()));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize() + 1));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize()
				+ accesser.getSlotSize()
				/ 2));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize()
				+ accesser.getSlotSize()
				- 1));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize()
				+ accesser.getSlotSize()));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize()
				+ accesser.getSlotSize()
				+ 1));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize() * 10));
		byteMap.putAll(fillBytes(10, accesser.getSlotSize()
				* 20
				+ accesser.getSlotSize()
				/ 2));
		byteMap.putAll(fillBytesRnd(40, accesser.getSlotSize() * 40));

		return byteMap;
	}

	private Map<Long, String> compondFillStr() throws IOException
	{
		Map<Long, String> strMap = new HashMap<Long, String>();
		strMap.putAll(fillStr(10, 0));
		strMap.putAll(fillStr(10, 1));
		strMap.putAll(fillStr(10, accesser.getSlotSize() / 2));
		strMap.putAll(fillStr(10, accesser.getSlotSize() - 1));
		strMap.putAll(fillStr(10, accesser.getSlotSize()));
		strMap.putAll(fillStr(10, accesser.getSlotSize() + 1));
		strMap.putAll(fillStr(10, accesser.getSlotSize()
				+ accesser.getSlotSize()
				/ 2));
		strMap.putAll(fillStr(10, accesser.getSlotSize()
				+ accesser.getSlotSize()
				- 1));
		strMap.putAll(fillStr(10, accesser.getSlotSize()
				+ accesser.getSlotSize()));
		strMap.putAll(fillStr(10, accesser.getSlotSize()
				+ accesser.getSlotSize()
				+ 1));
		strMap.putAll(fillStr(10, accesser.getSlotSize() * 10));
		strMap.putAll(fillStr(10, accesser.getSlotSize()
				* 20
				+ accesser.getSlotSize()
				/ 2));
		strMap.putAll(fillStrRnd(40, accesser.getSlotSize() * 40));

		return strMap;
	}

	public void testCommonMethods() throws IOException
	{
		Map<Long, byte[]> byteMap = compondFillBytes();
		Map<Long, String> strMap = compondFillStr();

		containsBytes(byteMap);
		containsStr(strMap);

		reopen(accesser.poolSize());

		containsBytes(byteMap);
		containsStr(strMap);

		Map<Long, String> newStrMap = setBytesRnd(byteMap, 40 * accesser
				.getSlotSize());
		Map<Long, byte[]> newByteMap = setStrRnd(strMap, 40 * accesser
				.getSlotSize());
		byteMap = newByteMap;
		strMap = newStrMap;

		iterator();

		reopen(accesser.poolSize());

		iterator();

		removeBytes(byteMap);
		removeStr(strMap);

		byteMap = compondFillBytes();
		strMap = compondFillStr();

		clear(byteMap, strMap);

		reopen(accesser.poolSize());

		testClear(byteMap, strMap);

		byteMap = compondFillBytes();
		strMap = compondFillStr();

		containsBytes(byteMap);
		containsStr(strMap);

		reopen(accesser.poolSize());

		containsBytes(byteMap);
		containsStr(strMap);

		reopen(0);

		testClear(byteMap, strMap);
	}

	public void testPutTinyAndLarge() throws IOException
	{
		String str = "hello world: 1234";
		putStr(str);
		if (varRecord)
		{
			putStr(readText(dataFile));
		}
	}

	public void testContained() throws IOException
	{
		Map<Long, byte[]> byteMap = compondFillBytes();
		Map<Long, String> strMap = compondFillStr();

		contained(byteMap, strMap);
	}

	public void testGetIfContains() throws IOException
	{
		Map<Long, byte[]> byteMap = compondFillBytes();
		Map<Long, String> strMap = compondFillStr();

		getIfContains(byteMap, strMap);

		int mapSize = byteMap.size();
		for (int i = 0; i < mapSize / 2; i++)
		{
			long key = byteMap.keySet().toArray(new Long[0])[rand
					.nextInt(byteMap.size())];
			accesser.remove(key);
			byteMap.remove(key);
		}

		mapSize = strMap.size();
		for (int i = 0; i < mapSize / 2; i++)
		{
			long key = strMap.keySet().toArray(new Long[0])[rand
					.nextInt(byteMap.size())];
			accesser.remove(key);
			strMap.remove(key);
		}

		containsBytes(byteMap);
		containsStr(strMap);

		removeBytes(byteMap);
		removeStr(strMap);

		getIfContains(new HashMap<Long, byte[]>(), new HashMap<Long, String>());
	}

	public void testShrinkPoolSizeWhenInit() throws IOException
	{
		long idx = 0;
		assert (accesser.poolSize() == 1);
		for (int i = 1; i <= 10000; i++)
		{
			idx = accesser.put("hello world: " + i, delegate);
			assert (idx == i);
		}
		assert (accesser.poolSize() == 10001);
		for (int i = 5001; i <= 10000; i++)
		{
			accesser.remove(i);
		}

		assert (accesser.poolSize() == 10001);
		assert (accesser.size() == 5000);

		for (int i = 1; i <= 5000; i++)
		{
			try
			{
				assert (accesser.get(i, delegate).equals("hello world: " + i));
			}
			catch (DataFormatException ex)
			{
				assert (false);
			}
		}

		reopen(accesser.poolSize());
		assert (accesser.size() == 5000);
		assert (accesser.poolSize() == 5001);

		for (int i = 1; i <= 5000; i++)
		{
			try
			{
				assert (accesser.get(i, delegate).equals("hello world: " + i));
			}
			catch (DataFormatException ex)
			{
				assert (false);
			}
		}

		for (int i = 1; i <= 5000; i++)
		{
			accesser.remove(i);
		}
		assert (accesser.size() == 0);
		assert (accesser.poolSize() == 5001);

		reopen(accesser.poolSize());
		assert (accesser.size() == 0);
		assert (accesser.poolSize() == 1);

	}

	public void testSetWhenBlockSwap() throws IOException
	{
		if (!this.varRecord)
		{
			return;
		}

		Map<Long, byte[]> bmap = new HashMap<Long, byte[]>();
		Map<Long, String> smap = new HashMap<Long, String>();

		RecordPool innerPool = ((VarRecordPool) accesser.getAccesser())
				.getAccesser();

		byte[] bs = genBytes(accesser.getSlotSize());
		long key = accesser.put(bs);
		bmap.put(key, bs);
		containsBytes(bmap);
		containsStr(smap);
		contained(bmap, smap);
		assert (innerPool.size() == 1);

		bs = genBytes(accesser.getSlotSize() * 10);
		accesser.set(key, bs);
		bmap.remove(key);
		bmap.put(key, bs);
		containsBytes(bmap);
		containsStr(smap);
		contained(bmap, smap);
		assert (innerPool.size() == 10);

		bs = genBytes(accesser.getSlotSize() * 2);
		accesser.set(key, bs);
		bmap.remove(key);
		bmap.put(key, bs);
		containsBytes(bmap);
		containsStr(smap);
		contained(bmap, smap);
		assert (innerPool.size() == 2);

		bs = genBytes(accesser.getSlotSize() / 2);
		accesser.set(key, bs);
		bmap.remove(key);
		bmap.put(key, bs);
		containsBytes(bmap);
		containsStr(smap);
		contained(bmap, smap);
		assert (innerPool.size() == 1);
	}

	public void testReadRandom()
	{
		for (int i = 0; i < 1000; i++)
		{
			try
			{
				accesser.get(i);
			}
			catch (Exception ex)
			{
				assert (ex instanceof IllegalArgumentException);
			}
		}
	}

	public void testSetRandom()
	{
		for (int i = 0; i < 1000; i++)
		{
			try
			{
				accesser.set(i, new byte[0]);
			}
			catch (Exception ex)
			{
				assert (ex instanceof IllegalArgumentException);
			}
		}
	}

	public void testFaultRandom() throws IOException
	{
		this.fillFileRandom(accesser.getSlotSize() * 10000);
		testExceedSize();
		testReadRandom();
		testSetRandom();
		testCommonMethods();
		testPutTinyAndLarge();
	}

	public void testPerformance() throws IOException
	{
		Map<Long, byte[]> bmap=fillBytes(100, accesser.getSlotSize());
		setBytesRnd(bmap, accesser.getSlotSize());
	}
	
}
