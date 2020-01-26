/****************************************************************************
 * FILE: OutputReplacer.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;



import org.junit.Assert;



import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;





@Slf4j
public class StdCtrTest
{
	@Getter File _tmpFile;
	@Getter PrintStream _stdOut;





	public PrintStream replaceStdOut()
	{
		return replaceStdOut(false);
	}





	public PrintStream replaceStdOut(boolean printTempFile_)
	{
		try
		{
			_tmpFile = File.createTempFile(getClass().getSimpleName(), ".tmp");
			_tmpFile.deleteOnExit();
			if (printTempFile_)
				System.out.println("temp file: " + _tmpFile.getAbsolutePath());
			PrintStream newOut = new PrintStream(_tmpFile);
			_stdOut = System.out;
			resetStdOut(newOut);
			return _stdOut;
		}
		catch (FileNotFoundException ex_)
		{
			_logger.error(ex_.toString(), ex_);
		}
		catch (IOException ex_)
		{
			_logger.error(ex_.toString(), ex_);
		}

		return null;
	}





	public Path getTempFilePath()
	{
		if (_tmpFile != null)
			return _tmpFile.toPath();
		return null;
	}





	public void resetStdOut()
	{
		resetStdOut(_stdOut);
	}





	public void resetStdOut(@NonNull PrintStream ps_)
	{
		System.setOut(ps_);
	}





	public boolean compareFiles(Path expectedFile_)
	{
		try
		{
			byte[] expected = Files.readAllBytes(expectedFile_);
			byte[] actual = Files.readAllBytes(getTempFilePath());
			return Arrays.equals(expected, actual);
		}
		catch (IOException ex_)
		{
			_logger.error(ex_.toString(), ex_);
		}

		return false;
	}
}
