/****************************************************************************
 * FILE: CtrailPropsTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.nio.file.Path;
import java.nio.file.Paths;



import org.junit.Assert;
import org.junit.Test;





public class CaseSensitiveTest
{
	@Test
	public void testWithCaseSensitive()
	{
		System.setProperty("CTRAIL_CFG", Paths.get("./src/test/resources/configs/ctrail-case-sensitive.xml").toString());
		Path p = Paths.get(".", "src", "test", "resources", "sources", "test.log");
		String args[] = new String[]
		{
				p.toString(),
		};

		CtrailEntryPoint ep = new CtrailEntryPoint(args);
		ep.start(100);
	}
}
