/****************************************************************************
 * FILE: ColorUnitTestCase.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static org.junit.Assert.assertEquals;



import java.nio.file.Paths;
import java.util.Hashtable;



import org.junit.Test;





public class ColorUnitTestCase
{


	@Test
	public void testCorrectNumberOfColorings()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, Paths.get(".", "src", "test", "resources", "configs", "ctrail-case-ignore.xml").toString());
		CtrailProps.getInstance();
		final CtrailProps props = CtrailProps.getInstance();
		Hashtable<String, Integer> k2c = props.getKeysToColorCount();

		assertEquals(3, k2c.get("yellow").intValue());
		assertEquals(3, k2c.get("blue").intValue());
		assertEquals(1, k2c.get("cyan").intValue());
		assertEquals(2, k2c.get("red").intValue());
		assertEquals(1, k2c.get("purple").intValue());
	}

}
