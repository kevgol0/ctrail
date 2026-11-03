/****************************************************************************
 * FILE: ColorSplitLineFormatterTest.java
 * DSCRPT: CTRAIL-23 - the bytes ctrail writes for a pair with a text color
 *         and a background, for its filename prefix, and for unmatched lines.
 ****************************************************************************/





package com.kagr.tools.ctrail.unit;





import static org.junit.Assert.assertEquals;



import java.nio.file.Paths;



import org.junit.After;
import org.junit.Test;



import com.kagr.tools.ctrail.ConsoleColors;
import com.kagr.tools.ctrail.props.CtrailProps;





public class ColorSplitLineFormatterTest
{
	private static final String WHITE_ON_RED = ConsoleColors.WHITE + ConsoleColors.RED_BACKGROUND;
	private static final String RESET = ConsoleColors.RESET;





	@After
	public void dropTheFixtureConfig()
	{
		CtrailProps.resetInstance();
	}





	@Test
	public void lineIsWhiteOnRed()
	{
		assertEquals(WHITE_ON_RED + "ERROR x" + RESET, formatter("ctrail-color-split.xml").format(line(null, "ERROR x")));
	}





	@Test
	public void filenamePrefixInheritsTheBackground()
	{
		assertEquals(WHITE_ON_RED + "app.log:" + WHITE_ON_RED + "ERROR x" + RESET,
				formatter("ctrail-color-split.xml").format(line("app.log", "ERROR x")));
	}





	@Test
	public void flcolorOverridesThePrefixOnly()
	{
		assertEquals(ConsoleColors.YELLOW + "app.log:" + WHITE_ON_RED + "FLTEXT x" + RESET,
				formatter("ctrail-color-split.xml").format(line("app.log", "FLTEXT x")));
	}





	@Test
	public void defaultBackgroundAppliesToUnmatchedLines()
	{
		assertEquals(ConsoleColors.WHITE + ConsoleColors.BLUE_BACKGROUND + "nothing here" + RESET,
				formatter("ctrail-color-split.xml").format(line(null, "nothing here")));
	}





	@Test
	public void defaultBackgroundDoesNotTouchMatchedLines()
	{
		assertEquals(ConsoleColors.CYAN + "AFTER x" + RESET, formatter("ctrail-color-split.xml").format(line(null, "AFTER x")));
	}





	/**
	 * The shipped config sets no background anywhere: unmatched and matched
	 * lines must be exactly what 1.5.0 wrote.
	 */
	@Test
	public void withoutBackgroundsOutputIsUnchanged()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, "./etc/ctrail.xml");
		final LineFormatter formatter = new LineFormatter();

		assertEquals(ConsoleColors.WHITE + "plain line" + RESET, formatter.format(line(null, "plain line")));
	}





	private static LineFormatter formatter(final String fileName_)
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", fileName_).toString());
		return new LineFormatter();
	}





	private static LogLine line(final String fileName_, final String text_)
	{
		return new LogLine(fileName_, text_, null);
	}
}
