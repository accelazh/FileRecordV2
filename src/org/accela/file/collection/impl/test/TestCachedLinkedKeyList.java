package org.accela.file.collection.impl.test;

import java.io.IOException;

import org.accela.file.collection.ElementList;
import org.accela.file.collection.impl.LinkedKeyList;

public class TestCachedLinkedKeyList extends TestLinkedKeyList
{
	@Override
	protected ElementList<Long> createElementList(boolean restore)
			throws IOException
	{
		long poolSize = this.list != null ? ((LinkedKeyList) list)
				.getAccesser().poolSize() : 0;
		long key = this.list != null ? ((LinkedKeyList) list).getKey() : 0;
		if (!restore)
		{
			poolSize = 0;
			key = 0;
		}

		return new LinkedKeyList(new CachedVarRecordPoolFactory().create(128,
				poolSize), key);
	}

}
