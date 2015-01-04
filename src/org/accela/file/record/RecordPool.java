package org.accela.file.record;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import org.accela.file.common.Clearable;
import org.accela.file.common.PoolSizable;
import org.accela.file.common.Sizable;
import org.accela.file.common.SlotSizable;

public interface RecordPool extends Closeable, Flushable, SlotSizable, Sizable,
		PoolSizable, Clearable
{
	public void clear() throws IOException;
	
	// ��Ҫ�淶��Ҫ�󷵻ص�key>0����key<poolSize��key==0��ͳһ������null��ַ
	public long put(byte[] data) throws IOException;

	// ���κ�keyֵ��������ֵ���ܹ��ж�
	public boolean contains(long key) throws IOException;

	// if not contains key, then return false
	public boolean setIfContains(long key, byte[] data) throws IOException;

	// if not contains key, then return null
	public byte[] getIfContains(long key) throws IOException;

	// if not contains key, then return false
	public boolean removeIfContains(long key) throws IOException;

	public RecordPoolKeyIterator iterator();
}
