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





public class CtrailProps
{

	@Getter @Setter private int _maxNbrInputFiles;
	@Getter @Setter private int _maxProcessingLinesPerThread;
	@Getter @Setter private int _maxPendingLines;
	@Getter @Setter private int _skipAheadInBytes;
	@Getter @Setter private int _noChangeSleepTimeMillis;

	@Getter @Setter private String _defaultColor;

	@Getter @Setter private boolean _lineSearchCaseSensitiveMatching;
	@Getter @Setter private boolean _prependFilenameToLine;
	@Getter @Setter private boolean _blankLineOnFileChange;
	@Getter private Hashtable<String, String> _keysToColors;




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
			setDefaultColor(config.getString("coloring.linecolors.defaultColor"));
			setBlankLineOnFileChange(config.getBoolean("coloring.filename.blankLineOnFileChange", false));

			String key = "";
			String color = "";
			for (int i = 0; i < Integer.MAX_VALUE; i++)
			{
				try
				{
					key = config.getString("coloring.linecolors.colorpair(" + i + ").keyword");
					color = config.getString("coloring.linecolors.colorpair(" + i + ").fgcolor");


					color = getColorCode(color);

					if (isLineSearchCaseSensitiveMatching())
						_keysToColors.put(key, color);
					else
						_keysToColors.put(key.toLowerCase(), color);
				}
				catch (IllegalArgumentException ex_)
				{
					System.err.printf("error for key:%s, bad value:%s; ignoring...", key, color);
				}
				catch (NoSuchElementException ex_)
				{
					break;
				}
			}


			_keysToColors.put("DEFAULT", getColorCode(config.getString("coloring.linecolors.defaultColor")));

		}
		catch (Exception ex_)
		{
			ex_.printStackTrace(System.err);
		}
	}





	private String getColorCode(String color)
	{
		switch (color.toUpperCase())
		{
		case "BLACK":
			color = ConsoleColors.BLACK;
			break;
		case "RED":
			color = ConsoleColors.RED;
			break;
		case "GREEN":
			color = ConsoleColors.GREEN_BOLD;
			break;
		case "YELLOW":
			color = ConsoleColors.YELLOW;
			break;
		case "BLUE":
			color = ConsoleColors.BLUE;
			break;
		case "PURPLE":
			color = ConsoleColors.PURPLE;
			break;
		case "CYAN":
			color = ConsoleColors.CYAN;
			break;
		case "WHITE":
			color = ConsoleColors.WHITE;
			break;
		}
		return color;
	}





	private static Path getConfigFile()
	{

		Path cfgFilePath = Paths.get(".", "ctrail.xml");
		if (Files.exists(cfgFilePath))
		{
			return cfgFilePath;
		}



		String cfgOverride = System.getenv("CTRAIL_CFG");
		if (!StringUtils.isEmpty(cfgOverride))
		{
			cfgFilePath = Paths.get(cfgOverride);
			if (Files.exists(cfgFilePath))
			{
				return cfgFilePath;
			}
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
