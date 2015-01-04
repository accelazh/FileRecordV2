package org.accela.file.collection.impl;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import org.accela.common.Assertion;
import org.accela.file.collection.KeyList;
import org.accela.file.collection.ListIteratorMove;
import org.accela.file.collection.ListKeyIterator;
import org.accela.file.collection.util.Node;
import org.accela.file.collection.util.NodePersistanceDelegate;
import org.accela.file.common.DataFormatException;
import org.accela.file.common.StructureCorruptedException;
import org.accela.file.record.RecordPool;
import org.accela.file.record.impl.ObjectRecordPool;

//链表的所有的一切都是建立在迭代器的基础上
//链表具有结构崩溃检测功能：StructureCorruptedException
//链表具有重启自动修复功能：init()方法，重启后，链表的损坏部分可能会被截断。但是链表的结构完整性能够得到恢复
public class LinkedKeyList implements KeyList
{
	private CoreList list = null;

	public LinkedKeyList(RecordPool accesser, long key) throws IOException
	{
		this.list = new CoreList(accesser, key);
	}

	@Override
	public RecordPool getAccesser()
	{
		return this.list.getAccesser();
	}

	@Override
	public synchronized long getKey()
	{
		return this.list.getKey();
	}

	@Override
	public synchronized void clear() throws IOException,
			StructureCorruptedException
	{
		ListKeyIterator itr = iterator(0);
		while (itr.hasNext())
		{
			itr.next();
			itr.remove();
		}

		assert (list.size() == 0) : Assertion.declare();
	}

	@Override
	public synchronized Long get(long idx) throws IOException,
			StructureCorruptedException
	{
		if (idx < 0 || idx >= list.size())
		{
			throw new IllegalArgumentException("idx out of bound: " + idx);
		}

		ListKeyIterator itr = iterator(idx);
		return itr.next();
	}

	@Override
	public synchronized long indexOf(long idx, Long element)
			throws IOException, StructureCorruptedException
	{
		if (null == element)
		{
			throw new IllegalArgumentException("element should not be null");
		}

		long ret = -1;
		ListKeyIterator itr = iterator(idx);
		while (itr.hasNext())
		{
			long value = itr.next();
			if (value == element)
			{
				ret = itr.prevIndex();
				break;
			}
		}

		assert (-1 == ret || (ret >= idx && get(ret).equals(element))) : Assertion
				.declare();
		return ret;
	}

	@Override
	public synchronized void insert(long idx, Long element) throws IOException,
			StructureCorruptedException
	{
		if (null == element)
		{
			throw new IllegalArgumentException("element should not be null");
		}
		if (element < 0)
		{
			throw new IllegalArgumentException("element should not be negative");
		}

		ListKeyIterator itr = iterator(idx);
		itr.add(element);

		assert (get(idx).equals(element)) : Assertion.declare();
	}

	private ListKeyIterator iterator(boolean head) throws IOException
	{
		return list.iterator(head);
	}

	@Override
	public synchronized ListKeyIterator iterator(long idx) throws IOException,
			StructureCorruptedException
	{
		if (idx < 0 || idx > list.size())
		{
			throw new IllegalArgumentException("idx out of bound: " + idx);
		}

		ListKeyIterator itr = null;
		if (idx <= list.size() / 2)
		{
			itr = this.iterator(true);
		}
		else
		{
			itr = this.iterator(false);
		}

		while (itr.nextIndex() > idx && itr.hasPrev())
		{
			itr.prev();
		}
		while (itr.nextIndex() < idx && itr.hasNext())
		{
			itr.next();
		}

		assert (itr.nextIndex() == idx) : Assertion.declare();
		return itr;
	}

	@Override
	public synchronized void remove(long idx) throws IOException,
			StructureCorruptedException
	{
		if (idx < 0 || idx >= list.size())
		{
			throw new IllegalArgumentException("idx out of bound: " + idx);
		}

		ListKeyIterator itr = iterator(idx);
		itr.next();
		itr.remove();
	}

