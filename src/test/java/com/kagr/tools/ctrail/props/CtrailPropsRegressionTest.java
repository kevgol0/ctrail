/****************************************************************************
 * FILE: CtrailPropsRegressionTest.java
 * DSCRPT: coverage for config-loading defects
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;



import java.nio.file.Paths;



import org.junit.Test;



import com.kagr.tools.ctrail.ConsoleColors;





public class CtrailPropsRegressionTest
{

	private static String configPath(final String name_)
	{
		return Paths.get(".", "src", "test", "resources", "configs", name_).toString();
	}





	/**
	 * commons-configuration returns a bare String rather than a Collection when
	 * a config holds exactly one colorpair. The old raw cast threw, leaving the
	 * pair count at zero, so a single-colorpair config produced no coloring.
	 */
	@Test
	public void testSingleColorPairIsLoaded()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, configPath("ctrail-single-colorpair.xml"));
		final CtrailProps props = CtrailProps.getInstance();

		assertEquals(1, props.getKeys().size());
		assertEquals("(err)", props.getKeys().get(0));
		assertEquals(ConsoleColors.RED, props.getKeysToColors().get("(err)"));
	}





	/**
	 * getInstance() resolved the config path on every call, costing up to three
	 * Files.exists() syscalls per formatted line. Repeat calls must be cached.
	 */
	@Test
	public void testInstanceIsCachedBetweenCalls()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, configPath("ctrail-single-colorpair.xml"));
		final CtrailProps first = CtrailProps.getInstance();
		final CtrailProps second = CtrailProps.getInstance();
		assertSame("repeat getInstance() calls must not rebuild the config", first, second);
	}





	@Test
	public void testInstanceIsRebuiltWhenConfigOverrideChanges()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, configPath("ctrail-single-colorpair.xml"));
		final CtrailProps first = CtrailProps.getInstance();

		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, configPath("ctrail-case-sensitive.xml"));
		final CtrailProps second = CtrailProps.getInstance();

		assertTrue("changing CTRAIL_CFG must reload the config", first != second);
	}





	/**
	 * A bad color name yields null, which used to be concatenated into every
	 * output line as the literal text "null".
	 */
	@Test
	public void testDefaultFgColorNeverNull()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, configPath("ctrail-single-colorpair.xml"));
		final CtrailProps props = CtrailProps.getInstance();
		assertNotNull(props.getDefaultFgColor());
	}





	/**
	 * The exclude half of filtering must be toggleable on its own; -v used to
	 * set the same flag as -f.
	 */
	@Test
	public void testExcludeFilteringToggleIsIndependent()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, configPath("ctrail-file-search-filter.xml"));
		final CtrailProps props = CtrailProps.getInstance();

		final FileSearchFilter filter = new FileSearchFilter("some.log", true);
		filter.getExcldueTerms().add("noise");

		props.setEnabledExcludeFiltering(true);
		assertTrue(filter.shouldExcludeLineDueToSeachTerms("a noise line"));

		props.setEnabledExcludeFiltering(false);
		assertFalse("disabling excludes must stop the exclude list from firing",
				filter.shouldExcludeLineDueToSeachTerms("a noise line"));

		// leave the shared singleton the way we found it
		props.setEnabledExcludeFiltering(true);
	}

}
