/****************************************************************************
 * FILE: QuickModePropsTest.java
 * DSCRPT: CTRAIL-22 - quick mode's config is built from compiled defaults and
 *         never reads ctrail.xml, so nothing in the file can filter or recolour.
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;



import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;



import org.junit.After;
import org.junit.Before;
import org.junit.Test;



import com.kagr.tools.ctrail.ConsoleColors;





public class QuickModePropsTest
{
	private static final List<QuickPattern> ONE_PATTERN =
			List.of(new QuickPattern("ERROR", Pattern.compile("ERROR"), ConsoleColors.RED_BOLD));





	@Before
	public void pointAtAConfigFullOfFiltersAndColours()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-quick-isolation.xml").toString());
	}





	@After
	public void leaveQuickMode()
	{
		CtrailProps.resetInstance();
	}





	/**
	 * Guards the fixture: if the file stopped loading, the isolation tests below
	 * would pass for the wrong reason.
	 */
	@Test
	public void fixtureReallyHasFiltersAndColoursInNormalMode()
	{
		final CtrailProps normal = CtrailProps.getInstance();

		assertFalse(normal.getKeys().isEmpty());
		assertFalse(normal.getFileSearchFilters().isEmpty());
		assertTrue(normal.resolveStdinFilter() != null);
		assertFalse(normal.isQuickMode());
	}





	@Test
	public void quickModeLoadsNothingFromTheConfigFile()
	{
		final CtrailProps quick = CtrailProps.installQuickDefaults(ONE_PATTERN, false);

		assertTrue(quick.getKeys().isEmpty());
		assertTrue(quick.getKeysToColors().isEmpty());
		assertTrue(quick.getFileSearchFilters().isEmpty());
		assertNull(quick.resolveStdinFilter());
		assertFalse(quick.isEnabledFileFiltering());
	}





	@Test
	public void quickModeUsesItsFixedSettings()
	{
		final CtrailProps quick = CtrailProps.installQuickDefaults(ONE_PATTERN, false);

		assertEquals(CtrailProps.QUICK_TAIL_LINES, quick.getTailLastCount());
		assertEquals(TailUnit.LINES, quick.getTailLastUnit());
		assertFalse(quick.isReadEntireFile());
		assertEquals("idle notices off", 0, quick.getIdleNoticeSeconds());
		assertTrue(quick.isShowStartupBanner());
		assertTrue(quick.isQuickMode());
		assertEquals(ONE_PATTERN, quick.getQuickPatterns());
	}





	/**
	 * The field initialiser holds the colour NAME; only the file path converted it.
	 * Without the fix every unmatched line would start with the literal "white".
	 */
	@Test
	public void defaultColourIsAnAnsiCodeNotAName()
	{
		assertEquals(ConsoleColors.WHITE, CtrailProps.installQuickDefaults(ONE_PATTERN, false).getDefaultFgColor());
	}





	@Test
	public void filenamePrefixOnlyForSeveralFiles()
	{
		assertFalse(CtrailProps.installQuickDefaults(ONE_PATTERN, false).isPrependFilenameToLine());
		assertTrue(CtrailProps.installQuickDefaults(ONE_PATTERN, true).isPrependFilenameToLine());
	}





	@Test
	public void getInstanceKeepsReturningTheQuickConfig()
	{
		final CtrailProps quick = CtrailProps.installQuickDefaults(ONE_PATTERN, false);

		assertSame(quick, CtrailProps.getInstance());
	}





	@Test
	public void resetHandsTheNextCallerANormalConfig()
	{
		CtrailProps.installQuickDefaults(ONE_PATTERN, false);

		CtrailProps.resetInstance();

		assertFalse(CtrailProps.getInstance().isQuickMode());
	}





	@Test(expected = IllegalArgumentException.class)
	public void quickModeNeedsAPattern()
	{
		CtrailProps.installQuickDefaults(List.of(), false);
	}
}
