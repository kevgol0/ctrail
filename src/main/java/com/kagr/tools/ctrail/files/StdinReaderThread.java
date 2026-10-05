/****************************************************************************
 * FILE: StdinReaderThread.java
 * DSCRPT:
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.BlockingDeque;

import org.apache.commons.lang3.StringUtils;



import com.kagr.tools.ctrail.IShutdownManager;
import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.FileSearchFilter;
import com.kagr.tools.ctrail.unit.LogLine;



import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;





@Slf4j
public class StdinReaderThread implements Runnable
{
	private final InputStream _iStream;

	//
	// must stay a BlockingDeque: the queue is bounded by maxPendingLines and
	// Deque.add() throws once it fills. put() applies back-pressure instead
	//
	@Getter @Setter(AccessLevel.PROTECTED) private BlockingDeque<LogLine> _output;

	@Getter private final String _match;

	@Getter private final FileSearchFilter _searchFilter;

	@Getter private final IShutdownManager _ender;

	@Getter private final ActivityState _activityState;

	/** config captured once; nothing changes it after startup */
	private final CtrailProps _props;

	/** -m needle, already case-folded; null when no match was requested */
	private final String _needle;

	private final boolean _caseSensitive;




	public StdinReaderThread(final InputStream is_,
			@NonNull final BlockingDeque<LogLine> output_,
			final String match_,
			final IShutdownManager shutdownMgr_,
			final FileSearchFilter filter_)
	{
		_iStream = is_;
		_match = match_;
		_ender = shutdownMgr_;
		_searchFilter = filter_;
		_props = CtrailProps.getInstance();


		//
		// fold the needle once, the way FileReaderThread does. CTRAIL-20 removed
		// this duplication from the file path and left the stdin path folding
		// both the needle and the line on every line
		//
		_caseSensitive = _props.isLineSearchCaseSensitiveMatching();
		_needle = match_ == null ? null : (_caseSensitive ? match_ : match_.toLowerCase(Locale.ROOT));


		//
		// liveness bookkeeping for the idle monitor. the notice always names the
		// source "stdin" regardless of prependFilenameToLine, which governs only
		// the per-line prefix
		//
		_activityState = new ActivityState(CtrailProps.STDIN_FILTER_NAME,
				System.currentTimeMillis(),
				_props.getIdleNoticeSeconds() * 1000L);

		setOutput(output_);
	}





	@Override
	public void run()
	{
		//
		// a single read-loop covers every combination. the old code branched
		// filter-or-match and read nothing at all when neither was supplied,
		// so a plain "cat file | ctr" produced no output whatsoever. it also
		// ignored -m entirely whenever a stdin filter happened to be configured
		//
		readStdin();


		if (_logger.isTraceEnabled())
		{
			_logger.trace("finished read from std-in");
		}


		//
		// the pipe is closed: mark the source finished so the idle monitor stops
		// announcing silence that can never end
		//
		_activityState.setFinished(true);


		//
		// stop the output thread as
		// soon as they finish processing
		//
		if (_ender != null)
		{
			_ender.initiateShutdown();
		}
	}





	private void readStdin()
	{
		//
		// the source name was hardcoded, so piped input always carried a
		// "stdin:" prefix even with prependFilenameToLine=false, which the file
		// reader has always respected
		//
		final String source = _props.isPrependFilenameToLine()
				? CtrailProps.STDIN_FILTER_NAME
				: null;

		//
		// explicit charset: the default constructor takes the platform default, so
		// `cat f | ctr` and `ctr f` could render identical bytes differently
		//
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(_iStream, _props.getCharset())))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (!shouldEmit(line))
				{
					continue;
				}

				//
				// record movement BEFORE the line is queued, so a "resumed"
				// notice lands ahead of the data that ended the silence. only
				// lines that survive shouldEmit count - output you cannot see is
				// not movement
				//
				IdleMonitorThread.noteActivity(_activityState, _output);
				_output.put(new LogLine(source, line, _searchFilter));
			}
		}
		catch (final InterruptedException ex_)
		{
			_logger.warn("Interrupted! - breaking out of std-in read-loop");
			Thread.currentThread().interrupt();
		}
		catch (final Exception ex_)
		{
			_logger.error(ex_.toString());
		}
	}





	private boolean shouldEmit(final String line_)
	{
		//
		// command line dynamic match, honoring the configured case sensitivity
		// the same way the file reader does
		//
		if (_needle != null)
		{
			final String haystack = _caseSensitive ? line_ : line_.toLowerCase(Locale.ROOT);
			if (!StringUtils.contains(haystack, _needle))
			{
				return false;
			}
		}

		if (_searchFilter == null)
		{
			return true;
		}


		//
		// should I show this line - based off of config
		//
		if (_searchFilter.shouldExcludeLineDueToSeachTerms(line_))
		{
			return false;
		}


		//
		// the "default-should-include" is taken care of in the include check
		//
		return _searchFilter.shouldIncludeLineDueToSeachTerms(line_);
	}

}
