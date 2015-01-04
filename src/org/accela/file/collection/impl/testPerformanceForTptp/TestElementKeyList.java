package org.accela.file.collection.impl.testPerformanceForTptp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.accela.file.collection.ElementList;
import org.accela.file.collection.impl.ElementKeyList;
import org.accela.file.collection.impl.LinkedKeyList;
import org.accela.file.record.RecordPool;

public class TestElementKeyList extends TestElementList<String>
{
	private Map<Long, String> strTable = new HashMap<Long, String>();

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
		return new ElementKeyList<String>(keyList, pool, delegate);
	}

	@Override
	protected String gen(Long i)
	{
		if (null == i)
		{
			return null;
		}
		if (0 == i)
		{
			return null;
		}
		else
		{
			String ret = strTable.get(i);
			if (null == ret)
			{
				ret = genStrRnd(256) + ": " + i;
				strTable.put(i, ret);
			}
			return ret;
		}
	}

	@Override
	protected String genLarge()
	{
		String text = null;
		try
		{
			text = this.readText(dataFile);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			assert (false);
		}
		assert (text.length() > 10000);
		return text;
	}

	@Override
	protected long getHeadKey()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected RecordPool getPool()
	{
		return ((ElementKeyList<String>) list).getPool();
	}

	@Override
	protected long getTailKey()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean isCheckConnectivity()
	{
		return false;
	}

	public static void main(String[] args) throws Exception
	{
		TestElementKeyList t = new TestElementKeyList();
		t.setUp();
		long startTime = System.nanoTime();
		t.testPerformance();
		System.out.println(System.nanoTime() - startTime);
		t.tearDown();
	}

}
