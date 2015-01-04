package org.accela.file.record.impl.test;

import java.io.IOException;

import org.accela.file.record.RecordPool;

public class TestCachedVarRecordPool extends TestRecordPool
{
	public TestCachedVarRecordPool()
	{
		super(true, true);
	}

	@Override
	protected RecordPool createRecordPool(int slotSize, long poolSize)
			throws IOException
	{
		return new CachedVarRecordPoolFactory().create(slotSize, poolSize);
	}
}
