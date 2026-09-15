/****************************************************************************
 * FILE: FileSearchTerms.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
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
	//
	// '*' and '.' get their own cases in toRegEx; these are the rest
	//
	private static final String REGEX_METACHARS = "\\+?^$[]{}()|";

	@NonNull @Getter @Setter private String _fileName;

	@Getter private List<String> _includeTerms = new LinkedList<String>();

	@Getter private List<String> _excldueTerms = new LinkedList<String>();

	@Getter private boolean _defLineInclude;





	public FileSearchFilter(final String fileName_, boolean isDefaultInclude_)
	{
		_fileName = toRegEx(fileName_);
		_logger.debug("filename:{}, results in:{}", fileName_, _fileName);
		_defLineInclude = isDefaultInclude_;
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
				//
				// '*' is our only wildcard; every other regex metacharacter in a
				// filename is a literal and must be escaped, or a name such as
				// "app(1).log" compiles into a completely different pattern
				//
				if (REGEX_METACHARS.indexOf(val) >= 0)
				{
					buff.append('\\');
				}
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
		if (line_ == null)
		{
			return false;
		}
		final boolean caseSensitive = CtrailProps.getInstance().isLineSearchCaseSensitiveMatching();
		final String normalizedLine = caseSensitive ? line_ : line_.toLowerCase(Locale.ROOT);
		//
		// includes trump excludes... this MUST happen first
		//
		for (int i = 0; i < getIncludeTerms().size(); i++)
		{
			final String includeTerm = getIncludeTerms().get(i);
			final String normalizedTerm = caseSensitive ? includeTerm : includeTerm.toLowerCase(Locale.ROOT);
			if (normalizedLine.contains(normalizedTerm))
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
		if (line_ == null)
		{
			return false;
		}

		//
		// -v / filtering.excludesEnabled turns the exclude list off wholesale
		//
		if (!CtrailProps.getInstance().isEnabledExcludeFiltering())
		{
			return false;
		}

		final boolean caseSensitive = CtrailProps.getInstance().isLineSearchCaseSensitiveMatching();
		final String normalizedLine = caseSensitive ? line_ : line_.toLowerCase(Locale.ROOT);
		for (int i = 0; i < getExcldueTerms().size(); i++)
		{
			final String excludeTerm = getExcldueTerms().get(i);
			final String normalizedTerm = caseSensitive ? excludeTerm : excludeTerm.toLowerCase(Locale.ROOT);
			if (normalizedLine.contains(normalizedTerm))
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
