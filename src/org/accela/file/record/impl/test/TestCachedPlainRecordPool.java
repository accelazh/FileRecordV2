package org.accela.file.record.impl.test;

import java.io.IOException;

import org.accela.file.record.RecordPool;

public class TestCachedPlainRecordPool extends TestRecordPool
{
	public TestCachedPlainRecordPool()
	{
		super(false, false);
	}

	@Override
	protected RecordPool createRecordPool(int slotSize, long poolSize)
			throws IOException
	{
		return new CachedPlainRecordPoolFactory().create(slotSize, poolSize);
	}
}
