/****************************************************************************
 * FILE: QuickModeCliTest.java
 * DSCRPT: CTRAIL-22 - -q parsing through the real constructor: repeatable,
 *         regex-checked, overriding every other option and the config file.
 ****************************************************************************/





package com.kagr.tools.ctrail;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;



import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;



import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;



import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.QuickPattern;
import com.kagr.tools.ctrail.props.TailUnit;





public class QuickModeCliTest
{
	@Rule public TemporaryFolder _tmp = new TemporaryFolder();

	/** named to match the fixture's <filefilter>, so normal mode attaches a filter */
	private File _log;

	private CtrailEntryPoint _entryPoint;





	@Before
	public void setUp() throws IOException
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-quick-isolation.xml").toString());
		CtrailProps.resetInstance();
		_log = _tmp.newFile("quick-app.log");
		Files.write(_log.toPath(), "keep ERROR\ndrop WARN\n".getBytes(StandardCharsets.UTF_8));
	}





	@After
	public void tearDown()
	{
		if (_entryPoint != null)
		{
			_entryPoint.shutdown();
		}
		CtrailProps.resetInstance();
	}





	/**
	 * Guards the fixture: without -q the file's filter reaches the tracker, so the
	 * quick-mode test below is proving something.
	 */
	@Test
	public void normalModeAttachesTheConfigFilter()
	{
		_entryPoint = new CtrailEntryPoint(new String[] { _log.getPath() });

		assertNotNull(_entryPoint.getFileTrackers().peek().getFileSearchFilter());
	}





	@Test
	public void quickModeAttachesNoFilter()
	{
		_entryPoint = new CtrailEntryPoint(new String[] { "-q", "ERROR", _log.getPath() });

		assertNull("quick mode must never hide a line", _entryPoint.getFileTrackers().peek().getFileSearchFilter());
	}





	@Test
	public void repeatedPatternsKeepTheirOrderAndColours()
	{
		_entryPoint = new CtrailEntryPoint(new String[] { "-q", "ERROR", "-q", "time(d)?out", _log.getPath() });

		final List<QuickPattern> patterns = CtrailProps.getInstance().getQuickPatterns();
		assertEquals(2, patterns.size());
		assertEquals("ERROR", patterns.get(0).getSource());
		assertEquals("time(d)?out", patterns.get(1).getSource());
		assertEquals(CtrailProps.QUICK_PALETTE.get(0), patterns.get(0).getColor());
		assertEquals(CtrailProps.QUICK_PALETTE.get(1), patterns.get(1).getColor());
	}





	@Test
	public void otherOptionsAreIgnored()
	{
		_entryPoint = new CtrailEntryPoint(new String[] {
				"-q", "ERROR", "-m", "keep", "-n", "5", "-c", "9", "-e", "-f", "true", "-v", "true", _log.getPath() });

		final CtrailProps props = CtrailProps.getInstance();
		assertNull("-m would filter", _entryPoint.getMatchpattern());
		assertEquals(CtrailProps.QUICK_TAIL_LINES, props.getTailLastCount());
		assertEquals(TailUnit.LINES, props.getTailLastUnit());
		assertFalse(props.isReadEntireFile());
		assertFalse(props.isEnabledFileFiltering());
		assertFalse(props.isEnabledExcludeFiltering());
	}





	@Test
	public void filenamePrefixFollowsTheFileCount() throws IOException
	{
		_entryPoint = new CtrailEntryPoint(new String[] { "-q", "ERROR", _log.getPath() });
		assertFalse(CtrailProps.getInstance().isPrependFilenameToLine());
		_entryPoint.shutdown();

		final File second = _tmp.newFile("quick-other.log");
		_entryPoint = new CtrailEntryPoint(new String[] { "-q", "ERROR", _log.getPath(), second.getPath() });
		assertTrue(CtrailProps.getInstance().isPrependFilenameToLine());
	}





	@Test
	public void invalidRegexIsAUsageErrorNamingThePattern()
	{
		final CtrailUsageException ex = expectUsageError("-q", "[", _log.getPath());

		assertTrue(ex.getMessage(), ex.getMessage().contains("-q [ is not a valid regex"));
	}





	@Test
	public void quickWithoutAPatternIsAUsageError()
	{
		final CtrailUsageException ex = expectUsageError("-q");

		assertTrue(ex.getMessage(), ex.getMessage().contains("ctr -h"));
	}





	/**
	 * Every parse error is now a usage error. Before, the raw arguments - options
	 * included - were treated as file names.
	 */
	@Test
	public void unknownOptionIsAUsageError()
	{
		expectUsageError("--bogus", _log.getPath());
	}





	@Test
	public void bannerNamesEveryPattern()
	{
		final List<QuickPattern> patterns = CtrailEntryPoint.compileQuickPatterns(new String[] { "ERROR", "time(d)?out" });

		assertEquals("quick mode, highlighting \"ERROR\", \"time(d)?out\" - config and other options ignored",
				CtrailEntryPoint.describeQuickMode(patterns));
	}





	private CtrailUsageException expectUsageError(final String... args_)
	{
		try
		{
			_entryPoint = new CtrailEntryPoint(args_);
		}
		catch (final CtrailUsageException ex_)
		{
			return ex_;
		}
		throw new AssertionError("expected a CtrailUsageException");
	}
}
