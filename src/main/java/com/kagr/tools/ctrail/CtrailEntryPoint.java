/****************************************************************************
 * FILE: CtrailEntryPoint.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;



import com.kagr.tools.ctrail.files.StdoutWriterThread;
import com.kagr.tools.ctrail.files.FileReaderThread;
import com.kagr.tools.ctrail.files.FileTailTracker;
import com.kagr.tools.ctrail.files.StdinReaderThread;





public class CtrailEntryPoint
{
	private Thread _reader;
	private Thread _writer;
	private BlockingDeque<String> _output;
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
		_output = new LinkedBlockingDeque<String>(CtrailProps.getInstance().getMaxPendingLines());
		_fileTrackers = new LinkedBlockingDeque<FileTailTracker>();
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
		LinkedBlockingDeque<FileTailTracker> deq = new LinkedBlockingDeque<>();
		for (String s : args_)
		{
			try
			{
				RandomAccessFile raf = new RandomAccessFile(new File(s), "r");
				deq.add(new FileTailTracker(s, raf));
			}
			catch (Exception ex_)
			{
				ex_.printStackTrace(System.err);
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





	private void start()
	{

		try
		{
			synchronized (this)
			{
				wait();
			}
		}
		catch (InterruptedException ex_)
		{
			ex_.printStackTrace(System.err);
		}
	}





	public static void main(String[] args_)
	{
		CtrailEntryPoint trailer = new CtrailEntryPoint(args_);
		trailer.start();
	}

}
