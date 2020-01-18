/****************************************************************************
 * FILE: CtrailPropsTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.nio.file.Path;
import java.nio.file.Paths;



import org.junit.Assert;
import org.junit.Test;





public class LastWordMatchTest
{
	@Test
	public void testWithLastWordMatch()
	{
		System.setProperty("CTRAIL_CFG", Paths.get(".", "src", "test", "resources", "configs", "ctrail-last-word-match.xml").toString());
		Path p = Paths.get(".", "src", "test", "resources", "sources", "test.log");
		String args[] = new String[]
		{
				p.toString(),
		};

		CtrailEntryPoint ep = new CtrailEntryPoint(args);
		ep.start(100);
	}
}
