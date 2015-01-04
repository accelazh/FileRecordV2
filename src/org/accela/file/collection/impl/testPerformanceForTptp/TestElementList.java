package org.accela.file.collection.impl.testPerformanceForTptp;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.accela.file.collection.ElementList;
import org.accela.file.collection.ListElementIterator;
import org.accela.file.collection.ListIteratorMove;
import org.accela.file.collection.util.Node;
import org.accela.file.common.DataFormatException;
import org.accela.file.common.StructureCorruptedException;
import org.accela.file.record.RecordPool;
import org.accela.file.record.impl.ObjectRecordPool;

public abstract class TestElementList<T> extends TestList
{
	protected ElementList<T> list = null;

	public TestElementList()
	{
		super(new File("testList.txt"), new File("sina.txt"));
	}

	@Override
	protected void close() throws IOException
	{
		if (list != null)
		{
			list.close();
		}
	}

	@Override
	protected void open(boolean restore) throws IOException
	{
		this.list = createElementList(restore);
	}

	protected abstract ElementList<T> createElementList(boolean restore)
			throws IOException;

	protected abstract T gen(Long i);

	protected abstract T genLarge();

	protected abstract RecordPool getPool();

	protected abstract long getHeadKey();

	protected abstract long getTailKey();

	protected boolean isEqual(T a, T b)
	{
		if (null == a)
		{
			return null == b;
		}
		else
		{
			return a.equals(b);
		}
	}

	private Node getNode(long key) throws IOException
	{
		try
		{
			return nodeDelegate.readBytes(new ObjectRecordPool(getPool())
					.getIfContains(key));
		}
		catch (DataFormatException ex)
		{
			ex.printStackTrace();
			assert (false);

			throw new RuntimeException(ex);
		}
	}

	protected abstract boolean isCheckConnectivity();

	private List<Long> connectivity() throws IOException
	{
		if (!isCheckConnectivity())
		{
			return null;
		}

		long headKey = getHeadKey();
		long tailKey = getTailKey();

		assert (headKey != 0 && tailKey != 0);
		assert (headKey != tailKey);

		Node headNode = getNode(headKey);
		Node tailNode = getNode(tailKey);

		assert (headNode.getPrev() == 0);
		assert (tailNode.getNext() == 0);

		assert (headNode.isFake());
		assert (tailNode.isFake());

		long curKey = headKey;
		Node curNode = headNode;
		List<Long> headList = new LinkedList<Long>();
		headList.add(curNode.getElement());
		while (curNode.getNext() != 0)
		{
			if (curKey != headKey && curKey != tailKey)
			{
				assert (!curNode.isFake());
			}

			curKey = curNode.getNext();
			curNode = getNode(curKey);
			headList.add(curNode.getElement());
		}
		assert (curKey == tailKey);

		curKey = tailKey;
		curNode = tailNode;
		List<Long> tailList = new LinkedList<Long>();
		tailList.add(curNode.getElement());
		while (curNode.getPrev() != 0)
		{
			if (curKey != headKey && curKey != tailKey)
			{
				assert (!curNode.isFake());
			}

			curKey = curNode.getPrev();
			curNode = getNode(curKey);
			tailList.add(curNode.getElement());
		}
		assert (curKey == headKey);

		Collections.reverse(tailList);
		assert (headList.equals(tailList));
		assert (headList.size() >= 2);
		headList.remove(0);
		headList.remove(headList.size() - 1);

		return headList;
	}

	private void consistency(List<T> elements) throws IOException,
			DataFormatException, StructureCorruptedException
	{
		assert (list.size() == elements.size());

		List<Long> innerElements = connectivity();

		assert (null == innerElements || innerElements.equals(elements));
		consistencyOfGet(elements);
		consistencyOfIdx(elements);
		consistencyOfItr(elements);
		assert (list.size() == elements.size());
	}

	private void consistencyOfGet(List<T> elements) throws IOException,
			StructureCorruptedException
	{
		for (long i = 0; i < list.size(); i++)
		{
			try
			{
				assert (isEqual(list.get(i), elements.get((int) i)));
			}
			catch (DataFormatException ex)
			{
				ex.printStackTrace();
				assert (false);
			}
		}
	}

	private void consistencyOfIdx(List<T> elements) throws IOException,
			DataFormatException, StructureCorruptedException
	{
		for (int i = 0; i < elements.size(); i++)
		{
			T element = elements.get(i);

			long lastIdx = -1;
			do
			{
				lastIdx = list.indexOf(lastIdx + 1, element);
				if (lastIdx == i)
				{
					break;
				}
			}
			while (lastIdx != -1);
			assert (lastIdx == i);
		}
	}

	private void consistencyOfItr(List<T> elements) throws IOException,
			DataFormatException, StructureCorruptedException
	{
		int[] idxes = new int[] {
				0,
				(int) list.size(),
				Math.min((int) list.size(), 1),
				Math.max(0, (int) (list.size() - 1)),
				(int) list.size() / 2, };

		for (int i = 0; i < idxes.length; i++)
		{
			ListElementIterator<T> itr = list.iterator(idxes[i]);
			consistencyItrSingle(idxes[i], itr, elements);
		}
	}

