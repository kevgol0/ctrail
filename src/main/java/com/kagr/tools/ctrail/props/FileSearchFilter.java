/****************************************************************************
 * FILE: FileSearchTerms.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import org.apache.commons.io.FilenameUtils;



import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;





@Data
@Slf4j
public class FileSearchFilter
{
	@NonNull @Getter @Setter private String _fileName;

	@Getter private List<String> _includeTerms = new LinkedList<String>();

	@Getter private List<String> _excldueTerms = new LinkedList<String>();





	public FileSearchFilter(String fileName_)
	{
		_fileName = toRegEx(fileName_);
		_logger.debug("filename:{}, results in:{}", _fileName, fileName_);
	}





	private @NonNull String toRegEx(String fileName_)
	{
		StringBuilder buff = new StringBuilder();
		char val;
		for (int i = 0; i < fileName_.length(); i++)
		{
			val = fileName_.charAt(i);
			switch (val)
			{
			case '*':
				buff.append("(\\w)*");
				break;
			case '.':
				buff.append("\\.");
				break;
			default:
				buff.append(val);
				break;
			}
		}

		return buff.toString();
	}





	public boolean doesMatchFilename(@NonNull String fname_)
	{
		boolean rv = false;
		try
		{
			Pattern p = Pattern.compile(_fileName);
			Matcher m = p.matcher(fname_);
			rv = m.find();
			if (_logger.isDebugEnabled())
				_logger.debug("{} matches {}:{}", _fileName, fname_, rv);
		}
		catch (Exception ex_)
		{
			_logger.error(ex_.toString());
		}
		return rv;
	}



}
