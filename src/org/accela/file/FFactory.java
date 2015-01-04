package org.accela.file;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

import org.accela.common.Assertion;
import org.accela.file.collection.ElementList;
import org.accela.file.collection.KeyList;
import org.accela.file.collection.ListElementIterator;
import org.accela.file.collection.impl.ElementKeyList;
import org.accela.file.collection.impl.FullElementList;
import org.accela.file.collection.impl.LinkedKeyList;
import org.accela.file.common.BytePersistanceDelegate;
import org.accela.file.common.DataFormatException;
import org.accela.file.common.PersistanceDelegate;
import org.accela.file.common.StructureCorruptedException;
import org.accela.file.record.FileAccesser;
import org.accela.file.record.RecordArray;
import org.accela.file.record.RecordArrayFactory;
import org.accela.file.record.RecordPool;
import org.accela.file.record.RecordPoolFactory;
import org.accela.file.record.impl.CachedRecordArray;
import org.accela.file.record.impl.PlainRecordArray;
import org.accela.file.record.impl.PlainRecordPool;
import org.accela.file.record.impl.RandomFileAccesser;
import org.accela.file.record.impl.VarRecordPool;

//如何改进FList的创建和保存？在运用了一堆多态后，这真有点难办
public class FFactory<T> implements PersistanceDelegate<FList<T>>
{
	public static <T> FFactory<T> getInstance(PersistanceDelegate<T> delegate)
	{
		return new FFactory<T>(delegate);
	}

	// =======================================================================

	private PersistanceDelegate<T> delegate = null;

	private FFactory(PersistanceDelegate<T> delegate)
	{
		if (null == delegate)
		{
			throw new IllegalArgumentException("delegate should not be null");
		}

		this.delegate = delegate;
	}

	public PersistanceDelegate<T> getDelegate()
	{
		return this.delegate;
	}

	public FList<T> create(File file) throws IOException
	{
		return this.create(file, 128, 0);
	}

	public FList<T> create(File file, int slotSize) throws IOException
	{
		return this.create(file, slotSize, 0);
	}

	public FList<T> create(File file, int slotSize, int cacheCapacity)
			throws IOException
	{
		return create(new FListAttr<T>(file, slotSize, 0, cacheCapacity, 0));
	}

	private FList<T> create(FListAttr<T> attr) throws IOException
	{
		assert (attr != null);

		// create pool
		RecordPool pool = null;
		if (attr.getCacheCapacity() <= 0)
		{
			pool = new VarRecordPoolFactory(attr.getFile()).create(attr
					.getSlotSize(), attr.getPoolSize());
		}
		else
		{
			pool = new CachedVarRecordPoolFactory(attr.getFile(), attr
					.getCacheCapacity()).create(attr.getSlotSize(), attr
					.getPoolSize());
		}

		// create KeyList
		KeyList keyList = new LinkedKeyList(pool, attr.getKey());

		// create ElementList
		ElementList<T> elementList = new ElementKeyList<T>(keyList, pool,
				delegate);

		// create FullElementList
		FullElementList<T> fullElementList = new FullElementList<T>(elementList);

		// create FList
		FList<T> flist = new FListImpl<T>(fullElementList);

		return flist;
	}

	private FListAttr<T> getAttr(FList<T> list)
	{
		assert (list != null);

		// get FList
		FListImpl<T> flist = (FListImpl<T>) list;

		// get FullElementList
		FullElementList<T> fullElementList = flist.getList();

		// get ElementList
		ElementKeyList<T> elementList = (ElementKeyList<T>) fullElementList
				.getList();

		// get KeyList
		LinkedKeyList keyList = (LinkedKeyList) elementList.getList();

		// get VarRecordPool
		VarRecordPool varPool = (VarRecordPool) elementList.getPool();

		// get PlainRecordPool
		PlainRecordPool plainPool = (PlainRecordPool) varPool.getAccesser();

		// get RecordArray
		RecordArray array = plainPool.getAccesser();

		// get CachedRecordArray
		CachedRecordArray cachedArray = null;
		if (array instanceof CachedRecordArray)
		{
			cachedArray = (CachedRecordArray) array;
		}
		else
		{
			cachedArray = null;
		}

		// get PlainRecordArray
		PlainRecordArray plainArray = null;
		if (cachedArray != null)
		{
			plainArray = (PlainRecordArray) cachedArray.getAccesser();
		}
		else
		{
			plainArray = (PlainRecordArray) array;
		}

		// get FileAccesser
		FileAccesser fileAccesser = plainArray.getAccesser();

		// get FListAttr
		return new FListAttr<T>(fileAccesser.getFile(), varPool.getSlotSize(),
				varPool.poolSize(), cachedArray != null ? cachedArray
						.getCapacity() : 0, keyList.getKey());
	}

