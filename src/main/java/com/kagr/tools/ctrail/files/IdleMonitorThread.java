/****************************************************************************
 * FILE: IdleMonitorThread.java
 * DSCRPT: daemon watchdog that asks each source whether it has gone quiet.
 *         The transitions themselves live on ActivityState, which owns the
 *         state they mutate; this class only supplies the clock.
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.util.Deque;
import java.util.List;



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





	/**
	 * @param sources_ a snapshot list of the sources to watch. It must NOT be the
	 *                 live tracker deque: the reader thread takes trackers out of
	 *                 that deque while reading them, so a monitor iterating it
	 *                 would intermittently not see them
	 * @param output_  the queue notices are written to
	 */
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
				Thread.currentThread().interrupt();
				break;
			}


			//
			// iteration, not streams; the state machine lives on ActivityState
			//
			final long now = System.currentTimeMillis();
			for (int i = 0; i < _sources.size(); i++)
			{
				_sources.get(i).checkForIdle(_output, now);
			}
		}

		_logger.debug("idle monitor stopped");
	}





	/**
	 * Records that a source produced a line. Kept here as a one-line helper so the
	 * readers do not each have to reach for the clock.
	 *
	 * @param source_ the source that produced a line, may be null
	 * @param output_ the queue a resume notice is written to
	 * @return true when a resume notice was emitted
	 */
	public static boolean noteActivity(final ActivityState source_, final Deque<LogLine> output_)
	{
		if (source_ == null)
		{
			return false;
		}

		return source_.noteActivity(output_, System.currentTimeMillis());
	}
}
