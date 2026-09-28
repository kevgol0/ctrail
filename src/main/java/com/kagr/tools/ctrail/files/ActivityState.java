/****************************************************************************
 * FILE: ActivityState.java
 * DSCRPT: liveness bookkeeping for one input source - a tailed file or stdin.
 *         Written by a reader thread, read by the idle monitor, hence volatile.
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;





public class ActivityState
{
	/** display name used in notices, e.g. "app.log" or "stdin" */
	@Getter private final String _name;

	/** how long this source may stay silent before a notice; <= 0 disables notices */
	@Getter private final long _idleIntervalMillis;

	@Getter @Setter private volatile long _lastActivityMillis;

	@Getter @Setter private volatile long _idleNoticeDueMillis;

	/** true once an idle notice has fired and no data has arrived since */
	@Getter @Setter private volatile boolean _idle;

	/** true once the source can produce nothing further, e.g. stdin at EOF */
	@Getter @Setter private volatile boolean _finished;





	public ActivityState(@NonNull final String name_, final long nowMillis_, final long idleIntervalMillis_)
	{
		_name = name_;
		_idleIntervalMillis = idleIntervalMillis_;
		_lastActivityMillis = nowMillis_;


		//
		// with notices switched off the due time is pushed out of reach, so the
		// monitor's hot path stays a single comparison
		//
		_idleNoticeDueMillis = idleIntervalMillis_ > 0 ? nowMillis_ + idleIntervalMillis_ : Long.MAX_VALUE;
	}





	@Override
	public String toString()
	{
		return _name;
	}
}
