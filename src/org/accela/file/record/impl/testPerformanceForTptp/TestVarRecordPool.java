package org.accela.file.record.impl.testPerformanceForTptp;

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
	
	public static void main(String[] args) throws Exception
	{
		TestVarRecordPool t=new TestVarRecordPool();
		t.setUp();
		long startTime=System.nanoTime();
		t.testPerformance();
		System.out.println(System.nanoTime()-startTime);
		t.tearDown();
	}

}
