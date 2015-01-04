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
	 * Half-Safe, Actually�� ��hasNext�����У�������һ��Ҫ���ʵ�Block�Ƿ���ڣ�Ȼ��next�����Ż���ʡ�
	 * �����hasNext�����ļ���next�����ķ���֮�䣬�ļ���ĳ��Ī����ԭ����ģ���
	 * ����һ��Ҫ���ʵ�Block�������ˣ��ͻᵼ��next����������һ��Blockʱ������
	 * accesser.get(long)�������׳�IllegalArgumentException�����³�����Ϊ��� �쳣����ֹ��
	 * ���SafeBlockIterator��������ȫ������ΪIO�����ļ������𻵶����µ���� �쳣���׳����Լ����׵��Զ��޸���
	 * ������Ϣ�ǣ�hasNext�������Զ��޸���������������������Ĵ����������г���
	 * ��ֻҪ�ļ�û����hasNext������next����֮�䱻�޸ģ���ô�Ͳ����׳�����쳣��
	 * ��������׳�IllegalArgumentException�Ĵ�����Ҫ�ǳ���׼ʱ�����ܷ�������û��
	 * ��������д�������ļ���ʱ�򣬼���û�п��ܻᷢ�����ִ���
	 * 
	 * Ϊʲô��һֱ��Ҫ������IO��������µ�����쳣�أ�����ͼ�� IO����-->�ļ�������-->�ɹ���ȡ���ݣ���ȡʱδ����IO���󣩣����Ƕ�ȡ������
	 * �����Ǵ����-->����Щ�����½�����ORM��������Ѱ���������һ��ȵ�-->
	 * ��Ϊ�����������Ϊ�����������׳�IllegalArgumentException��
	 * 
	 * Σ���������档���ȣ�����쳣��һ����д���õĳ����У��ǲ�����׽�ģ���Ϊ����
	 * ��ӳ���ǳ�������Ĵ��󣬱������ڷ��������ʱ�������˸����±ꡣ��Щ������
	 * ������Ӧ���ڲ�����ɵĳ����б�����������쳣һ�㲻Ӧ����try-catch��׽����
	 * Ϊ��ȷ�ĳ���Ӧ���׳�����쳣������������쳣ʱ������ֱ����ֹ����ʾ������ Ϣ��Ҳ�᷽���ҵ����ڵı������
	 * ����IOException�����쳣�Ƿ�����쳣����ʹ��ȷ�ĳ���Ҳ�޷����⣬��ʱ��ʱ��
	 * ���������������������δ��׳�������ʾ�ش������ǡ�IOException����һ�������ǣ�
	 * ������������󣬻���ʱ�����У����ǿ��������������ٷ����ˣ�Ҳ����˵��IOException ����żȻ�ԡ�
	 * ��IOExceptionżȻ�������ļ��д洢�����ݳ���ʱ�����������10����������-10��
	 * ��ô�Ժ��Ҷ�ȡ�����Ķ���-10�������½������������Ҫ�������������ʱ��
	 * ����ͻᵼ�£���ʹ�ҵĳ�������ȷ�ģ�ȴ��Ϊһ��żȻ��IO���󣬶������쳣��ֹ��
	 * �����׳���IllegalArgumentException�������ܷ�ӳ��ʵ�Ĵ���ԭ��
	 * ���⻹��һ�������صĺ������ȻIO������żȻ�����ģ������ҵĳ������Ϊ10���
	 * ��-10����ÿ�Ρ����жԻ���ΪIllegalArgumentException����ֹ�������񵥴���IO
	 * �����������г���Ϳ��������������ٷ����ˡ���Ϊ�ҵĳ����������ļ��е�����
	 * �������������������ᷢ�����ҵĳ����Լ��������ļ��У��ͻᵼ���ҵĳ���
	 * ���Գ����ȶ������У�����һ���������������ļ����᲻��ʹ�ã������ǽ�������� һ�����ݱ�������������������Ȼ�ܹ�����ʹ�á�
	 * 
	 * Ŀǰ�ҵĴ�������ǣ�����ӽ��ļ����ݵĵײ㣬���˭������IO����������ļ�����
	 * �𻵣���ô���׳�DataFormatException������쳣�Ƿ�����쳣�����Ҳ��̳�IOException��
	 * ֮���Բ��̳���IOException������ΪIOException����żȻ�ԣ���DataFormateException һ������
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

		// �ڱ�֤һ����ȷ�Ե�����£��������޸����������������һ���pointer����
		// ����0��Ҳ�����޸���
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
