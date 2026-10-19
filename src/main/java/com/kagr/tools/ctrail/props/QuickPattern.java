/****************************************************************************
 * FILE: QuickPattern.java
 * DSCRPT: one -q pattern in quick mode: the text as typed, compiled once,
 *         and the colour a matching line is drawn in.
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import java.util.regex.Pattern;



import lombok.NonNull;
import lombok.Value;





@Value
public class QuickPattern
{
	/** the pattern as the user typed it, for the startup banner */
	@NonNull String _source;

	/** compiled at argument parsing, never per line */
	@NonNull Pattern _pattern;

	/** ANSI colour for a line this pattern matches */
	@NonNull String _color;





	/**
	 * @param line_ the line to test
	 * @return true when the pattern is found anywhere in the line
	 */
	public boolean matches(@NonNull final String line_)
	{
		return _pattern.matcher(line_).find();
	}
}
