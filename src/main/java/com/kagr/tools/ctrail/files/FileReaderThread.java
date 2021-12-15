/****************************************************************************
 * FILE: FileReaderThread.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.BlockingDeque;

import com.kagr.tools.ctrail.IShutdownManager;
import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.FileSearchFilter;
import com.kagr.tools.ctrail.unit.LogLine;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;





@Slf4j
public class FileReaderThread implements Runnable
{
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private BlockingDeque<FileTailTracker> _fileTrackers;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private Deque<LogLine> _output;

    @Getter
    @Setter
    private int _maxLinesPerThread;

    @Getter
    @Setter
    private boolean _defLineExclude;

    // command line matching
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private String _match;

    private final CtrailProps _props;
    private final IShutdownManager _ender;





    public FileReaderThread(@NonNull final BlockingDeque<FileTailTracker> fileTrackers_, final @NonNull Deque<LogLine> strOutput_, final String match_, final @NonNull IShutdownManager smgr_)
    {
        _props = CtrailProps.getInstance();
        setFileTrackers(fileTrackers_);
        setOutput(strOutput_);
        setMatch(match_);
        setMaxLinesPerThread(CtrailProps.getInstance().getMaxProcessingLinesPerThread());
        setDefLineExclude(!CtrailProps.getInstance().isFileFilterDefaultsToInclude());

        _ender = smgr_;
    }





    @Override
    public void run()
    {
        long szToRead;
        FileTailTracker tracker;
        int nFiles = _fileTrackers.size();
        int nEmptyItrCnt = 0;
        final int sleepTime = _props.getNoChangeSleepTimeMillis();
        while (true)
        {
            try
            {
                tracker = _fileTrackers.take();
                if (tracker == null)
                {
                    _logger.error("null tracker retrieed, exiting run loop");
                    break;
                }



                //
                // check for advancements... if there is a 
                // failure in file ptr, then this file will 
                // not be added back into the queue...
                //
                szToRead = tracker.getRemainingSize();
                if (szToRead > 0)
                {
                    nEmptyItrCnt = 0;

                    // there is more data to read, do so...
                    // if this fails, (meaning there was a file read error)
                    // then the file will not return unto the queue...
                    readToFilePosition(tracker);


                    if (_props.isBlankLineOnFileChange())
                    {
                        _output.add(new LogLine(null, "", null));
                    }
                }
                else
                {
                    nEmptyItrCnt += 1;
                }

                _fileTrackers.putLast(tracker);


                //
                // i have check all the files and no one
                // has advanced, take a break
                //
                if (nEmptyItrCnt > nFiles)
                {
                    Thread.sleep(sleepTime);
                    nEmptyItrCnt = 0;
                }
            }
            catch (final InterruptedException ex_)
            {
                _logger.error(ex_.toString());
                _logger.warn("Interrupted! - breaking out of read-loop");
                break;
            }
            catch (final IOException ex_)
            {
                // either a ptr-seek, or file read error happened
                // ither way, there is one less file to read from..
                nFiles -= 1;

                // show the error
                _logger.error(ex_.toString());

                // am I done?
                if (_fileTrackers.size() <= 0)
                {
                    _logger.warn("I/O error resulted in no additional files for processing, breaking out of read-loop");
                    _ender.initiateShutdown();
                    break;
                }
            }
        }
    }





    private final int readToFilePosition(final FileTailTracker tracker_) throws IOException
    {
        long readPos = tracker_.getFile().getFilePointer();
        int nReadLines = 0;
        final long eof = tracker_.getFile().length();
        String line;
        while (readPos < eof)
        {
            line = tracker_.getFile().readLine();
            if (line == null)
            {
                break;
            }

            if (_match != null && !line.contains(_match))
            {
                continue;
            }

            if (shouldExcludeLineDueToSeachTerms(line, tracker_.getFileSearchFilter()))
            {
                continue;
            }

            if (_props.isPrependFilenameToLine())
            {
                _output.add(new LogLine(tracker_.getFileName(), line, tracker_.getFileSearchFilter()));
            }
            else
            {
                _output.add(new LogLine(null, line, tracker_.getFileSearchFilter()));
            }
            readPos = tracker_.getFile().getFilePointer();
            tracker_.setLastReadPosition(readPos);


            nReadLines += 1;
            if (nReadLines >= _maxLinesPerThread)
            {
                return nReadLines;
            }
        }
        return nReadLines;
    }





    private final boolean shouldExcludeLineDueToSeachTerms(final String line_, final FileSearchFilter fileSearchTerms_)
    {
        //
        // see if this file has FileSearchTerms specified
        //
        if (fileSearchTerms_ != null)
        {
            //
            // includes trump excludes... this MUST happen first
            //
            for (int i = 0; i < fileSearchTerms_.getIncludeTerms().size(); i++)
            {
                if (line_.contains(fileSearchTerms_.getIncludeTerms().get(i)))
                {
                    //
                    // this file has a filter set, and i 
                    // found a search term specified in the include
                    // filter... I want to INCLUDE this line
                    //
                    return false;
                }
            }


            for (int i = 0; i < fileSearchTerms_.getExcldueTerms().size(); i++)
            {
                if (line_.contains(fileSearchTerms_.getExcldueTerms().get(i)))
                {
                    //
                    // this file has a filter set, and i 
                    // found a search term specified in the exclude
                    // filter... I want to EXCLUDE this line
                    //
                    return true;
                }
            }



            //
            // this file has a filter set, but i 
            // did not find any of the terms specified
            // in either the include or the exclude list
            //
            return _defLineExclude;
        }



        //
        // no search term associated with this line,
        // i want to INCLUDE it
        //
        return false;
    }



}
