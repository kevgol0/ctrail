/****************************************************************************
 * FILE: CtrailProps.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static java.text.MessageFormat.format;



import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;



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

	public static final String	CTRAIL_XML		= "ctrail.xml";
	public static final String	CTRAIL_CFG_KEY	= "CTRAIL_CFG";

	//
	// the name reported for piped input, and the <filename> a legacy
	// <filefilter> had to use to target std-in
	//
	public static final String STDIN_FILTER_NAME = "stdin";

	@Getter @Setter private int _maxNbrInputFiles = 100;

	@Getter @Setter private int _maxProcessingLinesPerThread = 1000;

	@Getter @Setter private int _maxPendingLines = 100000;

	@Getter @Setter private int _skipAheadInBytes = 1000;

	@Getter @Setter private int _noChangeSleepTimeMillis = 100;

	@Getter @Setter private boolean _lineSearchCaseSensitiveMatching = false;

	@Getter @Setter private boolean _prependFilenameToLine = true;

	@Getter @Setter private boolean _blankLineOnFileChange = false;

	@Getter @Setter private boolean _matchFirstWord = true;

	@Getter @Setter private boolean _enabledFileFiltering = true;

	//
	// controls the exclude-term half of file filtering; -v on the
	// command line toggles this independently of -f (include side)
	//
	@Getter @Setter private boolean _enabledExcludeFiltering = true;

	@Getter @Setter private boolean _fileFilterDefaultsToInclude = true;

	@Getter @Setter private String _defaultFgColor = "white";

	@Getter @Setter private String _defaultFlColor = "";

	@Getter private String _version;

	@Getter private final Hashtable<String, String> _keysToColors;

	@Getter private final Hashtable<String, String> _keysToFileColors;

	@Getter private final Hashtable<String, Integer> _keysToColorCount;

	@Getter private final Hashtable<String, FileSearchFilter> _fileSearchFilters;

	//
	// <filtering><stdinfilter>; null when the config declares none
	//
	@Getter @Setter private FileSearchFilter _stdinFilter;

	@Getter private final LinkedList<String> _keys;





	private static volatile CtrailProps _instance;

	//
	// the raw -DCTRAIL_CFG value the cached instance was built from. resolving
	// the config file costs up to three Files.exists() calls, and getInstance()
	// is on the per-line hot path, so only re-resolve when the override changes.
	//
	private static String _instanceCfgOverride;

	private String _configFile;

	public static synchronized CtrailProps getInstance()
	{
		final String cfgOverride = System.getProperty(CTRAIL_CFG_KEY);
		if (_instance != null && StringUtils.equals(cfgOverride, _instanceCfgOverride))
		{
			return _instance;
		}

		_instance = new CtrailProps(getConfigFile());
		_instanceCfgOverride = cfgOverride;
		return _instance;
	}

	public CtrailProps()
	{
		this(getConfigFile());
	}

	private CtrailProps(final String propsFileName_)
	{
		_configFile = propsFileName_;
		_keysToColors = new Hashtable<>();
		_keysToFileColors = new Hashtable<>();
		_fileSearchFilters = new Hashtable<>();
		_keysToColorCount = new Hashtable<>();
		_keys = new LinkedList<>();
		final String propsFileName = _configFile;
		_logger.debug("filename:{}", propsFileName);

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
			//
			// an unrecognized color name yields null, which would otherwise be
			// concatenated into every output line as the literal text "null"
			//
			final String defFgColor = getColorCode(config.getString("coloring.linecolors.defaultFgColor", "white"));
			setDefaultFgColor(defFgColor == null ? ConsoleColors.WHITE : defFgColor);
			setMatchFirstWord(config.getBoolean("execution.matchFirstWord", _matchFirstWord));
			setEnabledFileFiltering(config.getBoolean("filtering.enabled", _enabledFileFiltering));
			setEnabledExcludeFiltering(config.getBoolean("filtering.excludesEnabled", _enabledExcludeFiltering));
			setFileFilterDefaultsToInclude(config.getBoolean("filtering.fileFilterDefaultsToInclude", _fileFilterDefaultsToInclude));

			initColoring(config);
			initFiltering(config);

		}
		catch (final Exception ex_)
		{
			_logger.error(ex_.toString());
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
		//
		// commons-config hands back a bare String (not a Collection) when the
		// config holds exactly one colorpair; extractCount handles both shapes,
		// a raw cast used to throw and leave the count at 0 -- no colors at all
		//
		int lineColorCfgSz = 0;
		try
		{
			lineColorCfgSz = extractCount(config_, "coloring.linecolors.colorpair.fgcolor");
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
				origfgcolor = config_.getString("coloring.linecolors.colorpair(" + i + ").fgcolor");
				origflcolor = config_.getString("coloring.linecolors.colorpair(" + i + ").flcolor", "");

				fgcolor = getColorCode(origfgcolor);
				flcolor = getColorCode(origflcolor);


				String[] keys = (String[]) config_.getArray(String.class, "coloring.linecolors.colorpair(" + i + ").keyword");
				_logger.debug("found:{}", Arrays.toString(keys));
				for (int j = 0; j < keys.length; j++)
				{

					key = keys[j];
					StringBuilder dbg = new StringBuilder(key);
					dbg.append("=[");
					if (isLineSearchCaseSensitiveMatching())
					{
						_keys.add(key);
						if (!StringUtils.isEmpty(fgcolor))
						{
							_keysToColors.put(key, fgcolor);
							_keysToColorCount.put(origfgcolor, _keysToColorCount.getOrDefault(origfgcolor, 0) + 1);
							dbg.append(origfgcolor);
						}
						if (!StringUtils.isEmpty(flcolor))
						{
							_keysToFileColors.put(key, flcolor);
							dbg.append(";").append(origflcolor);
						}
					}
					else
					{
						_keys.add(key.toLowerCase(Locale.ROOT));
						if (!StringUtils.isEmpty(fgcolor))
						{
							_keysToColors.put(key.toLowerCase(Locale.ROOT), fgcolor);
							_keysToColorCount.put(origfgcolor, _keysToColorCount.getOrDefault(origfgcolor, 0) + 1);
							dbg.append(origfgcolor);
						}
						if (!StringUtils.isEmpty(flcolor))
						{
							_keysToFileColors.put(key.toLowerCase(Locale.ROOT), flcolor);
							dbg.append(";").append(origflcolor);
						}
					}
					dbg.append("]");
					_logger.trace(dbg.toString());
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

		initStdinFilter(config_);

		FileSearchFilter fst;
		String fname = "";
		for (int i = 0; i < filterCfgSz; i++)
		{
			try
			{
				fname = config_.getString("filtering.filefilter(" + i + ").filename");
				fst = new FileSearchFilter(fname, _fileFilterDefaultsToInclude);

				//
				// Hashtable.contains() tests VALUES; the map is keyed by the
				// regex form of the name, so build the filter first and then
				// check the key we would actually store under
				//
				if (_fileSearchFilters.containsKey(fst.getFileName()))
				{
					_logger.debug("already contains file filter:{}", fname);
					continue;
				}

				loadFilterTerms(config_, format("filtering.filefilter({0})", i), fst);

				_fileSearchFilters.put(fst.getFileName(), fst);
				if (_logger.isDebugEnabled())
				{
					_logger.debug("loaded file search term:{}", fst.toString());
				}
			}
			catch (final IllegalArgumentException ex_)
			{
				_logger.error("error loading file filter:{}, cause:{}", fname, ex_.toString());
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





	/**
	 * Loads the &lt;includes&gt;/&lt;excludes&gt; keyword lists hanging off
	 * basePath_ into filter_. Shared by &lt;filefilter&gt; and
	 * &lt;stdinfilter&gt; so the two stay in step.
	 */
	private void loadFilterTerms(final XMLConfiguration config_, final String basePath_, final FileSearchFilter filter_)
	{
		final List<String> includes = filter_.getIncludeTerms();
		int termCount = extractCount(config_, basePath_ + ".includes.keyword");
		String val;
		for (int j = 0; j < termCount; j++)
		{
			val = config_.getString(format("{0}.includes.keyword({1})", basePath_, j));
			if (isDuplicate(val, includes))
			{
				continue;
			}
			includes.add(val);
		}

		final List<String> excludes = filter_.getExcldueTerms();
		termCount = extractCount(config_, basePath_ + ".excludes.keyword");
		for (int j = 0; j < termCount; j++)
		{
			val = config_.getString(format("{0}.excludes.keyword({1})", basePath_, j));

			//
			// includes trump excludes, so a term listed in both is dropped
			// from the exclude side rather than silently winning
			//
			if (isDuplicate(val, includes) || isDuplicate(val, excludes))
			{
				continue;
			}
			excludes.add(val);
		}
	}





	/**
	 * &lt;filtering&gt;&lt;stdinfilter&gt; applies when ctrail is reading from a
	 * pipe rather than from named files. It needs no &lt;filename&gt; -- there is
	 * only ever one standard-in.
	 */
	private void initStdinFilter(final XMLConfiguration config_)
	{
		if (config_.immutableChildConfigurationsAt("filtering.stdinfilter").size() <= 0)
		{
			_logger.trace("'stdinfilter' not present in config");
			return;
		}

		try
		{
			final FileSearchFilter filter = new FileSearchFilter(STDIN_FILTER_NAME, _fileFilterDefaultsToInclude);
			loadFilterTerms(config_, "filtering.stdinfilter", filter);
			_stdinFilter = filter;
			if (_logger.isDebugEnabled())
			{
				_logger.debug("loaded stdin filter:{}", filter.toString());
			}
		}
		catch (final Exception ex_)
		{
			_logger.error("error loading stdin filter, cause:{}", ex_.toString());
		}
	}





	/**
	 * The filter to apply to piped input, or null for none.
	 *
	 * &lt;stdinfilter&gt; wins. Failing that, a &lt;filefilter&gt; whose
	 * &lt;filename&gt; is literally "stdin" is honored: that was the only way to
	 * filter std-in before &lt;stdinfilter&gt; existed, so configs in the wild
	 * may rely on it.
	 *
	 * Returns null when filtering is switched off, so -f behaves the same for
	 * piped input as it does for files.
	 */
	public FileSearchFilter resolveStdinFilter()
	{
		if (!isEnabledFileFiltering())
		{
			_logger.debug("filtering disabled, no stdin filter will be applied");
			return null;
		}

		if (_stdinFilter != null)
		{
			return _stdinFilter;
		}

		final FileSearchFilter legacy = _fileSearchFilters.get(new FileSearchFilter(STDIN_FILTER_NAME, _fileFilterDefaultsToInclude).getFileName());
		if (legacy != null)
		{
			_logger.debug("using legacy <filefilter><filename>stdin</filename>; prefer <stdinfilter>");
		}
		return legacy;
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

		// Bold
		case "BLACK_BOLD":
			return ConsoleColors.BLACK_BOLD;
		case "RED_BOLD":
			return ConsoleColors.RED_BOLD;
		case "GREEN_BOLD":
			return ConsoleColors.GREEN_BOLD;
		case "ORANGE":
			return ConsoleColors.ORANGE;
		case "YELLOW_BOLD":
			return ConsoleColors.ORANGE;
		case "BLUE_BOLD":
			return ConsoleColors.BLUE_BOLD;
		case "PURPLE_BOLD":
			return ConsoleColors.PURPLE_BOLD;
		case "CYAN_BOLD":
			return ConsoleColors.CYAN_BOLD;
		case "WHITE_BOLD":
			return ConsoleColors.WHITE_BOLD;

		// Bright (high intensity)
		case "BLACK_BRIGHT":
			return ConsoleColors.BLACK_BRIGHT;
		case "RED_BRIGHT":
			return ConsoleColors.RED_BRIGHT;
		case "GREEN_BRIGHT":
			return ConsoleColors.GREEN_BRIGHT;
		case "YELLOW_BRIGHT":
			return ConsoleColors.YELLOW_BRIGHT;
		case "BLUE_BRIGHT":
			return ConsoleColors.BLUE_BRIGHT;
		case "PURPLE_BRIGHT":
			return ConsoleColors.PURPLE_BRIGHT;
		case "CYAN_BRIGHT":
			return ConsoleColors.CYAN_BRIGHT;
		case "WHITE_BRIGHT":
			return ConsoleColors.WHITE_BRIGHT;

		// Background
		case "BLACK_BACKGROUND":
			return ConsoleColors.BLACK_BACKGROUND;
		case "RED_BACKGROUND":
			return ConsoleColors.RED_BACKGROUND;
		case "GREEN_BACKGROUND":
			return ConsoleColors.GREEN_BACKGROUND;
		case "YELLOW_BACKGROUND":
			return ConsoleColors.YELLOW_BACKGROUND;
		case "BLUE_BACKGROUND":
			return ConsoleColors.BLUE_BACKGROUND;
		case "PURPLE_BACKGROUND":
			return ConsoleColors.PURPLE_BACKGROUND;
		case "CYAN_BACKGROUND":
			return ConsoleColors.CYAN_BACKGROUND;
		case "WHITE_BACKGROUND":
			return ConsoleColors.WHITE_BACKGROUND;

		// Bold Bright (bold + high intensity)
		case "BLACK_BOLD_BRIGHT":
			return ConsoleColors.BLACK_BOLD_BRIGHT;
		case "RED_BOLD_BRIGHT":
			return ConsoleColors.RED_BOLD_BRIGHT;
		case "GREEN_BOLD_BRIGHT":
			return ConsoleColors.GREEN_BOLD_BRIGHT;
		case "YELLOW_BOLD_BRIGHT":
			return ConsoleColors.YELLOW_BOLD_BRIGHT;
		case "BLUE_BOLD_BRIGHT":
			return ConsoleColors.BLUE_BOLD_BRIGHT;
		case "PURPLE_BOLD_BRIGHT":
			return ConsoleColors.PURPLE_BOLD_BRIGHT;
		case "CYAN_BOLD_BRIGHT":
			return ConsoleColors.CYAN_BOLD_BRIGHT;
		case "WHITE_BOLD_BRIGHT":
			return ConsoleColors.WHITE_BOLD_BRIGHT;

		// Background Bright (high intensity background)
		case "BLACK_BACKGROUND_BRIGHT":
			return ConsoleColors.BLACK_BACKGROUND_BRIGHT;
		case "RED_BACKGROUND_BRIGHT":
			return ConsoleColors.RED_BACKGROUND_BRIGHT;
		case "GREEN_BACKGROUND_BRIGHT":
			return ConsoleColors.GREEN_BACKGROUND_BRIGHT;
		case "YELLOW_BACKGROUND_BRIGHT":
			return ConsoleColors.YELLOW_BACKGROUND_BRIGHT;
		case "BLUE_BACKGROUND_BRIGHT":
			return ConsoleColors.BLUE_BACKGROUND_BRIGHT;
		case "PURPLE_BACKGROUND_BRIGHT":
			return ConsoleColors.PURPLE_BACKGROUND_BRIGHT;
		case "CYAN_BACKGROUND_BRIGHT":
			return ConsoleColors.CYAN_BACKGROUND_BRIGHT;
		case "WHITE_BACKGROUND_BRIGHT":
			return ConsoleColors.WHITE_BACKGROUND_BRIGHT;

		default:
			_logger.warn("color:{} not recognized, returning null", color_);
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
			else
			{
				_logger.warn("specified file does not exist:{}", cfgOverride);
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





	public String getVersion()
	{
		if (!StringUtils.isEmpty(_version))
		{
			return _version;
		}

		_version = calcVersion();
		return _version;
	}





	protected String calcVersion()
	{
		if (_version != null)
		{
			return _version;
		}

		try
		{
			Class clazz = CtrailProps.class;
			String className = clazz.getSimpleName() + ".class";
			String classPath = clazz.getResource(className).toString();
			if (!classPath.startsWith("jar"))
			{
				// Class not from JAR
				return "UNK";
			}
			String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1)
					+ "/META-INF/MANIFEST.MF";
			Manifest manifest = new Manifest(new URL(manifestPath).openStream());
			Attributes attr = manifest.getMainAttributes();
			final String implVersion = attr.getValue("Implementation-Version");
			if (StringUtils.isEmpty(implVersion))
			{
				return "UNK";
			}

			_version = implVersion;
			return _version;
		}
		catch (Exception ex_)
		{
			_logger.error(ex_.toString(), ex_);
		}

		return getClass().getPackage().getImplementationVersion();
	}
}