	@Override
	public synchronized void set(long idx, Long element) throws IOException,
			StructureCorruptedException
	{
		if (idx < 0 || idx >= list.size())
		{
			throw new IllegalArgumentException("idx out of bound: " + idx);
		}
		if (null == element)
		{
			throw new IllegalArgumentException("element should not be null");
		}
		if (element < 0)
		{
			throw new IllegalArgumentException("element should not be negative");
		}

		ListKeyIterator itr = iterator(idx);
		itr.next();
		itr.set(element);

		assert (get(idx).equals(element));
	}

	@Override
	public synchronized void close() throws IOException
	{
		this.list.close();
	}

	@Override
	public synchronized void flush() throws IOException
	{
		this.list.flush();
	}

	@Override
	public synchronized long size()
	{
		return list.size();
	}

	// =========================================================================
	// 为了简化LinkedKeyList的实现，使用内部类LinkedCoreKeyList作为内部类，分离链表
	// 迭代器的操作
	// =========================================================================
	private static class CoreList
	{
		private ObjectRecordPool accesser = null;

		private NodePersistanceDelegate delegate = null;

		private long head = 0;

		private long tail = 0;

		private long size = 0;

		private long modCount = 0;

		public CoreList(RecordPool accesser, long key) throws IOException
		{
			if (null == accesser)
			{
				throw new IllegalArgumentException(
						"accesser should not be null");
			}

			this.delegate = new NodePersistanceDelegate();
			this.accesser = new ObjectRecordPool(accesser);

			init(key);

			this.modCount = 0;
		}

		public RecordPool getAccesser()
		{
			return this.accesser.getAccesser();
		}

		private void init(long key) throws IOException
		{
			repairByHead(key);
		}

		// construct list, and init head, tail and size
		private void repairByHead(long key) throws IOException
		{
			// get head
			Node headNode = tryGet(key);
			if (null == headNode)
			{
				headNode = new Node(0, 0, 0);
				key = put(headNode);
			}
			head = key;

			// find tail
			tail = findLastAndRepairConnectivity(head);
			if (tail == head)
			{
				tail = 0;
			}

			Node tailNode = tryGet(tail);
			if (null == tailNode)
			{
				tailNode = new Node(head, 0, 0);
				tail = put(tailNode);

				headNode.setNext(tail);
				setIfContains(head, headNode);
			}

			// set all not fake
			setAllUnfake(head);

			// repair head
			headNode.setPrev(0);
			headNode.setFake(true);
			setIfContains(head, headNode);

			// repair tail
			tailNode.setNext(0);
			tailNode.setFake(true);
			setIfContains(tail, tailNode);

			// count size
			long headSize = countHeadSize();
			long tailSize = countTailSize();

			if (headSize != tailSize)
			{
				throw new IOException("list data corrupted");
			}

			this.size = headSize;
		}

		private long findLastAndRepairConnectivity(long key) throws IOException
		{
			long curKey = key;
			Node curNode = tryGet(key);
			if (null == curNode)
			{
				return 0;
			}

			Node nextNode = null;
			long count = 0;
			while ((nextNode = tryGet(curNode.getNext())) != null
					&& count < accesser.poolSize())
			{
				nextNode.setPrev(curKey);
				setIfContains(curNode.getNext(), nextNode);

				curKey = curNode.getNext();
				curNode = nextNode;
				count++;
			}

			return curKey;
		}

		private void setAllUnfake(long key) throws IOException
		{
			long curKey = key;
			Node node = null;
			while ((node = tryGet(curKey)) != null)
			{
				node.setFake(false);
				setIfContains(curKey, node);

				curKey = node.getNext();
			}
		}

		private long countHeadSize() throws IOException
		{
			Node headNode = tryGet(head);
			if (null == headNode)
			{
				return 0;
			}

			long count = 0;
			NodeIterator itr = new NodeIterator(head, headNode.getNext());
			while (itr.hasNext())
			{
				count++;
				itr.next();
			}

			return count;
		}

		private long countTailSize() throws IOException
		{
			Node tailNode = tryGet(tail);
			if (null == tailNode)
			{
				return 0;
			}

			long count = 0;
			NodeIterator itr = new NodeIterator(tailNode.getPrev(), tail);
			while (itr.hasPrev())
			{
				count++;
				itr.prev();
			}

			return count;
		}

