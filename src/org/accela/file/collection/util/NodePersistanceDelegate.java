package org.accela.file.collection.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.accela.file.common.DataFormatException;
import org.accela.file.common.PersistanceDelegate;

public class NodePersistanceDelegate implements PersistanceDelegate<Node>
{
	@Override
	public Node read(DataInput in) throws IOException, DataFormatException
	{
		boolean fake = in.readBoolean();
		long prev = in.readLong();
		long next = in.readLong();
		long element = in.readLong();

		try
		{
			Node node = new Node(prev, next, element);
			node.setFake(fake);
			return node;
		}
		catch (IllegalArgumentException ex)
		{
			throw new DataFormatException(ex);
		}
	}

	@Override
	public void write(DataOutput out, Node node) throws IOException
	{
		out.writeBoolean(node.isFake());
		out.writeLong(node.getPrev());
		out.writeLong(node.getNext());
		out.writeLong(node.getElement());
	}

}