	@Override
	public FList<T> read(DataInput in) throws IOException, DataFormatException
	{
		try
		{
			File file = new File(in.readUTF());
			int slotSize = in.readInt();
			long poolSize = in.readLong();
			int cacheCapacity = in.readInt();
			long key = in.readLong();
			return create(new FListAttr<T>(file, slotSize, poolSize,
					cacheCapacity, key));
		}
		catch (IllegalArgumentException ex)
		{
			throw new DataFormatException(ex);
		}
	}

	@Override
	public void write(DataOutput out, FList<T> list) throws IOException
	{
		FListAttr<T> attr = getAttr(list);
		out.writeUTF(attr.getFile().getPath());
		out.writeInt(attr.getSlotSize());
		out.writeLong(attr.getPoolSize());
		out.writeInt(attr.getCacheCapacity());
		out.writeLong(attr.getKey());
	}

	public FList<T> reopen(FList<T> list) throws IOException
	{
		if (null == list)
		{
			throw new IllegalArgumentException("list should not be null");
		}

		BytePersistanceDelegate<FList<T>> bd = new BytePersistanceDelegate<FList<T>>(
				this);
		try
		{
			byte[] data = bd.writeBytes(list);
			list.close();
			return bd.readBytes(data);
		}
		catch (DataFormatException ex)
		{
			ex.printStackTrace();
			assert (false) : Assertion.declare();
			return null;
		}
	}

	private static class FListAttr<T>
	{
		private File file = null;
		private int slotSize = 0;
		private long poolSize = 0;
		private int cacheCapacity = 0;
		private long key = 0;

		public FListAttr(File file,
				int slotSize,
				long poolSize,
				int cacheCapacity,
				long key)
		{
			this.file = file;
			this.slotSize = slotSize;
			this.poolSize = poolSize;
			this.cacheCapacity = cacheCapacity;
			this.key = key;
		}

		public File getFile()
		{
			return file;
		}

		public int getSlotSize()
		{
			return slotSize;
		}

		public long getPoolSize()
		{
			return poolSize;
		}

		public int getCacheCapacity()
		{
			return cacheCapacity;
		}

		public long getKey()
		{
			return key;
		}

	}

	private static abstract class AbstractRecordFileFactory
	{
		private File file = null;

		public AbstractRecordFileFactory(File file)
		{
			if (null == file)
			{
				throw new IllegalArgumentException("file should not be null");
			}

			this.file = file;
		}

		public File getFile()
		{
			return this.file;
		}
	}

	private static class PlainRecordArrayFactory extends
			AbstractRecordFileFactory implements RecordArrayFactory
	{

		public PlainRecordArrayFactory(File file)
		{
			super(file);
		}

		@Override
		public RecordArray create(int slotSize) throws IOException
		{
			return new PlainRecordArray(new RandomFileAccesser(getFile()),
					slotSize);
		}

	}

	private static class CachedPlainRecordArrayFactory extends
			AbstractRecordFileFactory implements RecordArrayFactory
	{
		private int cacheCapacity = 0;

		public CachedPlainRecordArrayFactory(File file, int cacheCapacity)
		{
			super(file);

			if (cacheCapacity < 1)
			{
				throw new IllegalArgumentException("illegal cacheCapacity: "
						+ cacheCapacity);
			}

			this.cacheCapacity = cacheCapacity;
		}

		@Override
		public RecordArray create(int slotSize) throws IOException
		{
			return new CachedRecordArray(new PlainRecordArrayFactory(getFile())
					.create(slotSize), this.cacheCapacity);
		}
	}

