package org.accela.file.record.impl;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.accela.common.Assertion;
import org.accela.file.common.DataFormatException;
import org.accela.file.record.RecordPool;
import org.accela.file.record.RecordPoolFactory;
import org.accela.file.record.RecordPoolKeyIterator;
import org.accela.file.record.util.ByteSlicer;
import org.accela.file.record.util.LinkedBuffer;
import org.accela.file.record.util.LinkedBufferFactory;

public class VarRecordPool implements RecordPool
{
	private ObjectRecordPool accesser = null;

	private ByteSlicer slicer = new ByteSlicer();

	private int slotSize = 0;

	private LinkedBufferFactory delegate = null;

	private long size = 0;

	public VarRecordPool(RecordPoolFactory factory, int slotSize, long poolSize)
			throws IOException
	{
		if (null == factory)
		{
			throw new IllegalArgumentException("factory should not be null");
		}
		if (slotSize < 1)
		{
			throw new IllegalArgumentException(
					"slotSize should not be less than 1: " + slotSize);
		}
		if (poolSize < 0)
		{
			throw new IllegalArgumentException(
					"poolSize should not be negative");
		}

		this.slotSize = slotSize;
		delegate = new LinkedBufferFactory(getSlotSize());

		RecordPool recordPool = factory.create(this.getBlockSize(), poolSize);
		if (null == recordPool)
		{
			throw new IllegalArgumentException("factory should not create null");
		}
		this.accesser = new ObjectRecordPool(recordPool);

		init();
	}

	public RecordPool getAccesser()
	{
		return this.accesser.getAccesser();
	}

	@Override
	public int getSlotSize()
	{
		assert (this.slotSize > 0) : Assertion.declare();
		return this.slotSize;
	}

	private int getBlockSize()
	{
		return delegate.getBlockSize();
	}

	@Override
	public synchronized void close() throws IOException
	{
		this.accesser.close();
	}

	@Override
	public synchronized void flush() throws IOException
	{
		this.accesser.flush();
	}

	@Override
	public synchronized long size()
	{
		assert (this.size <= accesser.size()) : Assertion.declare();
		assert (this.size >= 0);
		return this.size;
	}

	@Override
	public synchronized long poolSize()
	{
		return accesser.poolSize();
	}

	private long put(LinkedBuffer buffer) throws IOException
	{
		assert (buffer != null) : Assertion.declare();
		return accesser.put(buffer, delegate);
	}

	private LinkedBuffer unsafeGetIfContains(long key) throws IOException,
			DataFormatException
	{
		return accesser.getIfContains(key, delegate);
	}

	private LinkedBuffer tryGet(long key) throws IOException
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

	private boolean setIfContains(long key, LinkedBuffer buffer)
			throws IOException
	{
		assert (buffer != null) : Assertion.declare();
		return accesser.setIfContains(key, buffer, delegate);
	}

	private void init() throws IOException
	{
		long realSize = 0;
		RecordPoolKeyIterator itr = this.accesser.iterator();
		while (itr.hasNext())
		{
			if (contains(itr.next()))
			{
				realSize++;
			}
		}
		this.size = realSize;
	}

	private long putSlices(List<byte[]> slices) throws IOException
	{
		assert (slices != null);
		assert (!slices.contains(null));

		ListIterator<byte[]> itr = slices.listIterator(slices.size());
		long lastKey = 0;
		while (itr.hasPrevious())
		{
			byte[] slice = itr.previous();
			assert (slice.length <= getSlotSize()) : Assertion.declare();

			lastKey = put(delegate.createBuffer(false, lastKey, slice));
		}

		assert (lastKey < poolSize()) : Assertion.declare();
		assert ((lastKey > 0) || (0 == lastKey && slices.size() == 0)) : Assertion
				.declare();
		return lastKey;
	}

	@Override
	public synchronized long put(byte[] data) throws IOException
	{
		if (null == data)
		{
			throw new IllegalArgumentException("data should not be null");
		}

		List<byte[]> slices = slicer.slice(data, getSlotSize());
		if (slices.size() == 0)
		{
			slices.add(new byte[0]);
		}

		long lastKey = putSlices(slices.subList(1, slices.size()));
		lastKey = put(delegate.createBuffer(true, lastKey, slices.get(0)));

		size++;
		assert (lastKey < poolSize()) : Assertion.declare();
		assert (lastKey > 0) : Assertion.declare();
		return lastKey;
	}

