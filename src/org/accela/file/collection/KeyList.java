package org.accela.file.collection;

import java.io.IOException;

import org.accela.file.common.StructureCorruptedException;
import org.accela.file.record.RecordPool;

public interface KeyList extends ElementList<Long>
{
	public Long get(long idx) throws IOException, StructureCorruptedException;

	public ListKeyIterator iterator(long idx) throws IOException, StructureCorruptedException;

	public long getKey();
	
	public RecordPool getAccesser();
}
