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


		// only look at the end of the file
		try
		{
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
		// default
		//
		return false;
	}





	@Override
	public String toString()
	{
		return _fileName;
	}

}
