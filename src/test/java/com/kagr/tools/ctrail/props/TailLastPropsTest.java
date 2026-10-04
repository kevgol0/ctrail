/****************************************************************************
 * FILE: TailLastPropsTest.java
 * DSCRPT: CTRAIL-8 - <tailLast> replaces tailLastLines and skipAheadInBytes.
 *         The new key wins; the deprecated keys keep their legacy meaning;
 *         bad values degrade with a WARN instead of failing to load.
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



import java.nio.file.Paths;



import org.junit.Test;





public class TailLastPropsTest
{
	@Test
	public void explicitTailLastWinsOverBothDeprecatedKeys()
	{
		final CtrailProps props = load("ctrail-taillast-bytes.xml");

		assertEquals(199, props.getTailLastCount());
		assertEquals(TailUnit.BYTES, props.getTailLastUnit());
		assertFalse(props.isReadEntireFile());
	}





	@Test
	public void countAllReadsTheWholeFile()
	{
		assertTrue(load("ctrail-taillast-all.xml").isReadEntireFile());
	}





	@Test
	public void unrecognisedUnitDegradesToLines()
	{
		final CtrailProps props = load("ctrail-taillast-bad-unit.xml");

		assertEquals(5, props.getTailLastCount());
		assertEquals(TailUnit.LINES, props.getTailLastUnit());
	}





	/**
	 * Also proves a bad value cannot abort loading: getInt() would have thrown
	 * out of the constructor and skipped every setting after it.
	 */
	@Test
	public void nonNumericCountDegradesToTheDefaultAndLoadingContinues()
	{
		final CtrailProps props = load("ctrail-taillast-bad-count.xml");

		assertEquals(CtrailProps.DEFAULT_TAIL_LAST_COUNT, props.getTailLastCount());
		assertEquals(TailUnit.LINES, props.getTailLastUnit());
		assertEquals("settings after tailLast must still load", 0, props.getIdleNoticeSeconds());
	}





	@Test
	public void noSettingAtAllMeansTenLines()
	{
		final CtrailProps props = load("ctrail-taillast-none.xml");

		assertEquals(10, props.getTailLastCount());
		assertEquals(TailUnit.LINES, props.getTailLastUnit());
		assertFalse(props.isReadEntireFile());
	}





	@Test
	public void zeroCountIsKeptAsStartAtEnd()
	{
		final CtrailProps props = load("ctrail-taillast-none.xml");

		props.applyTailLast("0", "bytes");

		assertEquals(0, props.getTailLastCount());
		assertFalse("0 is start-at-end, not whole file", props.isReadEntireFile());
	}





	@Test
	public void negativeCountDegradesToTheDefault()
	{
		final CtrailProps props = load("ctrail-taillast-none.xml");

		props.applyTailLast("-4", "bytes");

		assertEquals(CtrailProps.DEFAULT_TAIL_LAST_COUNT, props.getTailLastCount());
		assertEquals(TailUnit.LINES, props.getTailLastUnit());
	}





	/**
	 * The CTRAIL-8 defect: skipAheadInBytes alone was unreachable because the
	 * tailLastLines default of 10 always won.
	 */
	@Test
	public void deprecatedByteSkipAloneIsHonoured()
	{
		final CtrailProps props = load("ctrail-taillast-none.xml");

		props.applyLegacyTailKeys(null, "500000");

		assertEquals(500000, props.getTailLastCount());
		assertEquals(TailUnit.BYTES, props.getTailLastUnit());
	}





	@Test
	public void positiveDeprecatedLineCountWinsOverDeprecatedByteSkip()
	{
		final CtrailProps props = load("ctrail-taillast-none.xml");

		props.applyLegacyTailKeys("25", "500");

		assertEquals(25, props.getTailLastCount());
		assertEquals(TailUnit.LINES, props.getTailLastUnit());
	}





	/**
	 * tailLastLines=0 meant "off, use the byte skip" - the 1.2 README said so -
	 * so it defers rather than winning as 0 lines.
	 */
	@Test
	public void zeroDeprecatedLineCountDefersToByteSkip()
	{
		final CtrailProps props = load("ctrail-taillast-none.xml");

		props.applyLegacyTailKeys("0", "750");

		assertEquals(750, props.getTailLastCount());
		assertEquals(TailUnit.BYTES, props.getTailLastUnit());
	}





	@Test
	public void zeroDeprecatedLineCountAloneUsesTheOldByteDefault()
	{
		final CtrailProps props = load("ctrail-taillast-none.xml");

		props.applyLegacyTailKeys("0", null);

		assertEquals(CtrailProps.LEGACY_SKIP_AHEAD_DEFAULT, props.getTailLastCount());
		assertEquals(TailUnit.BYTES, props.getTailLastUnit());
	}





	@Test
	public void zeroDeprecatedByteSkipStillMeansTheWholeFile()
	{
		final CtrailProps props = load("ctrail-taillast-none.xml");

		props.applyLegacyTailKeys(null, "0");

		assertTrue(props.isReadEntireFile());
	}





	@Test
	public void unitNamesAreCaseAndSpaceInsensitive()
	{
		assertEquals(TailUnit.BYTES, TailUnit.fromConfigValue(" Bytes "));
		assertEquals(TailUnit.LINES, TailUnit.fromConfigValue("LINES"));
		assertEquals(null, TailUnit.fromConfigValue(""));
	}





	/**
	 * A fresh instance per test - not the singleton - so no test inherits
	 * another's tail setting.
	 */
	private CtrailProps load(final String fileName_)
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", fileName_).toString());
		return new CtrailProps();
	}
}
