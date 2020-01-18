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
public class OutputWriterThread implements Runnable
{
	@Getter @Setter(AccessLevel.PRIVATE) BlockingDeque<LogLine> _output;
	@Getter @Setter PrintStream _sout;

	@Getter LineFormatter _formatter;




	public OutputWriterThread(@NonNull BlockingDeque<LogLine> output_, @NonNull PrintStream out_)
	{
		setOutput(output_);
		setSout(out_);
		_formatter = new LineFormatter();
	}





	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				_sout.println(_formatter.format(_output.take()));
			}
			catch (InterruptedException ex_)
			{
				_logger.error(ex_.toString());
			}
		}
	}

}
