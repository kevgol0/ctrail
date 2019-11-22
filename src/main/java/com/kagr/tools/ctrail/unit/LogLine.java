/****************************************************************************
 * FILE: LogLine.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.unit;





import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;





@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class LogLine
{
	private String _origFilename;

	@NonNull private String _line;

}
