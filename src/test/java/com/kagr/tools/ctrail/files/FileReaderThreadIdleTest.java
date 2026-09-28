/****************************************************************************
 * FILE: FileReaderThreadIdleTest.java
 * DSCRPT: end-to-end checks that tail-N history reaches the output queue and
 *         that a resume notice arrives ahead of the data that ended the silence.
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;



import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;



import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;



import com.kagr.tools.ctrail.IShutdownManager;
import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.unit.LogLine;





public class FileReaderThreadIdleTest
{
	@Rule public TemporaryFolder _tmp = new TemporaryFolder();

	private BlockingDeque<LogLine> _out;
	private BlockingDeque<FileTailTracker> _trackers;
	private IShutdownManager _noopShutdown;
	private Thread _readerThread;
	private IdleMonitorThread _monitor;
	private RandomAccessFile _raf;





	@Before
	public void setUp()
	{
		//
		// tailLastLines=3 and idleNoticeSeconds=1, so the suite stays quick
		//
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-liveness.xml").toString());
		CtrailProps.getInstance();

		_out = new LinkedBlockingDeque<>();
		_trackers = new LinkedBlockingDeque<>();
		_noopShutdown = new IShutdownManager()
		{
			@Override
			public void initiateShutdown()
			{
			}
		};
	}





	@After
	public void tearDown() throws IOException
	{
		if (_readerThread != null)
		{
			_readerThread.interrupt();
		}
		if (_monitor != null)
		{
			_monitor.setShouldContinue(false);
			_monitor.interrupt();
		}
		if (_raf != null)
		{
			_raf.close();
		}
	}





	@Test
	public void tailNHistoryReachesTheOutputQueue() throws Exception
	{
		startReaderOn("l1\nl2\nl3\nl4\nl5\n");

		final List<String> lines = new ArrayList<>();
		for (int i = 0; i < 3; i++)
		{
			final LogLine line = _out.poll(3, TimeUnit.SECONDS);
			assertNotNull("expected tail history line " + i, line);
			lines.add(line.getLine());
		}

		assertEquals("l3", lines.get(0));
		assertEquals("l4", lines.get(1));
		assertEquals("l5", lines.get(2));
	}





	@Test
	public void resumeNoticeLandsAheadOfTheLineThatEndedTheSilence() throws Exception
	{
		final FileTailTracker tracker = trackerOn("l1\nl2\nl3\nl4\nl5\n");
		tracker.getActivityState().setIdle(true);
		_trackers.add(tracker);
		startReader();

		final LogLine notice = _out.poll(3, TimeUnit.SECONDS);
		assertNotNull(notice);
		assertTrue("the resume notice must come first", notice.isNotice());
		assertTrue(notice.getLine().startsWith("ctrail: app.log - resumed after"));

		final LogLine data = _out.poll(3, TimeUnit.SECONDS);
		assertNotNull(data);
		assertFalse(data.isNotice());
		assertEquals("l3", data.getLine());
	}





	@Test
	public void aQuietFileIsAnnouncedByTheMonitor() throws Exception
	{
		final FileTailTracker tracker = trackerOn("l1\n");
		_trackers.add(tracker);

		final List<ActivityState> sources = new ArrayList<>();
		sources.add(tracker.getActivityState());
		_monitor = new IdleMonitorThread(sources, _out);

		startReader();
		_monitor.start();

		//
		// drain the single history line, then wait out the one second interval
		//
		LogLine line = _out.poll(3, TimeUnit.SECONDS);
		while (line != null && !line.isNotice())
		{
			line = _out.poll(3, TimeUnit.SECONDS);
		}

		assertNotNull("expected an idle notice", line);
		assertTrue(line.getLine().startsWith("ctrail: app.log - no movement in"));
	}





	private void startReaderOn(final String content_) throws IOException
	{
		_trackers.add(trackerOn(content_));
		startReader();
	}





	private FileTailTracker trackerOn(final String content_) throws IOException
	{
		final File f = _tmp.newFile("app.log");
		Files.write(f.toPath(), content_.getBytes(StandardCharsets.UTF_8));
		_raf = new RandomAccessFile(f, "r");
		return new FileTailTracker("app.log", _raf);
	}





	private void startReader()
	{
		_readerThread = new Thread(new FileReaderThread(_trackers, _out, null, _noopShutdown));
		_readerThread.setDaemon(true);
		_readerThread.start();
	}
}
