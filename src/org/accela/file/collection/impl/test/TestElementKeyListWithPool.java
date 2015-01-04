package org.accela.file.collection.impl.test;

import java.io.File;
import java.io.IOException;

import org.accela.file.collection.ElementList;
import org.accela.file.collection.impl.ElementKeyList;
import org.accela.file.collection.impl.LinkedKeyList;
import org.accela.file.record.RecordArray;
import org.accela.file.record.RecordArrayFactory;
import org.accela.file.record.RecordPool;
import org.accela.file.record.RecordPoolFactory;
import org.accela.file.record.impl.PlainRecordArray;
import org.accela.file.record.impl.PlainRecordPool;
import org.accela.file.record.impl.RandomFileAccesser;
import org.accela.file.record.impl.VarRecordPool;

public class TestElementKeyListWithPool extends TestElementKeyList
{
	private LinkedKeyList keyList = null;

	private RecordPool listPool = null;

	private RecordPool anotherPool = null;

	@Override
	protected ElementList<String> createElementList(boolean restore)
			throws IOException
	{
		if (null == list || !restore)
		{
			this.listPool = new VarRecordPoolFactory().create(128, 0);
			this.keyList = new LinkedKeyList(this.listPool, 0);
			this.anotherPool = new AnotherVarRecordPoolFactory().create(60, 0);
		}
		else
		{
			this.listPool = new VarRecordPoolFactory().create(128,
					this.listPool.poolSize());
			this.keyList = new LinkedKeyList(this.listPool, this.keyList
					.getKey());
			this.anotherPool = new AnotherVarRecordPoolFactory().create(60,
					this.anotherPool.poolSize());
		}
		return new ElementKeyList<String>(keyList, anotherPool, delegate);
	}

	private class AnotherRecordArrayFactory implements RecordArrayFactory
	{

		@Override
		public RecordArray create(int slotSize) throws IOException
		{
			return new PlainRecordArray(new RandomFileAccesser(new File(
					"testAnotherPool.txt")), slotSize);
		}

	}

	private class AnotherPlainRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new PlainRecordPool(new AnotherRecordArrayFactory(),
					slotSize, poolSize);
		}

	}

	private class AnotherVarRecordPoolFactory implements RecordPoolFactory
	{

		@Override
		public RecordPool create(int slotSize, long poolSize)
				throws IOException
		{
			return new VarRecordPool(new AnotherPlainRecordPoolFactory(),
					slotSize, poolSize);
		}

	}
}
