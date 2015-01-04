package org.accela.file.record.impl.test;

import java.io.IOException;

import org.accela.file.record.RecordArray;
import org.accela.file.record.impl.CachedRecordArray;
import org.accela.file.record.impl.PlainRecordArray;
import org.accela.file.record.impl.RandomFileAccesser;

public class TestCachedRecordArray extends TestRecordArray
{
	@Override
	protected RecordArray createRecordArray() throws IOException
	{
		return new CachedRecordArray(new PlainRecordArray(
				new RandomFileAccesser(testFile), 128), 1024);
	}
}
