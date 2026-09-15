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





	/**
	 * '*' is the only wildcard; every other regex metacharacter in a filename is
	 * a literal. Unescaped, "app(1).log" compiled into a different pattern and
	 * matched the wrong files.
	 */
	@Test
	public void testRegexMetacharactersInFilenameAreEscaped()
	{
		FileSearchFilter fsf = new FileSearchFilter("app(1).log", false);
		assertTrue(fsf.doesMatchFilename("/var/log/app(1).log"));
		assertFalse("the parentheses must not be treated as a group", fsf.doesMatchFilename("/var/log/app1.log"));
	}





	@Test
	public void testPlusInFilenameIsEscaped()
	{
		FileSearchFilter fsf = new FileSearchFilter("a+b.log", false);
		assertTrue(fsf.doesMatchFilename("/var/log/a+b.log"));
		assertFalse(fsf.doesMatchFilename("/var/log/aaab.log"));
	}





	/**
	 * Matching must respect useCaseSensitiveSarch for include and exclude terms
	 * alike.
	 */
	@Test
	public void testIncludeAndExcludeAreCaseInsensitiveByConfig()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, Paths.get(".", "src", "test", "resources", "configs", "ctrail-file-search-filter.xml").toString());
		CtrailProps props = CtrailProps.getInstance();
		props.setEnabledExcludeFiltering(true);

		FileSearchFilter fsf = new FileSearchFilter("case.log", false);
		fsf.getIncludeTerms().add("keepme");
		fsf.getExcldueTerms().add("dropme");

		assertTrue(fsf.shouldIncludeLineDueToSeachTerms("a KEEPME line"));
		assertTrue(fsf.shouldExcludeLineDueToSeachTerms("a DROPME line"));
	}





	/**
	 * Null lines used to NPE inside the term scan.
	 */
	@Test
	public void testNullLineIsSafe()
	{
		FileSearchFilter fsf = new FileSearchFilter("null.log", true);
		assertFalse(fsf.shouldIncludeLineDueToSeachTerms(null));
		assertFalse(fsf.shouldExcludeLineDueToSeachTerms(null));
	}





	/**
	 * With no include terms configured, the fileFilterDefaultsToInclude setting
	 * decides.
	 */
	@Test
	public void testDefaultIncludeBehaviour()
	{
		assertTrue(new FileSearchFilter("d.log", true).shouldIncludeLineDueToSeachTerms("anything"));
		assertFalse(new FileSearchFilter("d.log", false).shouldIncludeLineDueToSeachTerms("anything"));
	}

}
