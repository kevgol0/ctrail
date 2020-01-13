/****************************************************************************
 * FILE: CtrailEntryPoint.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.io.File;
import java.io.RandomAccessFile;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;



import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;



import com.kagr.tools.ctrail.files.FileReaderThread;
import com.kagr.tools.ctrail.files.FileTailTracker;
import com.kagr.tools.ctrail.files.StdinReaderThread;
import com.kagr.tools.ctrail.files.StdoutWriterThread;
import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.FileSearchFilter;
import com.kagr.tools.ctrail.unit.LogLine;



import lombok.extern.slf4j.Slf4j;





@Slf4j
public class CtrailEntryPoint
{
	private Thread _reader;
	private Thread _writer;
	private BlockingDeque<LogLine> _output;
	private BlockingDeque<FileTailTracker> _fileTrackers;
	private String _matchpattern;





	public CtrailEntryPoint(String[] args_)
	{
		//
		// props
		// 
		loadProps();
		String[] remainingArgs = loadArgsAndOverrides(args_);




		//
		// start them up
		//
		_output = new LinkedBlockingDeque<LogLine>(CtrailProps.getInstance().getMaxPendingLines());
		_fileTrackers = new LinkedBlockingDeque<FileTailTracker>(CtrailProps.getInstance().getMaxNbrInputFiles());
		initWriterThreads();
		initReaderThreads(remainingArgs);

	}





	private void initReaderThreads(String[] args_)
	{
		_fileTrackers = getFilesFromArgs(args_);
		if (_fileTrackers.size() <= 0)
		{
			_reader = new Thread(new StdinReaderThread(System.in, _output, _matchpattern));
		}
		else
		{
			_reader = new Thread(new FileReaderThread(_fileTrackers, _output, _matchpattern));
		}

		_reader.start();
	}





	private void initWriterThreads()
	{
		_writer = new Thread(new StdoutWriterThread(_output));
		_writer.start();
	}





	private BlockingDeque<FileTailTracker> getFilesFromArgs(String[] args_)
	{
		int maxFileCnt = CtrailProps.getInstance().getMaxNbrInputFiles();
		Hashtable<String, FileSearchFilter> fstMap = CtrailProps.getInstance().getFileSearchFilters();
		LinkedBlockingDeque<FileTailTracker> deq = new LinkedBlockingDeque<>(maxFileCnt);
		int cntr = 0;
		File file;
		for (String s : args_)
		{
			try
			{
				file = new File(s);
				if (!file.isFile() || !file.canRead())
				{
					if (_logger.isInfoEnabled())
						_logger.info("{} is either not a file or not readable", s);
					continue;
				}

				if (cntr++ >= maxFileCnt)
				{
					_logger.warn("max number of files exceeded:{}, ignoring remaining files", cntr);
					break;
				}

				FileTailTracker ftracker = new FileTailTracker(s, new RandomAccessFile(file, "r"));
				findAndSetFileTracker(fstMap, s, ftracker);
				deq.add(ftracker);
			}
			catch (Exception ex_)
			{
				_logger.error(ex_.toString());
			}
		}
		return deq;
	}





	private void findAndSetFileTracker(Hashtable<String, FileSearchFilter> fstMap_, String fileName_, FileTailTracker ftracker_)
	{
		FileSearchFilter fst;
		Iterator<String> itr = fstMap_.keySet().iterator();
		while (itr.hasNext())
		{
			fst = fstMap_.get(itr.next());
			if (fst.doesMatchFilename(fileName_))
			{
				ftracker_.setFileSearchTerms(fst);
				_logger.debug("file:{} matches file-name in tracker:{}, setting a filter on this tracker",
						fst.getFileName(), ftracker_);
			}
		}
	}





	private CtrailProps loadProps()
	{
		return CtrailProps.getInstance();
	}





	private String[] loadArgsAndOverrides(String[] args_)
	{
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();

		options.addOption(Option.builder("e").longOpt("entirefile").desc("runs through the entire file").build());
		options.addOption(Option.builder("m").longOpt("match").hasArg().argName("STR")
				.desc("only show lines that match STR")
				.build());
		options.addOption(Option.builder("f").longOpt("filters").hasArg().desc("overrides config for file-filtering; <arg=true|false>").build());
		options.addOption(Option.builder("h").longOpt("help").desc("print command line directives").build());


		try
		{
			CommandLine line = parser.parse(options, args_);

			if (line.hasOption("e"))
				CtrailProps.getInstance().setSkipAheadInBytes(0);
			if (line.hasOption("m"))
				_matchpattern = line.getOptionValue("m");
			if (line.hasOption("f"))
				CtrailProps.getInstance().setEnableFileSearchTerms(Boolean.parseBoolean(line.getOptionValue("f")));

			if (line.hasOption("h"))
			{
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("ctr", options);
				System.exit(1);
			}


			return line.getArgs();
		}
		catch (ParseException ex_)
		{
			_logger.error(ex_.toString());
		}


		return args_;
	}





	protected void start(int millis_)
	{

		try
		{
			synchronized (this)
			{
				if (millis_ > 0)
					wait(millis_);
				else
					wait();

			}
		}
		catch (InterruptedException ex_)
		{
			_logger.error(ex_.toString());
		}
	}





	public static void main(String[] args_)
	{
		CtrailEntryPoint trailer = new CtrailEntryPoint(args_);
		trailer.start(0);
	}

}
