/****************************************************************************
 * FILE: FilterDefaultSemanticsTest.java
 * DSCRPT: pins what fileFilterDefaultsToInclude actually means, including for a
 *         <filefilter> that declares no <includes>.
 *
 *         Raised as CTRAIL-2 ("an excludes-only filter hides the whole file")
 *         and closed as working-as-designed. These tests exist so it is not
 *         re-filed: the behaviour is deliberate, and the supported way to write
 *         a deny-list is documented below.
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;



import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Iterator;



import org.junit.Before;
import org.junit.Test;





public class FilterDefaultSemanticsTest
{
	private FileSearchFilter _excludesOnly;
	private FileSearchFilter _allowList;





	@Before
	public void setUp()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-filter-default-semantics.xml").toString());

		//
		// select the filter the way production does - by filename match - rather
		// than by the regex the map happens to be keyed on
		//
		_excludesOnly = filterFor("excludes-only.log");
		_allowList = filterFor("allow-list.log");
		assertNotNull(_excludesOnly);
		assertNotNull(_allowList);
	}





	private FileSearchFilter filterFor(final String fileName_)
	{
		final Hashtable<String, FileSearchFilter> all = CtrailProps.getInstance().getFileSearchFilters();
		final Iterator<String> itr = all.keySet().iterator();
		while (itr.hasNext())
		{
			final FileSearchFilter candidate = all.get(itr.next());
			if (candidate.doesMatchFilename(fileName_))
			{
				return candidate;
			}
		}
		return null;
	}





	/**
	 * DELIBERATE, not a defect. fileFilterDefaultsToInclude decides for a filter
	 * with no include terms just as it does for one whose includes did not match,
	 * so an excludes-only filter under a false default emits nothing.
	 *
	 * If you want a deny-list, see denyListRequiresDefaultsToInclude below.
	 */
	@Test
	public void excludesOnlyFilterUnderAFalseDefaultEmitsNothing()
	{
		assertFalse("documented behaviour: the default decides when no include matched",
				_excludesOnly.shouldIncludeLineDueToSeachTerms("INFO started"));
	}





	/**
	 * The supported way to express "hide DEBUG, show everything else":
	 * fileFilterDefaultsToInclude=true plus an excludes list.
	 */
	@Test
	public void denyListRequiresDefaultsToInclude()
	{
		final FileSearchFilter denyList = new FileSearchFilter("deny.log", true);
		denyList.getExcldueTerms().add("DEBUG");

		assertTrue("everything not excluded is shown", denyList.shouldIncludeLineDueToSeachTerms("INFO started"));
		assertTrue("the exclude term is still honoured", denyList.shouldExcludeLineDueToSeachTerms("DEBUG noisy"));
		assertFalse(denyList.shouldExcludeLineDueToSeachTerms("INFO started"));
	}





	/**
	 * A filter that does declare includes is a strict allow-list under a false
	 * default - the case the setting exists for.
	 */
	@Test
	public void aFilterWithIncludesIsAStrictAllowList()
	{
		assertTrue(_allowList.shouldIncludeLineDueToSeachTerms("keep alpha"));
		assertFalse(_allowList.shouldIncludeLineDueToSeachTerms("drop bravo"));
	}





	/**
	 * The excludes list is independent of the include decision either way.
	 */
	@Test
	public void excludeTermsApplyRegardlessOfTheDefault()
	{
		assertTrue(_excludesOnly.shouldExcludeLineDueToSeachTerms("DEBUG noisy"));
		assertFalse(_excludesOnly.shouldExcludeLineDueToSeachTerms("INFO started"));
	}
}
