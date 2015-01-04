package org.accela.file.record;

import java.io.IOException;

public interface RecordArrayFactory
{
	public RecordArray create(int slotSize) throws IOException;
}