	@Override
	public synchronized boolean contains(long key) throws IOException
	{
		LinkedBuffer buffer = null;
		try
		{
			buffer = this.unsafeGetIfContains(key);
		}
		catch (DataFormatException ex)
		{
			return false;
		}

		if (null == buffer)
		{
			return false;
		}

		return buffer.isUsable();
	}

	@Override
	public synchronized boolean setIfContains(long key, byte[] data)
			throws IOException
	{
		if (null == data)
		{
			throw new IllegalArgumentException("data should not be null");
		}

		List<byte[]> slices = slicer.slice(data, getSlotSize());
		if (slices.size() == 0)
		{
			slices.add(new byte[0]);
		}

		SafeBlockIterator blockItr = new SafeBlockIterator(key);
		ListIterator<byte[]> sliceItr = slices.listIterator();
		boolean first = true;
		while (blockItr.hasNext())
		{
			LinkedBuffer buffer = blockItr.next();
			if (first && !buffer.isUsable())
			{
				return false;
			}
			first = false;

			if (!sliceItr.hasNext())
			{
				blockItr.remove();
				continue;
			}

			byte[] slice = sliceItr.next();
			if (!sliceItr.hasNext())
			{
				blockItr
						.set(delegate.createBuffer(buffer.isUsable(), 0, slice));
				continue;
			}

			if (blockItr.hasNext())
			{
				blockItr.set(delegate.createBuffer(buffer.isUsable(), buffer
						.getPointer(), slice));
				continue;
			}

			List<byte[]> remainedSlices = slices.subList(sliceItr.nextIndex(),
					slices.size());
			assert (remainedSlices.size() > 0);
			long remainedKey = putSlices(remainedSlices);
			blockItr.set(delegate.createBuffer(buffer.isUsable(),
					remainedKey,
					slice));

		}
		if (first)
		{
			return false;
		}

		return true;
	}

	@Override
	public synchronized byte[] getIfContains(long key) throws IOException
	{
		List<byte[]> slices = new LinkedList<byte[]>();
		SafeBlockIterator itr = new SafeBlockIterator(key);
		boolean first = true;
		while (itr.hasNext())
		{
			LinkedBuffer buffer = itr.next();
			if (first && !buffer.isUsable())
			{
				return null;
			}
			first = false;
			slices.add(buffer.getData());
		}
		if (first)
		{
			return null;
		}

		return slicer.catenate(slices);
	}

	@Override
	public synchronized boolean removeIfContains(long key) throws IOException
	{
		SafeBlockIterator itr = new SafeBlockIterator(key);
		boolean first = true;
		while (itr.hasNext())
		{
			LinkedBuffer buffer = itr.next();
			if (first && !buffer.isUsable())
			{
				return false;
			}
			first = false;
			itr.remove();
		}
		if (first)
		{
			return false;
		}

		size--;
		assert (!contains(key)) : Assertion.declare();
		return true;
	}

	@Override
	public synchronized void clear() throws IOException
	{
		accesser.clear();
		init();
	}

	@Override
	public synchronized RecordPoolKeyIterator iterator()
	{
		return new RecordPoolKeyIterator(this);
	}

