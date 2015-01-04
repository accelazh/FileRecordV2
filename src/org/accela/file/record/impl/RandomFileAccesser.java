package org.accela.file.record.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.accela.file.record.FileAccesser;

public class RandomFileAccesser implements FileAccesser
{
	private File file = null;

	private RandomAccessFile raf = null;

	public RandomFileAccesser(File file) throws FileNotFoundException
	{
		if (null == file)
		{
			throw new IllegalArgumentException("file should not be null");
		}

		this.file = file;
		this.raf = new RandomAccessFile(file, "rw");
	}

	@Override
	public long length() throws IOException
	{
		return raf.length();
	}

	@Override
	public long pos() throws IOException
	{
		return raf.getFilePointer();
	}

	@Override
	public void seek(long pos) throws IOException
	{
		raf.seek(pos);
	}

	@Override
	public void setLength(long newLength) throws IOException
	{
		raf.setLength(newLength);
	}

	@Override
	public boolean readBoolean() throws IOException
	{
		return raf.readBoolean();
	}

	@Override
	public byte readByte() throws IOException
	{
		return raf.readByte();
	}

	@Override
	public char readChar() throws IOException
	{
		return raf.readChar();
	}

	@Override
	public double readDouble() throws IOException
	{
		return raf.readDouble();
	}

	@Override
	public float readFloat() throws IOException
	{
		return raf.readFloat();
	}

	@Override
	public void readFully(byte[] b) throws IOException
	{
		raf.readFully(b);
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException
	{
		raf.readFully(b, off, len);
	}

	@Override
	public int readInt() throws IOException
	{
		return raf.readInt();
	}

	@Override
	public String readLine() throws IOException
	{
		return raf.readLine();
	}

	@Override
	public long readLong() throws IOException
	{
		return raf.readLong();
	}

	@Override
	public short readShort() throws IOException
	{
		return raf.readShort();
	}

	@Override
	public String readUTF() throws IOException
	{
		return raf.readUTF();
	}

	@Override
	public int readUnsignedByte() throws IOException
	{
		return raf.readUnsignedByte();
	}

	@Override
	public int readUnsignedShort() throws IOException
	{
		return raf.readUnsignedShort();
	}

	@Override
	public int skipBytes(int n) throws IOException
	{
		return raf.skipBytes(n);
	}

	@Override
	public void write(int b) throws IOException
	{
		raf.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException
	{
		raf.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		raf.write(b, off, len);
	}

	@Override
	public void writeBoolean(boolean v) throws IOException
	{
		raf.writeBoolean(v);
	}

	@Override
	public void writeByte(int v) throws IOException
	{
		raf.writeByte(v);
	}

	@Override
	public void writeBytes(String s) throws IOException
	{
		raf.writeBytes(s);
	}

	@Override
	public void writeChar(int v) throws IOException
	{
		raf.writeChar(v);
	}

	@Override
	public void writeChars(String s) throws IOException
	{
		raf.writeChars(s);
	}

	@Override
	public void writeDouble(double v) throws IOException
	{
		raf.writeDouble(v);
	}

	@Override
	public void writeFloat(float v) throws IOException
	{
		raf.writeFloat(v);
	}

	@Override
	public void writeInt(int v) throws IOException
	{
		raf.writeInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException
	{
		raf.writeLong(v);
	}

	@Override
	public void writeShort(int v) throws IOException
	{
		raf.writeShort(v);
	}

	@Override
	public void writeUTF(String s) throws IOException
	{
		raf.writeUTF(s);
	}

	@Override
	public void close() throws IOException
	{
		raf.close();
	}

	@Override
	public File getFile()
	{
		return this.file;
	}

}
