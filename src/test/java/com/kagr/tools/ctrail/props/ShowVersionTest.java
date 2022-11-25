/****************************************************************************
 * FILE: ShowVersionTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static org.junit.Assert.*;



import org.apache.commons.lang3.StringUtils;
import org.junit.Test;





public class ShowVersionTest
{

	@Test
	public void test()
	{
		final String version = CtrailProps.getInstance().getVersion();
		assertNotNull(version);
		assertFalse(StringUtils.isEmpty(version));
	}

}
