/****************************************************************************
 * FILE: IdleMonitorThread.java
 * DSCRPT: the single idle -> notice -> repeat -> resume state machine, shared
 *         by file tailing and stdin so the two can never drift apart.
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.util.Deque;
import java.util.List;



import com.kagr.tools.ctrail.unit.DurationFormatter;
import com.kagr.tools.ctrail.unit.LogLine;



import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;





@Slf4j
public class IdleMonitorThread extends Thread
{
	/** wake interval; small enough that an idleNoticeSeconds of 1 still fires on time */
	private static final long _tickMillis = 250L;

	private final List<ActivityState> _sources;

	private final Deque<LogLine> _output;

	@Getter @Setter private volatile boolean _shouldContinue;





	public IdleMonitorThread(@NonNull final List<ActivityState> sources_, @NonNull final Deque<LogLine> output_)
	{
		super("idle-monitor");
		setDaemon(true);
		_sources = sources_;
		_output = output_;
		_shouldContinue = true;
	}





	@Override
	public void run()
	{
		if (_logger.isDebugEnabled())
		{
			_logger.debug("idle monitor started, watching {} source(s)", _sources.size());
		}

		while (_shouldContinue)
		{
			try
			{
				Thread.sleep(_tickMillis);
			}
			catch (final InterruptedException ex_)
			{
				_logger.trace("idle monitor interrupted, exiting run-loop");
				break;
			}


			//
			// iterate a snapshot list - never the live tracker deque, whose
			// entries are transiently removed by the reader thread
			//
			final long now = System.currentTimeMillis();
			for (int i = 0; i < _sources.size(); i++)
			{
				checkForIdle(_sources.get(i), now);
			}
		}

		_logger.debug("idle monitor stopped");
	}





	/**
	 * Emits a "no movement" notice when a source has been silent for its configured
	 * interval, and schedules the next one. Sources that are finished or have notices
	 * disabled are skipped.
	 *
	 * @param source_ the source to examine, may be null
	 * @param now_    the current time in epoch millis
	 * @return true when a notice was emitted
	 */
	protected final boolean checkForIdle(final ActivityState source_, final long now_)
	{
		if (source_ == null || source_.isFinished() || source_.getIdleIntervalMillis() <= 0)
		{
			return false;
		}

		if (now_ < source_.getIdleNoticeDueMillis())
		{
			return false;
		}


		//
		// elapsed is measured from the last line seen, not from the last notice,
		// so a repeat reads "1m 00s" rather than "30s" all over again
		//
		source_.setIdle(true);
		source_.setIdleNoticeDueMillis(now_ + source_.getIdleIntervalMillis());
		emitNotice(_output, source_.getName() + " - no movement in " + DurationFormatter.format(now_ - source_.getLastActivityMillis()));
		return true;
	}





	/**
	 * Records that a source produced a line. When the source had gone quiet this also
	 * emits the "resumed" notice, which is why readers must call it BEFORE enqueuing
	 * the line itself - the notice then lands ahead of the data that ended the silence.
	 *
	 * @param source_ the source that produced a line, may be null
	 * @param output_ the queue the notice is written to
	 * @return true when a resume notice was emitted
	 */
	public static boolean noteActivity(final ActivityState source_, final Deque<LogLine> output_)
	{
		return noteActivity(source_, output_, System.currentTimeMillis());
	}





	/**
	 * Clock-injecting form of {@link #noteActivity(ActivityState, Deque)}, so the resume
	 * path can be driven deterministically from a test.
	 *
	 * @param source_ the source that produced a line, may be null
	 * @param output_ the queue the notice is written to
	 * @param now_    the current time in epoch millis
	 * @return true when a resume notice was emitted
	 */
	protected static boolean noteActivity(final ActivityState source_, final Deque<LogLine> output_, final long now_)
	{
		if (source_ == null)
		{
			return false;
		}

		final long now = now_;
		boolean resumed = false;


		//
		// the elapsed figure has to be read before the timestamp is refreshed
		//
		if (source_.isIdle())
		{
			source_.setIdle(false);
			emitNotice(output_, source_.getName() + " - resumed after " + DurationFormatter.format(now - source_.getLastActivityMillis()));
			resumed = true;
		}

		source_.setLastActivityMillis(now);
		source_.setIdleNoticeDueMillis(source_.getIdleIntervalMillis() > 0 ? now + source_.getIdleIntervalMillis() : Long.MAX_VALUE);
		return resumed;
	}





	/**
	 * Offers a notice to the output queue, dropping it if the queue is full rather
	 * than throwing - a liveness message must never take down the thread reporting it.
	 *
	 * @param output_  the queue to write to
	 * @param message_ the notice text, without the ctrail prefix
	 */
	private static void emitNotice(final Deque<LogLine> output_, final String message_)
	{
		if (output_ == null)
		{
			return;
		}

		if (!output_.offer(new LogLine(null, "ctrail: " + message_, null, true)))
		{
			_logger.warn("output queue full, dropping liveness notice:{}", message_);
		}
	}
}
