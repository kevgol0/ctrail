/****************************************************************************
 * FILE: CtrailEntryPoint.java
 * DSCRPT: 
 * 
 * this only has 2 threads:
 * 1) output to terminal
 * 2) reading from files (or stdin)
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;



import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;



import com.kagr.tools.ctrail.files.ActivityState;
import com.kagr.tools.ctrail.files.FileReaderThread;
import com.kagr.tools.ctrail.files.FileTailTracker;
import com.kagr.tools.ctrail.files.IdleMonitorThread;
import com.kagr.tools.ctrail.files.OutputWriterThread;
import com.kagr.tools.ctrail.files.StdinReaderThread;
import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.FileSearchFilter;
import com.kagr.tools.ctrail.unit.DurationFormatter;
import com.kagr.tools.ctrail.unit.LogLine;



import lombok.Getter;
import lombok.extern.slf4j.Slf4j;





@Slf4j
public class CtrailEntryPoint implements IShutdownManager
{
	private Thread					_reader;
	private Thread					_writer;
	private IdleMonitorThread		_idleMonitor;
	private BlockingDeque<LogLine>	_output;
	private String					_matchpattern;
	private final Object			_runtimeHolder;

	/**
	 * set under _runtimeHolder before notifyAll(); a notify with no waiter is
	 * otherwise lost, and a spurious wakeup is otherwise taken as an instruction
	 */
	@Getter private volatile boolean _shutdownRequested;

	/** snapshot handed to the idle monitor; never the live tracker deque */
	private final List<ActivityState> _activitySources = new ArrayList<>();

	@Getter private BlockingDeque<FileTailTracker> _fileTrackers;

	/** exit status when file arguments were given and none could be read */
	static final int EXIT_NO_READABLE_INPUT = 2;





	public CtrailEntryPoint(final String[] args_)
	{
		//
		// props
		// 
		loadProps();
		final String[] remainingArgs = loadArgsAndOverrides(args_);
		_runtimeHolder = new Object();


		//
		// terminal writer
		//
		initConsoleWriterThread();


		//
		// readers
		//
		initInputReaderThread(remainingArgs);
	}





	private void initInputReaderThread(final String[] args_)
	{
		if (_output == null)
		{
			throw new RuntimeException("System not ready, initReaderThread called with no output mechanism");
		}

		//
		// stdin only when no file was named. Keying this on "no trackers" also
		// caught "every named file was unreadable", which silently tailed stdin
		// and announced it - a typo'd filename hung on the terminal
		//
		_fileTrackers = getFilesFromArgs(args_);
		if (args_.length > 0 && _fileTrackers.isEmpty())
		{
			throw new NoReadableInputException(StringUtils.join(
					"none of the files given could be read: ", StringUtils.join(args_, ", ")));
		}

		if (args_.length == 0)
		{
			//
			// resolveStdinFilter() picks <stdinfilter>, falls back to the
			// legacy <filefilter><filename>stdin</filename>, and honors the
			// filtering master switch the same way the file path does
			//
			final FileSearchFilter filter = CtrailProps.getInstance().resolveStdinFilter();
			if (filter != null)
			{
				_logger.trace("filter for stdin found:{}", filter.toString());
			}
			final StdinReaderThread stdinReader = new StdinReaderThread(System.in, _output, _matchpattern, this, filter);
			_activitySources.add(stdinReader.getActivityState());
			emitBanner("watching " + CtrailProps.STDIN_FILTER_NAME);
			_reader = new Thread(stdinReader);
			_reader.setName("istream-reader");
		}
		else
		{
			_reader = new Thread(new FileReaderThread(_fileTrackers, _output, _matchpattern, this));
		}


		//
		// the watchdog that reports silence; only worth a thread when notices
		// are actually switched on
		//
		if (CtrailProps.getInstance().getIdleNoticeSeconds() > 0 && !_activitySources.isEmpty())
		{
			_idleMonitor = new IdleMonitorThread(_activitySources, _output);
		}
	}





	private void initConsoleWriterThread()
	{
		_output = new LinkedBlockingDeque<LogLine>(CtrailProps.getInstance().getMaxPendingLines());
		_writer = new OutputWriterThread(_output, System.out);
	}





	private BlockingDeque<FileTailTracker> getFilesFromArgs(final String[] args_)
	{
		final int maxFileCnt = CtrailProps.getInstance().getMaxNbrInputFiles();
		final Hashtable<String, FileSearchFilter> fstMap = CtrailProps.getInstance().getFileSearchFilters();
		final LinkedBlockingDeque<FileTailTracker> deq = new LinkedBlockingDeque<>(maxFileCnt);
		int cntr = 0;
		File file;
		for (final String s : args_)
		{
			try
			{
				file = new File(s);
				final Path p = Paths.get(s);
				final String filename = p.getName(p.getNameCount() - 1).toString();
				if (!file.isFile() || !file.canRead())
				{
					//
					// WARN, not INFO: logback's root is warn, so at INFO a typo'd
					// filename was skipped with no visible trace
					//
					_logger.warn("{} is either not a file or not readable, skipping", s);
					continue;
				}

				if (cntr >= maxFileCnt)
				{
					_logger.warn("max number of files exceeded:{}, ignoring remaining files", cntr);
					break;
				}

				//
				// the tracker owns the handle once constructed; until it is safely
				// on the deque a throw would orphan it with no reference to close
				//
				final RandomAccessFile raf = new RandomAccessFile(file, "r");
				FileTailTracker ftracker = null;
				try
				{
					ftracker = new FileTailTracker(filename, raf);
					findAndSetFileTracker(fstMap, filename, ftracker);
					deq.add(ftracker);
				}
				catch (final Exception ex_)
				{
					if (ftracker != null)
					{
						ftracker.close();
					}
					else
					{
						raf.close();
					}
					throw ex_;
				}


				//
				// announce the file and register it with the idle monitor before
				// any tailing starts, so the banner is the first thing on screen
				//
				emitBanner(describeFile(file, filename));
				_activitySources.add(ftracker.getActivityState());
				cntr += 1;
			}
			catch (final Exception ex_)
			{
				_logger.error(ex_.toString());
			}
		}
		return deq;
	}





	/**
	 * Builds the startup banner text for a file: how big it is and how long ago it last
	 * changed. A file that has not moved in days is then obvious the instant ctrail starts.
	 *
	 * @param file_        the file being tailed
	 * @param displayName_ the short name shown to the user
	 * @return the banner text, without the ctrail prefix
	 */
	private String describeFile(final File file_, final String displayName_)
	{
		final long ageMillis = System.currentTimeMillis() - file_.lastModified();
		return StringUtils.join("watching ", displayName_, " - ",
				FileUtils.byteCountToDisplaySize(file_.length()),
				", modified ", DurationFormatter.format(ageMillis), " ago");
	}





	/**
	 * Queues one of ctrail's own startup messages, when banners are enabled.
	 *
	 * @param message_ the banner text, without the ctrail prefix
	 */
	private void emitBanner(final String message_)
	{
		if (!CtrailProps.getInstance().isShowStartupBanner())
		{
			return;
		}

		_output.offer(new LogLine(null, "ctrail: " + message_, null, true));
	}





	private void findAndSetFileTracker(final Hashtable<String, FileSearchFilter> fstMap_, final String fileName_, final FileTailTracker ftracker_)
	{
		if (!CtrailProps.getInstance().isEnabledFileFiltering())
		{
			return;
		}

		FileSearchFilter fst;
		final Iterator<String> itr = fstMap_.keySet().iterator();
		while (itr.hasNext())
		{
			fst = fstMap_.get(itr.next());
			if (fst.doesMatchFilename(fileName_) && ftracker_.getFileSearchFilter() == null)
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





	/**
	 * Applies the -n/--lines override. A value that is not a number leaves the configured
	 * setting alone rather than failing the run - the tool still has a sane default.
	 *
	 * @param value_ the raw command line value
	 */
	protected void setTailLastLinesFromArg(final String value_)
	{
		//
		// parse is the guard. StringUtils.isNumeric only tests digit-ness, so an
		// all-digit value above Integer.MAX_VALUE passed it and then threw out of
		// the constructor, killing the run this method promises not to fail
		//
		final int lines;
		try
		{
			lines = Integer.parseInt(StringUtils.trimToEmpty(value_));
		}
		catch (final NumberFormatException ex_)
		{
			_logger.warn("ignoring unusable value for -n/--lines:{} ({})", value_, ex_.getMessage());
			return;
		}


		//
		// a negative count is not meaningful and would be read as "disabled" by
		// the tail-N branch, which is not what the user asked for
		//
		if (lines < 0)
		{
			_logger.warn("ignoring negative value for -n/--lines:{}", value_);
			return;
		}

		_logger.debug("tail-last-lines overridden from command line:{}", lines);
		CtrailProps.getInstance().setTailLastLines(lines);
	}





	private String[] loadArgsAndOverrides(final String[] args_)
	{
		final CommandLineParser parser = new DefaultParser();
		final Options options = new Options();

		//
		// the options
		//
		options.addOption(Option.builder("e")
				.longOpt("entirefile").desc("runs through the entire file")
				.build());

		options.addOption(Option.builder("m")
				.longOpt("match").hasArg()
				.argName("STR")
				.desc("only show lines that match STR")
				.build());

		options.addOption(Option.builder("n")
				.longOpt("lines").hasArg()
				.argName("N")
				.desc("show the last N lines of each file on open (default 10; 0 disables)")
				.build());

		options.addOption(Option.builder("f")
				.longOpt("filters").hasArg()
				.argName("true|false")
				.desc("enable/disable per-file filtering entirely; overrides <filtering><enabled> in the config")
				.build());

		options.addOption(Option.builder("v")
				.longOpt("exclude-filters").hasArg()
				.argName("true|false")
				.desc("enable/disable only the <excludes> terms; overrides <filtering><excludesEnabled> in the config")
				.build());

		options.addOption(Option.builder("h")
				.longOpt("help")
				.desc("print command line directives")
				.build());
		
		options.addOption(Option.builder()
				.longOpt("version")
				.desc("show version")
				.build());


		try
		{
			final CommandLine line = parser.parse(options, args_);

			if (line.hasOption("m"))
			{
				_matchpattern = line.getOptionValue("m");
			}
			if (line.hasOption("n"))
			{
				setTailLastLinesFromArg(line.getOptionValue("n"));
			}


			//
			// -e is applied AFTER -n so that it wins, which is what the README and
			// the help text promise. Applied before, -n silently overwrote it and
			// `ctr -e -n 50` showed 50 lines instead of the whole file
			//
			if (line.hasOption("e"))
			{
				//
				// the whole file means no tail positioning of any kind
				//
				CtrailProps.getInstance().setSkipAheadInBytes(0);
				CtrailProps.getInstance().setTailLastLines(0);
			}
			if (line.hasOption("f"))
			{
				CtrailProps.getInstance().setEnabledFileFiltering(Boolean.parseBoolean(line.getOptionValue("f")));
			}
			if (line.hasOption("v"))
			{
				//
				// -v toggles the exclude half of filtering; it used to set the
				// same flag as -f, so the two options were indistinguishable
				//
				CtrailProps.getInstance().setEnabledExcludeFiltering(Boolean.parseBoolean(line.getOptionValue("v")));
			}
			if (line.hasOption("version"))
			{
				System.out.println("ctrail, version:" + CtrailProps.getInstance().getVersion());
				System.exit(0);
			}

			if (line.hasOption("h"))
			{
				final HelpFormatter formatter = new HelpFormatter();
				final String header = "Color Trail - a tail -f replacement with colored output\n\n";
				final String footer = "\nAvailable colors (case-insensitive):\n"
						+ "  Regular:     BLACK  RED  GREEN  YELLOW  BLUE  PURPLE  CYAN  WHITE\n"
						+ "  Bold:        *_BOLD  (e.g. RED_BOLD)  also: ORANGE (= YELLOW_BOLD)\n"
						+ "  Underlined:  *_UNDERLINED  (e.g. RED_UNDERLINED)\n"
						+ "  Bright:      *_BRIGHT  (e.g. RED_BRIGHT)\n"
						+ "  Bold Bright: *_BOLD_BRIGHT  (e.g. RED_BOLD_BRIGHT)\n"
						+ "  Background:  *_BACKGROUND  (e.g. RED_BACKGROUND)\n"
						+ "  Bg Bright:   *_BACKGROUND_BRIGHT  (e.g. RED_BACKGROUND_BRIGHT)\n";
				formatter.setWidth(100);
				formatter.printHelp("ctr [options] [file ...]", header, options, footer);
				System.exit(0);
			}


			return line.getArgs();
		}
		catch (final ParseException ex_)
		{
			_logger.error(ex_.toString());
		}


		return args_;
	}





	@Override
	public void initiateShutdown()
	{
		try
		{
			synchronized (_runtimeHolder)
			{
				_shutdownRequested = true;
				_runtimeHolder.notifyAll();
			}
		}
		catch (Exception ex_)
		{
			_logger.error(ex_.toString(), ex_);
		}
	}





	protected void start()
	{
		if (_reader == null || _writer == null)
		{
			throw new RuntimeException("Reader/Writer not initialized correctly");
		}

		_logger.trace("strating worker threads");
		_reader.start();
		_writer.start();

		if (_idleMonitor != null)
		{
			_idleMonitor.start();
		}
	}





	/**
	 * awaits indefinitely.
	 */
	protected void awaitShutdownInstruction()
	{
		awaitShutdownInstruction(0);
	}





	/**
	 * millis_<=0 will await indefinitely.
	 */
	protected void awaitShutdownInstruction(int millis_)
	{
		try
		{
			_logger.trace("awaiting shutdown instructions, millis-to-wait:{}", millis_);
			synchronized (_runtimeHolder)
			{
				if (millis_ > 0)
				{
					awaitShutdownFor(millis_);
				}
				else
				{
					awaitShutdownIndefinitely();
				}
			}
		}
		catch (InterruptedException ex_)
		{
			_logger.error(ex_.toString(), ex_);
			Thread.currentThread().interrupt();
		}

		return;
	}





	/**
	 * Waits until shutdown is requested. The flag, not the notify, is the
	 * condition: a request made before this thread waited is still seen, and a
	 * spurious wakeup goes back to waiting. Caller holds _runtimeHolder.
	 *
	 * @throws InterruptedException if the waiting thread is interrupted
	 */
	private void awaitShutdownIndefinitely() throws InterruptedException
	{
		while (!_shutdownRequested)
		{
			_runtimeHolder.wait();
		}
	}





	/**
	 * Waits until shutdown is requested or the timeout elapses, whichever comes
	 * first. Caller holds _runtimeHolder.
	 *
	 * @param millis_ the maximum time to wait, in milliseconds; must be positive
	 * @throws InterruptedException if the waiting thread is interrupted
	 */
	private void awaitShutdownFor(final int millis_) throws InterruptedException
	{
		final long deadline = System.currentTimeMillis() + millis_;
		long remaining = millis_;
		while (!_shutdownRequested && remaining > 0)
		{
			_runtimeHolder.wait(remaining);
			remaining = deadline - System.currentTimeMillis();
		}
	}





	protected void shutdown()
	{

		if (_logger.isDebugEnabled())
		{
			_logger.debug("starting shutdown process...");
		}



		//
		// the monitor goes first: stopped after the writer, a late notice could
		// be printed behind the last real line while the queue drains
		//
		if (_idleMonitor != null)
		{
			_logger.trace("stopping idle monitor");
			_idleMonitor.setShouldContinue(false);
			_idleMonitor.interrupt();
			_idleMonitor = null;
		}


		if (_reader != null)
		{
			_logger.trace("interrupting reader");
			_reader.interrupt();
			_reader = null;
		}


		//
		// release the file handles. Nothing closed these before, so a multi-file
		// tail held every descriptor until the JVM exited and this cleanup path
		// omitted them entirely
		//
		if (_fileTrackers != null)
		{
			final Iterator<FileTailTracker> trackers = _fileTrackers.iterator();
			while (trackers.hasNext())
			{
				trackers.next().close();
			}
		}


		if (_writer != null)
		{
			if (_writer instanceof OutputWriterThread)
			{
				_logger.trace("shutting down writer");
				((OutputWriterThread) _writer).setShouldContinue(false, false);
			}
			else
			{
				_logger.trace("interrupting writer");
				_writer.interrupt();
			}
			_writer = null;
		}
	}





	public static void main(final String[] args_)
	{
		//
		// nothing to tail is a usage error: say so and exit non-zero rather
		// than start threads that have nothing to read
		//
		final CtrailEntryPoint trailer;
		try
		{
			trailer = new CtrailEntryPoint(args_);
		}
		catch (final NoReadableInputException ex_)
		{
			//
			// stderr, not the logger: logback writes to stdout, which would put
			// the error into whatever the output is piped to, and print it twice
			//
			_logger.debug("exiting, no readable input:{}", ex_.getMessage());
			System.err.println("ctrail: " + ex_.getMessage());
			System.exit(EXIT_NO_READABLE_INPUT);
			return;
		}

		trailer.start();
		trailer.awaitShutdownInstruction();
		trailer.shutdown();
	}
}
