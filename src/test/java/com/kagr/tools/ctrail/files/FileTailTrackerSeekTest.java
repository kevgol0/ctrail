/****************************************************************************
 * FILE: FileTailTrackerSeekTest.java
 * DSCRPT: pins the backwards line-scan used to position a newly opened file.
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import static org.junit.Assert.assertEquals;



import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;



import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;



import com.kagr.tools.ctrail.props.CtrailProps;





public class FileTailTrackerSeekTest
{
	@Rule public TemporaryFolder _tmp = new TemporaryFolder();





	@Before
	public void useConfigThatLeavesPositioningToTheTest()
	{
		//
		// tail-N off in config, so the constructor does not pre-position and each
		// test drives seekToLastNLines directly
		//
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-liveness-disabled.xml").toString());
		CtrailProps.getInstance();
	}





	@Test
	public void emptyFileStartsAtZero() throws IOException
	{
		final long pos = seek("", 3);
		assertEquals(0, pos);
	}





	@Test
	public void tailsTheLastNLinesWhenFileEndsWithNewline() throws IOException
	{
		final List<String> lines = tail("a\nb\nc\n", 2);
		assertEquals(2, lines.size());
		assertEquals("b", lines.get(0));
		assertEquals("c", lines.get(1));
	}





	@Test
	public void tailsTheLastNLinesWhenFileHasNoTrailingNewline() throws IOException
	{
		final List<String> lines = tail("a\nb\nc", 2);
		assertEquals(2, lines.size());
		assertEquals("b", lines.get(0));
		assertEquals("c", lines.get(1));
	}





	@Test
	public void fewerLinesThanRequestedStartsAtBeginningOfFile() throws IOException
	{
		assertEquals(0, seek("a\nb\nc\n", 10));

		final List<String> lines = tail("a\nb\nc\n", 10);
		assertEquals(3, lines.size());
		assertEquals("a", lines.get(0));
	}





	@Test
	public void exactlyNLinesStartsAtBeginningOfFile() throws IOException
	{
		assertEquals(0, seek("a\nb\nc\n", 3));
	}





	@Test
	public void singleLineFileIsReturnedWhole() throws IOException
	{
		final List<String> lines = tail("only one line\n", 5);
		assertEquals(1, lines.size());
		assertEquals("only one line", lines.get(0));
	}





	@Test
	public void carriageReturnsAreNotTreatedAsContent() throws IOException
	{
		final List<String> lines = tail("a\r\nb\r\nc\r\n", 2);
		assertEquals(2, lines.size());
		assertEquals("b", lines.get(0));
		assertEquals("c", lines.get(1));
	}





	@Test
	public void scanCrossesChunkBoundariesOnALargeFile() throws IOException
	{
		//
		// well beyond the 8k backwards-scan chunk, so the loop has to walk
		// several chunks to find three line breaks
		//
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5000; i++)
		{
			sb.append("line ").append(i).append('\n');
		}

		final List<String> lines = tail(sb.toString(), 3);
		assertEquals(3, lines.size());
		assertEquals("line 4997", lines.get(0));
		assertEquals("line 4999", lines.get(2));
	}





	@Test
	public void nonPositiveLineCountIsANoOp() throws IOException
	{
		final File f = write("a\nb\nc\n");
		final RandomAccessFile raf = new RandomAccessFile(f, "r");
		try
		{
			final FileTailTracker tracker = new FileTailTracker(f.getName(), raf);
			tracker.setLastReadPosition(7);
			assertEquals(7, tracker.seekToLastNLines(0));
			assertEquals(7, tracker.seekToLastNLines(-4));
		}
		finally
		{
			raf.close();
		}
	}





	private File write(final String content_) throws IOException
	{
		final File f = _tmp.newFile();
		Files.write(f.toPath(), content_.getBytes(StandardCharsets.UTF_8));
		return f;
	}





	private long seek(final String content_, final int nLines_) throws IOException
	{
		final File f = write(content_);
		final RandomAccessFile raf = new RandomAccessFile(f, "r");
		try
		{
			return new FileTailTracker(f.getName(), raf).seekToLastNLines(nLines_);
		}
		finally
		{
			raf.close();
		}
	}





	private List<String> tail(final String content_, final int nLines_) throws IOException
	{
		final File f = write(content_);
		final RandomAccessFile raf = new RandomAccessFile(f, "r");
		try
		{
			final FileTailTracker tracker = new FileTailTracker(f.getName(), raf);
			tracker.seekToLastNLines(nLines_);

			final List<String> lines = new ArrayList<>();
			String line = raf.readLine();
			while (line != null)
			{
				lines.add(line);
				line = raf.readLine();
			}
			return lines;
		}
		finally
		{
			raf.close();
		}
	}
}
