package org.accela.file.record.impl.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{

	public static Test suite()
	{
		TestSuite suite = new TestSuite(
				"Test for org.accela.file.record.impl.test");
		//$JUnit-BEGIN$
		suite.addTestSuite(TestCachedCachedVarRecordPool.class);
		suite.addTestSuite(TestVarRecordPool.class);
		suite.addTestSuite(TestPlainRecordPool.class);
		suite.addTestSuite(TestCrossPool.class);
		suite.addTestSuite(TestCachedCachedVarVarRecordPool.class);
		suite.addTestSuite(TestCachedPlainRecordPool.class);
		suite.addTestSuite(TestCachedCachedRecordArray.class);
		suite.addTestSuite(TestVarVarRecordPool.class);
		suite.addTestSuite(TestPlainRecordArray.class);
		suite.addTestSuite(TestCachedVarRecordPool.class);
		suite.addTestSuite(TestCachedVarVarRecordPool.class);
		suite.addTestSuite(TestCachedCachedPlainRecordPool.class);
		suite.addTestSuite(TestCachedRecordArray.class);
		//$JUnit-END$
		return suite;
	}

}
