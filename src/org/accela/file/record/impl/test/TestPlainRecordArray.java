package org.accela.file.record.impl.test;

import java.io.FileNotFoundException;

import org.accela.file.record.RecordArray;
import org.accela.file.record.impl.PlainRecordArray;
import org.accela.file.record.impl.RandomFileAccesser;

public class TestPlainRecordArray extends TestRecordArray
{
	@Override
	protected RecordArray createRecordArray() throws FileNotFoundException
	{
		return new PlainRecordArray(new RandomFileAccesser(testFile), 128);
	}
}
