/****************************************************************************
 * FILE: FileTrailObject.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.io.IOException;
import java.io.RandomAccessFile;



import org.apache.commons.lang3.StringUtils;



import com.kagr.tools.ctrail.CtrailProps;



import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;





public class FileTailTracker
{
	@Getter @Setter private RandomAccessFile _file;
	@Getter @Setter private long _lastReadPosition;

	@Getter private String _fileName;





	public FileTailTracker(@NonNull String fname_, @NonNull RandomAccessFile file_)
	{
		_fileName = fname_;
		if (StringUtils.isEmpty(_fileName))
			_fileName = file_.toString();
		setFile(file_);


		// only look at the end of the file
		try
		{
			if (_file.length() > CtrailProps.getInstance().getSkipAheadInBytes())
				_lastReadPosition = _file.length() - CtrailProps.getInstance().getSkipAheadInBytes();
			_file.seek(_lastReadPosition);
		}
		catch (IOException ex_)
		{
			ex_.printStackTrace(System.err);
		}
	}

}
