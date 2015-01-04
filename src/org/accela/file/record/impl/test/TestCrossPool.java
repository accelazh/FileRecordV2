package org.accela.file.record.impl.test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.accela.file.record.RecordArray;
import org.accela.file.record.RecordArrayFactory;
import org.accela.file.record.RecordPool;
import org.accela.file.record.RecordPoolFactory;
import org.accela.file.record.impl.CachedRecordArray;
import org.accela.file.record.impl.ObjectRecordPool;
import org.accela.file.record.impl.PlainRecordPool;
import org.accela.file.record.impl.RandomFileAccesser;
import org.accela.file.record.impl.VarRecordPool;

import org.accela.file.record.impl.PlainRecordArray;

public class TestCrossPool extends TestRecordFile
{
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

	private ObjectRecordPool poolA = null;

	private ObjectRecordPool poolB = null;

	public TestCrossPool()
	{
		super(new File("testCrossPool.txt"), new File("sina.txt"));
	}

	@Override
	protected void close() throws IOException
	{
		if (poolA != null)
		{
			poolA.close();
		}
		if (poolB != null)
		{
			poolB.close();
		}
	}

	@Override
	protected void open(long poolSize) throws IOException
	{
		poolA = new ObjectRecordPool(new VarRecordPoolFactory().create(128,
				poolSize));
		poolB = null;
	}

	private Map<Long, byte[]> fillBytesRnd(ObjectRecordPool pool,
			long count,
			int length) throws IOException
	{
		long orignSize = pool.size();
		Map<Long, byte[]> map = new HashMap<Long, byte[]>();
		while (map.size() < count)
		{
			long oldSize = pool.size();
			byte[] bytes = genBytesRnd(length);
			long key = pool.put(bytes);
			map.put(key, bytes);
			assert (pool.size() == oldSize + 1);
			assert (pool.contains(key));
			assert (Arrays.equals(pool.get(key), bytes));

			if (rand.nextDouble() < 0.4)
			{
				oldSize = pool.size();
				key = map.keySet().toArray(new Long[0])[rand
						.nextInt(map.size())];
				assert (pool.contains(key));
				byte[] bs1 = map.remove(key);
				byte[] bs2 = pool.get(key);
				assert (Arrays.equals(bs1, bs2));
				pool.remove(key);
				assert (!pool.contains(key));
				assert (pool.size() == oldSize - 1);
			}
		}

		assert (pool.size() == orignSize + map.size());
		containsBytes(pool, map);
		return map;
	}

	private void containsBytes(ObjectRecordPool pool, Map<Long, byte[]> map)
			throws IOException
	{
		long orignSize = pool.size();
		for (long key : map.keySet())
		{
			assert (pool.contains(key));
			assert (Arrays.equals(pool.get(key), map.get(key)));
			assert (pool.size() >= map.size());
		}
		assert (pool.size() == orignSize);
	}

	private Map<Long, byte[]> setBytesRnd(ObjectRecordPool pool,
			Map<Long, byte[]> map,
			int length) throws IOException
	{
		Map<Long, byte[]> newMap = new HashMap<Long, byte[]>();

		long orignSize = pool.size();
		containsBytes(pool, map);
		for (long key : map.keySet())
		{
			long oldSize = pool.size();
			assert (Arrays.equals(pool.get(key), map.get(key)));

			byte[] newStr = genBytesRnd(length);
			pool.set(key, newStr);
			newMap.put(key, newStr);

			assert (pool.contains(key));
			assert (Arrays.equals(pool.get(key), newStr));
			assert (pool.size() == oldSize);
		}
		assert (pool.size() == orignSize);

		containsBytes(pool, newMap);

		return newMap;
	}

	public void testCrossPool() throws IOException
	{
		Map<Long, byte[]> bmap = fillBytesRnd(poolA,
				100,
				poolA.getSlotSize() * 2);
		containsBytes(poolA, bmap);

		poolB = new ObjectRecordPool(new CachedVarRecordPoolFactory()
				.create(128, poolA.poolSize()));
		containsBytes(poolB, bmap);

		bmap = setBytesRnd(poolB, bmap, poolA.getSlotSize() * 4);

		poolA.close();
		poolB.close();

		poolA = new ObjectRecordPool(new VarRecordPoolFactory().create(128,
				poolB.poolSize()));
		containsBytes(poolA, bmap);
	}

}
