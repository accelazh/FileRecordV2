package org.accela.file;

import java.io.IOException;

import org.accela.file.common.DataFormatException;
import org.accela.file.common.StructureCorruptedException;

public interface FListIterator<T>
{
	public boolean hasNext() throws IOException, StructureCorruptedException;

	public T next() throws IOException, DataFormatException,
			StructureCorruptedException;

	public boolean hasPrev() throws IOException, StructureCorruptedException;

	public T prev() throws IOException, DataFormatException,
			StructureCorruptedException;

	public void add(T element) throws IOException;

	public void remove() throws IOException;

	public void set(T element) throws IOException;

	public long nextIndex();

	public long prevIndex();
}
