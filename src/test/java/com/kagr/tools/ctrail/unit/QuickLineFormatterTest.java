/****************************************************************************
 * FILE: QuickLineFormatterTest.java
 * DSCRPT: CTRAIL-22 - in quick mode a line is coloured whole by the first -q
 *         regex it matches; every other line keeps the default colour.
 ****************************************************************************/





package com.kagr.tools.ctrail.unit;





import static org.junit.Assert.assertEquals;



import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;



import org.junit.After;
import org.junit.Before;
import org.junit.Test;



import com.kagr.tools.ctrail.ConsoleColors;
import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.QuickPattern;





public class QuickLineFormatterTest
{
	private static final String RED = CtrailProps.QUICK_PALETTE.get(0);
	private static final String SECOND = CtrailProps.QUICK_PALETTE.get(1);
	private static final String PLAIN = ConsoleColors.WHITE;
	private static final String RESET = ConsoleColors.RESET;





	@Before
	public void pointAtAConfigWithAWarnColourPair()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-quick-isolation.xml").toString());
	}





	@After
	public void leaveQuickMode()
	{
		CtrailProps.resetInstance();
	}





	@Test
	public void matchingLineIsColouredWhole()
	{
		assertEquals(RED + "x ERROR y" + RESET, formatterFor("ERROR").format(line("x ERROR y")));
	}





	@Test
	public void unmatchedLineKeepsTheDefaultColour()
	{
		assertEquals(PLAIN + "all quiet" + RESET, formatterFor("ERROR").format(line("all quiet")));
	}





	@Test
	public void matchingIsCaseSensitive()
	{
		assertEquals(PLAIN + "an error" + RESET, formatterFor("ERROR").format(line("an error")));
	}





	@Test
	public void inlineFlagGivesCaseInsensitiveMatching()
	{
		assertEquals(RED + "an error" + RESET, formatterFor("(?i)ERROR").format(line("an error")));
	}





	@Test
	public void patternsAreRegex()
	{
		final LineFormatter formatter = formatterFor("time(d)?out");

		assertEquals(RED + "timedout" + RESET, formatter.format(line("timedout")));
		assertEquals(RED + "timeout" + RESET, formatter.format(line("timeout")));
	}





	@Test
	public void eachPatternHasItsOwnColour()
	{
		assertEquals(SECOND + "a timeout" + RESET, formatterFor("ERROR", "timeout").format(line("a timeout")));
	}





	@Test
	public void firstPatternWinsWhenSeveralMatch()
	{
		assertEquals(RED + "ERROR timeout" + RESET, formatterFor("ERROR", "timeout").format(line("ERROR timeout")));
	}





	@Test
	public void paletteWrapsAfterTheLastColour()
	{
		final int size = CtrailProps.QUICK_PALETTE.size();
		final String[] sources = new String[size + 1];
		for (int i = 0; i < sources.length; i++)
		{
			sources[i] = "p" + i + "\\b";
		}

		assertEquals(RED + "p" + size + RESET, formatterFor(sources).format(line("p" + size)));
	}





	/**
	 * The fixture colours WARN yellow. In quick mode that colour pair must not
	 * apply - the only colouring is from -q.
	 */
	@Test
	public void configColourPairsDoNotApply()
	{
		CtrailProps.getInstance();
		assertEquals(PLAIN + "WARN disk" + RESET, formatterFor("ERROR").format(line("WARN disk")));
	}





	@Test
	public void noticesKeepTheNoticeColour()
	{
		final LogLine notice = new LogLine(null, "ctrail: ERROR in a notice", null, true);

		assertEquals(ConsoleColors.CYAN + "ctrail: ERROR in a notice" + RESET, formatterFor("ERROR").format(notice));
	}





	@Test
	public void filenamePrefixTakesTheLineColour()
	{
		assertEquals(RED + "app.log:" + RED + "ERROR" + RESET,
				formatterFor("ERROR").format(new LogLine("app.log", "ERROR", null)));
	}





	private LineFormatter formatterFor(final String... sources_)
	{
		final List<QuickPattern> patterns = new ArrayList<>();
		for (int i = 0; i < sources_.length; i++)
		{
			final String color = CtrailProps.QUICK_PALETTE.get(i % CtrailProps.QUICK_PALETTE.size());
			patterns.add(new QuickPattern(sources_[i], Pattern.compile(sources_[i]), color));
		}
		CtrailProps.installQuickDefaults(patterns, false);
		return new LineFormatter();
	}





	private static LogLine line(final String text_)
	{
		return new LogLine(null, text_, null);
	}
}
