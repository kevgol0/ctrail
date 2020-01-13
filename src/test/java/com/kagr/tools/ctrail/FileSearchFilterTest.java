/****************************************************************************
 * FILE: FileSearchFilterTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import static org.junit.Assert.*;



import java.nio.file.Path;
import java.nio.file.Paths;



import org.junit.Test;





public class FileSearchFilterTest
{

	@Test
	public void test()
	{
		System.setProperty("CTRAIL_CFG", Paths.get("./src/test/resources/ctrail-file-search-test.xml").toString());
		Path p1 = Paths.get(".", "src", "test", "resources", "test.log");
		Path p2 = Paths.get(".", "src", "test", "resources", "test2.log");
		Path p3 = Paths.get(".", "src", "test", "resources", "test3.log");
		String args[] = new String[]
		{
				p1.toString(), p2.toString(), p3.toString()
		};
		CtrailEntryPoint ep = new CtrailEntryPoint(args);
		ep.start(10);
	}

}
