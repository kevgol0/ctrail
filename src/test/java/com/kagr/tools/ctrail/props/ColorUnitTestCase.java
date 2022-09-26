/****************************************************************************
 * FILE: ColorUnitTestCase.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static org.junit.Assert.assertEquals;



import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;



import org.junit.Test;





public class ColorUnitTestCase
{


	@Test
	public void testSinglePairNoColor()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, "./src/test/resources/configs/ctrail-case-ignore.xml");
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
