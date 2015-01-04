package org.accela.file.common;

import java.io.IOException;

public interface Clearable
{
	public void clear() throws IOException, StructureCorruptedException;
}
