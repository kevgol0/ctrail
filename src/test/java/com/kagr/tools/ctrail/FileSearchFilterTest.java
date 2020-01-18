/****************************************************************************
 * FILE: FileSearchFilterTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;



import org.junit.Assert;
import org.junit.Test;



import lombok.extern.slf4j.Slf4j;





@Slf4j
public class FileSearchFilterTest
{

	@Test
	public void test()
	{
		System.setProperty("CTRAIL_CFG", Paths.get("./src/test/resources/configs/ctrail-file-search-test.xml").toString());
		Path p1 = Paths.get(".", "src", "test", "resources", "sources", "test.log");
		Path p2 = Paths.get(".", "src", "test", "resources", "sources", "test2.log");
		Path p3 = Paths.get(".", "src", "test", "resources", "sources", "test3.log");
		String args[] = new String[]
		{
				p1.toString(), p2.toString(), p3.toString()
		};



		try
		{
			File tempFile = File.createTempFile("filter-unit-test", ".tmp");
			tempFile.deleteOnExit();
			PrintStream out = new PrintStream(tempFile);
			PrintStream origOut = System.out;
			System.setOut(out);

			CtrailEntryPoint ep = new CtrailEntryPoint(args);
			ep.start(10);
			System.setOut(origOut);
			out.flush();


			byte[] expected = Files.readAllBytes(Paths.get("./src/test/resources/expected/ctrail-file-search-expected-result.log"));
			byte[] actual = Files.readAllBytes(tempFile.toPath());
			Assert.assertTrue(Arrays.equals(expected, actual));
		}
		catch (Exception ex_)
		{
			_logger.error(ex_.toString(), ex_);
		}
	}

}