		private Node unsafeGetIfContains(long key) throws IOException,
				DataFormatException
		{
			return this.accesser.getIfContains(key, delegate);
		}

		private Node tryGet(long key) throws IOException
		{
			try
			{
				return unsafeGetIfContains(key);
			}
			catch (DataFormatException ex)
			{
				return null;
			}
		}

		private boolean setIfContains(long key, Node node) throws IOException
		{
			assert (node != null);
			return accesser.setIfContains(key, node, delegate);
		}

		private long put(Node node) throws IOException
		{
			assert (node != null);
			return accesser.put(node, delegate);
		}

		public synchronized long getKey()
		{
			return this.head;
		}

		public synchronized ListKeyIterator iterator(boolean head)
				throws IOException
		{
			return new SyncListKeyIterator(new DetectorListKeyIterator(
					new IndexedListNodeIterator(new ListNodeIterator(head),
							head ? 0 : size())));
		}

		public synchronized long size()
		{
			assert (size >= 0);
			return this.size;
		}

		public synchronized void close() throws IOException
		{
			accesser.close();
		}

		public synchronized void flush() throws IOException
		{
			accesser.flush();
		}

		// don't rely on head, tail or size
		// don't do any repair
		// don't iterator over fake nodes
		private class NodeIterator
		{
			private long nextKey = 0;

			private Node nextPreload = null;

			private long prevKey = 0;

			private Node prevPreload = null;

			private ListIteratorMove lastMove = ListIteratorMove.none;

			private long count = 0;

			public NodeIterator(long prevKey, long nextKey)
			{
				this.nextKey = nextKey;
				this.prevKey = prevKey;

				this.lastMove = ListIteratorMove.none;
				this.count = 0;
				this.nextPreload = null;
				this.prevPreload = null;
			}

			private long lastKey()
			{
				if (lastMove.equals(ListIteratorMove.next))
				{
					return prevKey;
				}
				else if (lastMove.equals(ListIteratorMove.prev))
				{
					return nextKey;
				}
				else if (lastMove.equals(ListIteratorMove.none))
				{
					return 0;
				}
				else
				{
					assert (false);
					return 0;
				}
			}

			private void preloadLast() throws IOException
			{
				if (lastMove.equals(ListIteratorMove.next))
				{
					preloadPrev();
				}
				else if (lastMove.equals(ListIteratorMove.prev))
				{
					preloadNext();
				}
				else if (lastMove.equals(ListIteratorMove.none))
				{
					// do nothing
				}
				else
				{
					assert (false);
				}
			}

			private Node lastPreload()
			{
				if (lastMove.equals(ListIteratorMove.next))
				{
					return prevPreload;
				}
				else if (lastMove.equals(ListIteratorMove.prev))
				{
					return nextPreload;
				}
				else if (lastMove.equals(ListIteratorMove.none))
				{
					return null;
				}
				else
				{
					assert (false);
					return null;
				}
			}

			private void preloadNext() throws IOException
			{
				if (null == this.nextPreload)
				{
					this.nextPreload = tryGet(nextKey);
				}
			}

			private void preloadPrev() throws IOException
			{
				if (null == this.prevPreload)
				{
					this.prevPreload = tryGet(prevKey);
				}
			}

			public boolean hasNext() throws IOException
			{
				preloadNext();
				return nextPreload != null
						&& !nextPreload.isFake()
						&& Math.abs(count) < accesser.poolSize();
			}

			public boolean hasPrev() throws IOException
			{
				preloadPrev();
				return prevPreload != null
						&& !prevPreload.isFake()
						&& Math.abs(count) < accesser.poolSize();
			}

			public long next() throws IOException
			{
				if (!hasNext())
				{
					throw new NoSuchElementException();
				}

				Node node = nextPreload;
				assert (node != null);

				prevKey = nextKey;
				nextKey = node.getNext();
				lastMove = ListIteratorMove.next;
				prevPreload = node;
				nextPreload = null;
				count++;

				return node.getElement();
			}

			public long prev() throws IOException
			{
				if (!hasPrev())
				{
					throw new NoSuchElementException();
				}

				Node node = prevPreload;
				assert (node != null);

				nextKey = prevKey;
				prevKey = node.getPrev();
				lastMove = ListIteratorMove.prev;
				nextPreload = node;
				prevPreload = null;
				count--;

				return node.getElement();
			}

