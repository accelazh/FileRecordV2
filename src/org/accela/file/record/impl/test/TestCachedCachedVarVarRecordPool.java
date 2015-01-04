package org.accela.file.record.impl.test;

import java.io.IOException;

import org.accela.file.record.RecordPool;

public class TestCachedCachedVarVarRecordPool extends TestRecordPool
{
	public TestCachedCachedVarVarRecordPool()
	{
		super(true, true);
	}

	@Override
	protected RecordPool createRecordPool(int slotSize, long poolSize)
			throws IOException
	{
		return new CachedCachedVarVarRecordPoolFactory().create(slotSize, poolSize);
	}
}
