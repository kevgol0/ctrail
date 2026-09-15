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

    // cached config, refreshed only when the props instance is swapped
    private CtrailProps _props;
    private Hashtable<String, String> _keysToColors;
    private Hashtable<String, String> _keysToFileColors;
    private List<String> _keys;
    private String[] _keyArray;
    private int _keysSz;
    private String _defFgColor;
    private boolean _firstWordMatch;
    private boolean _caseSensitive;

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
        // bail out fast when the props instance has not changed
        final CtrailProps props = CtrailProps.getInstance();
        if (props == _props)
        {
            return;
        }

        // config was swapped (e.g. reload) -- refresh every cached field
        _props = props;
        _keysToColors = _props.getKeysToColors();
        _keysToFileColors = _props.getKeysToFileColors();
        _keys = _props.getKeys();
        _keysSz = _keys.size();
        _defFgColor = _props.getDefaultFgColor() == null ? ConsoleColors.WHITE : _props.getDefaultFgColor();
        _firstWordMatch = _props.isMatchFirstWord();
        _caseSensitive = _props.isLineSearchCaseSensitiveMatching();

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

        // pick up a swapped config, then use locals so format() stays thread-safe
        refreshProps();
        String logClr = null;
        String fileClr = null;

        //
        // keys are lower-cased with Locale.ROOT at load time; match that here so
        // a Turkish-locale JVM does not fold "I" differently
        //
        final String searchLine = _caseSensitive ? line_.getLine() : line_.getLine().toLowerCase(Locale.ROOT);

        // find matching color keyword against the cached key array
        for (int i = 0; i < _keysSz; i++)
        {
            final String key = _keyArray[i];
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
