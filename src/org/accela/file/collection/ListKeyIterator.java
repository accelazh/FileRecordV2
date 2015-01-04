package org.accela.file.collection;

import java.io.IOException;

import org.accela.file.common.StructureCorruptedException;

//之所以继承ListElementIterator<T>，是因为这个迭代器遍历的其实还是链表中装有的元素。
//只不过链表中的元素都是长整型的键
public interface ListKeyIterator extends ListElementIterator<Long>
{
	public Long next() throws IOException, StructureCorruptedException;

	public Long prev() throws IOException, StructureCorruptedException;

	public Long getLast() throws IOException;
}
