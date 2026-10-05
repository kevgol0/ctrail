/****************************************************************************
 * FILE: FileReaderThreadFilterTest.java
 * DSCRPT: regression coverage for file-side filtering and read-position
 *         bookkeeping
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;



import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;



import org.junit.Before;
import org.junit.Test;



import com.kagr.tools.ctrail.IShutdownManager;
import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.FileSearchFilter;
import com.kagr.tools.ctrail.unit.LogLine;





public class FileReaderThreadFilterTest
{
	private BlockingDeque<LogLine> _output;

	/** upper bound on how long a reader is given to drain a test fixture */
	private static final long _drainTimeoutMillis = 5000L;

	/** how often the drain condition is sampled */
	private static final long _drainPollMillis = 20L;

	/** consecutive quiet samples required before the reader is stopped */
	private static final int  _drainStableSamples = 3;



	private final IShutdownManager _mgr = new IShutdownManager()
	{
		@Override
		public void initiateShutdown()
		{
		}
	};





	@Before
	public void setUp()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-file-search-filter.xml").toString());

		final CtrailProps props = CtrailProps.getInstance();

		// read from byte 0 so the fixture content is actually seen
		props.setReadEntireFile(true);
		props.setEnabledFileFiltering(true);
		props.setEnabledExcludeFiltering(true);
		props.setPrependFilenameToLine(false);
		props.setBlankLineOnFileChange(false);

		//
		// CtrailProps is a singleton, so a test that flips this would otherwise
		// leak into whichever test runs next. Pin it here; the two tests that
		// need it false set it themselves
		//
		props.setFileFilterDefaultsToInclude(true);

		_output = new LinkedBlockingDeque<>();
	}


	/**
	 * With fileFilterDefaultsToInclude=false - which is what the shipped
	 * etc/ctrail.xml uses - a file matching no &lt;filefilter&gt; used to emit
	 * nothing at all: the startup banner and then silence. That setting is the
	 * verdict for a line matching neither list WITHIN a filter; it must not
	 * decide anything for a source that has no filter.
	 */
	@Test
	public void unmatchedFileEmitsEveryLineEvenWhenDefaultsToExclude() throws Exception
	{
		CtrailProps.getInstance().setFileFilterDefaultsToInclude(false);

		final File f = writeTempLog("one\ntwo\nthree\n");
		final FileTailTracker tracker = trackerFor(f, null);
		runReaderBriefly(tracker, null);

		final List<String> lines = drain();
		assertEquals("a file with no matching filter must show every line", 3, lines.size());
		assertEquals("one", lines.get(0));
		assertEquals("three", lines.get(2));
	}





	/**
	 * The other half of the same setting: a file that DOES have a filter keeps
	 * strict allow-list behaviour, so the fix above cannot be weakening filtering.
	 */
	@Test
	public void matchedFileStillHonorsStrictAllowList() throws Exception
	{
		CtrailProps.getInstance().setFileFilterDefaultsToInclude(false);

		final FileSearchFilter filter = new FileSearchFilter("ctrail-reader.*\\.log", false);
		filter.getIncludeTerms().add("keep");

		final File f = writeTempLog("keep alpha\ndrop bravo\nkeep charlie\n");
		final FileTailTracker tracker = trackerFor(f, filter);
		runReaderBriefly(tracker, null);

		final List<String> lines = drain();
		assertEquals("a filtered file must stay a strict allow-list", 2, lines.size());
		assertEquals("keep alpha", lines.get(0));
		assertEquals("keep charlie", lines.get(1));
	}





	private File writeTempLog(final String content_) throws IOException
	{
		final File f = File.createTempFile("ctrail-reader", ".log");
		f.deleteOnExit();
		try (PrintWriter pw = new PrintWriter(f, StandardCharsets.UTF_8.name()))
		{
			pw.print(content_);
		}
		return f;
	}





	private FileTailTracker trackerFor(final File file_, final FileSearchFilter filter_) throws IOException
	{
		final FileTailTracker tracker = new FileTailTracker(file_.getName(), new RandomAccessFile(file_, "r"));
		if (filter_ != null)
		{
			tracker.setFileSearchTerms(filter_);
		}
		return tracker;
	}





	/**
	 * Runs the reader long enough to consume the fixture, then stops it. The
	 * reader loop is infinite by design (it is a tail), so it is interrupted.
	 */
	/**
	 * Runs a reader against the tracker until the file is fully consumed and the
	 * output has stopped growing, then stops it.
	 *
	 * This used to sleep a flat 400ms, which made every test in this class
	 * timing-dependent: on a cold JVM the reader had not drained the file inside
	 * that window and the assertions saw zero lines. Waiting on a real condition
	 * removes the flake and is faster in the normal case.
	 */
	private void runReaderBriefly(final FileTailTracker tracker_, final String match_) throws InterruptedException, IOException
	{
		final BlockingDeque<FileTailTracker> trackers = new LinkedBlockingDeque<>();
		trackers.add(tracker_);

		final Thread t = new Thread(new FileReaderThread(trackers, _output, match_, _mgr));
		t.setDaemon(true);
		t.start();

		waitUntilDrained(tracker_);

		t.interrupt();
		t.join(2000);
	}





	/**
	 * Blocks until the tracker has no unread bytes AND the output queue has held
	 * the same size for several consecutive samples.
	 *
	 * Both halves are needed: the reader records the read position BEFORE it
	 * queues the line, so "nothing left to read" on its own can still be one
	 * put() short of the line the test is about to assert on.
	 */
	private void waitUntilDrained(final FileTailTracker tracker_) throws InterruptedException, IOException
	{
		final long deadline = System.currentTimeMillis() + _drainTimeoutMillis;
		int stableSamples = 0;
		int previousSize = -1;
		while (System.currentTimeMillis() < deadline)
		{
			Thread.sleep(_drainPollMillis);

			final int size = _output.size();
			final boolean quiet = tracker_.getRemainingSize() <= 0 && size == previousSize;
			previousSize = size;

			stableSamples = quiet ? stableSamples + 1 : 0;
			if (stableSamples >= _drainStableSamples)
			{
				return;
			}
		}
	}





	private List<String> drain()
	{
		final List<String> lines = new ArrayList<>();
		LogLine ll;
		while ((ll = _output.poll()) != null)
		{
			lines.add(ll.getLine());
		}
		return lines;
	}





	/**
	 * Bytes consumed by a line that gets filtered out were never credited to
	 * lastReadPosition, so getRemainingSize() stayed above zero forever and the
	 * run-loop spun without ever sleeping.
	 */
	@Test
	public void testExcludedLinesStillAdvanceReadPosition() throws Exception
	{
		final File f = writeTempLog("drop one\ndrop two\ndrop three\n");

		final FileSearchFilter filter = new FileSearchFilter(f.getName(), true);
		filter.getExcldueTerms().add("drop");

		final FileTailTracker tracker = trackerFor(f, filter);
		runReaderBriefly(tracker, null);

		assertEquals("every line was filtered, so nothing should be emitted", 0, _output.size());
		assertEquals("read position must reach EOF even when all lines are filtered",
				0, tracker.getRemainingSize());
	}





	/**
	 * The include list was only ever consulted for std-in; tailing a file
	 * ignored &lt;includes&gt; completely.
	 */
	@Test
	public void testIncludeTermsAreHonoredForFiles() throws Exception
	{
		final File f = writeTempLog("keep alpha\nskip bravo\nkeep charlie\n");

		// defaults-to-include false => only lines hitting an include term show
		final FileSearchFilter filter = new FileSearchFilter(f.getName(), false);
		filter.getIncludeTerms().add("keep");

		final FileTailTracker tracker = trackerFor(f, filter);
		runReaderBriefly(tracker, null);

		final List<String> lines = drain();
		assertEquals(2, lines.size());
		assertEquals("keep alpha", lines.get(0));
		assertEquals("keep charlie", lines.get(1));
	}





	@Test
	public void testNoFilterEmitsEveryLine() throws Exception
	{
		final File f = writeTempLog("one\ntwo\nthree\n");

		final FileTailTracker tracker = trackerFor(f, null);
		runReaderBriefly(tracker, null);

		final List<String> lines = drain();
		assertEquals(3, lines.size());
		assertEquals("one", lines.get(0));
		assertEquals("three", lines.get(2));
	}





	@Test
	public void testMatchIsCaseInsensitiveByConfigWithAnUppercaseNeedle() throws Exception
	{
		//
		// the mirror of the test below: there the LINE is uppercase and the needle
		// lowercase, so only the line's folding is exercised. The needle is folded
		// once at construction now, and nothing caught it being skipped
		//
		final File f = writeTempLog("alpha here\nbravo here\n");

		final FileTailTracker tracker = trackerFor(f, null);
		runReaderBriefly(tracker, "ALPHA");

		final List<String> lines = drain();
		assertEquals("an uppercase -m term must match a lowercase line", 1, lines.size());
		assertEquals("alpha here", lines.get(0));
	}





	@Test
	public void testMatchIsCaseInsensitiveByConfig() throws Exception
	{
		final File f = writeTempLog("ALPHA here\nbravo here\n");

		final FileTailTracker tracker = trackerFor(f, null);
		runReaderBriefly(tracker, "alpha");

		final List<String> lines = drain();
		assertEquals(1, lines.size());
		assertEquals("ALPHA here", lines.get(0));
		assertTrue(tracker.getRemainingSize() == 0);
	}

}
