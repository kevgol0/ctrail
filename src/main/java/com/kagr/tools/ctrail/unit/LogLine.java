/****************************************************************************
 * FILE: LogLine.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.unit;





import com.kagr.tools.ctrail.props.FileSearchFilter;



import lombok.Data;





@Data
public class LogLine
{
	private String _origFilename;

	private String _line;

	FileSearchFilter _fileSearchFilters;





	public LogLine(String origFileName_, String line_, FileSearchFilter fst_)
	{
		setOrigFilename(origFileName_);
		setLine(line_);
		setFileSearchFilters(fst_);
	}

}
