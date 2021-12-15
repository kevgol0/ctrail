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
import com.kagr.tools.ctrail.files.OutputWriterThread;
import com.kagr.tools.ctrail.files.StdinReaderThread;
import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.FileSearchFilter;
import com.kagr.tools.ctrail.unit.LogLine;



import lombok.extern.slf4j.Slf4j;





@Slf4j
public class CtrailEntryPoint implements IShutdownManager
{
    private Thread _reader;
    private Thread _writer;
    private BlockingDeque<LogLine> _output;
    private BlockingDeque<FileTailTracker> _fileTrackers;
    private String _matchpattern;
    private final Object _runtimeHolder;





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
            throw new RuntimeException("Sytem not ready, initReaderThread called with no output mechanism");
        }

        _fileTrackers = getFilesFromArgs(args_);
        if (_fileTrackers.size() <= 0)
        {
            _reader = new Thread(new StdinReaderThread(System.in, _output, _matchpattern, this));
            _reader.setName("istream-reader");
        }
        else
        {
            _reader = new Thread(new FileReaderThread(_fileTrackers, _output, _matchpattern, this));
        }

        _reader.start();
    }





    private void initConsoleWriterThread()
    {
        _output = new LinkedBlockingDeque<LogLine>(CtrailProps.getInstance().getMaxPendingLines());
        _writer = new OutputWriterThread(_output, System.out);
        _writer.start();
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
                    if (_logger.isInfoEnabled())
                    {
                        _logger.info("{} is either not a file or not readable", s);
                    }
                    continue;
                }

                if (cntr++ >= maxFileCnt)
                {
                    _logger.warn("max number of files exceeded:{}, ignoring remaining files", cntr);
                    break;
                }

                final FileTailTracker ftracker = new FileTailTracker(filename, new RandomAccessFile(file, "r"));
                findAndSetFileTracker(fstMap, filename, ftracker);
                deq.add(ftracker);
            }
            catch (final Exception ex_)
            {
                _logger.error(ex_.toString());
            }
        }
        return deq;
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





    private String[] loadArgsAndOverrides(final String[] args_)
    {
        final CommandLineParser parser = new DefaultParser();
        final Options options = new Options();

        options.addOption(Option.builder("e").longOpt("entirefile").desc("runs through the entire file").build());
        options.addOption(Option.builder("m").longOpt("match").hasArg().argName("STR")
                .desc("only show lines that match STR")
                .build());
        options.addOption(Option.builder("f").longOpt("filters").hasArg().desc("overrides config for file-filtering; <arg=true|false>").build());
        options.addOption(Option.builder("h").longOpt("help").desc("print command line directives").build());


        try
        {
            final CommandLine line = parser.parse(options, args_);

            if (line.hasOption("e"))
            {
                CtrailProps.getInstance().setSkipAheadInBytes(0);
            }
            if (line.hasOption("m"))
            {
                _matchpattern = line.getOptionValue("m");
            }
            if (line.hasOption("f"))
            {
                CtrailProps.getInstance().setEnabledFileFiltering(Boolean.parseBoolean(line.getOptionValue("f")));
            }

            if (line.hasOption("h"))
            {
                final HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("ctr", options);
                System.exit(1);
            }


            return line.getArgs();
        }
        catch (final ParseException ex_)
        {
            _logger.error(ex_.toString());
        }


        return args_;
    }





    protected void start(int millis_)
    {
        try
        {
            synchronized (_runtimeHolder)
            {
                if (millis_ > 0)
                {
                    _runtimeHolder.wait(millis_);
                }
                else
                {
                    _runtimeHolder.wait();
                }
            }

            return;
        }
        catch (final InterruptedException ex_)
        {
            _logger.error(ex_.toString());
        }
    }





    @Override
    public void initiateShutdown()
    {
        if (_logger.isDebugEnabled())
        {
            _logger.debug("starting shutdown process...");
        }
        
        if (_writer != null)
        {
            if (_writer instanceof OutputWriterThread)
            {
                ((OutputWriterThread) _writer).setShouldContinue(false, false);
            }
        }

        try
        {
            synchronized (_runtimeHolder)
            {
                _runtimeHolder.notifyAll();
            }
        }
        catch (Exception ex_)
        {
            _logger.error(ex_.toString(), ex_);
        }
    }





    public static void main(final String[] args_)
    {
        final CtrailEntryPoint trailer = new CtrailEntryPoint(args_);
        trailer.start(0);
    }
}
