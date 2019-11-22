/****************************************************************************
 * FILE: StringFormatter.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.util.Collections;
import java.util.Hashtable;
import java.util.List;





public class StringFormatter
{
	private static String _reset = ConsoleColors.RESET;


	private static CtrailProps _props = CtrailProps.getInstance();
	private static Hashtable<String, String> _keysToColors = _props.getKeysToColors();
	private static List<String> _keys = Collections.list(_keysToColors.keys());;
	private static int _keysSz = _keys.size();


	private transient String _tmpKey;
	private transient String _tmpRslt;
	private transient String _tmpStr;
	private transient String _tmpRsltClr;





	public String format(String str_)
	{
		if (str_ == null) return str_;
		_tmpRslt = null;
		_tmpKey = null;
		_tmpRsltClr = null;
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
				_tmpRsltClr = _keysToColors.get(_tmpKey);
				break;
			}
		}


		if (_tmpRsltClr == null)
			_tmpRslt = _keysToColors.get("DEFAULT") + str_ + _reset;
		else
			_tmpRslt = _tmpRsltClr + str_ + _reset;

		return _tmpRslt;
	}
}
