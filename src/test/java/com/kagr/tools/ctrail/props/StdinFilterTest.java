/****************************************************************************
 * FILE: StdinFilterTest.java
 * DSCRPT: coverage for <filtering><stdinfilter> and its resolution rules
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;



import java.nio.file.Paths;



import org.junit.Test;





public class StdinFilterTest
{

	private static CtrailProps load(final String cfgName_)
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", cfgName_).toString());
		return CtrailProps.getInstance();
	}





	@Test
	public void testStdinFilterIsParsed()
	{
		final CtrailProps props = load("ctrail-stdin-filter.xml");
		final FileSearchFilter filter = props.getStdinFilter();

		assertNotNull("<stdinfilter> should have been loaded", filter);
		assertEquals(2, filter.getIncludeTerms().size());
		assertTrue(filter.getIncludeTerms().contains("keepme"));
		assertTrue(filter.getIncludeTerms().contains("alsokeep"));
		assertEquals(1, filter.getExcldueTerms().size());
		assertTrue(filter.getExcldueTerms().contains("dropme"));
	}





	/**
	 * A &lt;stdinfilter&gt; must not be mistaken for a file filter.
	 */
	@Test
	public void testStdinFilterDoesNotLeakIntoFileFilters()
	{
		final CtrailProps props = load("ctrail-stdin-filter.xml");
		assertEquals("only the real <filefilter> belongs in the file map", 1, props.getFileSearchFilters().size());
	}





	@Test
	public void testStdinFilterAppliesIncludeAndExclude()
	{
		final CtrailProps props = load("ctrail-stdin-filter.xml");
		final FileSearchFilter filter = props.resolveStdinFilter();
		assertNotNull(filter);

		assertTrue(filter.shouldIncludeLineDueToSeachTerms("a keepme line"));
		assertTrue(filter.shouldIncludeLineDueToSeachTerms("an alsokeep line"));
		assertFalse("defaults-to-include is false, so unmatched lines drop",
				filter.shouldIncludeLineDueToSeachTerms("an unrelated line"));
		assertTrue(filter.shouldExcludeLineDueToSeachTerms("a dropme line"));
	}





	/**
	 * Absent from the config, there is simply no stdin filter.
	 */
	@Test
	public void testNoStdinFilterConfigured()
	{
		final CtrailProps props = load("ctrail-file-search-filter.xml");
		assertNull(props.getStdinFilter());
		assertNull(props.resolveStdinFilter());
	}





	/**
	 * -f false must disable stdin filtering, exactly as it does for files. It
	 * previously had no effect on the stdin path at all.
	 */
	@Test
	public void testFilteringDisabledYieldsNoStdinFilter()
	{
		final CtrailProps props = load("ctrail-stdin-filter.xml");
		assertNotNull(props.resolveStdinFilter());

		props.setEnabledFileFiltering(false);
		assertNull("-f false must disable the stdin filter", props.resolveStdinFilter());

		props.setEnabledFileFiltering(true);
		assertNotNull(props.resolveStdinFilter());
	}





	/**
	 * Before &lt;stdinfilter&gt; existed, the only way to filter piped input was
	 * a &lt;filefilter&gt; named literally "stdin". That must keep working.
	 */
	@Test
	public void testLegacyStdinFileFilterStillHonored()
	{
		final CtrailProps props = load("ctrail-stdin-filter-legacy.xml");

		assertNull("no <stdinfilter> in this config", props.getStdinFilter());

		final FileSearchFilter filter = props.resolveStdinFilter();
		assertNotNull("legacy <filename>stdin</filename> must still resolve", filter);
		assertTrue(filter.getIncludeTerms().contains("legacykeep"));
	}





	@Test
	public void testStdinFilterWinsOverLegacyFileFilter()
	{
		final CtrailProps props = load("ctrail-stdin-filter-both.xml");

		final FileSearchFilter filter = props.resolveStdinFilter();
		assertNotNull(filter);
		assertTrue("<stdinfilter> must take precedence", filter.getIncludeTerms().contains("newwins"));
		assertFalse(filter.getIncludeTerms().contains("legacykeep"));
	}

}
