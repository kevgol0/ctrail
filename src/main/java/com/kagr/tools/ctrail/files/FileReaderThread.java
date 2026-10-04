/****************************************************************************
 * FILE: FileReaderThread.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.io.IOException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
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
public class FileReaderThread implements Runnable
{
	@Getter @Setter(AccessLevel.PRIVATE) private BlockingDeque<FileTailTracker> _fileTrackers;

	//
	// must stay a BlockingDeque: the queue is bounded by maxPendingLines and
	// Deque.add() throws IllegalStateException once it fills, which killed this
	// thread before it could signal shutdown. put() blocks instead
	//
	@Getter @Setter(AccessLevel.PRIVATE) private BlockingDeque<LogLine> _output;

	@Getter @Setter private int _maxLinesPerThread;


	// command line matching
	@Getter @Setter(AccessLevel.PRIVATE) private String _match;

	private final CtrailProps		_props;

	/** -m needle, already case-folded; null when no match was requested */
	private final String			_needle;

	/** cached once: the config cannot change for the life of this thread */
	private final boolean			_caseSensitive;
	private final IShutdownManager	_ender;





	public FileReaderThread(@NonNull final BlockingDeque<FileTailTracker> fileTrackers_,
			@NonNull final BlockingDeque<LogLine> strOutput_,
			final String match_,
			@NonNull final IShutdownManager smgr_)
	{
		_props = CtrailProps.getInstance();
		_ender = smgr_;
		setFileTrackers(fileTrackers_);
		setOutput(strOutput_);
		setMatch(match_);
		setMaxLinesPerThread(CtrailProps.getInstance().getMaxProcessingLinesPerThread());


		//
		// fold the needle once. It was folded per line, twice over, because the
		// same match ran inline and again inside shouldEmit
		//
		_caseSensitive = _props.isLineSearchCaseSensitiveMatching();
		_needle = match_ == null ? null : (_caseSensitive ? match_ : match_.toLowerCase(Locale.ROOT));
	}





	@Override
	public void run()
	{
		long szToRead;
		FileTailTracker tracker = null;
		int nFiles = _fileTrackers.size();
		int nEmptyItrCnt = 0;
		final int sleepTime = _props.getNoChangeSleepTimeMillis();
		while (true)
		{
			try
			{
				tracker = _fileTrackers.take();
				if (tracker == null)
				{
					_logger.error("null tracker retrieed, exiting run loop");
					break;
				}



				//
				// check for advancements... if there is a 
				// failure in file ptr, then this file will 
				// not be added back into the queue...
				//
				//
				// a rotated file has shrunk below the read position; reset and say
				// so before measuring, or the tail goes permanently silent
				//
				tracker.handleRotation(_output);

				szToRead = tracker.getRemainingSize();
				if (szToRead > 0)
				{
					nEmptyItrCnt = 0;

					// there is more data to read, do so...
					// if this fails, (meaning there was a file read error)
					// then the file will not return unto the queue...
					readToFilePosition(tracker);


					if (_props.isBlankLineOnFileChange())
					{
						_output.put(new LogLine(null, "", null));
					}
				}
				else
				{
					nEmptyItrCnt += 1;
				}

				_fileTrackers.putLast(tracker);


				//
				// i have check all the files and no one
				// has advanced, take a break
				//
				if (nEmptyItrCnt > nFiles)
				{
					Thread.sleep(sleepTime);
					nEmptyItrCnt = 0;
				}
			}
			catch (final InterruptedException ex_)
			{
				_logger.error(ex_.toString());
				_logger.warn("Interrupted! - breaking out of read-loop");
				break;
			}
			catch (final IOException ex_)
			{
				// either a ptr-seek, or file read error happened
				// ither way, there is one less file to read from..
				nFiles -= 1;

				// show the error
				_logger.error(ex_.toString());


				//
				// the tracker was taken off the deque and is not going back. Close
				// its handle, mark the source finished so the idle monitor stops
				// announcing a silence that can never end, and tell the user on
				// stdout - the exception itself only reaches the log
				//
				if (tracker != null)
				{
					tracker.getActivityState().setFinished(true);
					if (!_output.offer(new LogLine(null, "ctrail: " + tracker.getFileName() + " - read error, no longer watching", null, true)))
					{
						_logger.warn("output queue full, dropping read-error notice for:{}", tracker.getFileName());
					}
					tracker.close();
				}

				// am I done?
				if (_fileTrackers.size() <= 0)
				{
					_logger.warn("I/O error resulted in no additional files for processing, breaking out of read-loop");
					break;
				}
			}
		}


		//
		// initiate shutdown
		//
		if (_ender != null)
		{
			_ender.initiateShutdown();
		}
	}





	private final int readToFilePosition(final FileTailTracker tracker_) throws IOException, InterruptedException
	{
		String line;
		int nReadLines = 0;
		long readPos = tracker_.getFile().getFilePointer();
		final long eof = tracker_.getFile().length();
		while (readPos < eof)
		{
			line = tracker_.readLine(_props.getCharset());
			if (line == null)
			{
				break;
			}

			//
			// consumed bytes must be recorded whether or not the line survives
			// filtering. only crediting emitted lines left lastReadPosition
			// permanently behind EOF, so getRemainingSize() never reached zero
			// and the run-loop spun without ever sleeping. record this BEFORE any
			// filter `continue` below, or the spin bug returns for filtered lines
			//
			readPos = tracker_.getFile().getFilePointer();
			tracker_.setLastReadPosition(readPos);

			if (!shouldEmit(tracker_, line))
			{
				continue;
			}

			//
			// record movement BEFORE the line is queued, so that a "resumed"
			// notice lands ahead of the data that ended the silence. Only lines
			// that survive matching and filtering count - output you cannot see
			// is not movement
			//
			IdleMonitorThread.noteActivity(tracker_.getActivityState(), _output);

			if (_props.isPrependFilenameToLine())
			{
				_output.put(new LogLine(tracker_.getFileName(), line, tracker_.getFileSearchFilter()));
			}
			else
			{
				_output.put(new LogLine(null, line, tracker_.getFileSearchFilter()));
			}

			nReadLines += 1;
			if (nReadLines >= _maxLinesPerThread)
			{
				return nReadLines;
			}
		}
		return nReadLines;
	}





	/**
	 * Command-line match first, then the file's exclude list, then its include
	 * list. Includes were previously only honored for stdin, so configured
	 * &lt;includes&gt; entries had no effect at all when tailing files.
	 */
	private boolean shouldEmit(final FileTailTracker tracker_, final String line_)
	{
		//
		// _needle is folded once in the constructor rather than per line; only the
		// line itself has to be folded here
		//
		if (_needle != null)
		{
			final String haystack = _caseSensitive ? line_ : line_.toLowerCase(Locale.ROOT);
			if (!StringUtils.contains(haystack, _needle))
			{
				return false;
			}
		}

		if (tracker_.shouldExcludeLineDueToSeachTerms(line_))
		{
			return false;
		}

		return tracker_.shouldIncludeLineDueToSeachTerms(line_);
	}





}
