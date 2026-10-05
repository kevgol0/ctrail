/****************************************************************************
 * FILE: LineFormatterCachingTest.java
 * DSCRPT: CTRAIL-19 - the formatter captures its config once, at construction,
 *         rather than re-reading it on every formatted line
 ****************************************************************************/





package com.kagr.tools.ctrail.unit;





import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



import java.lang.reflect.Method;
import java.nio.file.Paths;



import org.junit.Test;



import com.kagr.tools.ctrail.ConsoleColors;
import com.kagr.tools.ctrail.props.CtrailProps;





public class LineFormatterCachingTest
{
	private void useConfig(final String fixture_)
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", fixture_).toString());
		CtrailProps.getInstance();
	}





	/**
	 * The formatter is built after CtrailEntryPoint has settled the config, and
	 * nothing changes it afterwards, so a swapped singleton must not reach a
	 * formatter that already exists.
	 *
	 * This is the assertion that makes the per-line lookup removable: if the
	 * formatter still called refreshProps() per line it would pick the new
	 * config up here and the notice would come back cyan.
	 */
	@Test
	public void configIsCapturedAtConstructionNotPerLine()
	{
		// purple notices
		useConfig("ctrail-liveness.xml");
		final LineFormatter formatter = new LineFormatter();

		// swap the singleton underneath it - this config has no noticeColor,
		// so it falls back to the compiled default of cyan
		useConfig("ctrail-single-colorpair.xml");

		final String out = formatter.format(new LogLine(null, "ctrail: app.log - no movement in 30s", null, true));
		assertTrue("the formatter must keep the config it was built with", out.startsWith(ConsoleColors.PURPLE));
		assertFalse("a swapped config must not reach an existing formatter", out.startsWith(ConsoleColors.CYAN));
	}





	/**
	 * A formatter built AFTER the swap sees the new config, which is what makes
	 * the test above a statement about caching rather than about the fixtures.
	 */
	@Test
	public void aFormatterBuiltAfterTheSwapSeesTheNewConfig()
	{
		useConfig("ctrail-liveness.xml");
		useConfig("ctrail-single-colorpair.xml");

		final String out = new LineFormatter()
				.format(new LogLine(null, "ctrail: app.log - no movement in 30s", null, true));
		assertTrue("a new formatter must pick up the current config", out.startsWith(ConsoleColors.CYAN));
	}





	/**
	 * Guards the removal itself: refreshProps() existed to be called per line,
	 * so its return would mean the per-line lookup had come back.
	 */
	@Test
	public void refreshPropsIsGone()
	{
		final Method[] methods = LineFormatter.class.getDeclaredMethods();
		for (final Method method : methods)
		{
			assertFalse("LineFormatter must not re-read config per line",
					"refreshProps".equals(method.getName()));
		}
	}
}
