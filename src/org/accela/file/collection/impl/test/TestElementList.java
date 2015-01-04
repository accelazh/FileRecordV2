package org.accela.file.collection.impl.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.accela.file.collection.ElementList;
import org.accela.file.collection.KeyList;
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

	public void testAcceptNonnegative() throws IOException
	{
		try
		{
			list.insert(0, gen(0L));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			assert (false);
		}
		try
		{
			list.insert(0, gen(-1L));
			if (list instanceof KeyList)
			{
				assert (false);
			}
		}
		catch (Exception ex)
		{
			assert (ex instanceof IllegalArgumentException);
		}
		try
		{
			list.insert(0, gen(10L));
		}
		catch (Exception ex)
		{
			assert (false);
		}
		try
		{
			list.insert(0, gen(Long.MAX_VALUE));
		}
		catch (Exception ex)
		{
			assert (false);
		}
		try
		{
			list.insert(0, gen(null));
			if (list instanceof KeyList)
			{
				assert (false);
			}
		}
		catch (Exception ex)
		{
			assert (ex instanceof IllegalArgumentException);
		}

	}

	public void testConcurrentModify() throws IOException, DataFormatException,
			StructureCorruptedException
	{
		ListElementIterator<T> itr = list.iterator(0);
		for (long i = 0; i < 100; i++)
		{
			itr.add(gen(i));
		}

		ListElementIterator<T> itr2 = list.iterator(0);
		itr2.next();
		itr2.next();

		itr.add(gen(100L));

		try
		{
			itr2.next();
			assert (false);
		}
		catch (Exception ex)
		{
			assert (ex instanceof ConcurrentModificationException);
		}

		itr.prev();
		itr.add(gen(101L));

		ListElementIterator<T> itr3 = list.iterator(0);
		itr3.next();
		itr3.next();

		itr3.set(gen(0L));
		itr3.set(gen(99L));

		try
		{
			itr2.next();
			assert (false);
		}
		catch (Exception ex)
		{
			assert (ex instanceof ConcurrentModificationException);
		}

		try
		{
			itr.next();
		}
		catch (Exception ex)
		{
			assert (false);
		}

		itr3.remove();

		try
		{
			itr2.next();
			assert (false);
		}
		catch (Exception ex)
		{
			assert (ex instanceof ConcurrentModificationException);
		}
		try
		{
			itr.prev();
			assert (false);
		}
		catch (Exception ex)
		{
			assert (ex instanceof ConcurrentModificationException);
		}
		try
		{
			itr.set(gen(102L));
			assert (false);
		}
		catch (Exception ex)
		{
			assert (ex instanceof ConcurrentModificationException);
		}
		try
		{
			itr.next();
			assert (false);
		}
		catch (Exception ex)
		{
			assert (ex instanceof ConcurrentModificationException);
		}
		try
		{
			itr.add(gen(103L));
			assert (false);
		}
		catch (Exception ex)
		{
			assert (ex instanceof ConcurrentModificationException);
		}
	}

	public void testSpeed() throws IOException, DataFormatException,
			StructureCorruptedException
	{
		List<T> elements = new LinkedList<T>();
		for (int i = 0; i < 50; i++)
		{
			T e = genRndElement();
			list.insert(0, e);
			elements.add(0, e);
		}

		consistency(elements);
	}

	public void testRandomIterator() throws IOException, DataFormatException,
			StructureCorruptedException
	{
		final int TEST_SIZE = 50;

		List<T> elements = new LinkedList<T>();
		consistency(elements);
		ListElementIterator<T> itr = list.iterator(0);
		// System.out.println("test1...");
		for (int i = 0; i < TEST_SIZE; i++)
		{
			// System.out.println("i: " + i);
			randomItrTest(itr, elements);
		}

		reopen(true);
		consistency(elements);

		// reopen and test again
		reopen(false);

		elements = new LinkedList<T>();
		consistency(elements);
		itr = list.iterator(0);
		// System.out.println("test2...");
		for (int i = 0; i < TEST_SIZE; i++)
		{
			randomItrTest(itr, elements);
		}

		// test clear
		itr = list.iterator(0);
		initItrTest(itr);
		while (itr.hasNext())
		{
			itr.next();
			itr.remove();
		}
		assert (list.size() == 0);
		consistency(new LinkedList<T>());

		// reopen and test again
		reopen(false);

		elements = new LinkedList<T>();
		consistency(elements);
		itr = list.iterator(0);
		// System.out.println("test3...");
		for (int i = 0; i < TEST_SIZE; i++)
		{
			randomItrTest(itr, elements);
		}

		// test clear
		itr = list.iterator(list.size());
		initItrTest(itr);
		while (itr.hasPrev())
		{
			itr.prev();
			itr.remove();
		}
		assert (list.size() == 0);
		consistency(new LinkedList<T>());
	}

	private void initItrTest(ListElementIterator<T> itr)
	{
		try
		{
			itr.remove();
			assert (false);
		}
		catch (Exception ex)
		{
			assert (ex instanceof NoSuchElementException);
		}
		try
		{
			itr.set(gen(0L));
			assert (false);
		}
		catch (Exception ex)
		{
			assert (ex instanceof NoSuchElementException);
		}
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

				// System.out.println("next: " + elementIdx + ", " + element);
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

				// System.out.println("prev: " + elementIdx + ", " + element);
			}
		}

		// set
		if (lastIdx != -1 && set)
		{
			for (int i = 0; i < 2; i++)
			{
				T element = genRndElement();
				itr.set(element);
				elements.set((int) lastIdx, element);
				assert (isEqual(element, list.get(lastIdx)));

				// System.out.println("set element " + element);
			}
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

			try
			{
				itr.remove();
				assert (false);
			}
			catch (Exception ex)
			{
				assert (ex instanceof NoSuchElementException);
			}
			try
			{
				itr.set(gen(1L));
				assert (false);
			}
			catch (Exception ex)
			{
				assert (ex instanceof NoSuchElementException);
			}
			try
			{
				itr.getLast();
				assert (false);
			}
			catch (Exception ex)
			{
				assert (ex instanceof NoSuchElementException);
			}
			assert (itr.getLastMove().equals(ListIteratorMove.none));
		}

		// remove
		if (lastIdx != -1 && !add && remove)
		{
			itr.remove();
			elements.remove((int) lastIdx);

			if (rand.nextDouble() < 0.1)
			{
				consistency(elements);
			}

			// System.out.println("remove element, idx: " + lastIdx);

			try
			{
				itr.remove();
				assert (false);
			}
			catch (Exception ex)
			{
				assert (ex instanceof NoSuchElementException);
			}
			try
			{
				itr.set(gen(1L));
				assert (false);
			}
			catch (Exception ex)
			{
				assert (ex instanceof NoSuchElementException);
			}
			try
			{
				itr.getLast();
				assert (false);
			}
			catch (Exception ex)
			{
				assert (ex instanceof NoSuchElementException);
			}
			assert (itr.getLastMove().equals(ListIteratorMove.none));
		}

		// consistency
		// System.out.println("consistency: " + elements.size());
		consistency(elements);
	}

	public void testMyList() throws IOException, DataFormatException,
			StructureCorruptedException
	{
		final long[] data = new long[] {
				0,
				1,
				1,
				2,
				3,
				6,
				6,
				7,
				6,
				8,
				6,
				9,
				0,
				999,
				0,
				20,
				3,
				7,
				8,
				12,
				14,
				8,
				6,
				7,
				9,
				20,
				45,
				1 };

		List<T> elements = new LinkedList<T>();
		for (int i = 0; i < data.length; i++)
		{
			list.insert(list.size(), gen(data[i]));
			elements.add(gen(data[i]));
		}

		long idx = genRndIdx(false);
		T largeData = genLarge();
		list.insert(idx, largeData);
		elements.add((int) idx, largeData);

		consistency(elements);

		list.clear();
		elements.clear();
		consistency(elements);
	}

	public void testRandomList() throws IOException, DataFormatException,
			StructureCorruptedException
	{
		final int TEST_SIZE = 50;

		List<T> elements = new LinkedList<T>();
		consistency(elements);

		for (int i = 0; i < TEST_SIZE; i++)
		{
			// System.out.println("i: "+i);
			randomListTest(elements);
		}
		assert (elements.size() >= TEST_SIZE);
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

	public void testFaultRandom() throws IOException, DataFormatException,
			StructureCorruptedException
	{
		this.fillFileRandom(128 * 10000);
		ListElementIterator<T> itr = list.iterator(0);
		while (itr.hasNext())
		{
			itr.next();
		}
		assert (list.size() >= 0);
		list.clear();
		assert (0 == list.size());
	}

	public void testInteractivly() throws IOException,
			StructureCorruptedException, DataFormatException
	{
		boolean interactiveTest=false;
		if(!interactiveTest)
		{
			return;
		}
		
		ListElementIterator<T> itr = list.iterator(0);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while (true)
		{
			System.out.print("command> ");

			String command = in.readLine();
			if (command.startsWith("exit"))
			{
				break;
			}
			else if (command.startsWith("next"))
			{
				long idx=itr.nextIndex();
				T e=itr.next();
				System.out.println(idx+": "+e);
			}
			else if (command.startsWith("prev"))
			{
				long idx=itr.prevIndex();
				T e=itr.prev();
				System.out.println(idx+": "+e);
			}
			else if (command.startsWith("add"))
			{
				StringTokenizer tokens=new StringTokenizer(command);
				tokens.nextToken();
				T e=gen(Long.parseLong(tokens.nextToken()));
				itr.add(e);
				System.out.println("element added: "+e);
			}
			else if (command.startsWith("remove"))
			{
				itr.remove();
				System.out.println("element removed");
			}
			else if (command.startsWith("set"))
			{
				StringTokenizer tokens=new StringTokenizer(command);
				tokens.nextToken();
				T e=gen(Long.parseLong(tokens.nextToken()));
				itr.set(e);
				System.out.println("element set: "+e);
			}
			else
			{
				System.out.println("Illegal Command!");
			}
		}
	}
}
