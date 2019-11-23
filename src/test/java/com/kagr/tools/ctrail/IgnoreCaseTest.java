/****************************************************************************
 * FILE: CtrailPropsTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.nio.file.Path;
import java.nio.file.Paths;



import org.junit.Assert;
import org.junit.Test;





public class IgnoreCaseTest
{


	@Test
	public void testIgnoreCase()
	{
		System.setProperty("CTRAIL_CFG", Paths.get("./src/test/resources/ctrail-case-ignore.xml").toString());

		Path p = Paths.get(".", "src", "test", "resources", "test.log");
		String args[] = new String[]
		{
				p.toString()
		};

		CtrailEntryPoint ep = new CtrailEntryPoint(args);
		ep.start(10);
	}

}
