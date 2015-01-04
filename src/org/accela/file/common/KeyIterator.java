package org.accela.file.common;

import java.io.IOException;

public interface KeyIterator
{
	public boolean hasNext() throws IOException, StructureCorruptedException;

	public long next() throws IOException, StructureCorruptedException;
}
