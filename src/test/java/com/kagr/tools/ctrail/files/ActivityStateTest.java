/****************************************************************************
 * FILE: ActivityStateTest.java
 * DSCRPT: the idle -> notice -> repeat -> resume state machine, driven with
 *         hand-set clocks. Nothing here sleeps or touches a file.
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;



import org.junit.Before;
import org.junit.Test;



import com.kagr.tools.ctrail.unit.LogLine;





public class ActivityStateTest
{
	private static final long _interval = 1000L;
	private static final long _now = 1_700_000_000_000L;

	private Deque<LogLine> _out;





	@Before
	public void setUp()
	{
		_out = new LinkedBlockingDeque<>();
	}





	private ActivityState quiet()
	{
		return new ActivityState("app.log", _now, _interval);
	}





	private String takeLine()
	{
		final LogLine line = _out.poll();
		assertTrue("expected a notice line", line != null && line.isNotice());
		return line.getLine();
	}





	@Test
	public void noticeFiresOnceTheIntervalHasElapsed()
	{
		final ActivityState src = quiet();

		assertFalse("not due yet", src.checkForIdle(_out, _now + _interval - 1));
		assertTrue(src.checkForIdle(_out, _now + _interval));
		assertEquals("ctrail: app.log - no movement in 1s", takeLine());
		assertTrue(src.isIdle());
	}





	/**
	 * CTRAIL-17. The previous test asserted only message text, so deleting the
	 * reschedule left it green: without it the stale due time was also in the past
	 * and the second check fired anyway. This pins the cadence itself.
	 */
	@Test
	public void aNoticeIsNotRepeatedUntilAnotherFullIntervalHasPassed()
	{
		final ActivityState src = quiet();

		assertTrue("first notice", src.checkForIdle(_out, _now + _interval));
		_out.clear();

		//
		// one tick later - nowhere near another interval. Without the reschedule
		// the due time would still be in the past and this would fire every tick
		//
		assertFalse("must not re-notify 250ms later", src.checkForIdle(_out, _now + _interval + 250));
		assertFalse("nor at half an interval", src.checkForIdle(_out, _now + _interval + (_interval / 2)));
		assertTrue("queue must be untouched", _out.isEmpty());

		assertTrue("due again a full interval later", src.checkForIdle(_out, _now + (2 * _interval)));
		assertEquals("elapsed grows from the last line, not the last notice",
				"ctrail: app.log - no movement in 2s", takeLine());
	}





	@Test
	public void activityAfterIdleEmitsAResumeNoticeAheadOfTheData()
	{
		final ActivityState src = quiet();
		src.checkForIdle(_out, _now + _interval);
		_out.clear();

		assertTrue(src.noteActivity(_out, _now + _interval + 500));
		assertEquals("ctrail: app.log - resumed after 1s", takeLine());
		assertFalse(src.isIdle());
	}





	@Test
	public void activityOnASourceThatWasNeverIdleSaysNothing()
	{
		assertFalse(quiet().noteActivity(_out, _now + 10));
		assertTrue(_out.isEmpty());
	}





	@Test
	public void activityReschedulesTheNextNotice()
	{
		final ActivityState src = quiet();
		src.noteActivity(_out, _now + 900);

		assertFalse("the clock restarts on movement", src.checkForIdle(_out, _now + _interval));
		assertTrue(src.checkForIdle(_out, _now + 900 + _interval));
	}





	@Test
	public void finishedSourcesAreNotAnnounced()
	{
		final ActivityState src = quiet();
		src.setFinished(true);

		assertFalse(src.checkForIdle(_out, _now + (10 * _interval)));
		assertTrue(_out.isEmpty());
	}





	@Test
	public void aZeroIntervalDisablesNoticesEntirely()
	{
		final ActivityState src = new ActivityState("app.log", _now, 0);

		assertFalse(src.checkForIdle(_out, _now));
		assertFalse(src.checkForIdle(_out, _now + 3_600_000));
		assertTrue(_out.isEmpty());
	}





	/**
	 * CTRAIL-10. _idle used to be cleared before the notice was queued, so a
	 * notice dropped by a full queue was never retried and the preceding
	 * "no movement" was never retracted.
	 */
	@Test
	public void aResumeNoticeDroppedByAFullQueueIsRetriedOnTheNextLine()
	{
		final Deque<LogLine> tiny = new LinkedBlockingDeque<>(1);
		final ActivityState src = quiet();

		src.checkForIdle(tiny, _now + _interval);
		assertTrue("the idle notice filled the queue", src.isIdle());

		//
		// queue is full: the resume notice cannot be queued, so the source must
		// stay idle rather than silently claim it resumed
		//
		assertFalse(src.noteActivity(tiny, _now + _interval + 500));
		assertTrue("still idle, so the notice is retried", src.isIdle());

		tiny.clear();
		assertTrue(src.noteActivity(tiny, _now + _interval + 900));
		assertFalse(src.isIdle());
	}





	/**
	 * CTRAIL-4 is a race, so no deterministic single-threaded test can catch the
	 * missing `synchronized` directly. What CAN be pinned is the property that
	 * prevents it: every mutation of the idle state happens inside a synchronized
	 * transition on the owning object, and nothing outside can poke the fields.
	 *
	 * If someone re-adds a public setter - which is how the race was reachable in
	 * the first place - this fails.
	 */
	@Test
	public void idleStateIsOnlyMutableThroughSynchronizedTransitions()
	{
		for (final Method m : ActivityState.class.getDeclaredMethods())
		{
			if (!Modifier.isPublic(m.getModifiers()) || m.isSynthetic())
			{
				continue;
			}

			final String name = m.getName();
			if ("setFinished".equals(name))
			{
				// the one exception: a plain volatile flag, set once at EOF
				continue;
			}

			assertFalse("no public setter may bypass the synchronized transitions: " + name,
					name.startsWith("set"));

			if ("noteActivity".equals(name) || "checkForIdle".equals(name))
			{
				assertTrue(name + " must be synchronized", Modifier.isSynchronized(m.getModifiers()));
			}
		}
	}





	/**
	 * A full queue must not turn the watchdog into a tight re-notify loop: the due
	 * time advances whether or not the notice was queued.
	 */
	@Test
	public void aFullQueueDoesNotCauseRepeatedIdleAttempts()
	{
		final Deque<LogLine> full = new LinkedBlockingDeque<>(1);
		full.add(new LogLine(null, "already full", null));

		final ActivityState src = quiet();
		assertFalse("notice could not be queued", src.checkForIdle(full, _now + _interval));
		assertFalse("but the schedule still advanced", src.checkForIdle(full, _now + _interval + 250));
		assertEquals(1, full.size());
	}
}
