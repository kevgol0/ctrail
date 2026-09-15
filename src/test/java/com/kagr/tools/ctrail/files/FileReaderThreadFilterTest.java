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
		props.setSkipAheadInBytes(0);
		props.setEnabledFileFiltering(true);
		props.setEnabledExcludeFiltering(true);
		props.setPrependFilenameToLine(false);
		props.setBlankLineOnFileChange(false);

		_output = new LinkedBlockingDeque<>();
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
	private void runReaderBriefly(final FileTailTracker tracker_, final String match_) throws InterruptedException
	{
		final BlockingDeque<FileTailTracker> trackers = new LinkedBlockingDeque<>();
		trackers.add(tracker_);

		final Thread t = new Thread(new FileReaderThread(trackers, _output, match_, _mgr));
		t.setDaemon(true);
		t.start();
		Thread.sleep(400);
		t.interrupt();
		t.join(2000);
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
