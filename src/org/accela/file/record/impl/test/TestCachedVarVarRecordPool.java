package org.accela.file.record.impl.test;

import java.io.IOException;

import org.accela.file.record.RecordPool;

public class TestCachedVarVarRecordPool extends TestRecordPool
{
	public TestCachedVarVarRecordPool()
	{
		super(true, true);
	}

	@Override
	protected RecordPool createRecordPool(int slotSize, long poolSize)
			throws IOException
	{
		return new CachedVarVarRecordPoolFactory().create(slotSize, poolSize);
	}
}
