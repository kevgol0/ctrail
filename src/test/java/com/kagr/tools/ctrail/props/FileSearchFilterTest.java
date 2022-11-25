/****************************************************************************
 * FILE: FileSearchFilterTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



import java.nio.file.Paths;
import java.util.Hashtable;



import org.junit.Test;



import lombok.extern.slf4j.Slf4j;





@Slf4j
public class FileSearchFilterTest
{

	@Test
	public void testBasic()
	{
		FileSearchFilter fsf = new FileSearchFilter("test.log", false);
		assertTrue(fsf.doesMatchFilename("/tmp/aa/bb/cc/dd/test.log"));
		assertFalse(fsf.doesMatchFilename("/tmp/aa/bb/cc/test.log/tt"));
	}





	@Test
	public void testRegexFileName()
	{
		FileSearchFilter fsf = new FileSearchFilter("test*.log", false);
		assertTrue(fsf.doesMatchFilename("/tmp/aa/bb/cc/dd/test12.log"));
		assertFalse(fsf.doesMatchFilename("/tmp/aa/bb/cc/test12.log/tt"));
	}





	@Test
	public void testExcludeFilter()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, Paths.get(".", "src", "test", "resources", "configs", "ctrail-file-search-filter.xml").toString());
		CtrailProps props = CtrailProps.getInstance();
		Hashtable<String, FileSearchFilter> filters = props.getFileSearchFilters();
		assertEquals(2, filters.size());
	}





	@Test
	public void testExcludeFilterCli()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, Paths.get(".", "src", "test", "resources", "configs", "ctrail-file-search-filter-disabled-for-cli.xml").toString());
		CtrailProps props = CtrailProps.getInstance();
		props.setEnabledFileFiltering(true);
		assertEquals(true, props.isEnabledFileFiltering());
		Hashtable<String, FileSearchFilter> filters = props.getFileSearchFilters();
		assertEquals(2, filters.size());
	}

}
