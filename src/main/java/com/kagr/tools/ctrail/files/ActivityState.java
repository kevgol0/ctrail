/****************************************************************************
 * FILE: ActivityState.java
 * DSCRPT: liveness bookkeeping for one input source - a tailed file or stdin -
 *         and the owner of its idle -> notice -> repeat -> resume transitions.
 *
 *         The transitions are synchronized instance methods rather than setters
 *         driven from outside, because the monitor thread and a reader thread
 *         both act on the same state. volatile gives visibility, not atomicity:
 *         with check-then-act split across two threads the monitor could pass
 *         its "is it due?" test, be descheduled while the reader recorded a
 *         line, then resume and announce "no movement in 0s" immediately after
 *         the line that had just arrived.
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.util.Deque;



import com.kagr.tools.ctrail.unit.DurationFormatter;
import com.kagr.tools.ctrail.unit.LogLine;



import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;





@Slf4j
public class ActivityState
{
	/** display name used in notices, e.g. "app.log" or "stdin" */
	@Getter private final String _name;

	/** how long this source may stay silent before a notice; <= 0 disables notices */
	@Getter private final long _idleIntervalMillis;

	private long _lastActivityMillis;

	private long _idleNoticeDueMillis;

	/** true once an idle notice has fired and no data has arrived since */
	private boolean _idle;

	/** true once the source can produce nothing further, e.g. stdin at EOF */
	private volatile boolean _finished;





	/**
	 * @param name_               display name used in this source's notices
	 * @param nowMillis_          the current time in epoch millis
	 * @param idleIntervalMillis_ silence permitted before a notice; <= 0 disables
	 *                            notices for this source entirely
	 */
	public ActivityState(@NonNull final String name_, final long nowMillis_, final long idleIntervalMillis_)
	{
		_name = name_;
		_idleIntervalMillis = idleIntervalMillis_;
		_lastActivityMillis = nowMillis_;
		_idleNoticeDueMillis = nowMillis_ + idleIntervalMillis_;
	}





	/**
	 * Records that this source produced a line, emitting the "resumed" notice when
	 * it had gone quiet. Readers must call this BEFORE enqueuing the line itself so
	 * the notice lands ahead of the data that ended the silence.
	 *
	 * @param output_ the queue the notice is written to
	 * @param now_    the current time in epoch millis
	 * @return true when a resume notice was emitted
	 */
	public synchronized boolean noteActivity(final Deque<LogLine> output_, final long now_)
	{
		boolean resumed = false;


		//
		// the elapsed figure has to be read before the timestamp is refreshed.
		// _idle is cleared only once the notice is actually queued: emitNotice is
		// best-effort, and clearing first meant a notice dropped by a full queue
		// was never retried and the preceding "no movement" was never retracted
		//
		if (_idle && emitNotice(output_, " - resumed after " + DurationFormatter.format(now_ - _lastActivityMillis)))
		{
			_idle = false;
			resumed = true;
		}

		_lastActivityMillis = now_;
		_idleNoticeDueMillis = now_ + _idleIntervalMillis;
		return resumed;
	}





	/**
	 * Emits a "no movement" notice when this source has been silent for its
	 * configured interval, and schedules the next one. Silent when notices are
	 * disabled or the source is finished.
	 *
	 * @param output_ the queue the notice is written to
	 * @param now_    the current time in epoch millis
	 * @return true when a notice was emitted
	 */
	public synchronized boolean checkForIdle(final Deque<LogLine> output_, final long now_)
	{
		if (_finished || _idleIntervalMillis <= 0 || now_ < _idleNoticeDueMillis)
		{
			return false;
		}


		//
		// elapsed is measured from the last line seen, not from the last notice,
		// so a repeat reads "1m 00s" rather than "30s" all over again. The due
		// time advances whether or not the notice is queued, otherwise a full
		// queue would turn this into a tight re-notify loop
		//
		_idleNoticeDueMillis = now_ + _idleIntervalMillis;
		_idle = true;
		return emitNotice(output_, " - no movement in " + DurationFormatter.format(now_ - _lastActivityMillis));
	}





	/**
	 * @return true once this source can produce nothing further
	 */
	public boolean isFinished()
	{
		return _finished;
	}





	/**
	 * Marks the source finished, so the monitor stops announcing a silence that
	 * can never end.
	 */
	public void setFinished(final boolean finished_)
	{
		_finished = finished_;
	}





	synchronized boolean isIdle()
	{
		return _idle;
	}





	synchronized long getLastActivityMillis()
	{
		return _lastActivityMillis;
	}





	synchronized long getIdleNoticeDueMillis()
	{
		return _idleNoticeDueMillis;
	}





	/**
	 * Offers a notice to the output queue, dropping it if the queue is full rather
	 * than throwing - a liveness message must never take down the thread reporting
	 * it, nor block the watchdog.
	 *
	 * @param output_  the queue to write to
	 * @param message_ the notice text, appended to this source's name
	 * @return true when the notice was queued
	 */
	private boolean emitNotice(final Deque<LogLine> output_, final String message_)
	{
		if (output_ == null)
		{
			return false;
		}

		if (output_.offer(new LogLine(null, "ctrail: " + _name + message_, null, true)))
		{
			return true;
		}

		_logger.warn("output queue full, dropping liveness notice for:{}", _name);
		return false;
	}





	@Override
	public String toString()
	{
		return _name;
	}
}
