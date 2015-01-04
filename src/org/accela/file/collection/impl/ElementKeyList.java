package org.accela.file.collection.impl;

import java.io.IOException;

import org.accela.common.Assertion;
import org.accela.file.collection.ElementList;
import org.accela.file.collection.KeyList;
import org.accela.file.collection.ListElementIterator;
import org.accela.file.collection.ListIteratorMove;
import org.accela.file.collection.ListKeyIterator;
import org.accela.file.common.DataFormatException;
import org.accela.file.common.PersistanceDelegate;
import org.accela.file.common.StructureCorruptedException;
import org.accela.file.record.RecordPool;
import org.accela.file.record.impl.ObjectRecordPool;

public class ElementKeyList<T> implements ElementList<T>
{
	private KeyList list = null;

	private PersistanceDelegate<T> delegate = null;

	private ObjectRecordPool pool = null;

	public ElementKeyList(KeyList list,
			RecordPool pool,
			PersistanceDelegate<T> delegate)
	{
		if (null == list)
		{
			throw new IllegalArgumentException("list should not be null");
		}
		if (null == pool)
		{
			throw new IllegalArgumentException("pool should not be null");
		}
		if (null == delegate)
		{
			throw new IllegalArgumentException("delegate should not be null");
		}

		this.delegate = delegate;
		this.list = list;
		this.pool = new ObjectRecordPool(pool);
	}

	public KeyList getList()
	{
		return this.list;
	}

	public RecordPool getPool()
	{
		return this.pool.getAccesser();
	}

	public PersistanceDelegate<T> getDelegate()
	{
		return this.delegate;
	}

	private long putInPool(T element) throws IOException
	{
		if (null == element)
		{
			return 0;
		}
		else
		{
			return pool.put(element, delegate);
		}
	}

	private void removeInPool(long key) throws IOException
	{
		if (0 == key)
		{
			// do nothing
		}
		else
		{
			pool.removeIfContains(key);
		}
	}

	private long setInPool(long key, T element) throws IOException
	{
		if (0 == key)
		{
			return putInPool(element);
		}
		else if (null == element)
		{
			removeInPool(key);
			return 0;
		}
		else
		{
			boolean succ = pool.setIfContains(key, element, delegate);
			if (!succ)
			{
				key = putInPool(element);
			}
			return key;
		}
	}

	private T unsafeGetInPool(long key) throws DataFormatException, IOException
	{
		if (0 == key)
		{
			return null;
		}
		else
		{
			T ret = pool.getIfContains(key, delegate);
			if (null == ret)
			{
				throw new DataFormatException("data lost");
			}
			return ret;
		}
	}

	private boolean checkEqual(long idx, T element) throws IOException,
			StructureCorruptedException
	{
		try
		{
			T e = get(idx);
			if (e != null)
			{
				return e.equals(element);
			}
			else
			{
				return null == element;
			}
		}
		catch (DataFormatException ex)
		{
			return false;
		}
	}

	@Override
	public synchronized T get(long idx) throws IOException,
			DataFormatException, StructureCorruptedException
	{
		return unsafeGetInPool(list.get(idx));
	}

	@Override
	public synchronized long indexOf(long idx, T element) throws IOException,
			StructureCorruptedException
	{
		long ret = -1;
		ListKeyIterator itr = list.iterator(idx);
		while (itr.hasNext())
		{
			T obj = null;
			try
			{
				obj = unsafeGetInPool(itr.next());
			}
			catch (DataFormatException ex)
			{
				continue;
			}

			if (null == obj ? null == element : obj.equals(element))
			{
				ret = itr.prevIndex();
				break;
			}
		}

		assert (-1 == ret || (ret >= idx && checkEqual(ret, element))) : Assertion
				.declare();
		return ret;
	}

	@Override
	public synchronized void insert(long idx, T element) throws IOException,
			StructureCorruptedException
	{
		list.insert(idx, putInPool(element));
		assert (checkEqual(idx, element)) : Assertion.declare();
	}

	@Override
	public synchronized ListElementIterator<T> iterator(long idx)
			throws IOException, StructureCorruptedException
	{
		return new ListElementKeyIterator(list.iterator(idx));
	}

	@Override
	public synchronized void remove(long idx) throws IOException,
			StructureCorruptedException
	{
		long key = list.get(idx);
		removeInPool(key);
		list.remove(idx);
	}

	@Override
	public synchronized void set(long idx, T element) throws IOException,
			StructureCorruptedException
	{
		long key = list.get(idx);
		list.set(idx, setInPool(key, element));

		assert (checkEqual(idx, element)) : Assertion.declare();
	}

	@Override
	public synchronized void close() throws IOException
	{
		list.close();
		pool.close();
	}

	@Override
	public synchronized void flush() throws IOException
	{
		list.flush();
		pool.flush();
	}

	@Override
	public synchronized long size()
	{
		return list.size();
	}

	@Override
	public synchronized void clear() throws IOException,
			StructureCorruptedException
	{
		ListKeyIterator itr = list.iterator(0);
		while (itr.hasNext())
		{
			removeInPool(itr.next());
		}

		list.clear();
	}

	private class ListElementKeyIterator implements ListElementIterator<T>
	{
		private ListKeyIterator itr = null;

		public ListElementKeyIterator(ListKeyIterator itr)
		{
			if (null == itr)
			{
				throw new IllegalArgumentException("itr should not be null");
			}

			this.itr = itr;
		}

		@Override
		public void add(T element) throws IOException
		{
			itr.add(putInPool(element));
		}

		@Override
		public ListIteratorMove getLastMove()
		{
			return itr.getLastMove();
		}

		@Override
		public boolean hasNext() throws IOException,
				StructureCorruptedException
		{
			return itr.hasNext();
		}

		@Override
		public boolean hasPrev() throws IOException,
				StructureCorruptedException
		{
			return itr.hasPrev();
		}

		@Override
		public T next() throws IOException, DataFormatException,
				StructureCorruptedException
		{
			return unsafeGetInPool(itr.next());
		}

		@Override
		public long nextIndex()
		{
			return itr.nextIndex();
		}

		@Override
		public T prev() throws IOException, DataFormatException,
				StructureCorruptedException
		{
			return unsafeGetInPool(itr.prev());
		}

		@Override
		public long prevIndex()
		{
			return itr.prevIndex();
		}

		@Override
		public void remove() throws IOException
		{
			long last = itr.getLast();
			removeInPool(last);
			itr.remove();
		}

		@Override
		public void set(T element) throws IOException
		{
			long last = itr.getLast();
			itr.set(setInPool(last, element));
		}

		@Override
		public T getLast() throws IOException, DataFormatException
		{
			return unsafeGetInPool(itr.getLast());
		}

	}

}