	/*
	 * Half-Safe, Actually。 在hasNext方法中，会检测下一个要访问的Block是否存在，然后next方法才会访问。
	 * 如果在hasNext方法的检测和next方法的访问之间，文件被某种莫名的原因更改，导
	 * 致下一个要访问的Block不存在了，就会导致next方法访问这一个Block时，调用
	 * accesser.get(long)方法而抛出IllegalArgumentException，导致程序因为免检 异常而终止。
	 * 因此SafeBlockIterator并不能完全避免因为IO错误，文件数据损坏而导致的免检 异常的抛出，以及彻底的自动修复。
	 * 但好消息是，hasNext方法有自动修复能力。如果发生了上述的错误，重新运行程序
	 * 后，只要文件没有在hasNext方法和next方法之间被修改，那么就不会抛出免检异常。
	 * 并且这个抛出IllegalArgumentException的错误需要非常凑准时机才能发生。在没有
	 * 其它程序写入数据文件的时候，几乎没有可能会发生这种错误。
	 * 
	 * 为什么我一直想要消除因IO错误而导致的免检异常呢？见下图： IO错误-->文件数据损坏-->成功读取数据（读取时未发生IO错误），但是读取出来的
	 * 数据是错误的-->用这些数据新建对象（ORM），或者寻找链表的下一项，等等-->
	 * 因为错误的数据作为参数，程序抛出IllegalArgumentException。
	 * 
	 * 危害有两方面。首先，免检异常在一个编写良好的程序中，是不被捕捉的，因为它们
	 * 反映的是程序自身的错误，比如你在访问数组的时候，输入了负数下标。这些错误能
	 * 够而且应该在测试完成的程序中被修正。免检异常一般不应该用try-catch捕捉，因
	 * 为正确的程序不应该抛出免检异常，而出现免检异常时，程序直接终止，显示错误信 息，也会方便找到存在的编码错误。
	 * 而向IOException这种异常是非免检异常，即使正确的程序也无法避免，它时不时地
	 * 发生，程序中总是声明何处抛出，并显示地处理它们。IOException的另一个性质是，
	 * 重新启动程序后，换个时间运行，总是可以期望它不会再发生了，也就是说，IOException 带有偶然性。
	 * 当IOException偶然地引发文件中存储的数据出错时，比如我想存10，结果存成了-10。
	 * 那么以后我读取出来的都是-10，用它新建对象，这个对象要求传入参数是正数时，
	 * 结果就会导致：即使我的程序是正确的，却因为一个偶然的IO错误，而导致异常终止，
	 * 并且抛出的IllegalArgumentException根本不能反映真实的错误原因。
	 * 另外还有一个更严重的后果，虽然IO错误是偶然发生的，但是我的程序会因为10变成
	 * 了-10，“每次”运行对会因为IllegalArgumentException而终止，而不像单纯的IO
	 * 错误，重新运行程序就可以期望它不会再发生了。因为我的程序不能容忍文件中的数据
	 * 发生错误，如果这种情况会发生在我的程序自己产生的文件中，就会导致我的程序
	 * 难以长期稳定地运行，并且一旦出错，整个数据文件都会不能使用，而不是仅仅出错的 一段数据被废弃，而其它数据仍然能够正常使用。
	 * 
	 * 目前我的处理策略是，在最接近文件数据的底层，如果谁发现了IO错误引起的文件数据
	 * 损坏，那么就抛出DataFormatException。这个异常是非免检异常，并且不继承IOException。
	 * 之所以不继承于IOException，是因为IOException具有偶然性，而DataFormateException 一旦发生
	 */
	private class SafeBlockIterator
	{
		private long lastKey = 0;

		private long nextKey = 0;

		private LinkedBuffer nextPreload = null;

		private long count = 0;

		private long orignPoolSize = 0;

		public SafeBlockIterator(long key) throws IOException
		{
			this.nextKey = key;
			this.lastKey = 0;
			this.count = 0;
			this.orignPoolSize = poolSize();
			this.nextPreload = null;
		}

		private void preloadNext() throws IOException
		{
			if (null==nextPreload)
			{
				nextPreload = tryGet(nextKey);
			}
		}

		// 在保证一定正确性的情况下，尽量少修复。比如这里，如果最后一块的pointer属性
		// 不是0，也不会修复它
		private boolean hasNext() throws IOException
		{
			preloadNext();
			return nextPreload != null && count < orignPoolSize;
		}

		public LinkedBuffer next() throws IOException
		{
			if (!hasNext())
			{
				throw new NoSuchElementException();
			}

			LinkedBuffer buffer = nextPreload;
			assert (buffer != null);

			lastKey = nextKey;
			nextKey = buffer.getPointer();
			nextPreload = null;
			count++;
			assert (lastKey > 0 && lastKey < poolSize()) : Assertion.declare();
			return buffer;
		}

		public void remove() throws IOException
		{
			if (0 == lastKey)
			{
				throw new NoSuchElementException();
			}

			accesser.removeIfContains(lastKey);
			lastKey = 0;
			nextPreload = null;
		}

		public void set(LinkedBuffer buffer) throws IOException
		{
			if (0 == lastKey)
			{
				throw new NoSuchElementException();
			}
			if (null == buffer)
			{
				throw new IllegalArgumentException("buffer should not be null");
			}

			VarRecordPool.this.setIfContains(lastKey, buffer);
		}
	}

}