	private static class PlainRecordPoolFactory extends
			AbstractRecordFileFactory implements RecordPoolFactory
	{
		public PlainRecordPoolFactory(File file)
		{
			super(file);
		}

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new PlainRecordPool(new PlainRecordArrayFactory(getFile()),
					slotSize, poolSize);
		}
	}

	private static class CachedPlainRecordPoolFactory extends
			AbstractRecordFileFactory implements RecordPoolFactory
	{
		private int cacheCapacity = 0;

		public CachedPlainRecordPoolFactory(File file, int cacheCapacity)
		{
			super(file);

			if (cacheCapacity < 1)
			{
				throw new IllegalArgumentException("illegal cacheCapacity: "
						+ cacheCapacity);
			}

			this.cacheCapacity = cacheCapacity;
		}

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new PlainRecordPool(new CachedPlainRecordArrayFactory(
					getFile(), this.cacheCapacity), slotSize, poolSize);
		}
	}

	private static class VarRecordPoolFactory extends AbstractRecordFileFactory
			implements RecordPoolFactory
	{
		public VarRecordPoolFactory(File file)
		{
			super(file);
		}

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new PlainRecordPoolFactory(getFile()),
					slotSize, poolSize);
		}
	}

	private static class CachedVarRecordPoolFactory extends
			AbstractRecordFileFactory implements RecordPoolFactory
	{
		private int cacheCapacity = 0;

		public CachedVarRecordPoolFactory(File file, int cacheCapacity)
		{
			super(file);

			if (cacheCapacity < 1)
			{
				throw new IllegalArgumentException("illegal cacheCapacity: "
						+ cacheCapacity);
			}

			this.cacheCapacity = cacheCapacity;
		}

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new CachedPlainRecordPoolFactory(
					getFile(), this.cacheCapacity), slotSize, poolSize);
		}

	}

	// =====================================================================

	private static class FListImpl<T> implements FList<T>
	{
		private FullElementList<T> list = null;

		public FListImpl(FullElementList<T> list)
		{
			if (null == list)
			{
				throw new IllegalArgumentException("list should not be null");
			}

			this.list = list;
		}

		public FullElementList<T> getList()
		{
			return this.list;
		}

		@Override
		protected void finalize() throws Throwable
		{
			super.finalize();
			if (list != null)
			{
				list.close();
			}
		}

		@Override
		public void add(T element) throws IOException,
				StructureCorruptedException
		{
			list.add(element);
		}

		@Override
		public void clear() throws IOException, StructureCorruptedException
		{
			list.clear();
		}

		@Override
		public void close() throws IOException
		{
			list.close();
		}

		@Override
		public boolean contains(T element) throws IOException,
				StructureCorruptedException
		{
			return list.contains(element);
		}

		@Override
		public boolean equals(Object obj)
		{
			return list.equals(obj);
		}

		@Override
		public void flush() throws IOException
		{
			list.flush();
		}

		@Override
		public T get(long idx) throws IOException, DataFormatException,
				StructureCorruptedException
		{
			return list.get(idx);
		}

		@Override
		public int hashCode()
		{
			return list.hashCode();
		}

		@Override
		public long indexOf(long idx, T element) throws IOException,
				StructureCorruptedException
		{
			return list.indexOf(idx, element);
		}

		@Override
		public long indexOf(T element) throws IOException,
				StructureCorruptedException
		{
			return list.indexOf(element);
		}

		@Override
		public void insert(long idx, T element) throws IOException,
				StructureCorruptedException
		{
			list.insert(idx, element);
		}

		@Override
		public boolean isEmpty()
		{
			return list.isEmpty();
		}

		@Override
		public FListIterator<T> iterator() throws IOException,
				StructureCorruptedException
		{
			return new FListIteratorImpl<T>(list.iterator());
		}

		@Override
		public FListIterator<T> iterator(long idx) throws IOException,
				StructureCorruptedException
		{
			return new FListIteratorImpl<T>(list.iterator(idx));
		}

		@Override
		public long lastIndexOf(T element) throws IOException,
				StructureCorruptedException
		{
			return list.lastIndexOf(element);
		}

		@Override
		public void remove(long idx) throws IOException,
				StructureCorruptedException
		{
			list.remove(idx);
		}

		@Override
		public boolean remove(T element) throws IOException,
				StructureCorruptedException
		{
			return list.remove(element);
		}

		@Override
		public void set(long idx, T element) throws IOException,
				StructureCorruptedException
		{
			list.set(idx, element);
		}

		@Override
		public long size()
		{
			return list.size();
		}

	}

	private static class FListIteratorImpl<T> implements FListIterator<T>
	{
		private ListElementIterator<T> itr = null;

		public FListIteratorImpl(ListElementIterator<T> itr)
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
			itr.add(element);
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
			return itr.next();
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
			return itr.prev();
		}

		@Override
		public long prevIndex()
		{
			return itr.prevIndex();
		}

		@Override
		public void remove() throws IOException
		{
			itr.remove();
		}

		@Override
		public void set(T element) throws IOException
		{
			itr.set(element);
		}
	}
}
