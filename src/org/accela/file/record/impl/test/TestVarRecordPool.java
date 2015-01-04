package org.accela.file.record.impl.test;

import java.io.IOException;

import org.accela.file.record.RecordPool;

public class TestVarRecordPool extends TestRecordPool
{
	public TestVarRecordPool()
	{
		super(true, true);
	}

	@Override
	protected RecordPool createRecordPool(int slotSize, long poolSize)
			throws IOException
	{
		return new VarRecordPoolFactory().create(slotSize, poolSize);
	}

}
