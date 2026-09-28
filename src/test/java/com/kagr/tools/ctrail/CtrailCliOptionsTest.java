/****************************************************************************
 * FILE: CtrailCliOptionsTest.java
 * DSCRPT: -n and -e parsing and precedence, driven through the real
 *         constructor rather than the private parse helper.
 ****************************************************************************/





package com.kagr.tools.ctrail;





import static org.junit.Assert.assertEquals;



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





public class CtrailCliOptionsTest
{
	@Rule public TemporaryFolder _tmp = new TemporaryFolder();

	private File _log;





	@Before
	public void setUp() throws IOException
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-liveness.xml").toString());
		CtrailProps.getInstance();

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
				3, CtrailProps.getInstance().getTailLastLines());
	}





	@Test
	public void nonNumericAndNegativeLineCountsAreIgnored()
	{
		new CtrailEntryPoint(new String[] { "-n", "abc", _log.getAbsolutePath() });
		assertEquals(3, CtrailProps.getInstance().getTailLastLines());

		new CtrailEntryPoint(new String[] { "-n", "-5", _log.getAbsolutePath() });
		assertEquals("a negative count would be read as 'disabled', which is not what was asked",
				3, CtrailProps.getInstance().getTailLastLines());
	}





	@Test
	public void aUsableLineCountIsApplied()
	{
		new CtrailEntryPoint(new String[] { "-n", "12", _log.getAbsolutePath() });
		assertEquals(12, CtrailProps.getInstance().getTailLastLines());
	}





	/**
	 * README: "-e ... disables -n and skipAheadInBytes". -e used to be applied
	 * before -n, so -n silently overwrote it and `ctr -e -n 50` showed 50 lines.
	 */
	@Test
	public void entireFileBeatsLineCountRegardlessOfArgumentOrder()
	{
		new CtrailEntryPoint(new String[] { "-e", "-n", "50", _log.getAbsolutePath() });
		assertEquals("-e must win over -n", 0, CtrailProps.getInstance().getTailLastLines());
		assertEquals(0, CtrailProps.getInstance().getSkipAheadInBytes());

		new CtrailEntryPoint(new String[] { "-n", "50", "-e", _log.getAbsolutePath() });
		assertEquals("-e must win irrespective of where it appears", 0, CtrailProps.getInstance().getTailLastLines());
		assertEquals(0, CtrailProps.getInstance().getSkipAheadInBytes());
	}
}
