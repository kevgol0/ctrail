/****************************************************************************
 * FILE: ColorSplitPropsTest.java
 * DSCRPT: CTRAIL-23 - <fgcolor> takes text colors only, <bgcolor> background
 *         colors only; a pair's two compose into one code at load time.
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;



import java.nio.file.Paths;
import java.util.List;



import org.junit.After;
import org.junit.Before;
import org.junit.Test;



import com.kagr.tools.ctrail.ConsoleColors;
import com.kagr.tools.ctrail.testutil.LogCapture;





public class ColorSplitPropsTest
{
	private LogCapture _log;

	private CtrailProps _props;





	@Before
	public void loadTheSplitFixture()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-color-split.xml").toString());
		_log = new LogCapture(CtrailProps.class);
		_props = new CtrailProps();
	}





	@After
	public void stopCapturing()
	{
		_log.close();
	}





	@Test
	public void textThenBackgroundComposeIntoOneCode()
	{
		assertEquals(ConsoleColors.WHITE + ConsoleColors.RED_BACKGROUND, _props.getKeysToColors().get("ERROR"));
	}





	@Test
	public void backgroundOnlyPairKeepsTheDefaultTextColor()
	{
		assertEquals(ConsoleColors.WHITE + ConsoleColors.RED_BACKGROUND, _props.getKeysToColors().get("BGONLY"));
	}





	/**
	 * Pairs used to be counted by their fgcolor values, so a pair without one
	 * shifted every later pair's index.
	 */
	@Test
	public void pairAfterABackgroundOnlyPairIsStillLoaded()
	{
		assertEquals(ConsoleColors.CYAN, _props.getKeysToColors().get("AFTER"));
	}





	@Test
	public void brightSuffixGivesTheHighIntensityBackground()
	{
		assertEquals(ConsoleColors.RED + ConsoleColors.RED_BACKGROUND_BRIGHT, _props.getKeysToColors().get("BRIGHT"));
	}





	@Test
	public void backgroundNameInFgcolorIsRejectedWithItsReplacement()
	{
		assertNull(_props.getKeysToColors().get("OLDBG"));

		final List<String> warnings = _log.warningsContaining("for keyword OLDBG");
		assertEquals(1, warnings.size());
		assertTrue(warnings.get(0), warnings.get(0).contains("is not a text color; use <bgcolor>red</bgcolor>"));
	}





	@Test
	public void textStyleInBgcolorIsRejectedAndTheTextColorStays()
	{
		assertEquals(ConsoleColors.BLUE, _props.getKeysToColors().get("BADBG"));
		assertEquals(1, _log.warningsContaining("<bgcolor>red_bold</bgcolor> for keyword BADBG is not a background color").size());
	}





	@Test
	public void unknownColorNameWarns()
	{
		assertNull(_props.getKeysToColors().get("UNKNOWN"));
		assertEquals(1, _log.warningsContaining("unknown color <fgcolor>chartreuse</fgcolor> for keyword UNKNOWN").size());
		assertEquals("exactly one warning for the name, in any form", 0, _log.warningsContaining("CHARTREUSE").size());
	}





	@Test
	public void flcolorIsTextOnlyToo()
	{
		assertNull(_props.getKeysToFileColors().get("FLBG"));
		assertEquals(1, _log.warningsContaining("<flcolor>yellow_background</flcolor> for keyword FLBG is not a text color").size());
		assertEquals(ConsoleColors.YELLOW, _props.getKeysToFileColors().get("FLTEXT"));
	}





	@Test
	public void noticeColorIsTextOnly()
	{
		assertNull(_props.getNoticeColor());
		assertEquals(1, _log.warningsContaining("<noticeColor>red_background</noticeColor> is not a text color").size());
	}





	@Test
	public void defaultBackgroundIsRead()
	{
		assertEquals(ConsoleColors.BLUE_BACKGROUND, _props.getDefaultBgColor());
	}





	@Test
	public void noDefaultBackgroundMeansNone()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, "./etc/ctrail.xml");

		assertEquals("", new CtrailProps().getDefaultBgColor());
	}





	@Test
	public void composeWithoutABackgroundIsTheTextCodeUnchanged()
	{
		assertEquals(ConsoleColors.RED, _props.composeColor(ConsoleColors.RED, null));
		assertNull(_props.composeColor(null, null));
	}





	@Test
	public void backgroundNamesAreCaseAndSpaceInsensitive()
	{
		assertEquals(ConsoleColors.GREEN_BACKGROUND_BRIGHT, _props.backgroundColorCode(" Green_Bright ", "bgcolor", ""));
		assertFalse(_log.warningsContaining("Green_Bright").size() > 0);
	}
}