			public void add(long element) throws IOException
			{
				if (element < 0)
				{
					throw new IllegalArgumentException(
							"element should not be negative");
				}

				Node node = new Node(prevKey, nextKey, element);
				long key = put(node);

				preloadPrev();
				Node prev = prevPreload;
				if (prev != null)
				{
					prev.setNext(key);
					CoreList.this.setIfContains(prevKey, prev);
				}

				preloadNext();
				Node next = nextPreload;
				if (next != null)
				{
					next.setPrev(key);
					CoreList.this.setIfContains(nextKey, next);
				}

				prevKey = key;
				lastMove = ListIteratorMove.none;
				prevPreload = node;
			}

			private Node getLastNext() throws IOException
			{
				preloadLast();
				Node last = lastPreload();
				if (null == last)
				{
					return null;
				}

				if (lastMove.equals(ListIteratorMove.next))
				{
					preloadNext();
					return nextPreload;
				}
				else if (lastMove.equals(ListIteratorMove.prev))
				{
					return tryGet(last.getNext());
				}
				else if (lastMove.equals(ListIteratorMove.none))
				{
					return null;
				}
				else
				{
					assert (false);
					return null;
				}
			}

			private Node getLastPrev() throws IOException
			{
				preloadLast();
				Node last = lastPreload();
				if (null == last)
				{
					return null;
				}

				if (lastMove.equals(ListIteratorMove.next))
				{
					return tryGet(last.getPrev());
				}
				else if (lastMove.equals(ListIteratorMove.prev))
				{
					preloadPrev();
					return prevPreload;
				}
				else if (lastMove.equals(ListIteratorMove.none))
				{
					return null;
				}
				else
				{
					assert (false);
					return null;
				}
			}

			public void remove() throws IOException
			{
				if (lastMove.equals(ListIteratorMove.none))
				{
					throw new NoSuchElementException();
				}

				final long lastKey = lastKey();

				preloadLast();
				Node last = lastPreload();
				if (null == last)
				{
					// do nothing
					return;
				}

				Node lastPrev = getLastPrev();
				if (lastPrev != null)
				{
					lastPrev.setNext(last.getNext());
					CoreList.this.setIfContains(last.getPrev(), lastPrev);
				}

				Node lastNext = getLastNext();
				if (lastNext != null)
				{
					lastNext.setPrev(last.getPrev());
					CoreList.this.setIfContains(last.getNext(), lastNext);
				}

				accesser.removeIfContains(lastKey);

				if (lastMove.equals(ListIteratorMove.next))
				{
					prevKey = last.getPrev();
					prevPreload = lastPrev;
				}
				else if (lastMove.equals(ListIteratorMove.prev))
				{
					nextKey = last.getNext();
					nextPreload = lastNext;
				}
				else
				{
					assert (false);
				}
				this.lastMove = ListIteratorMove.none;
				decrAbsCount();
			}

			private void decrAbsCount()
			{
				if (count > 0)
				{
					count--;
				}
				else if (count < 0)
				{
					count++;
				}
				else
				{
					count = 0;
				}
			}

			public void set(long element) throws IOException
			{
				if (lastMove.equals(ListIteratorMove.none))
				{
					throw new NoSuchElementException();
				}

				preloadLast();
				Node last = lastPreload();
				if (last != null)
				{
					last.setElement(element);
					CoreList.this.setIfContains(lastKey(), last);
				}
			}

			public ListIteratorMove getLastMove()
			{
				return this.lastMove;
			}

			public long getLast() throws IOException
			{
				if (lastMove.equals(ListIteratorMove.none))
				{
					throw new NoSuchElementException();
				}

				preloadLast();
				Node last = lastPreload();
				if (null == last)
				{
					return 0;
				}
				else
				{
					return last.getElement();
				}
			}
		}

		// rely on head, tail, and size
		private class ListNodeIterator
		{
			private NodeIterator itr = null;

