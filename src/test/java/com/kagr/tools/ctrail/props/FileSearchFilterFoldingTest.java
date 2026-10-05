/****************************************************************************
 * FILE: FileSearchFilterFoldingTest.java
 * DSCRPT: CTRAIL-21 - the search terms are folded once and cached, so this
 *         covers both that the folding is correct and that the cache is
 *         invalidated when terms arrive after the first match
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



import java.nio.file.Paths;



import org.junit.Before;
import org.junit.Test;





public class FileSearchFilterFoldingTest
{
	private static final String CASE_INSENSITIVE_CFG	= "ctrail-file-search-filter.xml";
	private static final String CASE_SENSITIVE_CFG		= "ctrail-case-sensitive.xml";





	@Before
	public void setUp()
	{
		useConfig(CASE_INSENSITIVE_CFG);
	}





	private void useConfig(final String fixture_)
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", fixture_).toString());
		CtrailProps.getInstance();
	}





	private FileSearchFilter filterWithInclude(final String term_)
	{
		final FileSearchFilter filter = new FileSearchFilter("folding-test\\.log", false);
		filter.getIncludeTerms().add(term_);
		return filter;
	}





	/**
	 * The failure mode the cache introduces: terms arrive AFTER construction, via
	 * the exposed list, and may arrive after the filter has already matched a
	 * line and folded what it had. A term added later must still be honoured.
	 *
	 * This is the test to look at first if the folding cache is ever reworked.
	 */
	@Test
	public void aTermAddedAfterTheFirstMatchIsStillHonored()
	{
		final FileSearchFilter filter = filterWithInclude("alpha");

		// first call folds and caches the single term it has
		assertTrue("the initial term must match", filter.shouldIncludeLineDueToSeachTerms("alpha here"));
		assertFalse("bravo is not a term yet", filter.shouldIncludeLineDueToSeachTerms("bravo here"));

		// the new term must invalidate the folded copy
		filter.getIncludeTerms().add("bravo");
		assertTrue("a term added after the first match must be honored",
				filter.shouldIncludeLineDueToSeachTerms("bravo here"));
		assertTrue("the original term must still match", filter.shouldIncludeLineDueToSeachTerms("alpha here"));
	}





	/**
	 * The same invalidation on the exclude side, which has its own cached copy.
	 */
	@Test
	public void anExcludeTermAddedAfterTheFirstMatchIsStillHonored()
	{
		final FileSearchFilter filter = new FileSearchFilter("folding-test\\.log", true);
		filter.getExcldueTerms().add("noise");

		assertTrue("the initial exclude term must match", filter.shouldExcludeLineDueToSeachTerms("noise here"));
		assertFalse("chatter is not excluded yet", filter.shouldExcludeLineDueToSeachTerms("chatter here"));

		filter.getExcldueTerms().add("chatter");
		assertTrue("an exclude term added after the first match must be honored",
				filter.shouldExcludeLineDueToSeachTerms("chatter here"));
	}





	/**
	 * Folding the term, not just the line: a mixed-case configured term has to
	 * match a lowercase line. Folding only the line would miss this.
	 */
	@Test
	public void aMixedCaseTermMatchesCaseInsensitively()
	{
		final FileSearchFilter filter = filterWithInclude("AlPhA");
		assertTrue("a mixed-case term must match a lowercase line",
				filter.shouldIncludeLineDueToSeachTerms("alpha here"));
	}





	/**
	 * The mirror: the LINE is folded too, so a lowercase term matches an
	 * uppercase line.
	 */
	@Test
	public void aLowercaseTermMatchesAnUppercaseLine()
	{
		final FileSearchFilter filter = filterWithInclude("alpha");
		assertTrue("a lowercase term must match an uppercase line",
				filter.shouldIncludeLineDueToSeachTerms("ALPHA HERE"));
	}





	/**
	 * With case-sensitive matching configured, neither side is folded - the
	 * cached copy must carry the verdict it was built under rather than
	 * silently reusing a lowercased array.
	 */
	@Test
	public void caseSensitiveConfigDoesNotFold()
	{
		useConfig(CASE_SENSITIVE_CFG);

		final FileSearchFilter filter = filterWithInclude("alpha");
		assertTrue("an exact-case match must still match", filter.shouldIncludeLineDueToSeachTerms("alpha here"));
		assertFalse("case-sensitive matching must not fold the line",
				filter.shouldIncludeLineDueToSeachTerms("ALPHA HERE"));
	}
}
