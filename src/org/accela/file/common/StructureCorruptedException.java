package org.accela.file.common;

/* �������������������ݽṹ�ж�ȡ����ʱ�����ܷ�����������Ӷ��ѣ�size=20������
 * ��ȴ�޷��ҵ�idx>10��Ԫ�أ������Ĵ������ִ���ʶ��ΪStructureCorruptedException��
 * ��DataFormatException��ͬ��DataFormatExceptionͨ����ζ�ŵ������ݵ�Ԫ������
 * �𻵣�����Ӱ���������ݽṹ����StructureCorruptedException����ζ����������
 * �ṹ�������⣬�����޷�����������ͨ����������������ݽṹ�����Զ��޸���
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
