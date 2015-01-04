package org.accela.file.test;

import java.io.File;
import java.io.IOException;

import org.accela.file.FFactory;
import org.accela.file.FList;

public class TestPlainFList extends TestFList
{

	@Override
	protected FList<String> createFList(File file) throws IOException
	{
		return FFactory.getInstance(delegate).create(file, 64);
	}

}
