/****************************************************************************
 * FILE: MaxFilesTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.nio.file.Path;
import java.nio.file.Paths;



import org.junit.Test;



import com.kagr.tools.ctrail.props.CtrailProps;





public class MaxFilesTest
{

	@Test
	public void test()
	{
		CtrailProps props = CtrailProps.getInstance();
		props.setMaxNbrInputFiles(1);
		Path p = Paths.get(".", "src", "test", "resources", "sources", "test.log");
		Path p2 = Paths.get(".", "src", "test", "resources", "sources", "test2.log");
		String args[] = new String[]
		{
				p.toString(),
				p2.toString()
		};

		CtrailEntryPoint ep = new CtrailEntryPoint(args);
		ep.start(100);
	}

}
