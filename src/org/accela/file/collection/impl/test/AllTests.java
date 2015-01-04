package org.accela.file.collection.impl.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{

	public static Test suite()
	{
		TestSuite suite = new TestSuite(
				"Test for org.accela.file.collection.impl.test");
		//$JUnit-BEGIN$
		suite.addTestSuite(TestLinkedKeyList.class);
		suite.addTestSuite(TestFullElementList.class);
		suite.addTestSuite(TestCachedElementKeyList.class);
		suite.addTestSuite(TestElementKeyList.class);
		suite.addTestSuite(TestElementKeyListWithPool.class);
		suite.addTestSuite(TestCachedElementKeyListWithPool.class);
		suite.addTestSuite(TestCachedLinkedKeyList.class);
		//$JUnit-END$
		return suite;
	}

}
