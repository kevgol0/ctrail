/****************************************************************************
 * FILE: FileTrailObject.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Deque;



import org.apache.commons.lang3.StringUtils;



import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.FileSearchFilter;
import com.kagr.tools.ctrail.unit.LogLine;



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
			//
			// a tracker whose position could not be established is worse than no
			// tracker: the file pointer and _lastReadPosition disagree, which
			// either spins the reader at 100% CPU or dumps the whole file
			//
			_logger.error("could not position {}: {}", _fileName, ex_.toString());
			close();
			throw new IllegalStateException("cannot position file: " + _fileName, ex_);
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
		// a terminator sitting at EOF closes the final line rather than opening a
		// new one, so it must not be counted. \r\n is one terminator, so both
		// bytes are stepped over
		//
		long scanEnd = length;
		_file.seek(length - 1);
		final int lastByte = _file.read();
		if (lastByte == '\n' || lastByte == '\r')
		{
			scanEnd = length - 1;
			if (lastByte == '\n' && scanEnd > 0)
			{
				_file.seek(scanEnd - 1);
				if (_file.read() == '\r')
				{
					scanEnd -= 1;
				}
			}
		}


		//
		// walk backwards counting line breaks; the Nth one found marks the first
		// byte of the oldest line we intend to show
		//
		final byte[] buffer = new byte[_backScanChunkSize];
		int newlinesSeen = 0;
		long result = 0;
		boolean found = false;

		// the byte one position higher than the cursor, carried across chunks
		int previousByte = lastByte;
		while (scanEnd > 0 && !found)
		{
			final long chunkStart = Math.max(0, scanEnd - _backScanChunkSize);
			final int bytesRead = (int) (scanEnd - chunkStart);
			_file.seek(chunkStart);
			_file.readFully(buffer, 0, bytesRead);

			for (int i = bytesRead - 1; i >= 0; i--)
			{
				final byte b = buffer[i];


				//
				// RandomAccessFile.readLine() treats \r, \n and \r\n alike, so the
				// scan must too - counting only \n meant a CR-terminated file found
				// no breaks at all and replayed whole.
				//
				// A \r is a terminator only when it is not the first half of a
				// \r\n already counted. previousByte carries across chunk
				// boundaries, so a pair split by the 8k window is still one break
				//
				final boolean isBreak = b == '\n' || (b == '\r' && previousByte != '\n');
				previousByte = b;
				if (!isBreak)
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





	/**
	 * Detects a rotated file - one that has shrunk below where we were reading -
	 * and restarts from the beginning of the new content.
	 *
	 * Without this the read position stays past EOF, getRemainingSize() is
	 * permanently negative, and the tail goes silent forever. Worse, the idle
	 * monitor then reports that silence as fact, so ctrail asserts "no movement"
	 * about a file that is actively being written.
	 *
	 * Rename-and-create rotation is NOT handled: the open RandomAccessFile keeps
	 * the old inode, so following that needs a reopen by path, which is a feature
	 * rather than this fix.
	 *
	 * @param output_ the queue the rotation notice is written to, may be null
	 * @return true when a rotation was detected and the position reset
	 * @throws IOException if the file cannot be measured or seeked
	 */
	public final boolean handleRotation(final Deque<LogLine> output_) throws IOException
	{
		if (_file.length() >= _lastReadPosition)
		{
			return false;
		}


		//
		// say so rather than silently restarting: when ctrail changes what it is
		// doing, it tells you - the same principle the liveness notices rest on
		//
		_logger.info("file:{} shrank below the read position, treating as rotated", _fileName);
		_lastReadPosition = 0;
		_file.seek(0);

		if (output_ != null && !output_.offer(new LogLine(null, "ctrail: " + _fileName + " - rotated, following new file", null, true)))
		{
			_logger.warn("output queue full, dropping rotation notice for:{}", _fileName);
		}
		return true;
	}





	/**
	 * Releases the underlying file handle. Nothing closed these before, so a
	 * multi-file tail held every descriptor until the JVM exited and shutdown()
	 * omitted them entirely.
	 */
	public final void close()
	{
		try
		{
			_file.close();
		}
		catch (final IOException ex_)
		{
			_logger.warn("failed closing:{} - {}", _fileName, ex_.toString());
		}
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
		// no filter on this source, so there is nothing to exclude against.
		// fileFilterDefaultsToInclude is the verdict for a line that matched
		// neither list WITHIN a filter, and FileSearchFilter already applies it.
		// Applying it a second time here hid every line of any file that matched
		// no <filefilter> - which, with the shipped config, is most files
		//
		return false;
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
