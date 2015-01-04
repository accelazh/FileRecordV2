package org.accela.file.record.impl.test;

import java.io.IOException;

import org.accela.file.record.RecordPool;

public class TestCachedCachedPlainRecordPool extends TestRecordPool
{

	public TestCachedCachedPlainRecordPool()
	{
		super(false, false);
	}

	@Override
	protected RecordPool createRecordPool(int slotSize, long poolSize)
			throws IOException
	{
		return new CachedCachedPlainRecordPoolFactory().create(slotSize, poolSize);
	}

}