	private void consistencyItrSingle(int idx,
			ListElementIterator<T> itr,
			List<T> elements) throws IOException, DataFormatException,
			StructureCorruptedException
	{
		assert (itr.nextIndex() == idx);
		assert (itr.prevIndex() == idx - 1);
		if (0 == idx || list.size() == idx)
		{
			assert (itr.getLastMove() == ListIteratorMove.none);
			try
			{
				itr.getLast();
				assert (false);
			}
			catch (Exception ex)
			{
				assert (ex instanceof NoSuchElementException);
			}
		}

		int curIdx = idx;
		while (itr.hasPrev())
		{
			assert (itr.prevIndex() == curIdx - 1);
			T prevElement = itr.prev();
			assert (isEqual(prevElement, elements.get(curIdx - 1)));

			assert (itr.getLastMove() == ListIteratorMove.prev);
			assert (isEqual(itr.getLast(), prevElement));

			curIdx--;
		}
		assert (curIdx == 0);
		assert (itr.prevIndex() == -1);
		assert (itr.nextIndex() == 0);

		while (itr.hasNext())
		{
			assert (itr.nextIndex() == curIdx);
			T nextElement = itr.next();
			assert (isEqual(nextElement, elements.get(curIdx)));

			assert (itr.getLastMove() == ListIteratorMove.next);
			assert (isEqual(itr.getLast(), nextElement));

			curIdx++;
		}
		assert (curIdx == list.size());
		assert (itr.prevIndex() == list.size() - 1);
		assert (itr.nextIndex() == list.size());

		while (itr.hasPrev())
		{
			assert (itr.prevIndex() == curIdx - 1);
			T prevElement = itr.prev();
			assert (isEqual(prevElement, elements.get(curIdx - 1)));

			assert (itr.getLastMove() == ListIteratorMove.prev);
			assert (isEqual(itr.getLast(), prevElement));

			curIdx--;
		}
		assert (curIdx == 0);
		assert (itr.prevIndex() == -1);
		assert (itr.nextIndex() == 0);
	}

	private void randomItrTest(ListElementIterator<T> itr, List<T> elements)
			throws IOException, DataFormatException,
			StructureCorruptedException
	{
		boolean next = rand.nextBoolean();
		boolean add = rand.nextDouble() > 0.3;
		boolean remove = rand.nextDouble() > 0.6;
		boolean set = rand.nextDouble() > 0.3;

		// next or previous
		long lastIdx = -1;
		if (next)
		{
			if (!itr.hasNext())
			{
				assert (itr.nextIndex() == list.size());
			}
			else
			{
				long elementIdx = itr.nextIndex();
				T element = itr.next();
				lastIdx = elementIdx;
				assert (isEqual(element, elements.get((int) elementIdx)));
			}
		}
		else
		{
			if (!itr.hasPrev())
			{
				assert (itr.prevIndex() == -1);
			}
			else
			{
				long elementIdx = itr.prevIndex();
				T element = itr.prev();
				lastIdx = elementIdx;
				assert (isEqual(element, elements.get((int) elementIdx)));
			}
		}

		// set
		if (lastIdx != -1 && set)
		{
			T element = genRndElement();
			itr.set(element);
			elements.set((int) lastIdx, element);
			assert (isEqual(element, list.get(lastIdx)));
		}

		// add
		if (add)
		{
			for (int i = 0; i < 2; i++)
			{
				T element = genRndElement();
				long oldNextIdx = itr.nextIndex();
				itr.add(element);
				elements.add((int) oldNextIdx, element);
				assert (isEqual(element, list.get(oldNextIdx)));

				// System.out.println("add element " + element);
			}
		}

		// remove
		if (lastIdx != -1 && !add && remove)
		{
			itr.remove();
			elements.remove((int) lastIdx);
		}

		// consistency
		consistency(elements);
	}

	private long genRndIdx(boolean lessThanSize)
	{
		long idx = 0;
		if (lessThanSize)
		{
			idx = rand.nextInt((int) list.size());
		}
		else
		{
			idx = rand.nextInt((int) list.size() + 1);
		}
		return idx;
	}

	private T genRndElement()
	{
		long element = rand.nextDouble() > 0.2 ? Math.abs(rand.nextLong()) : 0;
		return gen(element);
	}

	private void randomListTest(List<T> elements) throws IOException,
			DataFormatException, StructureCorruptedException
	{
		long idx = -1;
		T element = null;

		for (int i = 0; i < 2; i++)
		{
			idx = genRndIdx(false);
			element = genRndElement();

			list.insert(idx, element);
			elements.add((int) idx, element);

			assert (isEqual(list.get(idx), element));
		}

		idx = genRndIdx(true);
		list.remove(idx);
		elements.remove((int) idx);

		idx = genRndIdx(true);
		element = genRndElement();
		list.set(idx, element);
		elements.set((int) idx, element);

		consistency(elements);
	}

	public void testPerformance() throws IOException, DataFormatException,
			StructureCorruptedException
	{
		final int TEST_SIZE = 50;

		List<T> elements = new LinkedList<T>();
		ListElementIterator<T> itr = list.iterator(0);
		for (int i = 0; i < TEST_SIZE; i++)
		{
			randomItrTest(itr, elements);
		}

		for (int i = 0; i < TEST_SIZE; i++)
		{
			randomListTest(elements);
		}

	}
}
