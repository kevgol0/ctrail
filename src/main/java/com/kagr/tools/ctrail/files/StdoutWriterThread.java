/****************************************************************************
 * FILE: CtrailWriterThread.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.util.concurrent.BlockingDeque;



import com.kagr.tools.ctrail.unit.LogLine;
import com.kagr.tools.ctrail.unit.LineFormatter;



import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;




@Slf4j
public class StdoutWriterThread implements Runnable
{
	@Getter @Setter(AccessLevel.PRIVATE) BlockingDeque<LogLine> _output;
	@Getter LineFormatter _formatter;





	public StdoutWriterThread(BlockingDeque<LogLine> output_)
	{
		setOutput(output_);
		_formatter = new LineFormatter();
	}





	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				System.out.println(_formatter.format(_output.take()));
			}
			catch (InterruptedException ex_)
			{
				_logger.error(ex_.toString());
			}
		}
	}

}
