package org.accela.file.test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import junit.framework.TestCase;

import org.accela.file.FFactory;
import org.accela.file.FList;
import org.accela.file.FListIterator;
import org.accela.file.common.BytePersistanceDelegate;
import org.accela.file.common.DataFormatException;
import org.accela.file.common.PersistanceDelegate;
import org.accela.file.common.StructureCorruptedException;

public abstract class TestFList extends TestCase
{
	private Random rand = new Random();

	private File testFile = new File("testFList.txt");

	private FList<String> list = null;

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

	@Override
	public void setUp() throws IOException
	{
		close();

		testFile.delete();
		if (testFile.exists())
		{
			throw new IOException("can't remove testFile");
		}

		reopen(false);
	}

	@Override
	public void tearDown() throws IOException
	{
		close();
	}

	private void open(boolean restore) throws IOException
	{
		if (restore && list != null)
		{
			BytePersistanceDelegate<FList<String>> bd = new BytePersistanceDelegate<FList<String>>(
					FFactory.getInstance(delegate));
			byte[] bytes = bd.writeBytes(list);
			bd = new BytePersistanceDelegate<FList<String>>(FFactory
					.getInstance(delegate));
			try
			{
				this.list = bd.readBytes(bytes);
			}
			catch (DataFormatException ex)
			{
				ex.printStackTrace();
				assert (false);
			}
		}
		else
		{
			list = createFList(testFile);
		}
	}

	protected abstract FList<String> createFList(File file) throws IOException;

	private void close() throws IOException
	{
		if (list != null)
		{
			list.close();
		}
	}

	private void reopen(boolean restore) throws IOException
	{
		close();
		open(restore);
	}

	public void testSimple() throws IOException, StructureCorruptedException,
			DataFormatException
	{
		List<String> elements = new LinkedList<String>();

		// test list methods and reopen
		list.add("hello, my name is flist");
		elements.add("hello, my name is flist");
		consistency(elements);
		reopen(true);
		consistency(elements);

		for (int i = 0; i < 100; i++)
		{
			long idx = rand.nextInt((int) list.size());
			list.insert(idx, "hello world: " + i);
			list.add(null);

			elements.add((int) idx, "hello world: " + i);
			elements.add(null);
		}

		consistency(elements);
		reopen(true);
		consistency(elements);
		list = FFactory.getInstance(delegate).reopen(list);
		consistency(elements);

		// System.out.println("test 1 passed ...");

		for (int i = 0; i < 20; i++)
		{
			long idx = rand.nextInt((int) list.size());
			list.remove(idx);

			elements.remove((int) idx);
		}
		consistency(elements);
		reopen(true);
		consistency(elements);
		list = FFactory.getInstance(delegate).reopen(list);
		consistency(elements);

		// System.out.println("test 2 passed ...");

		for (int i = 0; i < 20; i++)
		{
			long idx = rand.nextInt((int) list.size());
			boolean ret = list.remove(list.get(idx));
			assert (ret);

			ret = elements.remove(elements.get((int) idx));
			assert (ret);
		}
		consistency(elements);
		reopen(true);
		consistency(elements);
		list = FFactory.getInstance(delegate).reopen(list);
		consistency(elements);

		// System.out.println("test 3 passed ...");

		// test iterator
		FListIterator<String> itr = list.iterator(list.size() / 2);
		ListIterator<String> eItr = elements.listIterator(elements.size() / 2);
		while (itr.hasNext())
		{
			String str = itr.next();
			String eStr = eItr.next();
			assert (checkEqual(str, eStr));

			double rnd = rand.nextDouble();
			if (rnd < 0.33)
			{
				itr.add("nice world: " + itr.prevIndex());
				eItr.add("nice world: " + eItr.previousIndex());
			}
			else if (rnd < 0.66)
			{
				itr.set("big world: " + itr.nextIndex());
				eItr.set("big world: " + eItr.nextIndex());
			}
			else
			{
				itr.remove();
				eItr.remove();
			}
		}
		consistency(elements);
		reopen(true);
		consistency(elements);
		list = FFactory.getInstance(delegate).reopen(list);
		consistency(elements);
		assert (elements.size() > 0);

		// System.out.println("test 4 passed ...");

		// test reopen(false)
		reopen(false);
		assert (list.size() == 0);
		elements = new LinkedList<String>();
		consistency(elements);
		list = FFactory.getInstance(delegate).reopen(list);
		consistency(elements);

		// System.out.println("test 5 passed ...");

		// test clear
		list.add("hello, my name is flist");
		elements.add("hello, my name is flist");
		consistency(elements);
		reopen(true);
		consistency(elements);
		list = FFactory.getInstance(delegate).reopen(list);
		consistency(elements);

		for (int i = 0; i < 100; i++)
		{
			long idx = rand.nextInt((int) list.size());
			list.insert(idx, "hello world: " + i);
			list.add(null);

			elements.add((int) idx, "hello world: " + i);
			elements.add(null);
		}
		consistency(elements);
		reopen(true);
		consistency(elements);
		list = FFactory.getInstance(delegate).reopen(list);
		consistency(elements);

		list.clear();
		assert (list.size() == 0);
		elements = new LinkedList<String>();
		consistency(elements);
		list = FFactory.getInstance(delegate).reopen(list);
		consistency(elements);

		// System.out.println("test 6 passed ...");
	}

	private void consistency(List<String> elements) throws IOException,
			StructureCorruptedException, DataFormatException
	{
		assert (list.size() == elements.size());

		for (int i = 0; i < list.size(); i++)
		{
			assert (checkEqual(list.get(i), elements.get(i)));
		}
		int idx = 0;

		FListIterator<String> itr = list.iterator();
		while (itr.hasNext())
		{
			String s = itr.next();
			assert (checkEqual(s, elements.get(idx)));
			idx++;
		}
		assert (idx == elements.size());

		for (int i = 0; i < elements.size(); i++)
		{
			assert (list.contains(elements.get(i)));
		}

		itr = list.iterator(list.size());
		idx = (int) list.size() - 1;
		while (itr.hasPrev())
		{
			long prevIdx = itr.prevIndex();
			assert (checkEqual(itr.prev(), elements.get((int) prevIdx)));
			assert (prevIdx == idx);
			idx--;
		}

		assert (itr.prevIndex() == -1);
		assert (-1 == idx);

		if (elements.size() > 0)
		{
			assert (list.indexOf(elements.get(0)) == 0);
			assert (list.lastIndexOf(elements.get(elements.size() - 1)) == elements
					.size() - 1);
		}
	}

	private boolean checkEqual(String s1, String s2)
	{
		if (s1 != null)
		{
			return s1.equals(s2);
		}
		else
		{
			return null == s2;
		}
	}

	public void testAbleToRemoveTestFile()
	{
		// do nothing
	}

}
