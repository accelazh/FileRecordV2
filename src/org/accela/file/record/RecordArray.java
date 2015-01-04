package org.accela.file.record;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import org.accela.file.common.SlotSizable;

public interface RecordArray extends Closeable, Flushable, SlotSizable
{
	public byte[] get(long idx) throws IOException;

	public void set(long idx, byte[] data) throws IOException;

}
