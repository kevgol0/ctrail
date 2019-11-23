/****************************************************************************
 * FILE: CtrailEntryPoint.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;



import com.kagr.tools.ctrail.files.FileReaderThread;
import com.kagr.tools.ctrail.files.FileTailTracker;
import com.kagr.tools.ctrail.files.StdinReaderThread;
import com.kagr.tools.ctrail.files.StdoutWriterThread;
import com.kagr.tools.ctrail.unit.LogLine;



import lombok.extern.slf4j.Slf4j;





@Slf4j
public class CtrailEntryPoint
{
	private Thread _reader;
	private Thread _writer;
	private BlockingDeque<LogLine> _output;
	private BlockingDeque<FileTailTracker> _fileTrackers;





	public CtrailEntryPoint(String[] args_)
	{
		//
		// props
		// 
		loadProps();
		loadArgsAndOverrides(args_);




		//
		// start them up
		//
		_output = new LinkedBlockingDeque<LogLine>(CtrailProps.getInstance().getMaxPendingLines());
		_fileTrackers = new LinkedBlockingDeque<FileTailTracker>(CtrailProps.getInstance().getMaxNbrInputFiles());
		initWriterThreads();
		initReaderThreads(args_);

	}





	private void initReaderThreads(String[] args_)
	{
		_fileTrackers = getFilesFromArgs(args_);
		if (_fileTrackers.size() <= 0)
		{
			_reader = new Thread(new StdinReaderThread(System.in, _output));
		}
		else
		{
			_reader = new Thread(new FileReaderThread(_fileTrackers, _output));
		}
		_reader.start();
	}





	private void initWriterThreads()
	{
		_writer = new Thread(new StdoutWriterThread(_output));
		_writer.start();
	}





	private BlockingDeque<FileTailTracker> getFilesFromArgs(String[] args_)
	{
		int maxFileCnt = CtrailProps.getInstance().getMaxNbrInputFiles();
		LinkedBlockingDeque<FileTailTracker> deq = new LinkedBlockingDeque<>(maxFileCnt);
		int cntr = 0;
		File file;
		for (String s : args_)
		{
			try
			{
				file = new File(s);
				if (!file.isFile() || !file.canRead())
				{
					if (_logger.isInfoEnabled())
						_logger.info("{} is either not a file or not readable", s);
					continue;
				}

				if (cntr++ >= maxFileCnt)
				{
					_logger.warn("max number of files exceeded:{}, ignoring remaining files", cntr);
					break;
				}

				deq.add(new FileTailTracker(s, new RandomAccessFile(file, "r")));
			}
			catch (Exception ex_)
			{
				_logger.error(ex_.toString());
			}
		}
		return deq;
	}





	private CtrailProps loadProps()
	{
		return CtrailProps.getInstance();
	}





	private void loadArgsAndOverrides(String[] args_)
	{
	}





	protected void start(int millis_)
	{

		try
		{
			synchronized (this)
			{
				if (millis_ > 0)
					wait(millis_);
				else
					wait();

			}
		}
		catch (InterruptedException ex_)
		{
			_logger.error(ex_.toString());
		}
	}





	public static void main(String[] args_)
	{
		CtrailEntryPoint trailer = new CtrailEntryPoint(args_);
		trailer.start(0);
	}

}
