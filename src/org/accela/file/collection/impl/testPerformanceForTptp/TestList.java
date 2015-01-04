package org.accela.file.collection.impl.testPerformanceForTptp;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import org.accela.file.collection.util.Node;
import org.accela.file.collection.util.NodePersistanceDelegate;
import org.accela.file.common.BytePersistanceDelegate;
import org.accela.file.common.PersistanceDelegate;
import org.accela.file.record.RecordArray;
import org.accela.file.record.RecordArrayFactory;
import org.accela.file.record.RecordPool;
import org.accela.file.record.RecordPoolFactory;
import org.accela.file.record.impl.CachedRecordArray;
import org.accela.file.record.impl.PlainRecordArray;
import org.accela.file.record.impl.PlainRecordPool;
import org.accela.file.record.impl.RandomFileAccesser;
import org.accela.file.record.impl.VarRecordPool;

public abstract class TestList
{
	protected File testFile = null;

	protected File dataFile = null;

	protected Random rand = new Random();

	protected static final StringPersistanceDelegate delegate = new StringPersistanceDelegate();

	protected BytePersistanceDelegate<Node> nodeDelegate = new BytePersistanceDelegate<Node>(
			new NodePersistanceDelegate());

	private static class StringPersistanceDelegate implements
			PersistanceDelegate<String>
	{
		@Override
		public String read(DataInput in) throws IOException
		{
			return in.readUTF();
		}

		@Override
		public void write(DataOutput out, String object) throws IOException
		{
			out.writeUTF(object);
		}
	}

	protected class PlainRecordArrayFactory implements RecordArrayFactory
	{

		@Override
		public RecordArray create(int slotSize) throws IOException
		{
			return new PlainRecordArray(new RandomFileAccesser(testFile),
					slotSize);
		}

	}

	protected class CachedPlainRecordArrayFactory implements RecordArrayFactory
	{

		@Override
		public RecordArray create(int slotSize) throws IOException
		{
			return new CachedRecordArray(new PlainRecordArrayFactory()
					.create(slotSize), 128);
		}

	}

	protected class CachedCachedPlainRecordArrayFactory implements
			RecordArrayFactory
	{

		@Override
		public RecordArray create(int slotSize) throws IOException
		{
			return new CachedRecordArray(new CachedPlainRecordArrayFactory()
					.create(slotSize), 128);
		}

	}

	protected class PlainRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new PlainRecordPool(new PlainRecordArrayFactory(), slotSize,
					poolSize);
		}

	}

	protected class CachedPlainRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new PlainRecordPool(new CachedPlainRecordArrayFactory(),
					slotSize, poolSize);
		}

	}

	protected class CachedCachedPlainRecordPoolFactory implements
			RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new PlainRecordPool(
					new CachedCachedPlainRecordArrayFactory(), slotSize,
					poolSize);
		}

	}

	protected class VarRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new PlainRecordPoolFactory(), slotSize,
					poolSize);
		}

	}

	protected class VarVarRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new VarRecordPoolFactory(), slotSize,
					poolSize);
		}

	}

	protected class CachedVarRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new CachedPlainRecordPoolFactory(),
					slotSize, poolSize);
		}

	}

	protected class CachedCachedVarRecordPoolFactory implements
			RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new CachedCachedPlainRecordPoolFactory(),
					slotSize, poolSize);
		}

	}

	protected class CachedVarVarRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new CachedVarRecordPoolFactory(),
					slotSize, poolSize);
		}

	}

	protected class CachedCachedVarVarRecordPoolFactory implements
			RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new CachedCachedVarRecordPoolFactory(),
					slotSize, poolSize);
		}

	}

	public TestList(File testFile, File dataFile)
	{
		if (null == testFile)
		{
			throw new IllegalArgumentException("testFile should not be null");
		}
		if (null == dataFile)
		{
			throw new IllegalArgumentException("dataFile should not be null");
		}

		this.testFile = testFile;
		this.dataFile = dataFile;
	}

	protected void setUp() throws Exception
	{
		close();

		testFile.delete();
		if (testFile.exists())
		{
			throw new IOException("can't remove testFile");
		}

		reopen(false);
	}

	protected void tearDown() throws Exception
	{
		close();
	}

	protected void reopen(boolean restore) throws IOException
	{
		close();
		open(restore);
	}

	protected abstract void close() throws IOException;

	protected abstract void open(boolean restore) throws IOException;

	protected String readText(File file) throws IOException
	{
		StringBuffer buf = new StringBuffer();
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = in.readLine()) != null)
		{
			buf.append(line);
			buf.append("\n");
		}

		return buf.toString();
	}

	protected byte[] genBytes(int length)
	{
		byte[] bs = new byte[length];
		rand.nextBytes(bs);

		return bs;
	}

	protected byte[] genBytesRnd(int length)
	{
		return genBytes(rand.nextInt(length + 1));
	}

	protected String genStr(int length)
	{
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < length; i++)
		{
			buf.append((char) rand.nextInt());
		}

		String str = buf.toString();
		assert (str.length() == length);

		return str;
	}

	protected String genStrRnd(int length)
	{
		return genStr(rand.nextInt(length + 1));
	}

	protected void fillFileRandom(long length) throws IOException
	{
		final int BLOCK_LENGTH = 1024;

		RandomAccessFile raf = new RandomAccessFile(testFile, "rw");
		for (long i = 0; i < length / BLOCK_LENGTH + 1; i++)
		{
			raf.write(genBytes(BLOCK_LENGTH));
		}

		raf.close();
	}
}
