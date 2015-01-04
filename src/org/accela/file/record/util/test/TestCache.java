package org.accela.file.record.util.test;

import java.util.HashMap;
import java.util.Map;

import org.accela.file.record.util.Cache;

import junit.framework.TestCase;

public class TestCache extends TestCase
{
	private Cache<Integer, String, RuntimeException> cache = null;

	private TesterFlushHandler handler = null;

	private class TesterFlushHandler implements
			Cache.FlushHandler<Integer, String, RuntimeException>
	{
		public Map<Integer, String> map = new HashMap<Integer, String>();

		@Override
		public void flush(Integer key, String value)
		{
			map.put(key, value);
		}

	}

	@Override
	protected void setUp() throws Exception
	{
		constructCache(16);
	}

	private void constructCache(int capactiy)
	{
		cache = new Cache<Integer, String, RuntimeException>(capactiy);
		handler = new TesterFlushHandler();
		cache.setFlushHandler(handler);
	}

	public void testZeroCapacity()
	{
		constructCache(0);

		for (int i = 0; i < 100; i++)
		{
			cache.put(i, "" + i);
		}

		assert (handler.map.size() == 100);
		assert (cache.size() == 0);
	}

	public void testFlushAndDiscard()
	{
		// test flush

		assert (cache.size() == 0);

		cache.put(1, "1");
		assert (cache.get(1).equals("1"));
		cache.put(2, "2");
		assert (cache.get(2).equals("2"));
		cache.put(3, "3");
		assert (cache.get(3).equals("3"));

		assert (cache.size() == 3);
		assert (handler.map.size() == 0);

		cache.flush();

		assert (cache.size() == 3);
		assert (handler.map.size() == 0);

		cache.markDirty(1);
		cache.markDirty(2);
		cache.flush();

		assert (cache.size() == 3);
		assert (handler.map.size() == 2);
		assert (handler.map.containsKey(1));
		assert (handler.map.containsKey(2));
		assert (handler.map.get(1).equals("1"));
		assert (handler.map.get(2).equals("2"));

		handler.map.clear();
		cache.flush();
		assert (cache.size() == 3);
		assert (handler.map.size() == 0);

		assert (cache.contains(3));

		// test discard
		handler.map.clear();

		cache.markDirty(2);
		cache.markDirty(3);

		for (int i = 4; i <= 16; i++)
		{
			cache.put(i, "" + i);
		}
		assert (handler.map.size() == 0);
		assert (cache.size() == 16);

		cache.put(17, "17");
		assert (handler.map.size() == 1);
		assert (handler.map.containsKey(3));
		assert (handler.map.get(3).equals("3"));
		assert (cache.size() == 16);

		assert(cache.contains(1));
		cache.put(18, "18");
		assert (handler.map.size() == 1);
		assert(!cache.contains(1));
		assert (cache.size() == 16);

		cache.put(19, "19");
		assert (handler.map.size() == 2);
		assert (handler.map.containsKey(2));
		assert (handler.map.get(2).equals("2"));
		assert (cache.size() == 16);

		handler.map.clear();
		
		for (int i = 4; i <= 19; i++)
		{
			cache.put(i + 16, "" + (i + 16));
			assert (handler.map.size() == 0);
			assert (cache.size() == 16);
		}

		// test mark dirty and discard and flush
		handler.map.clear();

		cache.markDirty(20);
		cache.markDirty(30);
		cache.markDirty(26);
		cache.markDirty(35);

		cache.put(35, "35");
		assert (handler.map.size() == 0);

		cache.put(36, "36");
		assert (handler.map.size() == 1);
		assert (handler.map.containsKey(20));
		assert (handler.map.get(20).equals("" + 20));
		assert (cache.size() == 16);

		handler.map.clear();

		cache.flush();
		assert (handler.map.size() == 3):handler.map.size();
		assert (handler.map.containsKey(30));
		assert (handler.map.containsKey(26));
		assert (handler.map.containsKey(35));

		// test remove
		handler.map.clear();

		cache.markDirty(36);
		cache.markDirty(22);
		cache.markDirty(21);

		assert (cache.contains(36));
		assert (cache.contains(26));
		assert (cache.contains(21));
		assert (cache.contains(30));
		assert (cache.contains(29));

		cache.remove(36);
		cache.remove(26);
		cache.remove(21);
		cache.remove(30);
		cache.remove(29);

		assert (handler.map.size() == 2);
		assert (handler.map.containsKey(36));
		assert (handler.map.containsKey(21));

		handler.map.clear();
		cache.flush();
		assert (handler.map.size() == 1);
		assert (handler.map.containsKey(22));
		
		assert (!cache.contains(36));
		assert (!cache.contains(26));
		assert (!cache.contains(21));
		assert (!cache.contains(30));
		assert (!cache.contains(29));

	}

}
