/****************************************************************************
 * FILE: CtrailWriterThread.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.io.PrintStream;
import java.util.concurrent.BlockingDeque;



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

    @Getter
    boolean _shouldContinue;





    public OutputWriterThread(@NonNull BlockingDeque<LogLine> output_, @NonNull PrintStream out_)
    {
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
                _sout.println(_formatter.format(_output.take()));

                if (_logger.isTraceEnabled())
                {
                    _logger.trace("should-continue:{}", _shouldContinue);
                }
            }
            catch (InterruptedException ex_)
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
        int sz = _output.size() - 1;
        while (sz-- > 0)
        {
            try
            {
                _sout.println(_formatter.format(_output.take()));
                if (_logger.isTraceEnabled())
                {
                    _logger.trace("emptying q:{}", sz);
                }
            }
            catch (InterruptedException ex_)
            {
                _logger.error(ex_.toString());
            }
        }

        if (_logger.isTraceEnabled())
        {
            _logger.trace("q is empty:{}", sz);
        }
    }





    public synchronized void setShouldContinue(boolean sc_, boolean shouldInterrupt_)
    {

        _shouldContinue = sc_;
        if (!_shouldContinue && shouldInterrupt_)
        {
            Thread.currentThread().interrupt();
        }
    }

}
