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

    //
    // config captured once at construction. CtrailEntryPoint settles the config
    // - load, then the -e/-f/-v/-n overrides - before it builds the writer
    // thread that owns this formatter, so there is nothing left to pick up
    //
    private final Hashtable<String, String> _keysToColors;
    private final Hashtable<String, String> _keysToFileColors;
    private final String[] _keyArray;
    private final int _keysSz;
    private final String _defFgColor;
    private final String _noticeColor;
    private final boolean _firstWordMatch;
    private final boolean _caseSensitive;

    /**
     * Captures the config in effect now. Nothing in the application changes
     * configuration after startup, so a formatter never has to re-read it.
     *
     * This used to call refreshProps() on every formatted line. The reference
     * compare that was supposed to make that cheap optimised the cheap half -
     * copying the fields - and left a System.getProperty lookup running
     * unconditionally, once per output line, for a value that cannot change.
     */
    public LineFormatter()
    {
        final CtrailProps props = CtrailProps.getInstance();
        _keysToColors = props.getKeysToColors();
        _keysToFileColors = props.getKeysToFileColors();
        _defFgColor = props.getDefaultFgColor() == null ? ConsoleColors.WHITE : props.getDefaultFgColor();
        _noticeColor = props.getNoticeColor() == null ? ConsoleColors.CYAN : props.getNoticeColor();
        _firstWordMatch = props.isMatchFirstWord();
        _caseSensitive = props.isLineSearchCaseSensitiveMatching();

        //
        // getKeys() is a LinkedList, so get(i) in the per-line scan is O(n).
        // copy to an array once so formatting stays linear in key count
        //
        final List<String> keys = props.getKeys();
        _keyArray = keys.toArray(new String[0]);
        _keysSz = _keyArray.length;
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

        //
        // ctrail's own liveness messages are not tailed content: they carry no
        // filename prefix and must not pick up keyword coloring, or a notice
        // mentioning "error" would come out red
        //
        if (line_.isNotice())
        {
            return _noticeColor + line_.getLine() + _reset;
        }

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
