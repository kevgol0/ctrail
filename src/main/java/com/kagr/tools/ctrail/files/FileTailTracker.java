/****************************************************************************
 * FILE: FileTrailObject.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.io.IOException;
import java.io.RandomAccessFile;



import org.apache.commons.lang3.StringUtils;



import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.FileSearchFilter;



import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;





@Slf4j
public class FileTailTracker
{
	@Getter @Setter private RandomAccessFile _file;

	@Getter @Setter private long _lastReadPosition;

	@Getter private String _fileName;

	@Getter FileSearchFilter _fileSearchFilter;

	@Getter @Setter private boolean _defLineExclude;

	@Getter private final ActivityState _activityState;

	/** backwards scan granularity when locating the Nth-from-last line */
	private static final int _backScanChunkSize = 8192;





	public FileTailTracker(@NonNull final String fname_, @NonNull final RandomAccessFile file_)
	{
		_fileName = fname_;
		if (StringUtils.isEmpty(_fileName))
		{
			_fileName = file_.toString();
		}


		//
		// no reason to call the filter if this is false, 
		//
		setDefLineExclude(!CtrailProps.getInstance().isFileFilterDefaultsToInclude());


		setFile(file_);
		_logger.info("Filetracker for:{}", _fileName);


		//
		// liveness bookkeeping for the idle monitor; a file is never "finished",
		// it simply stops growing
		//
		_activityState = new ActivityState(_fileName,
				System.currentTimeMillis(),
				CtrailProps.getInstance().getIdleNoticeSeconds() * 1000L);


		// only look at the end of the file
		try
		{
			//
			// line-accurate tail is preferred; the legacy byte-skip remains the
			// fallback so an existing config with tailLastLines=0 is unchanged
			//
			final int tailLines = CtrailProps.getInstance().getTailLastLines();
			if (tailLines > 0)
			{
				seekToLastNLines(tailLines);
				return;
			}

			if (CtrailProps.getInstance().getSkipAheadInBytes() <= 0)
			{
				return;
			}

			if (_file.length() > CtrailProps.getInstance().getSkipAheadInBytes())
			{
				final long advacneBy = _file.length() - CtrailProps.getInstance().getSkipAheadInBytes();
				_logger.trace("advancing file:{} to position:{}, size:{}", _fileName, advacneBy, _file.length());
				_lastReadPosition = advacneBy;
			}
			_file.seek(_lastReadPosition);
		}
		catch (final IOException ex_)
		{
			_logger.error(ex_.toString());
		}
	}





	/**
	 * Positions the file pointer at the first byte of the Nth-from-last line, so that
	 * the normal read loop emits that history through the usual filtering and coloring
	 * path. Scans backwards in chunks; the file is never read whole.
	 *
	 * @param nLines_ the number of trailing lines to keep; values <= 0 are a no-op
	 * @return the position seeked to
	 * @throws IOException if the file cannot be read or seeked
	 */
	protected final long seekToLastNLines(final int nLines_) throws IOException
	{
		if (nLines_ <= 0)
		{
			return _lastReadPosition;
		}

		final long length = _file.length();
		if (length <= 0)
		{
			_lastReadPosition = 0;
			_file.seek(0);
			return 0;
		}


		//
		// a newline sitting at EOF terminates the final line rather than opening
		// a new one, so it must not be counted
		//
		long scanEnd = length;
		_file.seek(length - 1);
		if (_file.read() == '\n')
		{
			scanEnd = length - 1;
		}


		//
		// walk backwards counting line breaks; the Nth one found marks the first
		// byte of the oldest line we intend to show
		//
		final byte[] buffer = new byte[_backScanChunkSize];
		int newlinesSeen = 0;
		long result = 0;
		boolean found = false;
		while (scanEnd > 0 && !found)
		{
			final long chunkStart = Math.max(0, scanEnd - _backScanChunkSize);
			final int bytesRead = (int) (scanEnd - chunkStart);
			_file.seek(chunkStart);
			_file.readFully(buffer, 0, bytesRead);

			for (int i = bytesRead - 1; i >= 0; i--)
			{
				if (buffer[i] != '\n')
				{
					continue;
				}

				newlinesSeen += 1;
				if (newlinesSeen >= nLines_)
				{
					result = chunkStart + i + 1;
					found = true;
					break;
				}
			}

			scanEnd = chunkStart;
		}


		//
		// not enough lines in the file - start of file is the honest answer
		//
		_logger.trace("tail-{} for file:{} starts at position:{}, size:{}", nLines_, _fileName, result, length);
		_lastReadPosition = result;
		_file.seek(result);
		return result;
	}





	public void setFileSearchTerms(final FileSearchFilter fst_)
	{
		if (CtrailProps.getInstance().isEnabledFileFiltering())
		{
			_fileSearchFilter = fst_;
		}
		else
		{
			_logger.debug("file search terms disabled, not setting:{}", fst_.toString());
		}
	}





	public final long getRemainingSize() throws IOException
	{
		return _file.length() - getLastReadPosition();
	}





	public final boolean shouldExcludeLineDueToSeachTerms(final String line_)
	{
		if (line_ == null)
		{
			return false;
		}

		if (_fileSearchFilter != null)
		{
			return _fileSearchFilter.shouldExcludeLineDueToSeachTerms(line_);
		}


		//
		// no filter set — use the configured default
		//
		return _defLineExclude;
	}





	/**
	 * Applies the filter's include list. With no filter attached to this file
	 * every line is shown, which is what an unfiltered tail should do.
	 */
	public final boolean shouldIncludeLineDueToSeachTerms(final String line_)
	{
		if (line_ == null)
		{
			return false;
		}

		if (_fileSearchFilter != null)
		{
			return _fileSearchFilter.shouldIncludeLineDueToSeachTerms(line_);
		}


		//
		// default
		//
		return true;
	}





	@Override
	public String toString()
	{
		return _fileName;
	}

}
