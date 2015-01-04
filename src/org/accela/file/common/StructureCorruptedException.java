package org.accela.file.common;

/* 当从像链表这样的数据结构中读取数据时，可能发生链表的链接断裂，size=20的链表，
 * 你却无法找到idx>10的元素，这样的错误。这种错误被识别为StructureCorruptedException。
 * 与DataFormatException不同，DataFormatException通常意味着单个数据单元的数据
 * 损坏，但不影响整个数据结构，但StructureCorruptedException则意味着整个数据
 * 结构出现问题，可能无法正常工作。通常重新启动这个数据结构可以自动修复。
 */
public class StructureCorruptedException extends Exception
{
	private static final long serialVersionUID = 1L;
	
	public StructureCorruptedException()
	{
		super();
	}

	public StructureCorruptedException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public StructureCorruptedException(String message)
	{
		super(message);
	}

	public StructureCorruptedException(Throwable cause)
	{
		super(cause);
	}

}
