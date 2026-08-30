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
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(_iStream)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (!shouldEmit(line))
				{
					continue;
				}

				_output.put(new LogLine("stdin", line, null));
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
		if (_match != null)
		{
			final boolean caseSensitive = CtrailProps.getInstance().isLineSearchCaseSensitiveMatching();
			final String needle = caseSensitive ? _match : _match.toLowerCase(Locale.ROOT);
			final String haystack = caseSensitive ? line_ : line_.toLowerCase(Locale.ROOT);
			if (!haystack.contains(needle))
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
