/****************************************************************************
 * FILE: FileSearchTerms.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import com.kagr.tools.ctrail.files.FileReaderThread;



import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;





@Slf4j
public class FileSearchFilter
{
	@NonNull @Getter @Setter private String _fileName;

	@Getter private List<String> _includeTerms = new LinkedList<String>();

	@Getter private List<String> _excldueTerms = new LinkedList<String>();

	@Getter private boolean _defLineInclude;





	public FileSearchFilter(final String fileName_, boolean isDefaultExclude_)
	{
		_fileName = toRegEx(fileName_);
		_logger.debug("filename:{}, results in:{}", fileName_, _fileName);
		_defLineInclude = isDefaultExclude_;
	}





	private final String toRegEx(final String fileName_)
	{
		final StringBuilder buff = new StringBuilder();
		char val;
		for (int i = 0; i < fileName_.length(); i++)
		{
			val = fileName_.charAt(i);
			switch (val)
			{
			case '*':
				buff.append("(.)*");
				break;
			case '.':
				buff.append("\\.");
				break;
			default:
				buff.append(val);
				break;
			}
		}

		buff.append("$");
		return buff.toString();
	}





	public final boolean doesMatchFilename(@NonNull final String fname_)
	{
		boolean rv = false;
		try
		{
			final Pattern p = Pattern.compile(".*" + _fileName);
			final Matcher m = p.matcher(fname_);
			rv = m.find();
			if (_logger.isDebugEnabled())
			{
				_logger.debug("{} matches {}:{}", _fileName, fname_, rv);
			}

		}
		catch (final Exception ex_)
		{
			_logger.error(ex_.toString());
		}
		return rv;
	}





	public boolean shouldIncludeLineDueToSeachTerms(String line_)
	{
		//
		// includes trump excludes... this MUST happen first
		//
		for (int i = 0; i < getIncludeTerms().size(); i++)
		{
			if (line_.contains(getIncludeTerms().get(i)))
			{
				//
				// this file has a filter set, and i 
				// found a search term specified in the include
				// filter... I want to INCLUDE this line
				//
				return true;
			}
		}


		return _defLineInclude;
	}





	public final boolean shouldExcludeLineDueToSeachTerms(final String line_)
	{
		for (int i = 0; i < getExcldueTerms().size(); i++)
		{
			if (line_.contains(getExcldueTerms().get(i)))
			{
				//
				// this file has a filter set, and i 
				// found a search term specified in the exclude
				// filter... I want to EXCLUDE this line
				//
				return true;
			}
		}



		//
		// this file has a filter set, but i 
		// did not find any of the terms specified
		// in either the include or the exclude list
		//
		return false;
	}





	@Override
	public String toString()
	{
		StringBuilder buff = new StringBuilder(getClass().getSimpleName());
		buff.append(":");
		buff.append(getFileName());
		buff.append("; includes=");
		buff.append(_includeTerms.toString());
		buff.append("; excludes=");
		buff.append(_excldueTerms.toString());
		return buff.toString();
	}





}
