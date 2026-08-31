/****************************************************************************
 * FILE: StringFormatter.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.unit;





import java.util.Hashtable;
import java.util.List;



import org.apache.commons.lang3.StringUtils;



import com.kagr.tools.ctrail.ConsoleColors;
import com.kagr.tools.ctrail.props.CtrailProps;





public class LineFormatter
{
    private static final String _reset = ConsoleColors.RESET;

    private transient CtrailProps _props;
    private transient Hashtable<String, String> _keysToColors;
    private transient Hashtable<String, String> _keysToFileColors;
    private transient List<String> _keys;
    private transient int _keysSz;
    private transient String _defFgColor;
    private transient boolean _firstWordMatch;

    private transient String _tmpKey;
    private transient String _tmpRslt;
    private transient String _tmpStr;
    private transient String _tmpLogClr;
    private transient String _tmpFileClr;

    public LineFormatter()
    {
        refreshProps();
    }

    private void refreshProps()
    {
        _props = CtrailProps.getInstance();
        _keysToColors = _props.getKeysToColors();
        _keysToFileColors = _props.getKeysToFileColors();
        _keys = _props.getKeys();
        _keysSz = _keys.size();
        _defFgColor = _props.getDefaultFgColor();
        _firstWordMatch = _props.isMatchFirstWord();
    }

    public String format(final LogLine line_)
    {
        if (line_ == null)
        {
            return "";
        }
        if (StringUtils.isEmpty(line_.getLine()))
        {
            return "";
        }

        refreshProps();

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
            _tmpStr = line_.getLine().toLowerCase();
        }

        for (int i = 0; i < _keysSz; i++)
        {
            _tmpKey = _keys.get(i);
            if (_tmpStr.contains(_tmpKey))
            {
                _tmpLogClr = _keysToColors.get(_tmpKey);
                _tmpFileClr = _keysToFileColors.get(_tmpKey);

                if (_firstWordMatch)
                {
                    break;
                }
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
                _tmpRslt = _defFgColor + line_.getOrigFilename() + ":";
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
            _tmpRslt += _defFgColor + line_.getLine() + _reset;
        }

        return _tmpRslt;
    }

}
