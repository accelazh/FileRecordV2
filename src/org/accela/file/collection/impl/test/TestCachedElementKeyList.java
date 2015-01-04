package org.accela.file.collection.impl.test;

import java.io.IOException;

import org.accela.file.collection.ElementList;
import org.accela.file.collection.impl.ElementKeyList;
import org.accela.file.collection.impl.LinkedKeyList;
import org.accela.file.record.RecordPool;

public class TestCachedElementKeyList extends TestElementKeyList
{
	private LinkedKeyList keyList = null;

	private RecordPool pool = null;

	@Override
	protected ElementList<String> createElementList(boolean restore)
			throws IOException
	{
		if (null == list || !restore)
		{
			this.pool = new CachedVarRecordPoolFactory().create(128, 0);
			this.keyList = new LinkedKeyList(pool, 0);
		}
		else
		{
			this.pool=new CachedVarRecordPoolFactory().create(128, this.pool.poolSize());
			this.keyList=new LinkedKeyList(this.pool, this.keyList.getKey());
		}
		
		return new ElementKeyList<String>(keyList, pool, delegate);
	}

}
