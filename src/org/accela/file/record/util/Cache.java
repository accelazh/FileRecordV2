package org.accela.file.record.util;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.accela.common.Assertion;

public class Cache<K, V, Ex extends Exception>
{
	private Map<K, V> map = null;
	private Set<K> dirties = null;

	private FlushHandler<K, V, Ex> handler = null;

	private int capacity = 0;

	public Cache(int capacity)
	{
		if (capacity < 0)
		{
			throw new IllegalArgumentException(
					"capacity should not be negative");
		}

		this.capacity = capacity;
		this.map = new LinkedHashMap<K, V>(16, 0.75f, true);
		this.dirties = new HashSet<K>();
	}

	public int getCapacity()
	{
		return this.capacity;
	}

	public void setFlushHandler(FlushHandler<K, V, Ex> handler)
	{
		this.handler = handler;
	}

	public FlushHandler<K, V, Ex> getFlushHandler()
	{
		return this.handler;
	}

	public V put(K key, V value) throws Ex
	{
		if (null == key)
		{
			throw new IllegalArgumentException("key should not be null");
		}
		if (null == value)
		{
			throw new IllegalArgumentException("value should not be null");
		}

		V ret = map.put(key, value);
		if (ret != null)
		{
			assert (map.size() <= capacity) : Assertion.declare();
		}
		else if (map.size() > capacity)
		{
			K discardKey = findDiscard();
			assert (map.containsKey(discardKey)) : Assertion.declare();

			discard(discardKey, discardKey.equals(key));
			assert (map.size() <= capacity);
		}
		else
		{
			// do nothing
		}

		return ret;
	}

	private K findDiscard()
	{
		K key = map.keySet().iterator().next();
		assert (key != null) : Assertion.declare();
		return key;
	}

	private V discard(K key, boolean mustFlush) throws Ex
	{
		assert (key != null) : Assertion.declare();
		assert (map.containsKey(key)) : Assertion.declare();

		V value = map.remove(key);
		assert (value != null) : Assertion.declare();

		boolean dirty = dirties.remove(key);

		boolean flush = mustFlush||dirty;

		if (flush && handler != null)
		{
			handler.flush(key, value);
		}

		return value;
	}

	public boolean contains(K key)
	{
		if (null == key)
		{
			throw new IllegalArgumentException("key should not be null");
		}

		return map.containsKey(key);
	}

	public V remove(K key) throws Ex
	{
		if (null == key)
		{
			throw new IllegalArgumentException("key should not be null");
		}

		if (!contains(key))
		{
			return null;
		}

		return discard(key, false);
	}

	public V get(K key)
	{
		if (null == key)
		{
			throw new IllegalArgumentException("key should not be null");
		}

		if (!contains(key))
		{
			return null;
		}

		return map.get(key);
	}

	public void markDirty(K key)
	{
		if (null == key)
		{
			throw new IllegalArgumentException("key should not be null");
		}
		if (!contains(key))
		{
			throw new IllegalArgumentException("key not found: " + key);
		}

		dirties.add(key);
	}

	public void flush() throws Ex
	{
		for (K key : dirties)
		{
			assert (key != null);
			assert (map.containsKey(key)) : Assertion.declare();

			if (handler != null)
			{
				handler.flush(key, map.get(key));
			}
		}

		dirties.clear();
	}

	public boolean hasDirty()
	{
		return !dirties.isEmpty();
	}
	
	public int size()
	{
		return this.map.size();
	}

	public interface FlushHandler<K, V, Ex extends Exception>
	{
		public void flush(K key, V value) throws Ex;
	}
}
