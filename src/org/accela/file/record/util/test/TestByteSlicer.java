package org.accela.file.record.util.test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.accela.file.record.util.ByteSlicer;

import junit.framework.TestCase;

public class TestByteSlicer extends TestCase
{
	public void testSlicing()
	{
		ByteSlicer slicer = new ByteSlicer();
		assert (slicer.slice(new byte[0], 1).equals(new LinkedList<byte[]>()));

		byte[] bytes = new byte[] { 9 };
		List<byte[]> list = slicer.slice(bytes, 1);
		assert (list.size() == 1 && Arrays.equals(list.get(0), bytes));

		bytes = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		list = slicer.slice(bytes, 1);
		assert (list.size() == 10);
		int idx = 0;
		for (byte[] bs : list)
		{
			assert (Arrays.equals(bs, new byte[] { (byte) idx }));
			idx++;
		}

		bytes = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		list = slicer.slice(bytes, 10);
		assert (list.size() == 1);
		assert (Arrays.equals(list.get(0), bytes));

		bytes = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		list = slicer.slice(bytes, 12);
		assert (list.size() == 1);
		assert (Arrays.equals(list.get(0), bytes));
		assert (list.get(0).length == 10);

		bytes = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
		list = slicer.slice(bytes, 3);
		assert (list.size() == 4);
		for (int i = 0; i < list.size(); i++)
		{
			byte[] bs = list.get(i);
			assert (bs.length == 3);
			assert (Arrays.equals(bs, Arrays.copyOfRange(bytes,
					i * 3,
					i * 3 + 3)));
		}

		bytes = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
		list = slicer.slice(bytes, 5);
		assert (list.size() == 3);
		for (int i = 0; i < list.size(); i++)
		{
			byte[] bs = list.get(i);
			if (i < list.size() - 1)
			{
				assert (bs.length == 5);
				assert (Arrays.equals(bs, Arrays.copyOfRange(bytes,
						i * 5,
						i * 5 + 5)));
			}
			else
			{
				assert (bs.length == 3);
				assert (Arrays.equals(bs, new byte[] { 10, 11, 12 }));
			}
		}

		bytes = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, };
		list = slicer.slice(bytes, 5);
		assert (list.size() == 3);
		for (int i = 0; i < list.size(); i++)
		{
			byte[] bs = list.get(i);
			if (i < list.size() - 1)
			{
				assert (bs.length == 5);
				assert (Arrays.equals(bs, Arrays.copyOfRange(bytes,
						i * 5,
						i * 5 + 5)));
			}
			else
			{
				assert (bs.length == 1);
				assert (Arrays.equals(bs, new byte[] { 10 }));
			}
		}

	}

	public void testCatenate()
	{
		ByteSlicer slicer = new ByteSlicer();
		assert (Arrays.equals(slicer.catenate(new LinkedList<byte[]>()),
				new byte[0]));

		List<byte[]> list = new LinkedList<byte[]>();
		list.add(new byte[0]);
		assert (Arrays.equals(slicer.catenate(list), list.get(0)));

		list = new LinkedList<byte[]>();
		list.add(new byte[0]);
		list.add(new byte[0]);
		list.add(new byte[0]);
		list.add(new byte[0]);
		assert (Arrays.equals(slicer.catenate(list), list.get(0)));

		list = new LinkedList<byte[]>();
		list.add(new byte[0]);
		list.add(new byte[] { -1 });
		list.add(new byte[0]);
		list.add(new byte[] { 0, 1 });
		list.add(new byte[0]);
		list.add(new byte[] { 2, 3, 4 });
		list.add(new byte[0]);
		list.add(new byte[] { 5 });
		list.add(new byte[0]);
		list.add(new byte[] { 5 });
		list.add(new byte[0]);
		list.add(new byte[] { 6, 7, 8, 9 });
		list.add(new byte[0]);
		list.add(new byte[] { 10 });
		list.add(new byte[0]);
		assert (Arrays.equals(slicer.catenate(list), new byte[] {
				-1,
				0,
				1,
				2,
				3,
				4,
				5,
				5,
				6,
				7,
				8,
				9,
				10 }));

		list = new LinkedList<byte[]>();
		list.add(new byte[] { 9 });
		assert (Arrays.equals(slicer.catenate(list), list.get(0)));

		list = new LinkedList<byte[]>();
		list.add(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
		assert (Arrays.equals(slicer.catenate(list), list.get(0)));

		list = new LinkedList<byte[]>();
		list.add(new byte[] { 0 });
		list.add(new byte[] { 1 });
		list.add(new byte[] { 2 });
		list.add(new byte[] { 3 });
		list.add(new byte[] { 4 });
		list.add(new byte[] { 5 });
		list.add(new byte[] { 6 });
		list.add(new byte[] { 7 });
		list.add(new byte[] { 8 });
		list.add(new byte[] { 9 });
		assert (Arrays.equals(slicer.catenate(list), new byte[] {
				0,
				1,
				2,
				3,
				4,
				5,
				6,
				7,
				8,
				9 }));

		list = new LinkedList<byte[]>();
		list.add(new byte[] { 0, 1 });
		list.add(new byte[] { 2, 3, 4 });
		list.add(new byte[] { 5 });
		list.add(new byte[] { 6, 7, 8, 9 });
		assert (Arrays.equals(slicer.catenate(list), new byte[] {
				0,
				1,
				2,
				3,
				4,
				5,
				6,
				7,
				8,
				9 }));

	}
}
