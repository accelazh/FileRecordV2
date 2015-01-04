package org.accela.file.collection.impl;

import java.io.IOException;

import org.accela.file.collection.ElementList;
import org.accela.file.collection.ListElementIterator;
import org.accela.file.common.DataFormatException;
import org.accela.file.common.StructureCorruptedException;

public class FullElementList<T> implements ElementList<T>
{
	private ElementList<T> list = null;

	public FullElementList(ElementList<T> list)
	{
		if (null == list)
		{
			throw new IllegalArgumentException("list should not be null");
		}

		this.list = list;
	}

	public synchronized ElementList<T> getList()
	{
		return this.list;
	}

	@Override
	public synchronized void clear() throws IOException,
			StructureCorruptedException
	{
		list.clear();
	}

	@Override
	public synchronized void close() throws IOException
	{
		list.close();
	}

	@Override
	public synchronized void flush() throws IOException
	{
		list.flush();
	}

	@Override
	public synchronized T get(long idx) throws IOException,
			DataFormatException, StructureCorruptedException
	{
		return list.get(idx);
	}

	@Override
	public synchronized long indexOf(long idx, T element) throws IOException,
			StructureCorruptedException
	{
		return list.indexOf(idx, element);
	}

	@Override
	public synchronized void insert(long idx, T element) throws IOException,
			StructureCorruptedException
	{
		list.insert(idx, element);
	}

	@Override
	public synchronized ListElementIterator<T> iterator(long idx)
			throws IOException, StructureCorruptedException
	{
		return list.iterator(idx);
	}

	@Override
	public synchronized void remove(long idx) throws IOException,
			StructureCorruptedException
	{
		list.remove(idx);
	}

	@Override
	public synchronized void set(long idx, T element) throws IOException,
			StructureCorruptedException
	{
		list.set(idx, element);
	}

	@Override
	public synchronized long size()
	{
		return list.size();
	}

	// =====================================================

	@SuppressWarnings("unchecked")
	@Override
	public synchronized boolean equals(Object obj)
	{
		if (null == obj)
		{
			return false;
		}
		if (!(obj instanceof ElementList))
		{
			return false;
		}

		ElementList other = (ElementList) obj;
		if (this.size() != other.size())
		{
			return false;
		}

		try
		{
			ListElementIterator<T> selfItr = this.iterator(0);
			ListElementIterator otherItr = other.iterator(0);

			while (selfItr.hasNext() && otherItr.hasNext())
			{
				T selfElement = selfItr.next();
				Object otherElement = otherItr.next();

				if (null == selfElement)
				{
					if (otherElement != null)
					{
						return false;
					}
				}
				else
				{
					if (!selfElement.equals(otherElement))
					{
						return false;
					}
				}

			}// end of while

			if (selfItr.hasNext() || otherItr.hasNext())
			{
				return false;
			}

		}
		catch (DataFormatException ex)
		{
			return false;
		}
		catch (StructureCorruptedException ex)
		{
			return false;
		}
		catch (IOException ex)
		{
			return false;
		}

		return true;

	}

	@Override
	public synchronized int hashCode()
	{
		int hash = 0;
		try
		{
			ListElementIterator<T> itr = this.iterator(0);
			while (itr.hasNext())
			{
				T e = itr.next();
				hash = 31 * hash + (e != null ? e.hashCode() : 0);
			}
		}
		catch (DataFormatException ex)
		{
			// do nothing
		}
		catch (StructureCorruptedException e)
		{
			// do nothing
		}
		catch (IOException ex)
		{
			// do nothing
		}

		return hash;
	}

	public synchronized ListElementIterator<T> iterator() throws IOException,
			StructureCorruptedException
	{
		return this.iterator(0);
	}

	public synchronized void add(T element) throws IOException,
			StructureCorruptedException
	{
		list.insert(list.size(), element);
	}

	public synchronized boolean contains(T element) throws IOException,
			StructureCorruptedException
	{
		return list.indexOf(0, element) >= 0;
	}

	public synchronized boolean remove(T element) throws IOException,
			StructureCorruptedException
	{
		long idx = indexOf(element);
		if (idx >= 0)
		{
			list.remove(idx);
			return true;
		}
		else
		{
			return false;
		}
	}

	public synchronized boolean isEmpty()
	{
		return list.size() == 0;
	}

	public synchronized long indexOf(T element) throws IOException,
			StructureCorruptedException
	{
		return list.indexOf(0, element);
	}

	public synchronized long lastIndexOf(T element) throws IOException,
			StructureCorruptedException
	{
		long lastIdx = -1;
		long newIdx = -1;
		while ((newIdx = list.indexOf(lastIdx + 1, element)) >= 0)
		{
			lastIdx = newIdx;
		}

		return lastIdx;
	}

}
