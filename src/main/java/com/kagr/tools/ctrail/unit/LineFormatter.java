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

    private final CtrailProps _props;
    private final Hashtable<String, String> _keysToColors;
    private final Hashtable<String, String> _keysToFileColors;
    private final List<String> _keys;
    private final int _keysSz;
    private final String _defFgColor;
    private final boolean _firstWordMatch;
    private final boolean _caseSensitive;

    public LineFormatter()
    {
        _props = CtrailProps.getInstance();
        _keysToColors = _props.getKeysToColors();
        _keysToFileColors = _props.getKeysToFileColors();
        _keys = _props.getKeys();
        _keysSz = _keys.size();
        _defFgColor = _props.getDefaultFgColor();
        _firstWordMatch = _props.isMatchFirstWord();
        _caseSensitive = _props.isLineSearchCaseSensitiveMatching();
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

        // local variables for thread safety
        String logClr = null;
        String fileClr = null;

        // normalize line for keyword matching
        final String searchLine = _caseSensitive ? line_.getLine() : line_.getLine().toLowerCase();

        // find matching color keyword
        for (int i = 0; i < _keysSz; i++)
        {
            final String key = _keys.get(i);
            if (searchLine.contains(key))
            {
                logClr = _keysToColors.get(key);
                fileClr = _keysToFileColors.get(key);

                if (_firstWordMatch)
                {
                    break;
                }
            }
        }

        // build the filename prefix
        String result;
        if (line_.getOrigFilename() != null)
        {
            if (fileClr != null)
            {
                result = fileClr + line_.getOrigFilename() + ":";
            }
            else if (logClr != null)
            {
                result = logClr + line_.getOrigFilename() + ":";
            }
            else
            {
                result = _defFgColor + line_.getOrigFilename() + ":";
            }
        }
        else
        {
            result = "";
        }

        // append the colored line content
        if (logClr != null)
        {
            result += logClr + line_.getLine() + _reset;
        }
        else
        {
            result += _defFgColor + line_.getLine() + _reset;
        }

        return result;
    }

}
