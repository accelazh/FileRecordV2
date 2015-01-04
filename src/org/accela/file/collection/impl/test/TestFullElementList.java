package org.accela.file.collection.impl.test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.accela.file.collection.ElementList;
import org.accela.file.collection.ListElementIterator;
import org.accela.file.collection.impl.ElementKeyList;
import org.accela.file.collection.impl.FullElementList;
import org.accela.file.collection.impl.LinkedKeyList;
import org.accela.file.common.DataFormatException;
import org.accela.file.common.StructureCorruptedException;
import org.accela.file.record.RecordPool;

public class TestFullElementList extends TestElementKeyList
{
	private LinkedKeyList keyList = null;

	private RecordPool pool = null;

	@Override
	protected ElementList<String> createElementList(boolean restore)
			throws IOException
	{
		if (null == list || !restore)
		{
			this.pool = new VarRecordPoolFactory().create(128, 0);
			this.keyList = new LinkedKeyList(pool, 0);
		}
		else
		{
			this.pool = new VarRecordPoolFactory().create(128, pool.poolSize());
			this.keyList = new LinkedKeyList(pool, this.keyList.getKey());
		}
		return new FullElementList<String>(new ElementKeyList<String>(keyList,
				pool, delegate));
	}

	private FullElementList<String> getFullList()
	{
		return (FullElementList<String>) list;
	}

	public void testAdditonalMethod() throws IOException, DataFormatException,
			StructureCorruptedException
	{
		final String[] data = new String[] {
				"hello world 1",
				null,
				"hello world 2",
				null,
				"hello world 3",
				null,
				"hello world 4",
				null,
				"hello world 1",
				null,
				"hello world 2",
				null,
				"hello world 3",
				null,
				"hello world 4",
				null };

		List<String> elements = new LinkedList<String>();
		for (int i = 0; i < data.length; i++)
		{
			elements.add(data[i]);
		}

		FullElementList<String> list = getFullList();
		assert (list.equals(list));
		assert (list.hashCode() == 0);

		for (int i = 0; i < data.length; i++)
		{
			list.add(data[i]);
		}
		assert (!list.isEmpty());

		assert (list.equals(list));
		assert (list.hashCode() != 0);

		listTest(elements);
		for (int i = 0; i < data.length; i++)
		{
			assert (list.contains(data[i]));
			assert (list.indexOf(data[i]) == elements.indexOf(data[i]));
			assert (list.lastIndexOf(data[i]) == elements.lastIndexOf(data[i]));
		}

		String target = "hell world 5";
		assert (!list.contains(target));
		assert (!elements.contains(target));
		boolean ret = list.remove(target);
		assert (!ret);
		listTest(elements);

		while (elements.size() > 0)
		{
			target = elements.get(rand.nextInt(elements.size()));
			ret = list.remove(target);
			assert (ret);
			elements.remove(target);
			listTest(elements);
		}
		assert (list.size() == 0);
		assert (list.isEmpty());
	}

	private void listTest(List<String> elements) throws IOException,
			DataFormatException, StructureCorruptedException
	{
		FullElementList<String> list = getFullList();
		ListElementIterator<String> itr = list.iterator();
		for (int i = 0; i < elements.size(); i++)
		{
			assert (isEqual(itr.next(), elements.get(i)));
		}
	}

}
