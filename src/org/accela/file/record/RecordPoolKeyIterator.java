package org.accela.file.record;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.accela.file.common.KeyIterator;

public class RecordPoolKeyIterator implements KeyIterator
{
	private RecordPool accesser = null;

	private long curKey = 0;

	public RecordPoolKeyIterator(RecordPool accesser)
	{
		if (null == accesser)
		{
			throw new IllegalArgumentException("accesser should not be null");
		}

		this.accesser = accesser;
	}

	public RecordPool getAccesser()
	{
		return this.accesser;
	}

	@Override
	public boolean hasNext() throws IOException
	{
		while (curKey < accesser.poolSize())
		{
			if (accesser.contains(curKey))
			{
				return true;
			}

			curKey++;
		}

		return false;
	}

	@Override
	public long next() throws IOException
	{
		if (!hasNext())
		{
			throw new NoSuchElementException();
		}

		return curKey++;
	}

}
