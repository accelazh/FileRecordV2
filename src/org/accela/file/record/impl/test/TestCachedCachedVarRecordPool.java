package org.accela.file.record.impl.test;

import java.io.IOException;

import org.accela.file.record.RecordPool;

public class TestCachedCachedVarRecordPool extends TestRecordPool
{
	public TestCachedCachedVarRecordPool()
	{
		super(true, true);
	}

	@Override
	protected RecordPool createRecordPool(int slotSize, long poolSize)
			throws IOException
	{
		return new CachedCachedVarRecordPoolFactory().create(slotSize, poolSize);
	}

}
