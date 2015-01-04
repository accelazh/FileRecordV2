package org.accela.file.record.impl.testPerformanceForTptp;

import java.io.IOException;

import org.accela.file.record.RecordPool;

public class TestPlainRecordPool extends TestRecordPool
{
	public TestPlainRecordPool()
	{
		super(false, false);
	}

	@Override
	protected RecordPool createRecordPool(int slotSize, long poolSize)
			throws IOException
	{
		return new PlainRecordPoolFactory().create(slotSize, poolSize);
	}
	
	public static void main(String[] args) throws Exception
	{
		TestPlainRecordPool t=new TestPlainRecordPool();
		t.setUp();
		long startTime=System.nanoTime();
		t.testPerformance();
		System.out.println(System.nanoTime()-startTime);
		t.tearDown();
	}
	
}
