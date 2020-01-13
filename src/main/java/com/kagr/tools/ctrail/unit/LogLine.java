/****************************************************************************
 * FILE: LogLine.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.unit;





import com.kagr.tools.ctrail.props.FileSearchFilter;



import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;





@Data
@RequiredArgsConstructor
public class LogLine
{
	@NonNull private String _origFilename;

	@NonNull private String _line;

	private FileSearchFilter _fileSearchFilters;





	public LogLine(String origFileName_, String line_, FileSearchFilter fst_)
	{
		setOrigFilename(origFileName_);
		setLine(line_);
		setFileSearchFilters(fst_);
	}

}
