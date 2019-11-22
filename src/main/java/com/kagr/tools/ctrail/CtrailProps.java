/****************************************************************************
 * FILE: CtrailProps.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;
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

	@Getter @Setter private int _maxNbrInputFiles;
	@Getter @Setter private int _maxProcessingLinesPerThread;
	@Getter @Setter private int _maxPendingLines;
	@Getter @Setter private int _skipAheadInBytes;
	@Getter @Setter private int _noChangeSleepTimeMillis;

	@Getter @Setter private boolean _lineSearchCaseSensitiveMatching;
	@Getter @Setter private boolean _prependFilenameToLine;
	@Getter @Setter private boolean _blankLineOnFileChange;


	@Getter @Setter private String _defaultFgColor;
	@Getter @Setter private String _defaultFlColor;
	@Getter private Hashtable<String, String> _keysToColors;
	@Getter private Hashtable<String, String> _keysToFileColors;




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
		String propsFileName = getConfigFile().toString();
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
			setMaxNbrInputFiles(config.getInt("inputFiles.maxInputFileCount"));
			setMaxProcessingLinesPerThread(config.getInt("execution.maxProcessingLines"));
			setMaxPendingLines(config.getInt("execution.maxPendingLines"));
			setSkipAheadInBytes(config.getInt("execution.skipAheadInBytes", 1000));
			setPrependFilenameToLine(config.getBoolean("execution.prependFilenameToLine"));
			setNoChangeSleepTimeMillis(config.getInt("execution.noChangeSleepTimeMillis", 100));
			setLineSearchCaseSensitiveMatching(config.getBoolean("coloring.useCaseSensitiveSarch"));
			setBlankLineOnFileChange(config.getBoolean("coloring.filename.blankLineOnFileChange", false));
			setDefaultFgColor(getColorCode(config.getString("coloring.linecolors.defaultFgColor", "white")));

			String key = "";
			String fgcolor = "";
			String flcolor = "";
			for (int i = 0; i < Integer.MAX_VALUE; i++)
			{
				try
				{
					key = config.getString("coloring.linecolors.colorpair(" + i + ").keyword");
					fgcolor = config.getString("coloring.linecolors.colorpair(" + i + ").fgcolor");
					flcolor = config.getString("coloring.linecolors.colorpair(" + i + ").flcolor", "");

					fgcolor = getColorCode(fgcolor);
					flcolor = getColorCode(flcolor);

					if (isLineSearchCaseSensitiveMatching())
					{
						if (!StringUtils.isEmpty(fgcolor))
						{
							_keysToColors.put(key, fgcolor);
							_logger.trace("added FG:{}={}", key, fgcolor);
						}
						if (!StringUtils.isEmpty(flcolor))
						{
							_keysToFileColors.put(key, flcolor);
							_logger.trace("added FL:{}={}", key, flcolor);
						}
					}
					else
					{
						if (!StringUtils.isEmpty(fgcolor))
						{
							_keysToColors.put(key.toLowerCase(), fgcolor);
							_logger.trace("added FG:{}={}", key.toLowerCase(), fgcolor);
						}
						if (!StringUtils.isEmpty(flcolor))
						{
							_keysToFileColors.put(key.toLowerCase(), flcolor);
							_logger.trace("added FL:{}={}", key.toLowerCase(), flcolor);
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





	private static Path getConfigFile()
	{
		Path cfgFilePath;
		String cfgOverride = System.getProperty("CTRAIL_CFG");
		if (!StringUtils.isEmpty(cfgOverride))
		{
			cfgFilePath = Paths.get(cfgOverride);
			if (Files.exists(cfgFilePath))
			{
				return cfgFilePath;
			}
		}

		cfgFilePath = Paths.get(".", "ctrail.xml");
		if (Files.exists(cfgFilePath))
		{
			return cfgFilePath;
		}


		cfgFilePath = Paths.get("/etc", "ctrail.xml");
		if (Files.exists(cfgFilePath))
		{
			return cfgFilePath;
		}


		try
		{
			URL res = CtrailProps.class.getResource("/ctrail.xml");
			if (res != null)
				return Paths.get(res.toURI());
		}
		catch (URISyntaxException ex_)
		{
			ex_.printStackTrace();
		}


		throw new RuntimeException("config file not found");
	}
}
