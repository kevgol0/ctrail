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
import java.util.Arrays;



import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;





@Slf4j
public class StdCtrTest
{
    @Getter
    File _tmpFile;
    @Getter
    PrintStream _stdOut;





    public PrintStream replaceStdOut()
    {
        return replaceStdOut(false);
    }





    public PrintStream replaceStdOut(final boolean printTempFile_)
    {
        try
        {
            _tmpFile = File.createTempFile(getClass().getSimpleName(), ".tmp");
            _tmpFile.deleteOnExit();
            if (printTempFile_)
            {
                System.out.println("temp file: " + _tmpFile.getAbsolutePath());
            }
            final PrintStream newOut = new PrintStream(_tmpFile);
            _stdOut = System.out;
            resetStdOut(newOut);
            return _stdOut;
        }
        catch (final FileNotFoundException ex_)
        {
            _logger.error(ex_.toString(), ex_);
        }
        catch (final IOException ex_)
        {
            _logger.error(ex_.toString(), ex_);
        }

        return null;
    }





    public Path getTempFilePath()
    {
        if (_tmpFile != null)
        {
            return _tmpFile.toPath();
        }
        return null;
    }





    public void resetStdOut()
    {
        resetStdOut(_stdOut);
    }





    public void resetStdOut(@NonNull final PrintStream ps_)
    {
        System.setOut(ps_);
    }





    public boolean compareFiles(final Path expectedFile_)
    {
        try
        {
            final byte[] expected = Files.readAllBytes(expectedFile_);
            final byte[] actual = Files.readAllBytes(getTempFilePath());

            boolean rv = Arrays.equals(expected, actual);
            if (!rv)
            {
                for (int i = 0; i < expected.length; i++)
                {
                    if (expected[i] != actual[i])
                    {
                        System.err.println("i:" + i + ", expected=[" + expected[i] + "], actual=[" + actual[i] + "]");
                    }
                }
            }
            return rv;
        }
        catch (final IOException ex_)
        {
            _logger.error(ex_.toString(), ex_);
        }

        return false;
    }
}
