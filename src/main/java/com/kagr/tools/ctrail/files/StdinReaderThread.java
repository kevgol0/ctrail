/****************************************************************************
 * FILE: StdinReaderThread.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Deque;

import com.kagr.tools.ctrail.IShutdownManager;
import com.kagr.tools.ctrail.unit.LogLine;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;





@Slf4j
public class StdinReaderThread implements Runnable
{
    private final InputStream _iStream;

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private Deque<LogLine> _output;

    @Getter
    private final String _match;

    @Getter
    private final IShutdownManager _ender;





    public StdinReaderThread(final InputStream is_, @NonNull final Deque<LogLine> output_, final String match_, final IShutdownManager shutdownMgr_)
    {
        _iStream = is_;
        _match = match_;
        _ender = shutdownMgr_;

        setOutput(output_);
    }





    @Override
    public void run()
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(_iStream)))
        {
            String line;
            if (_match == null)
            {
                while ((line = reader.readLine()) != null)
                {
                    _output.add(new LogLine("stdin", line, null));
                }
            }
            else
            {
                while ((line = reader.readLine()) != null)
                {
                    if (line.contains(_match))
                    {
                        _output.add(new LogLine("stdin", line, null));
                    }
                }
            }
        }
        catch (final Exception ex_)
        {
            _logger.error(ex_.toString());
        }

        if (_logger.isTraceEnabled())
        {
            _logger.trace("finished read from std-in");
        }


        //
        // stop the output thread as 
        // soon as they finish processing
        //
        _ender.initiateShutdown();
    }

}
