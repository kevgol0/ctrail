/****************************************************************************
 * FILE: CtrailWriterThread.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.io.PrintStream;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

import com.kagr.tools.ctrail.unit.LineFormatter;
import com.kagr.tools.ctrail.unit.LogLine;



import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;




@Slf4j
public class OutputWriterThread extends Thread
{
    @Getter
    @Setter(AccessLevel.PRIVATE)
    BlockingDeque<LogLine> _output;

    @Getter
    @Setter
    PrintStream _sout;

    @Getter
    LineFormatter _formatter;

    //
    // written by the shutdown thread, read by this one -- without volatile the
    // run-loop may never observe the flag flipping to false
    //
    @Getter
    volatile boolean _shouldContinue;

    //
    // how long take() may park before the loop re-checks _shouldContinue
    //
    private static final long POLL_TIMEOUT_MILLIS = 100;





    public OutputWriterThread(@NonNull final BlockingDeque<LogLine> output_, @NonNull final PrintStream out_)
    {
        super("output-writter");
        setOutput(output_);
        setSout(out_);
        _shouldContinue = true;
        _formatter = new LineFormatter();
    }





    @Override
    public void run()
    {
        while (_shouldContinue)
        {
            try
            {
                //
                // poll rather than take: shutdown() flips _shouldContinue
                // without interrupting, and a parked take() would never notice,
                // leaving this non-daemon thread alive and the JVM hung
                //
                final LogLine next = _output.poll(POLL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                if (next == null)
                {
                    continue;
                }

                _sout.println(_formatter.format(next));

                if (_logger.isTraceEnabled())
                {
                    _logger.trace("should-continue:{}", _shouldContinue);
                }
            }
            catch (final InterruptedException ex_)
            {
                if (_shouldContinue)
                {
                    _logger.error(ex_.toString());
                }
            }
        }



        // remove the rest of the entries from the q
        // however, use the pending size (in case someone
        // else keeps adding to it) - this has been marked as close
        int sz = _output.size();
        if (_logger.isDebugEnabled())
        {
            _logger.debug("outside of standard runloop, will consume remaining messages:{}", sz);
        }

        LogLine line = null;
        while (sz > 0)
        {
            sz -= 1;
            try
            {
                //
                // oldest entry first - don't wait forever. pollLast() drained
                // the tail in reverse, printing the backlog out of order
                //
                line = _output.pollFirst(1, TimeUnit.MILLISECONDS);
                if (line == null)
                {
                    if (_logger.isTraceEnabled())
                    {
                        _logger.trace("poll-timeout, breaking out of loop");
                    }
                    break;
                }


                //
                // show
                //
                _sout.println(_formatter.format(line));
                if (_logger.isTraceEnabled())
                {
                    _logger.trace("emptying q:{}", sz);
                }
            }
            catch (final InterruptedException ex_)
            {
                _logger.error(ex_.toString());
            }
        }

        //
        // a redirected PrintStream is not auto-flushing, so the drained backlog
        // can otherwise sit in the buffer and never reach the destination
        //
        _sout.flush();

        if (_logger.isTraceEnabled())
        {
            _logger.trace("q is empty:{}", sz);
        }
    }





    public synchronized void setShouldContinue(final boolean sc_, final boolean shouldInterrupt_)
    {
        if (_logger.isDebugEnabled())
        {
            _logger.debug("sc:{}, interrupt:{}", sc_, shouldInterrupt_);
        }

        if (_shouldContinue == sc_)
        {
            return;
        }

        _shouldContinue = sc_;
        if (!_shouldContinue && shouldInterrupt_)
        {
            _logger.trace("about to interrupt");
            interrupt();
        }
    }

}
