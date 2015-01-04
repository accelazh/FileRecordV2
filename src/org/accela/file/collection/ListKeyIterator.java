package org.accela.file.collection;

import java.io.IOException;

import org.accela.file.common.StructureCorruptedException;

//֮���Լ̳�ListElementIterator<T>������Ϊ�����������������ʵ����������װ�е�Ԫ�ء�
//ֻ���������е�Ԫ�ض��ǳ����͵ļ�
public interface ListKeyIterator extends ListElementIterator<Long>
{
	public Long next() throws IOException, StructureCorruptedException;

	public Long prev() throws IOException, StructureCorruptedException;

	public Long getLast() throws IOException;
}
