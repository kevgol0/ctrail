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

import org.apache.commons.lang3.StringUtils;



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

	//
	// folded copies of the two term lists. The terms are constant for the life
	// of a run while the line varies, so folding them per line was T wasted
	// allocations per line. Rebuilt only when the source list grows or the
	// case-sensitivity verdict flips
	//
	private volatile FoldedTerms _foldedIncludes;

	private volatile FoldedTerms _foldedExcludes;





	public FileSearchFilter(final String fileName_, boolean isDefaultInclude_)
	{
		_fileName = toRegEx(fileName_);
		_logger.debug("filename:{}, results in:{}", fileName_, _fileName);
		_defLineInclude = isDefaultInclude_;
	}





	/**
	 * A folded snapshot of a term list together with what it was folded from.
	 * Held as one object and published through a single volatile reference write,
	 * so a reader thread can never see the array and its provenance disagree.
	 */
	private static final class FoldedTerms
	{
		private final String[]	_terms;
		private final int		_sourceSize;
		private final boolean	_caseSensitive;

		private FoldedTerms(final String[] terms_, final int sourceSize_, final boolean caseSensitive_)
		{
			_terms = terms_;
			_sourceSize = sourceSize_;
			_caseSensitive = caseSensitive_;
		}

		private boolean isCurrentFor(final int sourceSize_, final boolean caseSensitive_)
		{
			return _sourceSize == sourceSize_ && _caseSensitive == caseSensitive_;
		}
	}





	/**
	 * Folds every term once. The source lists are LinkedLists, so this iterates
	 * rather than indexing.
	 */
	private static FoldedTerms foldTerms(final List<String> terms_, final boolean caseSensitive_)
	{
		final String[] folded = new String[terms_.size()];
		int i = 0;
		for (final String term : terms_)
		{
			folded[i] = caseSensitive_ ? term : term.toLowerCase(Locale.ROOT);
			i += 1;
		}
		return new FoldedTerms(folded, folded.length, caseSensitive_);
	}





	/**
	 * The folded include terms, rebuilt only when the live list has grown or the
	 * case-sensitivity verdict has flipped since the last fold.
	 *
	 * Size is the staleness signal because that is how terms arrive: callers
	 * append through the exposed list (CtrailProps.loadFilterTerms, and tests).
	 * Nothing replaces a term in place - FileSearchFilterFoldingTest pins that.
	 */
	private String[] foldedIncludeTerms(final boolean caseSensitive_)
	{
		final FoldedTerms cached = _foldedIncludes;
		if (cached != null && cached.isCurrentFor(_includeTerms.size(), caseSensitive_))
		{
			return cached._terms;
		}

		final FoldedTerms rebuilt = foldTerms(_includeTerms, caseSensitive_);
		_foldedIncludes = rebuilt;
		return rebuilt._terms;
	}





	/**
	 * The exclude-side counterpart of {@link #foldedIncludeTerms(boolean)}.
	 */
	private String[] foldedExcludeTerms(final boolean caseSensitive_)
	{
		final FoldedTerms cached = _foldedExcludes;
		if (cached != null && cached.isCurrentFor(_excldueTerms.size(), caseSensitive_))
		{
			return cached._terms;
		}

		final FoldedTerms rebuilt = foldTerms(_excldueTerms, caseSensitive_);
		_foldedExcludes = rebuilt;
		return rebuilt._terms;
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
		// includes trump excludes... this MUST happen first. the terms are folded
		// once and reused; only the line is folded per call
		//
		final String[] includeTerms = foldedIncludeTerms(caseSensitive);
		for (int i = 0; i < includeTerms.length; i++)
		{
			if (StringUtils.contains(normalizedLine, includeTerms[i]))
			{
				//
				// this file has a filter set, and i 
				// found a search term specified in the include
				// filter... I want to INCLUDE this line
				//
				return true;
			}
		}


		//
		// no include term matched. fileFilterDefaultsToInclude decides, and it
		// decides for an empty <includes> list too: a filter with no includes and
		// the flag false hides the file entirely. That is deliberate - see
		// FileSearchFilterTest.testDefaultIncludeBehaviour. An excludes-only
		// filter therefore requires fileFilterDefaultsToInclude=true
		//
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

		// same folded-once treatment as the include side
		final String[] excludeTerms = foldedExcludeTerms(caseSensitive);
		for (int i = 0; i < excludeTerms.length; i++)
		{
			if (StringUtils.contains(normalizedLine, excludeTerms[i]))
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
