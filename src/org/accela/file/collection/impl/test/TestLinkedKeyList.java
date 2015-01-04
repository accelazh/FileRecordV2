package org.accela.file.collection.impl.test;

import java.io.IOException;
import java.lang.reflect.Field;

import org.accela.file.collection.ElementList;
import org.accela.file.collection.impl.LinkedKeyList;
import org.accela.file.record.RecordPool;

public class TestLinkedKeyList extends TestElementList<Long>
{
	@Override
	protected ElementList<Long> createElementList(boolean restore)
			throws IOException
	{
		long poolSize = this.list != null ? ((LinkedKeyList) list)
				.getAccesser().poolSize() : 0;
		long key = this.list != null ? ((LinkedKeyList) list).getKey() : 0;
		if (!restore)
		{
			poolSize = 0;
			key = 0;
		}

		return new LinkedKeyList(new VarRecordPoolFactory().create(128,
				poolSize), key);
	}

	@Override
	protected Long gen(Long i)
	{
		return i;
	}

	@Override
	protected Long genLarge()
	{
		return Long.MAX_VALUE;
	}

	@Override
	protected long getHeadKey()
	{
		long ret = 0;
		try
		{
			Field f = list.getClass().getDeclaredField("list");
			f.setAccessible(true);
			Object obj = f.get(list);
			f = obj.getClass().getDeclaredField("head");
			f.setAccessible(true);
			ret = (Long) f.get(obj);
		}
		catch (IllegalArgumentException ex)
		{
			ex.printStackTrace();
			assert (false);
		}
		catch (SecurityException ex)
		{
			ex.printStackTrace();
			assert (false);
		}
		catch (IllegalAccessException ex)
		{
			ex.printStackTrace();
			assert (false);
		}
		catch (NoSuchFieldException ex)
		{
			ex.printStackTrace();
			assert (false);
		}
		return ret;
	}

	@Override
	protected RecordPool getPool()
	{
		return ((LinkedKeyList) list).getAccesser();
	}

	@Override
	protected long getTailKey()
	{
		long ret = 0;
		try
		{
			Field f = list.getClass().getDeclaredField("list");
			f.setAccessible(true);
			Object obj = f.get(list);
			f = obj.getClass().getDeclaredField("tail");
			f.setAccessible(true);
			ret = (Long) f.get(obj);
		}
		catch (IllegalArgumentException ex)
		{
			ex.printStackTrace();
			assert (false);
		}
		catch (SecurityException ex)
		{
			ex.printStackTrace();
			assert (false);
		}
		catch (IllegalAccessException ex)
		{
			ex.printStackTrace();
			assert (false);
		}
		catch (NoSuchFieldException ex)
		{
			ex.printStackTrace();
			assert (false);
		}
		return ret;
	}

	@Override
	protected boolean isCheckConnectivity()
	{
		return true;
	}

}