			public ListNodeIterator(boolean head) throws IOException
			{
				if (head)
				{
					Node headNode = tryGet(CoreList.this.head);
					this.itr = new NodeIterator(CoreList.this.head,
							(headNode != null) ? headNode.getNext() : 0);
				}
				else
				{
					Node tailNode = tryGet(CoreList.this.tail);
					this.itr = new NodeIterator((tailNode != null) ? tailNode
							.getPrev() : 0, CoreList.this.tail);
				}
			}

			public boolean hasNext() throws IOException
			{
				return itr.hasNext();
			}

			public boolean hasPrev() throws IOException
			{
				return itr.hasPrev();
			}

			public long next() throws IOException
			{
				return itr.next();
			}

			public long prev() throws IOException
			{
				return itr.prev();
			}

			public void add(long element) throws IOException
			{
				itr.add(element);
				size++;
			}

			public void remove() throws IOException
			{
				itr.remove();
				size -= Math.min(1, size);
			}

			public void set(long element) throws IOException
			{
				itr.set(element);
			}

			public ListIteratorMove getLastMove()
			{
				return itr.getLastMove();
			}

			public long getLast() throws IOException
			{
				return itr.getLast();
			}

		}

		private static class IndexedListNodeIterator implements ListKeyIterator
		{
			private ListNodeIterator itr = null;

			private long nextIdx = 0;

			public IndexedListNodeIterator(ListNodeIterator itr,
					long initNextIndex)
			{
				if (null == itr)
				{
					throw new IllegalArgumentException("itr should not be null");
				}
				if (initNextIndex < 0)
				{
					throw new IllegalArgumentException(
							"initNextIndex should not be negative");
				}

				this.itr = itr;
				this.nextIdx = initNextIndex;
			}

			@Override
			public long nextIndex()
			{
				return nextIdx;
			}

			@Override
			public long prevIndex()
			{
				return nextIdx - 1;
			}

			@Override
			public void add(Long element) throws IOException
			{
				if (null == element)
				{
					throw new IllegalArgumentException(
							"element should not be null");
				}

				itr.add(element);
				nextIdx++;
			}

			@Override
			public boolean hasNext() throws IOException
			{
				return itr.hasNext();
			}

			@Override
			public boolean hasPrev() throws IOException
			{
				return itr.hasPrev();
			}

			@Override
			public Long next() throws IOException
			{
				Long ret = itr.next();
				nextIdx++;
				return ret;
			}

			@Override
			public Long prev() throws IOException
			{
				Long ret = itr.prev();
				nextIdx--;
				return ret;
			}

			@Override
			public void remove() throws IOException
			{
				ListIteratorMove lastMove = itr.getLastMove();
				itr.remove();
				if (lastMove.equals(ListIteratorMove.next))
				{
					nextIdx--;
				}
			}

			@Override
			public void set(Long element) throws IOException
			{
				if (null == element)
				{
					throw new IllegalArgumentException(
							"element should not be null");
				}

				itr.set(element);
			}

			@Override
			public ListIteratorMove getLastMove()
			{
				return itr.getLastMove();
			}

			@Override
			public Long getLast() throws IOException
			{
				return itr.getLast();
			}

		}

		private class DetectorListKeyIterator implements ListKeyIterator
		{
			private ListKeyIterator itr = null;

			public DetectorListKeyIterator(ListKeyIterator itr)
			{
				if (null == itr)
				{
					throw new IllegalArgumentException("itr should not be null");
				}

				this.itr = itr;
			}

			@Override
			public void add(Long element) throws IOException
			{
				itr.add(element);
			}

			@Override
			public Long getLast() throws IOException
			{
				return itr.getLast();
			}

			@Override
			public ListIteratorMove getLastMove()
			{
				return itr.getLastMove();
			}

			private boolean checkIdx(long idx)
			{
				return idx >= 0 && idx < CoreList.this.size();
			}

			@Override
			public boolean hasNext() throws IOException,
					StructureCorruptedException
			{
				boolean ret = itr.hasNext();
				if (checkIdx(itr.nextIndex()) ^ ret)
				{
					throw new StructureCorruptedException();
				}
				return ret;

			}

			@Override
			public boolean hasPrev() throws IOException,
					StructureCorruptedException
			{
				boolean ret = itr.hasPrev();
				if (checkIdx(itr.prevIndex()) ^ ret)
				{
					throw new StructureCorruptedException();
				}
				return ret;
			}

