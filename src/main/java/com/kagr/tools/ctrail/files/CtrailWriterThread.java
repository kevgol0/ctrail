/****************************************************************************
 * FILE: CtrailWriterThread.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import java.util.concurrent.BlockingDeque;



import com.kagr.tools.ctrail.StringFormatter;



import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;





public class CtrailWriterThread implements Runnable
{
	@Getter @Setter(AccessLevel.PRIVATE) BlockingDeque<String> _output;
	@Getter StringFormatter _formatter;





	public CtrailWriterThread(BlockingDeque<String> output_)
	{
		setOutput(output_);
		_formatter = new StringFormatter();
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
				ex_.printStackTrace(System.err);
			}
		}
	}

}
