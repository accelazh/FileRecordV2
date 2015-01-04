package org.accela.file.record.impl.test;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import org.accela.file.common.PersistanceDelegate;

import junit.framework.TestCase;

public abstract class TestRecordFile extends TestCase
{
	protected File testFile = null;

	protected File dataFile = null;

	protected Random rand = new Random();

	protected static final StringPersistanceDelegate delegate = new StringPersistanceDelegate();

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

	public TestRecordFile(File testFile, File dataFile)
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

	@Override
	protected void setUp() throws Exception
	{
		close();

		testFile.delete();
		if (testFile.exists())
		{
			throw new IOException("can't remove testFile");
		}

		reopen(0);
	}

	@Override
	protected void tearDown() throws Exception
	{
		close();
	}

	protected void reopen(long poolSize) throws IOException
	{
		close();
		open(poolSize);
	}

	protected abstract void close() throws IOException;

	protected abstract void open(long poolSize) throws IOException;

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