			@Override
			public Long next() throws IOException, StructureCorruptedException
			{
				Long ret = null;
				NoSuchElementException exception = null;
				long nextIdx = itr.nextIndex();
				try
				{
					ret = itr.next();
				}
				catch (NoSuchElementException ex)
				{
					exception = ex;
				}

				if (checkIdx(nextIdx) ^ (null == exception))
				{
					throw new StructureCorruptedException();
				}
				if (exception != null)
				{
					throw exception;
				}

				return ret;
			}

			@Override
			public long nextIndex()
			{
				assert (itr.nextIndex() >= 0 && itr.nextIndex() <= CoreList.this
						.size());
				return itr.nextIndex();
			}

			@Override
			public Long prev() throws IOException, StructureCorruptedException
			{
				Long ret = null;
				NoSuchElementException exception = null;
				long prevIdx = itr.prevIndex();
				try
				{
					ret = itr.prev();
				}
				catch (NoSuchElementException ex)
				{
					exception = ex;
				}

				if (checkIdx(prevIdx) ^ (null == exception))
				{
					throw new StructureCorruptedException();
				}
				if (exception != null)
				{
					throw exception;
				}

				return ret;
			}

			@Override
			public long prevIndex()
			{
				assert (itr.prevIndex() >= -1 && itr.prevIndex() < CoreList.this
						.size());
				return itr.prevIndex();
			}

			@Override
			public void remove() throws IOException
			{
				itr.remove();
			}

			@Override
			public void set(Long element) throws IOException
			{
				itr.set(element);
			}

		}

		private class SyncListKeyIterator implements ListKeyIterator
		{
			private ListKeyIterator itr = null;

			private long localModCount = 0;

			public SyncListKeyIterator(ListKeyIterator itr) throws IOException
			{
				if (null == itr)
				{
					throw new IllegalArgumentException("itr should not be null");
				}

				synchronized (CoreList.this)
				{
					this.localModCount = CoreList.this.modCount;
					this.itr = itr;
				}
			}

			private void testModCount()
			{
				if (localModCount != modCount)
				{
					throw new ConcurrentModificationException();
				}
			}

			@Override
			public boolean hasNext() throws IOException,
					StructureCorruptedException
			{
				synchronized (CoreList.this)
				{
					testModCount();
					return itr.hasNext();
				}
			}

			@Override
			public boolean hasPrev() throws IOException,
					StructureCorruptedException
			{
				synchronized (CoreList.this)
				{
					testModCount();
					return itr.hasPrev();
				}
			}

			@Override
			public Long next() throws IOException, StructureCorruptedException
			{
				synchronized (CoreList.this)
				{
					testModCount();
					return itr.next();
				}
			}

			@Override
			public Long prev() throws IOException, StructureCorruptedException
			{
				synchronized (CoreList.this)
				{
					testModCount();
					return itr.prev();
				}
			}

			@Override
			public void add(Long element) throws IOException
			{
				synchronized (CoreList.this)
				{
					testModCount();
					localModCount++;
					modCount++;
					itr.add(element);
				}
			}

			@Override
			public void remove() throws IOException
			{
				synchronized (CoreList.this)
				{
					testModCount();
					localModCount++;
					modCount++;
					itr.remove();
				}
			}

			@Override
			public void set(Long element) throws IOException
			{
				synchronized (CoreList.this)
				{
					testModCount();
					itr.set(element);
				}
			}

			@Override
			public ListIteratorMove getLastMove()
			{
				synchronized (CoreList.this)
				{
					testModCount();
					return itr.getLastMove();
				}
			}

			@Override
			public Long getLast() throws IOException
			{
				synchronized (CoreList.this)
				{
					testModCount();
					return itr.getLast();
				}
			}

			@Override
			public long nextIndex()
			{
				synchronized (CoreList.this)
				{
					testModCount();
					return itr.nextIndex();
				}
			}

			@Override
			public long prevIndex()
			{
				synchronized (CoreList.this)
				{
					testModCount();
					return itr.prevIndex();
				}
			}

		}

	}
}
