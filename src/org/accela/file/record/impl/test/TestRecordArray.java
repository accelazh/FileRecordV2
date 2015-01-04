package org.accela.file.record.impl.test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.accela.file.common.DataFormatException;
import org.accela.file.record.RecordArray;
import org.accela.file.record.impl.ObjectRecordArray;

public abstract class TestRecordArray extends TestRecordFile
{
	private ObjectRecordArray accesser = null;

	public TestRecordArray()
	{
		super(new File("testRecordArray.txt"), new File("sina.txt"));
	}

	@Override
	protected void close() throws IOException
	{
		if (this.accesser != null)
		{
			this.accesser.close();
		}
	}

	@Override
	protected void open(long poolSize) throws IOException
	{
		this.accesser = new ObjectRecordArray(createRecordArray());
	}

	protected abstract RecordArray createRecordArray() throws IOException;

	public void testExceedsSlotSize()
	{
		for (int i = 0; i < 100; i++)
		{
			try
			{
				accesser.set(0, new byte[accesser.getSlotSize()
						+ 1+rand.nextInt(100)]);
				assert (false);
			}
			catch (Exception ex)
			{
				assert (ex instanceof IllegalArgumentException);
			}
		}
	}

	public void testReadEmpty()
	{
		for (int i = 0; i < 10000; i++)
		{
			try
			{
				accesser.get(i);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				assert (false);
			}
		}
	}

	public void testFault() throws IOException
	{
		RandomAccessFile out = new RandomAccessFile(testFile, "rw");
		out.writeInt(99);
		out.writeUTF("hello world");
		out.seek(Integer.SIZE / Byte.SIZE + accesser.getSlotSize());
		out.writeInt(-10);
		out.writeUTF("hello world2");
		out.close();

		try
		{
			assert (accesser.get(0, delegate).equals("hello world"));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			assert (false);
		}
		assert (accesser.get(1).length == 0);

		out.close();
	}

	public void testReadWriteReopen() throws IOException
	{
		readWriteReopenTest(0);
		readWriteReopenTest(1);
		readWriteReopenTest(accesser.getSlotSize() / 2);
		readWriteReopenTest(accesser.getSlotSize() - 1);
		readWriteReopenTest(accesser.getSlotSize());
	}

	private Map<Long, byte[]> fillBytes(int length, int count)
			throws IOException
	{
		Map<Long, byte[]> bmap = new HashMap<Long, byte[]>();
		for (int i = 0; i < count; i++)
		{
			long idx = rand.nextInt(count * 10);
			byte[] data = genBytes(length);

			accesser.set(idx, data);
			bmap.put(idx, data);
		}

		for (Long idx : bmap.keySet())
		{
			assert (Arrays.equals(accesser.get(idx), bmap.get(idx)));
		}

		return bmap;
	}

	private Map<Long, String> fillStr(int length, int count) throws IOException
	{
		Map<Long, String> smap = new HashMap<Long, String>();
		for (int i = 0; i < count; i++)
		{
			long idx = rand.nextInt(count * 10);
			String data = genStr(Math.max(0, length / 4 - 4));

			accesser.set(idx, data, delegate);
			smap.put(idx, data);
		}

		for (Long idx : smap.keySet())
		{
			try
			{
				assert (smap.get(idx).equals(accesser.get(idx, delegate)));
			}
			catch (DataFormatException ex)
			{
				ex.printStackTrace();
				assert (false);
			}
		}

		return smap;
	}

	private void readWriteReopenTest(int length) throws IOException
	{
		// byte test
		Map<Long, byte[]> bmap = fillBytes(length, 1000);
		reopen(0);
		for (Long idx : bmap.keySet())
		{
			assert (Arrays.equals(accesser.get(idx), bmap.get(idx)));
		}

		// string test
		Map<Long, String> smap = fillStr(length, 1000);
		reopen(0);
		for (Long idx : smap.keySet())
		{
			try
			{
				assert (smap.get(idx).equals(accesser.get(idx, delegate)));
			}
			catch (DataFormatException ex)
			{
				ex.printStackTrace();
				assert (false);
			}
		}
	}

	private Map<Long, byte[]> refillBytes(int length, int count)
			throws IOException
	{
		Map<Long, byte[]> bmap = fillBytes(length, count);
		for (int i = 0; i < count; i++)
		{
			long idx = bmap.keySet().toArray(new Long[0])[rand.nextInt(bmap
					.size())];
			byte[] data = genBytes(length);

			accesser.set(idx, data);
			bmap.put(idx, data);
		}
		for (Long idx : bmap.keySet())
		{
			assert (Arrays.equals(accesser.get(idx), bmap.get(idx)));
		}

		return bmap;
	}

	public void testFlushA() throws IOException
	{
		Map<Long, byte[]> bmap = refillBytes(128, 1000);
		reopen(0);

		for (Long idx : bmap.keySet())
		{
			assert (Arrays.equals(accesser.get(idx), bmap.get(idx)));
		}
	}

	public void testFlushB() throws IOException
	{
		Map<Long, byte[]> bmap = refillBytes(128, 1000);

		this.accesser.flush();
		RecordArray oldAccesser = this.accesser;
		this.accesser = new ObjectRecordArray(createRecordArray());

		for (Long idx : bmap.keySet())
		{
			assert (Arrays.equals(accesser.get(idx), bmap.get(idx)));
		}

		oldAccesser.close();
	}

	public void testFaultRandom() throws IOException
	{
		this.fillFileRandom(accesser.getSlotSize() * 1000);
		testExceedsSlotSize();
		testReadEmpty();
		testFault();
		testReadWriteReopen();
		testFlushA();
		testFlushB();
	}

}
