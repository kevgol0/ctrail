/****************************************************************************
 * FILE: StringFormatter.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.unit;





import java.util.Collections;
import java.util.Hashtable;
import java.util.List;



import org.apache.commons.lang3.StringUtils;



import com.kagr.tools.ctrail.ConsoleColors;
import com.kagr.tools.ctrail.CtrailProps;





public class LineFormatter
{
	private static String _reset = ConsoleColors.RESET;


	private static CtrailProps _props = CtrailProps.getInstance();
	private static Hashtable<String, String> _keysToColors = _props.getKeysToColors();
	private static Hashtable<String, String> _keysToFileColors = _props.getKeysToFileColors();
	
	private static List<String> _keys =_props.getKeys();
	private static int _keysSz = _keys.size();
	
	private static final String DEF_FG_COLOR = _props.getDefaultFgColor();
	private static boolean firstWordMatch = _props.isMatchFirstWord();


	private transient String _tmpKey;
	private transient String _tmpRslt;
	private transient String _tmpStr;
	private transient String _tmpLogClr;
	private transient String _tmpFileClr;





	public String format(String str_)
	{
		if (str_ == null) return str_;
		_tmpRslt = null;
		_tmpKey = null;
		_tmpLogClr = null;
		for (int i = 0; i < _keysSz; i++)
		{
			_tmpKey = _keys.get(i);

			if (_props.isLineSearchCaseSensitiveMatching())
			{
				_tmpStr = str_;
			}
			else
			{
				_tmpKey = _tmpKey.toLowerCase();
				_tmpStr = str_.toLowerCase();
			}

			if (_tmpStr.contains(_tmpKey))
			{
				_tmpLogClr = _keysToColors.get(_tmpKey);
				break;
			}
		}


		if (_tmpLogClr == null)
			_tmpRslt = DEF_FG_COLOR + str_ + _reset;
		else
			_tmpRslt = _tmpLogClr + str_ + _reset;

		return _tmpRslt;
	}





	public String format(LogLine line_)
	{
		if (line_ == null)
			return "";
		else if (StringUtils.isEmpty(line_.getLine()))
			return "";


		_tmpRslt = null;
		_tmpKey = null;
		_tmpLogClr = null;
		_tmpFileClr = null;

		if (_props.isLineSearchCaseSensitiveMatching())
		{
			_tmpStr = line_.getLine();
		}
		else
		{
			// the key has already been made lower case
			// when initializing... only the line needs 
			// to be made lower case
			_tmpStr = line_.getLine().toLowerCase();
		}


		for (int i = 0; i < _keysSz; i++)
		{
			_tmpKey = _keys.get(i);
			if (_tmpStr.contains(_tmpKey))
			{
				_tmpLogClr = _keysToColors.get(_tmpKey);
				_tmpFileClr = _keysToFileColors.get(_tmpKey);

				if (firstWordMatch)
					break;
			}
		}



		if (line_.getOrigFilename() != null)
		{
			if (_tmpFileClr != null)
			{
				_tmpRslt = _tmpFileClr + line_.getOrigFilename() + ":";
			}
			else if (_tmpLogClr != null)
			{
				_tmpRslt = _tmpLogClr + line_.getOrigFilename() + ":";
			}
			else
			{
				_tmpRslt = DEF_FG_COLOR + line_.getOrigFilename() + ":";
			}
		}
		else
		{
			_tmpRslt = "";
		}



		if (_tmpLogClr != null)
		{
			_tmpRslt += _tmpLogClr + line_.getLine() + _reset;
		}
		else
		{
			_tmpRslt += DEF_FG_COLOR + line_.getLine() + _reset;
		}

		return _tmpRslt;
	}

}
