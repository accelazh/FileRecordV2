package org.accela.file.record;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

public interface FileAccesser extends DataInput, DataOutput, Closeable
{
	public void seek(long pos) throws IOException;
	
	public long pos() throws IOException;
	
	public long length() throws IOException;
	
	public void setLength(long newLength) throws IOException;
	
	public File getFile();
}

