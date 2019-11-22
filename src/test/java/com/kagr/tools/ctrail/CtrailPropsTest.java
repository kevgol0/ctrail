/****************************************************************************
 * FILE: CtrailPropsTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import org.junit.Assert;
import org.junit.Test;





public class CtrailPropsTest
{

	@Test
	public void test()
	{
		CtrailProps props = CtrailProps.getInstance();
		Assert.assertEquals(props.getMaxNbrInputFiles(), 1000);
	}

}
