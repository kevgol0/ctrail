/****************************************************************************
 * FILE: CtrailCliOptionsTest.java
 * DSCRPT: -n and -e parsing and precedence, driven through the real
 *         constructor rather than the private parse helper.
 ****************************************************************************/





package com.kagr.tools.ctrail;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;



import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;



import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.TailUnit;





public class CtrailCliOptionsTest
{
	@Rule public TemporaryFolder _tmp = new TemporaryFolder();

	private File _log;





	@Before
	public void setUp() throws IOException
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-liveness.xml").toString());
		//
		// the singleton survives between tests; pin the fixture's tail setting so
		// one test's -n/-c/-e cannot leak into the next
		//
		CtrailProps.getInstance().setTailLast(3, TailUnit.LINES);

		_log = _tmp.newFile("cli.log");
		try (PrintWriter pw = new PrintWriter(_log, StandardCharsets.UTF_8.name()))
		{
			for (int i = 1; i <= 50; i++)
			{
				pw.println("line " + i);
			}
		}
	}





	/**
	 * -n above Integer.MAX_VALUE is all digits, so the old StringUtils.isNumeric
	 * guard let it reach Integer.parseInt, which threw NumberFormatException out
	 * of the constructor and killed the run.
	 */
	@Test
	public void oversizedLineCountIsIgnoredRatherThanFatal()
	{
		new CtrailEntryPoint(new String[] { "-n", "99999999999", _log.getAbsolutePath() });

		assertEquals("an unusable -n must leave the configured value alone",
				3, CtrailProps.getInstance().getTailLastCount());
	}





	@Test
	public void nonNumericAndNegativeLineCountsAreIgnored()
	{
		new CtrailEntryPoint(new String[] { "-n", "abc", _log.getAbsolutePath() });
		assertEquals(3, CtrailProps.getInstance().getTailLastCount());

		new CtrailEntryPoint(new String[] { "-n", "-5", _log.getAbsolutePath() });
		assertEquals("a negative count would be read as 'disabled', which is not what was asked",
				3, CtrailProps.getInstance().getTailLastCount());
	}





	@Test
	public void aUsableLineCountIsApplied()
	{
		new CtrailEntryPoint(new String[] { "-n", "12", _log.getAbsolutePath() });
		assertEquals(12, CtrailProps.getInstance().getTailLastCount());
	}





	/**
	 * README: "-e ... disables -n and -c". -e used to be applied before -n, so
	 * -n silently overwrote it and `ctr -e -n 50` showed 50 lines.
	 */
	@Test
	public void entireFileBeatsLineCountRegardlessOfArgumentOrder()
	{
		new CtrailEntryPoint(new String[] { "-e", "-n", "50", _log.getAbsolutePath() });
		assertTrue("-e must win over -n", CtrailProps.getInstance().isReadEntireFile());

		CtrailProps.getInstance().setTailLast(3, TailUnit.LINES);
		new CtrailEntryPoint(new String[] { "-n", "50", "-e", _log.getAbsolutePath() });
		assertTrue("-e must win irrespective of where it appears", CtrailProps.getInstance().isReadEntireFile());

		CtrailProps.getInstance().setTailLast(3, TailUnit.LINES);
		new CtrailEntryPoint(new String[] { "-c", "50", "-e", _log.getAbsolutePath() });
		assertTrue("-e must win over -c", CtrailProps.getInstance().isReadEntireFile());
	}





	@Test
	public void byteCountIsAppliedInBytes()
	{
		new CtrailEntryPoint(new String[] { "-c", "200", _log.getAbsolutePath() });

		assertEquals(200, CtrailProps.getInstance().getTailLastCount());
		assertEquals(TailUnit.BYTES, CtrailProps.getInstance().getTailLastUnit());
	}





	@Test
	public void lineCountWinsWhenBothCountsAreGiven()
	{
		new CtrailEntryPoint(new String[] { "-c", "200", "-n", "7", _log.getAbsolutePath() });

		assertEquals(7, CtrailProps.getInstance().getTailLastCount());
		assertEquals(TailUnit.LINES, CtrailProps.getInstance().getTailLastUnit());
	}





	/**
	 * 0 now means "start at the end", like tail -n 0, so it must be accepted -
	 * not ignored as unusable, and not turned into a whole-file read.
	 */
	@Test
	public void zeroLineCountIsAppliedAsStartAtEnd()
	{
		new CtrailEntryPoint(new String[] { "-n", "0", _log.getAbsolutePath() });

		assertEquals(0, CtrailProps.getInstance().getTailLastCount());
		assertFalse(CtrailProps.getInstance().isReadEntireFile());
	}
}
