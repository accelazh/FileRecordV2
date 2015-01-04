package org.accela.file.record.impl.test;

import java.io.IOException;

import org.accela.file.record.RecordPool;

public class TestVarVarRecordPool extends TestRecordPool
{
	public TestVarVarRecordPool()
	{
		super(true, true);
	}

	@Override
	protected RecordPool createRecordPool(int slotSize, long poolSize)
			throws IOException
	{
		return new VarVarRecordPoolFactory().create(slotSize, poolSize);
	}
}
