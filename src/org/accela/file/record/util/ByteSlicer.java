package org.accela.file.record.util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

//由于Java泛型不支持基本类型，因此只能针对byte数组写
public class ByteSlicer
{
	public List<byte[]> slice(byte[] array, int slice)
	{
		if (null == array)
		{
			throw new IllegalArgumentException("array should not be null");
		}
		if (slice < 1)
		{
			throw new IllegalArgumentException(
					"slice should not be less than 1");
		}

		List<byte[]> slices = new LinkedList<byte[]>();

		int idx = 0;
		while (idx < array.length)
		{
			slices.add(Arrays.copyOfRange(array, idx, Math.min(idx + slice,
					array.length)));

			idx += slice;
		}

		return slices;
	}

	public byte[] catenate(List<byte[]> slices)
	{
		if (null == slices)
		{
			throw new IllegalArgumentException("slices should not be null");
		}

		int length=0;
		for (byte[] slice : slices)
		{
			if (null == slice)
			{
				continue;
			}

			length+=slice.length;
		}
		
		byte[] array=new byte[length];
		int idx=0;
		for (byte[] slice : slices)
		{
			if (null == slice)
			{
				continue;
			}

			System.arraycopy(slice, 0, array, idx, slice.length);
			idx+=slice.length;
		}

		return array;
	}
}
