/****************************************************************************
 * FILE: CtrailProps.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static java.text.MessageFormat.format;



import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;



import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.lang3.StringUtils;



import com.kagr.tools.ctrail.ConsoleColors;



import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;





@Slf4j
public class CtrailProps
{

    public static final String CTRAIL_XML = "ctrail.xml";
    public static final String CTRAIL_CFG_KEY = "CTRAIL_CFG";

    @Getter
    @Setter
    private int _maxNbrInputFiles = 100;

    @Getter
    @Setter
    private int _maxProcessingLinesPerThread = 1000;

    @Getter
    @Setter
    private int _maxPendingLines = 100000;

    @Getter
    @Setter
    private int _skipAheadInBytes = 1000;

    @Getter
    @Setter
    private int _noChangeSleepTimeMillis = 100;

    @Getter
    @Setter
    private boolean _lineSearchCaseSensitiveMatching = false;

    @Getter
    @Setter
    private boolean _prependFilenameToLine = true;

    @Getter
    @Setter
    private boolean _blankLineOnFileChange = false;

    @Getter
    @Setter
    private boolean _matchFirstWord = true;

    @Getter
    @Setter
    private boolean _enabledFileFiltering = true;

    @Getter
    @Setter
    private boolean _fileFilterDefaultsToInclude = true;

    @Getter
    @Setter
    private String _defaultFgColor = "white";

    @Getter
    @Setter
    private String _defaultFlColor = "";

    @Getter
    private final Hashtable<String, String> _keysToColors;

    @Getter
    private final Hashtable<String, String> _keysToFileColors;

    @Getter
    private final Hashtable<String, FileSearchFilter> _fileSearchFilters;

    @Getter
    private final LinkedList<String> _keys;





    public static class CtrailPropsHelper
    {
        public static final CtrailProps _instance = new CtrailProps();
    }





    public static CtrailProps getInstance()
    {
        return CtrailPropsHelper._instance;
    }





    public CtrailProps()
    {
        _keysToColors = new Hashtable<>();
        _keysToFileColors = new Hashtable<>();
        _fileSearchFilters = new Hashtable<>();
        _keys = new LinkedList<>();
        final String propsFileName = getConfigFile();
        final Parameters params = new Parameters();
        _logger.debug("config file: {}", propsFileName);
        final FileBasedConfigurationBuilder<XMLConfiguration> builder = new FileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class).configure(params.xml()
                .setThrowExceptionOnMissing(true)
                .setEncoding("UTF-8")
                .setListDelimiterHandler(new DefaultListDelimiterHandler(';'))
                .setValidating(false)

                .setFileName(propsFileName));
        try
        {

            final XMLConfiguration config = builder.getConfiguration();
            setMaxNbrInputFiles(config.getInt("inputFiles.maxInputFileCount", _maxNbrInputFiles));
            setMaxProcessingLinesPerThread(config.getInt("execution.maxProcessingLines", _maxProcessingLinesPerThread));
            setMaxPendingLines(config.getInt("execution.maxPendingLines", _maxPendingLines));
            setSkipAheadInBytes(config.getInt("execution.skipAheadInBytes", _skipAheadInBytes));
            setPrependFilenameToLine(config.getBoolean("execution.prependFilenameToLine", _prependFilenameToLine));
            setNoChangeSleepTimeMillis(config.getInt("execution.noChangeSleepTimeMillis", _noChangeSleepTimeMillis));
            setLineSearchCaseSensitiveMatching(config.getBoolean("execution.useCaseSensitiveSarch", _lineSearchCaseSensitiveMatching));
            setBlankLineOnFileChange(config.getBoolean("coloring.filename.blankLineOnFileChange", _blankLineOnFileChange));
            setDefaultFgColor(getColorCode(config.getString("coloring.linecolors.defaultFgColor", "white")));
            setMatchFirstWord(config.getBoolean("execution.matchFirstWord", _matchFirstWord));
            setEnabledFileFiltering(config.getBoolean("filtering.enabled", _enabledFileFiltering));
            setFileFilterDefaultsToInclude(config.getBoolean("filtering.fileFilterDefaultsToInclude", _fileFilterDefaultsToInclude));

            initColoring(config);
            initFiltering(config);

        }
        catch (final Exception ex_)
        {
            _logger.error(ex_.toString());
        }
    }





    private void initFiltering(final XMLConfiguration config_)
    {
        if (config_.immutableChildConfigurationsAt("filtering").size() <= 0)
        {
            _logger.trace("'file-filtering' not present in config");
            return;
        }
        _logger.trace("'file-filtering' FOUND");

        int filterCfgSz = 0;
        try
        {
            filterCfgSz = extractCount(config_, "filtering.filefilter.filename");
            _logger.trace("total number of line colors found:{}", filterCfgSz);
        }
        catch (final Exception ex_)
        {
            _logger.error(ex_.toString(), ex_);
        }

        FileSearchFilter fst;
        String fname = "";
        int filterTermsSz = 0;
        for (int i = 0; i < filterCfgSz; i++)
        {
            try
            {
                fname = config_.getString("filtering.filefilter(" + i + ").filename");
                if (_fileSearchFilters.contains(fname))
                {
                    _logger.debug("already contains file filter:{}", fname);
                    continue;
                }

                fst = new FileSearchFilter(fname);
                filterTermsSz = extractCount(config_, format("filtering.filefilter({0}).includes.keyword", i));
                final List<String> includes = fst.getIncludeTerms();
                String key;
                String val;
                for (int j = 0; j < filterTermsSz; j++)
                {
                    key = format("filtering.filefilter({0}).includes.keyword({1})", i, j);
                    val = config_.getString(key);
                    if (isDuplicate(val, includes))
                    {
                        continue;
                    }
                    includes.add(val);
                }

                filterTermsSz = extractCount(config_, format("filtering.filefilter({0}).excludes.keyword", i));
                final List<String> excludes = fst.getExcldueTerms();
                for (int j = 0; j < filterTermsSz; j++)
                {
                    key = format("filtering.filefilter({0}).excludes.keyword({1})", i, j);
                    val = config_.getString(key);
                    if (isDuplicate(val, includes) || isDuplicate(val, excludes))
                    {
                        continue;
                    }
                    excludes.add(val);
                }


                _fileSearchFilters.put(fst.getFileName(), fst);
                if (_logger.isDebugEnabled())
                {
                    _logger.debug("loaded file search term:{}", fst.toString());
                }
            }
            catch (final IllegalArgumentException ex_)
            {
                _logger.error("error for key:{}, bad value:{}", fname);
            }
            catch (final NoSuchElementException ex_)
            {
                if (ex_.toString().contains(".keyword"))
                {
                    break;
                }
                _logger.error(ex_.toString());
                break;
            }
        }
    }





    private boolean isDuplicate(final String key_, final List<String> other_)
    {
        final Iterator<String> itr = other_.iterator();
        while (itr.hasNext())
        {
            if (StringUtils.equals(itr.next(), key_))
            {
                if (_logger.isInfoEnabled())
                {
                    _logger.info("duplicate found:{}", key_);
                }
                return true;
            }
        }

        return false;
    }





    private int extractCount(final XMLConfiguration config_, final String key_)
    {
        if (config_.getProperty(key_) != null)
        {
            try
            {
                return ((Collection<?>) config_.getProperty(key_)).size();
            }
            catch (final ClassCastException ex_)
            {
                return 1;
            }
        }
        return 0;

    }





    private void initColoring(final XMLConfiguration config_)
    {
        int lineColorCfgSz = 0;
        try
        {
            lineColorCfgSz = ((Collection<?>) config_.getProperty("coloring.linecolors.colorpair.keyword")).size();
            _logger.trace("total number of line colors found:{}", lineColorCfgSz);
        }
        catch (final Exception ex_)
        {
            _logger.error(ex_.toString(), ex_);
        }

        String key = "";
        String origfgcolor = "";
        String fgcolor = "";
        String origflcolor = "";
        String flcolor = "";
        for (int i = 0; i < lineColorCfgSz; i++)
        {
            try
            {
                key = config_.getString("coloring.linecolors.colorpair(" + i + ").keyword");
                origfgcolor = config_.getString("coloring.linecolors.colorpair(" + i + ").fgcolor");
                origflcolor = config_.getString("coloring.linecolors.colorpair(" + i + ").flcolor", "");

                fgcolor = getColorCode(origfgcolor);
                flcolor = getColorCode(origflcolor);

                if (isLineSearchCaseSensitiveMatching())
                {
                    _keys.add(key);
                    if (!StringUtils.isEmpty(fgcolor))
                    {
                        _keysToColors.put(key, fgcolor);
                        _logger.trace("added FG:{}={}", key, origfgcolor);
                    }
                    if (!StringUtils.isEmpty(flcolor))
                    {
                        _keysToFileColors.put(key, flcolor);
                        _logger.trace("added FL:{}={}", key, origflcolor);
                    }
                }
                else
                {
                    _keys.add(key.toLowerCase());
                    if (!StringUtils.isEmpty(fgcolor))
                    {
                        _keysToColors.put(key.toLowerCase(), fgcolor);
                        _logger.trace("added FG:{}={}", key.toLowerCase(), origfgcolor);
                    }
                    if (!StringUtils.isEmpty(flcolor))
                    {
                        _keysToFileColors.put(key.toLowerCase(), flcolor);
                        _logger.trace("added FL:{}={}", key.toLowerCase(), origflcolor);
                    }
                }
            }
            catch (final IllegalArgumentException ex_)
            {
                _logger.error("error for key:{}, bad value:{}; ignoring...", key, fgcolor);
            }
            catch (final NoSuchElementException ex_)
            {
                if (ex_.toString().contains(".keyword"))
                {
                    break;
                }
                _logger.error(ex_.toString());
                break;
            }
        }
    }





    private String getColorCode(final String color_)
    {
        if (StringUtils.isEmpty(color_))
        {
            return null;
        }

        switch (color_.toUpperCase())
        {
            case "BLACK":
                return ConsoleColors.BLACK;
            case "RED":
                return ConsoleColors.RED;
            case "GREEN":
                return ConsoleColors.GREEN_BOLD;
            case "YELLOW":
                return ConsoleColors.YELLOW;
            case "BLUE":
                return ConsoleColors.BLUE;
            case "PURPLE":
                return ConsoleColors.PURPLE;
            case "CYAN":
                return ConsoleColors.CYAN;
            case "WHITE":
                return ConsoleColors.WHITE;
            case "BLACK_UNDERLINED":
                return ConsoleColors.BLACK_UNDERLINED;
            case "RED_UNDERLINED":
                return ConsoleColors.RED_UNDERLINED;
            case "GREEN_UNDERLINED":
                return ConsoleColors.GREEN_UNDERLINED;
            case "YELLOW_UNDERLINED":
                return ConsoleColors.YELLOW_UNDERLINED;
            case "BLUE_UNDERLINED":
                return ConsoleColors.BLUE_UNDERLINED;
            case "PURPLE_UNDERLINED":
                return ConsoleColors.PURPLE_UNDERLINED;
            case "CYAN_UNDERLINED":
                return ConsoleColors.CYAN_UNDERLINED;
            case "WHITE_UNDERLINED":
                return ConsoleColors.WHITE_UNDERLINED;
            default:
                _logger.warn("color:{} not regognized, returning null");
        }


        return null;
    }





    private static String getConfigFile()
    {
        Path cfgFilePath;
        final String cfgOverride = System.getProperty(CTRAIL_CFG_KEY);
        if (!StringUtils.isEmpty(cfgOverride))
        {
            cfgFilePath = Paths.get(cfgOverride);
            if (Files.exists(cfgFilePath))
            {
                return cfgFilePath.toString();
            }
        }

        cfgFilePath = Paths.get(".", CTRAIL_XML);
        if (Files.exists(cfgFilePath))
        {
            return cfgFilePath.toString();
        }


        cfgFilePath = Paths.get("/etc", CTRAIL_XML);
        if (Files.exists(cfgFilePath))
        {
            return cfgFilePath.toString();
        }

        // KAGR: bug - should read config from jar
        return "";
    }
}
