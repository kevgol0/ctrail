/****************************************************************************
 * FILE: FileNameRegExTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import static org.junit.Assert.*;



import java.nio.file.Path;
import java.nio.file.Paths;



import org.junit.Assert;
import org.junit.Test;



import lombok.extern.slf4j.Slf4j;





@Slf4j
public class FileNameRegExTest extends StdCtrTest
{

	@Test
	public void test()
	{
		System.setProperty("CTRAIL_CFG", Paths.get("./src/test/resources/configs/ctrail-file-regex.xml").toString());
		Path p1 = Paths.get(".", "src", "test", "resources", "sources", "test.log");
		Path p2 = Paths.get(".", "src", "test", "resources", "sources", "test2.log");
		Path p4 = Paths.get(".", "src", "test", "resources", "sources", "test40000b.log.0");
		Path p3 = Paths.get(".", "src", "test", "resources", "sources", "ace.log");
		String args[] = new String[]
		{
				p1.toString(), p2.toString(), p3.toString(), p4.toString()
		};

		try
		{
			//replaceStdOut();
			CtrailEntryPoint ep = new CtrailEntryPoint(args);
			ep.start(10);
			//resetStdOut();
			//Assert.assertTrue(compareFiles(Paths.get("./src/test/resources/expected/search-filter.log")));

		}
		catch (Exception ex_)
		{
			_logger.error(ex_.toString(), ex_);
		}
	}

}
