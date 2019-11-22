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
	private InputStream _iStream;

	@Getter @Setter(AccessLevel.PROTECTED) private Deque<LogLine> _output;





	public StdinReaderThread(InputStream is_, @NonNull Deque<LogLine> output_)
	{
		_iStream = is_;
		setOutput(output_);
	}





	public void run()
	{
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(_iStream)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				_output.add(new LogLine(line));
			}
		}
		catch (Exception ex_)
		{
			_logger.error(ex_.toString());

		}
	}

}
