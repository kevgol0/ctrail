/****************************************************************************
 * FILE: CtrailProps.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.NoSuchElementException;



import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.lang3.StringUtils;



import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;





@Slf4j
public class CtrailProps
{

	private static final String CTRAIL_XML = "ctrail.xml";
	@Getter @Setter private int _maxNbrInputFiles = 100;
	@Getter @Setter private int _maxProcessingLinesPerThread = 1000;
	@Getter @Setter private int _maxPendingLines = 100000;
	@Getter @Setter private int _skipAheadInBytes = 1000;
	@Getter @Setter private int _noChangeSleepTimeMillis = 100;

	@Getter @Setter private boolean _lineSearchCaseSensitiveMatching = false;
	@Getter @Setter private boolean _prependFilenameToLine = true;
	@Getter @Setter private boolean _blankLineOnFileChange = false;
	@Getter @Setter private boolean _matchFirstWord = true;


	@Getter @Setter private String _defaultFgColor = "white";
	@Getter @Setter private String _defaultFlColor = "";
	@Getter private Hashtable<String, String> _keysToColors;
	@Getter private Hashtable<String, String> _keysToFileColors;
	@Getter private LinkedList<String> _keys;




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
		_keys = new LinkedList<>();
		String propsFileName = getConfigFile();
		Parameters params = new Parameters();
		FileBasedConfigurationBuilder<XMLConfiguration> builder = new FileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class).configure(params.xml()
				.setThrowExceptionOnMissing(true)
				.setEncoding("UTF-8")
				.setListDelimiterHandler(new DefaultListDelimiterHandler(';'))
				.setValidating(false)
				.setFileName(propsFileName));
		try
		{

			XMLConfiguration config = builder.getConfiguration();
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


			int lineColorCfgSz = 0;
			try
			{
				lineColorCfgSz = ((Collection<?>) config.getProperty("coloring.linecolors.colorpair.keyword")).size();
				_logger.trace("total number of line colors found:{}", lineColorCfgSz);
			}
			catch (Exception ex_1)
			{
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
					key = config.getString("coloring.linecolors.colorpair(" + i + ").keyword");
					origfgcolor = config.getString("coloring.linecolors.colorpair(" + i + ").fgcolor");
					origflcolor = config.getString("coloring.linecolors.colorpair(" + i + ").flcolor", "");

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
				catch (IllegalArgumentException ex_)
				{
					_logger.error("error for key:%s, bad value:%s; ignoring...", key, fgcolor);
				}
				catch (NoSuchElementException ex_)
				{
					if (ex_.toString().contains(".keyword"))
						break;
					else
						_logger.error(ex_.toString());
					break;
				}
			}
		}
		catch (Exception ex_)
		{
			_logger.error(ex_.toString());
		}
	}





	private String getColorCode(String color_)
	{
		if (StringUtils.isEmpty(color_))
			return null;

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
		}

		_logger.warn("color:{} not regognized, returning null");
		return null;
	}





	private static String getConfigFile()
	{
		Path cfgFilePath;
		String cfgOverride = System.getProperty("CTRAIL_CFG");
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
