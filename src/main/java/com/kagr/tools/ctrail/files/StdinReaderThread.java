/****************************************************************************
 * FILE: StdinReaderThread.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Deque;



import com.kagr.tools.ctrail.IShutdownManager;
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

	@Getter @Setter(AccessLevel.PROTECTED) private Deque<LogLine> _output;

	@Getter private final String _match;

	@Getter private final FileSearchFilter _searchFilter;

	@Getter private final IShutdownManager _ender;




	public StdinReaderThread(final InputStream is_,
			@NonNull final Deque<LogLine> output_,
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
		if (_searchFilter != null)
		{
			runWithSearchFiler();
		}
		else if (_match != null)
		{
			runWithMatch();
		}


		if (_logger.isTraceEnabled())
		{
			_logger.trace("finished read from std-in");
		}


		//
		// stop the output thread as 
		// soon as they finish processing
		//
		_ender.initiateShutdown();
	}





	private void runWithSearchFiler()
	{
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(_iStream)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				//
				// should I show this line - based off of config
				//
				if (_searchFilter.shouldExcludeLineDueToSeachTerms(line))
				{
					continue;
				}


				//
				// the "default-should-include" is taken care of in the include check
				//
				else if (_searchFilter.shouldIncludeLineDueToSeachTerms(line))
				{
					_output.add(new LogLine("stdin", line, null));
				}
			}
		}
		catch (final Exception ex_)
		{
			_logger.error(ex_.toString());
		}

	}





	private void runWithMatch()
	{
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(_iStream)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				//
				// command line dynamic match?
				//
				if (line.contains(_match))
				{
					_output.add(new LogLine("stdin", line, null));
					continue;
				}
			}
		}
		catch (final Exception ex_)
		{
			_logger.error(ex_.toString());
		}
	}

}
