/****************************************************************************
 * FILE: MaxFilesTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import static org.junit.Assert.*;



import java.nio.file.Path;
import java.nio.file.Paths;



import org.junit.Test;





public class MaxFilesTest
{

	@Test
	public void test()
	{
		CtrailProps props = CtrailProps.getInstance();
		props.setMaxNbrInputFiles(1);
		Path p = Paths.get(".", "src", "test", "resources", "test.log");
		Path p2 = Paths.get(".", "src", "test", "resources", "test2.log");
		String args[] = new String[]
		{
				p.toString(),
				p2.toString()
		};

		CtrailEntryPoint ep = new CtrailEntryPoint(args);
		ep.start(100);
	}

}
