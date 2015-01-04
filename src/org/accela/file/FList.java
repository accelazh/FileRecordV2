package org.accela.file;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import org.accela.file.common.Clearable;
import org.accela.file.common.DataFormatException;
import org.accela.file.common.Sizable;
import org.accela.file.common.StructureCorruptedException;

public interface FList<T> extends Closeable, Flushable, Sizable, Clearable
{
	public void insert(long idx, T element) throws IOException,
			StructureCorruptedException;

	public void remove(long idx) throws IOException,
			StructureCorruptedException;

	public void set(long idx, T element) throws IOException,
			StructureCorruptedException;

	public T get(long idx) throws IOException, DataFormatException,
			StructureCorruptedException;

	public long indexOf(long idx, T element) throws IOException,
			StructureCorruptedException;

	public FListIterator<T> iterator(long idx) throws IOException,
			StructureCorruptedException;

	public FListIterator<T> iterator() throws IOException,
			StructureCorruptedException;

	public void add(T element) throws IOException, StructureCorruptedException;

	public boolean contains(T element) throws IOException,
			StructureCorruptedException;

	public boolean remove(T element) throws IOException,
			StructureCorruptedException;

	public boolean isEmpty();

	public long indexOf(T element) throws IOException,
			StructureCorruptedException;

	public long lastIndexOf(T element) throws IOException,
			StructureCorruptedException;
}
