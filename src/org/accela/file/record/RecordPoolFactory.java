package org.accela.file.record;

import java.io.IOException;

public interface RecordPoolFactory
{
	public RecordPool create(int slotSize, long poolSize) throws IOException;
}
