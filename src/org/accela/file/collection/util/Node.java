package org.accela.file.collection.util;

public class Node
{
	private boolean fake=false;
	
	private long prev = 0;

	private long next = 0;

	private long element = 0;

	public Node(long prev, long next, long element)
	{
		if (prev < 0)
		{
			throw new IllegalArgumentException("prev should not be negative");
		}
		if (next < 0)
		{
			throw new IllegalArgumentException("next should not be negative");
		}
		if (element < 0)
		{
			throw new IllegalArgumentException("element should not be negative");
		}

		this.fake=false;
		this.prev = prev;
		this.next = next;
		this.element = element;
	}

	public boolean isFake()
	{
		return fake;
	}

	public void setFake(boolean fake)
	{
		this.fake = fake;
	}

	public long getPrev()
	{
		return prev;
	}

	public void setPrev(long prev)
	{
		if (prev < 0)
		{
			throw new IllegalArgumentException("prev should not be negative");
		}

		this.prev = prev;
	}

	public long getNext()
	{
		return next;
	}

	public void setNext(long next)
	{
		if (next < 0)
		{
			throw new IllegalArgumentException("next should not be negative");
		}

		this.next = next;
	}

	public long getElement()
	{
		return element;
	}

	public void setElement(long element)
	{
		if (element < 0)
		{
			throw new IllegalArgumentException("element should not be negative");
		}

		this.element = element;
	}

}
