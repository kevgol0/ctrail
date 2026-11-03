/****************************************************************************
 * FILE: UnknownColoringElementTest.java
 * DSCRPT: CTRAIL-23 - an element under <coloring> that ctrail does not read
 *         is named in one WARN instead of being silently ignored.
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;



import java.nio.file.Paths;
import java.util.List;



import org.junit.Test;



import com.kagr.tools.ctrail.testutil.LogCapture;





public class UnknownColoringElementTest
{
	@Test
	public void typoInAPairWarnsOnceNamingItsKeyword()
	{
		try (LogCapture log = load("ctrail-color-unknown.xml"))
		{
			final List<String> warnings = log.warningsContaining("<bgcolour>");
			assertEquals(1, warnings.size());
			assertTrue(warnings.get(0), warnings.get(0).contains("for keyword ERROR - valid: keyword, fgcolor, bgcolor, flcolor"));
		}
	}





	@Test
	public void unknownElementOutsideAPairWarnsWithItsPath()
	{
		try (LogCapture log = load("ctrail-color-unknown.xml"))
		{
			assertEquals(1, log.warningsContaining("unknown element coloring.linecolors.sparkle").size());
		}
	}





	@Test
	public void theUnknownElementsAreTheOnlyWarnings()
	{
		try (LogCapture log = load("ctrail-color-unknown.xml"))
		{
			assertEquals(2, log.warningsContaining("unknown element").size());
		}
	}





	@Test
	public void everythingAfterColoringStillLoads()
	{
		try (LogCapture log = load("ctrail-color-unknown.xml"))
		{
			final CtrailProps props = new CtrailProps();
			assertEquals(7, props.getIdleNoticeSeconds());
			assertEquals(1, props.getFileSearchFilters().size());
			assertEquals("the pair with the typo keeps its valid colors", 2, props.getKeysToColors().size());
		}
	}





	@Test
	public void theShippedConfigHasNoUnknownElements()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, "./etc/ctrail.xml");
		try (LogCapture log = new LogCapture(CtrailProps.class))
		{
			final CtrailProps props = new CtrailProps();
			assertTrue("proves the file was read", props.getKeysToColors().size() > 0);
			assertEquals(0, log.warningsContaining("unknown element").size());
			assertEquals(0, log.warningsContaining("color").size());
		}
	}





	private static LogCapture load(final String fileName_)
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", fileName_).normalize().toString());
		final LogCapture log = new LogCapture(CtrailProps.class);
		new CtrailProps();
		return log;
	}
}
