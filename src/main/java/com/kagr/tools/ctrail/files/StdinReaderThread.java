/****************************************************************************
 * FILE: StdinReaderThread.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Deque;



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
    @Setter(AccessLevel.PRIVATE)
    private String _match;

    @Getter
    private final OutputWriterThread _outThread;





    public StdinReaderThread(final InputStream is_, @NonNull final Deque<LogLine> output_, final String match_, @NonNull final OutputWriterThread outThread_)
    {
        _iStream = is_;
        setOutput(output_);
        setMatch(match_);
        _outThread = outThread_;
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


        _outThread.setShouldContinue(false, true);
        if (_logger.isTraceEnabled())
        {
            _logger.trace("finished read from std-in");
        }
    }

}
