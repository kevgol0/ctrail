/****************************************************************************
 * FILE: StringFormatter.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.unit;





import java.util.Hashtable;
import java.util.List;
import java.util.Locale;



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
    private transient String[] _keyArray;
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

    /**
     * Re-reads the cached config only when the props instance has actually been
     * replaced. This runs once per output line, so it must stay a reference
     * compare -- copying every field per line was measurable overhead on a tail.
     */
    private void refreshProps()
    {
        final CtrailProps props = CtrailProps.getInstance();
        if (props == _props)
        {
            return;
        }

        _props = props;
        _keysToColors = _props.getKeysToColors();
        _keysToFileColors = _props.getKeysToFileColors();
        _keys = _props.getKeys();
        _keysSz = _keys.size();
        _defFgColor = _props.getDefaultFgColor() == null ? ConsoleColors.WHITE : _props.getDefaultFgColor();
        _firstWordMatch = _props.isMatchFirstWord();

        //
        // getKeys() is a LinkedList, so get(i) in the per-line scan is O(n).
        // copy to an array once so formatting stays linear in key count
        //
        _keyArray = _keys.toArray(new String[0]);
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
            //
            // keys are lower-cased with Locale.ROOT at load time; match that
            // here so a Turkish-locale JVM does not fold "I" differently
            //
            _tmpStr = line_.getLine().toLowerCase(Locale.ROOT);
        }

        for (int i = 0; i < _keysSz; i++)
        {
            _tmpKey = _keyArray[i];
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
