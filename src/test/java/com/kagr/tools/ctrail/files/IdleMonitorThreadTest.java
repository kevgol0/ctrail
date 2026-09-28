/****************************************************************************
 * FILE: IdleMonitorThreadTest.java
 * DSCRPT: drives the idle -> notice -> repeat -> resume state machine directly,
 *         with hand-set timestamps, so nothing here sleeps or touches a file.
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;



import org.junit.Before;
import org.junit.Test;



import com.kagr.tools.ctrail.unit.LogLine;





public class IdleMonitorThreadTest
{
	private static final long _interval = 1000L;
	private static final long _now = 1_700_000_000_000L;

	private Deque<LogLine> _out;
	private List<ActivityState> _sources;
	private IdleMonitorThread _monitor;





	@Before
	public void setUp()
	{
		_out = new LinkedBlockingDeque<>();
		_sources = new ArrayList<>();
		_monitor = new IdleMonitorThread(_sources, _out);
	}





	@Test
	public void noticeFiresOnceTheIntervalHasElapsed()
	{
		final ActivityState src = quietFor(1500);

		assertTrue(_monitor.checkForIdle(src, _now));
		assertEquals("ctrail: app.log - no movement in 1s", firstLine());
		assertTrue(src.isIdle());
	}





	@Test
	public void noticeIsSilentBeforeTheIntervalElapses()
	{
		final ActivityState src = new ActivityState("app.log", _now, _interval);

		assertFalse(_monitor.checkForIdle(src, _now + 500));
		assertTrue(_out.isEmpty());
		assertFalse(src.isIdle());
	}





	@Test
	public void repeatedNoticesShowAGrowingElapsedTime()
	{
		final ActivityState src = quietFor(1500);

		assertTrue(_monitor.checkForIdle(src, _now));
		assertTrue(_monitor.checkForIdle(src, _now + _interval));

		assertEquals(2, _out.size());
		assertEquals("ctrail: app.log - no movement in 1s", takeLine());
		assertEquals("ctrail: app.log - no movement in 2s", takeLine());
	}





	@Test
	public void finishedSourcesAreNotAnnounced()
	{
		final ActivityState src = quietFor(1500);
		src.setFinished(true);

		assertFalse(_monitor.checkForIdle(src, _now));
		assertTrue(_out.isEmpty());
	}





	@Test
	public void aZeroIntervalDisablesNoticesEntirely()
	{
		final ActivityState src = new ActivityState("app.log", _now - 60_000, 0);

		assertFalse(_monitor.checkForIdle(src, _now));
		assertFalse(_monitor.checkForIdle(src, _now + 3_600_000));
		assertTrue(_out.isEmpty());
	}





	@Test
	public void activityAfterIdleEmitsAResumeNotice()
	{
		final ActivityState src = quietFor(1500);
		_monitor.checkForIdle(src, _now);
		_out.clear();

		assertTrue(IdleMonitorThread.noteActivity(src, _out, _now));
		assertEquals("ctrail: app.log - resumed after 1s", firstLine());
		assertFalse(src.isIdle());
	}





	@Test
	public void activityOnASourceThatWasNeverIdleSaysNothing()
	{
		final ActivityState src = new ActivityState("app.log", _now, _interval);

		assertFalse(IdleMonitorThread.noteActivity(src, _out, _now));
		assertTrue(_out.isEmpty());
	}





	@Test
	public void activityReschedulesTheNextNotice()
	{
		final ActivityState src = quietFor(1500);
		IdleMonitorThread.noteActivity(src, _out, _now);
		_out.clear();

		//
		// the clock restarts on movement: a check that would have fired a moment
		// ago must now stay quiet
		//
		assertFalse(_monitor.checkForIdle(src, _now));
		assertTrue(_out.isEmpty());
	}





	@Test
	public void aFullOutputQueueDropsTheNoticeInsteadOfThrowing()
	{
		//
		// the watchdog must survive a saturated queue; throwing here would kill
		// the one thread whose job is reporting that nothing is happening
		//
		final Deque<LogLine> tiny = new LinkedBlockingDeque<>(1);
		tiny.add(new LogLine(null, "already full", null));

		final IdleMonitorThread monitor = new IdleMonitorThread(_sources, tiny);
		final ActivityState src = quietFor(1500);

		assertTrue(monitor.checkForIdle(src, _now));
		assertEquals(1, tiny.size());
	}





	@Test
	public void nullSourcesAreIgnored()
	{
		assertFalse(_monitor.checkForIdle(null, _now));
		assertFalse(IdleMonitorThread.noteActivity(null, _out, _now));
	}





	private ActivityState quietFor(final long millis_)
	{
		final ActivityState src = new ActivityState("app.log", _now - millis_, _interval);
		src.setIdleNoticeDueMillis(_now - millis_ + _interval);
		return src;
	}





	private String firstLine()
	{
		final LogLine line = _out.peek();
		assertTrue("expected a notice line", line != null && line.isNotice());
		return line.getLine();
	}





	private String takeLine()
	{
		final LogLine line = _out.poll();
		assertTrue("expected a notice line", line != null && line.isNotice());
		return line.getLine();
	}
}
