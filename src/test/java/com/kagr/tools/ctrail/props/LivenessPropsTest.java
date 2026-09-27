/****************************************************************************
 * FILE: LivenessPropsTest.java
 * DSCRPT: the four liveness settings must read from XML, and must fall back to
 *         their documented defaults when a config predates them.
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



import java.nio.file.Paths;



import org.junit.Test;



import com.kagr.tools.ctrail.ConsoleColors;





public class LivenessPropsTest
{
	@Test
	public void valuesAreReadFromTheConfig()
	{
		final CtrailProps props = load("ctrail-liveness.xml");

		assertEquals(3, props.getTailLastLines());
		assertTrue(props.isShowStartupBanner());
		assertEquals(1, props.getIdleNoticeSeconds());

		//
		// purple, not the compiled default, so this asserts a real config read
		//
		assertEquals(ConsoleColors.PURPLE, props.getNoticeColor());
	}





	@Test
	public void everyLivenessFeatureCanBeSwitchedOff()
	{
		final CtrailProps props = load("ctrail-liveness-disabled.xml");

		assertEquals(0, props.getTailLastLines());
		assertFalse(props.isShowStartupBanner());
		assertEquals(0, props.getIdleNoticeSeconds());
	}





	@Test
	public void switchingTailNOffLeavesTheLegacyByteSkipInPlace()
	{
		//
		// an existing install that never opts in must behave exactly as before
		//
		final CtrailProps props = load("ctrail-liveness-disabled.xml");

		assertEquals(0, props.getTailLastLines());
		assertEquals(1000, props.getSkipAheadInBytes());
	}





	@Test
	public void aConfigWithoutTheseKeysGetsTheDocumentedDefaults()
	{
		//
		// this fixture predates the feature and names none of the new keys
		//
		final CtrailProps props = load("ctrail-file-regex.xml");

		assertEquals(10, props.getTailLastLines());
		assertTrue(props.isShowStartupBanner());
		assertEquals(30, props.getIdleNoticeSeconds());
		assertEquals(ConsoleColors.CYAN, props.getNoticeColor());
	}





	private CtrailProps load(final String fileName_)
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", fileName_).toString());
		return CtrailProps.getInstance();
	}
}
