package org.accela.file.collection;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import org.accela.file.common.Clearable;
import org.accela.file.common.DataFormatException;
import org.accela.file.common.Sizable;
import org.accela.file.common.StructureCorruptedException;

public interface ElementList<T> extends Closeable, Flushable, Sizable, Clearable
{
	public void insert(long idx, T element) throws IOException, StructureCorruptedException;

	public void remove(long idx) throws IOException, StructureCorruptedException;

	public void set(long idx, T element) throws IOException, StructureCorruptedException;

	public T get(long idx) throws IOException, DataFormatException, StructureCorruptedException;

	public long indexOf(long idx, T element) throws IOException, StructureCorruptedException;

	public ListElementIterator<T> iterator(long idx) throws IOException, StructureCorruptedException;
}
