/****************************************************************************
 * FILE: FileReaderThread.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.BlockingDeque;



import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.FileSearchFilter;
import com.kagr.tools.ctrail.unit.LogLine;



import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;





@Slf4j
public class FileReaderThread implements Runnable
{
	@Getter @Setter(AccessLevel.PRIVATE) private BlockingDeque<FileTailTracker> _fileTrackers;
	@Getter @Setter(AccessLevel.PRIVATE) private Deque<LogLine> _output;
	@Getter @Setter private int _maxLinesPerThread;
	@Getter @Setter private boolean _defLineExclude;

	// command line matching
	@Getter @Setter(AccessLevel.PRIVATE) private String _match;

	private CtrailProps _props;





	public FileReaderThread(BlockingDeque<FileTailTracker> fileTrackers_, Deque<LogLine> strOutput_, String match_)
	{
		_props = CtrailProps.getInstance();
		setFileTrackers(fileTrackers_);
		setOutput(strOutput_);
		setMatch(match_);
		setMaxLinesPerThread(CtrailProps.getInstance().getMaxProcessingLinesPerThread());
		setDefLineExclude(!CtrailProps.getInstance().isFileFilterDefaultsToInclude());
	}





	@Override
	public void run()
	{
		long szToRead;
		FileTailTracker tracker;
		int nFiles = _fileTrackers.size();
		int nEmptyItrCnt = 0;
		int sleepTime = _props.getNoChangeSleepTimeMillis();
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
				// check for advancements 
				//
				szToRead = tracker.getFile().length() - tracker.getLastReadPosition();
				if (szToRead > 0)
				{
					nEmptyItrCnt = 0;

					// if this fails, (meaning there was a file read error)
					// then the file will not return unto the queue...
					readToFilePosition(tracker);


					if (_props.isBlankLineOnFileChange())
						_output.add(new LogLine(null, "", null));
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
			catch (InterruptedException ex_)
			{
				_logger.error(ex_.toString());
				_logger.warn("Interrupted! - breaking out of read-loop");
				break;
			}
			catch (IOException ex_)
			{
				_logger.error(ex_.toString());
				if (_fileTrackers.size() <= 0)
				{
					_logger.warn("I/O error resulted in no additional files for processing, breaking out of read-loop");
					break;
				}
			}

		}
	}





	private int readToFilePosition(FileTailTracker tracker_) throws IOException
	{
		long readPos = tracker_.getFile().getFilePointer();
		int nReadLines = 0;
		long eof = tracker_.getFile().length();
		String line;
		while (readPos < eof)
		{
			line = tracker_.getFile().readLine();
			if (line == null) break;

			if (_match != null && !line.contains(_match))
			{
				continue;
			}

			if (shouldExcludeLineDueToSeachTerms(line, tracker_.getFileSearchFilter()))
			{
				continue;
			}

			if (_props.isPrependFilenameToLine())
			{
				_output.add(new LogLine(tracker_.getFileName(), line, tracker_.getFileSearchFilter()));
			}
			else
			{
				_output.add(new LogLine(null, line, tracker_.getFileSearchFilter()));
			}
			readPos = tracker_.getFile().getFilePointer();
			tracker_.setLastReadPosition(readPos);


			nReadLines += 1;
			if (nReadLines >= _maxLinesPerThread)
			{
				return nReadLines;
			}
		}
		return nReadLines;
	}





	private boolean shouldExcludeLineDueToSeachTerms(String line_, FileSearchFilter fileSearchTerms_)
	{
		//
		// see if this file has FileSearchTerms specified
		//
		if (fileSearchTerms_ != null)
		{
			for (int i = 0; i < fileSearchTerms_.getExcldueTerms().size(); i++ )
			{
				if (line_.contains(fileSearchTerms_.getExcldueTerms().get(i)))
				{
					//
					// this file has a filter set, and i 
					// found a search term specified in the exclude
					// filter... I want to EXCLUDE this line
					//
					return true;
				}
			}
			
			
			for (int i = 0; i < fileSearchTerms_.getIncludeTerms().size(); i++ )
			{
				if (line_.contains(fileSearchTerms_.getIncludeTerms().get(i)))
				{
					//
					// this file has a filter set, and i 
					// found a search term specified in the include
					// filter... I want to INCLUDE this line
					//
					return false;
				}
			}


			//
			// this file has a filter set, but i 
			// did not find any of the terms specified
			// in either the include or the exclude list
			//
			return _defLineExclude;
		}



		//
		// no search term associated with this line,
		// i want to INCLUDE it
		//
		return false;
	}



}
