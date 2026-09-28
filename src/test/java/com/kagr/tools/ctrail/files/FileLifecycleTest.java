/****************************************************************************
 * FILE: FileLifecycleTest.java
 * DSCRPT: a tracker's lifetime - rotation, close, and refusing to exist in an
 *         inconsistent state. CTRAIL-5, -11, -13, -15.
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;



import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;



import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;



import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.unit.LogLine;





public class FileLifecycleTest
{
	@Rule public TemporaryFolder _tmp = new TemporaryFolder();

	private Deque<LogLine> _out;





	@Before
	public void setUp()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-liveness-disabled.xml").toString());
		CtrailProps.getInstance();
		_out = new LinkedBlockingDeque<>();
	}





	private File write(final String content_) throws IOException
	{
		final File f = _tmp.newFile();
		Files.write(f.toPath(), content_.getBytes(StandardCharsets.UTF_8));
		return f;
	}





	/**
	 * CTRAIL-5. A truncated file left the read position past EOF, so
	 * getRemainingSize() stayed negative and the tail went silent forever - while
	 * the idle monitor reported that silence as fact.
	 */
	@Test
	public void rotationResetsThePositionAndSaysSo() throws Exception
	{
		final File f = write("l1\nl2\nl3\nl4\nl5\n");
		final RandomAccessFile raf = new RandomAccessFile(f, "r");
		try
		{
			final FileTailTracker tracker = new FileTailTracker(f.getName(), raf);
			tracker.setLastReadPosition(f.length());

			// logrotate-style truncation, then new content
			Files.write(f.toPath(), "fresh\n".getBytes(StandardCharsets.UTF_8));
			assertTrue("remaining must be negative before the fix runs", tracker.getRemainingSize() < 0);

			assertTrue("rotation must be detected", tracker.handleRotation(_out));
			assertEquals("position restarts at the new content", 0, tracker.getLastReadPosition());
			assertEquals("remaining is now the new file's size", f.length(), tracker.getRemainingSize());

			final LogLine notice = _out.poll();
			assertTrue("rotation must be announced, not silent", notice != null && notice.isNotice());
			assertTrue(notice.getLine().endsWith(" - rotated, following new file"));
		}
		finally
		{
			raf.close();
		}
	}





	@Test
	public void aFileThatSimplyGrowsIsNotTreatedAsRotated() throws Exception
	{
		final File f = write("l1\nl2\n");
		final RandomAccessFile raf = new RandomAccessFile(f, "r");
		try
		{
			final FileTailTracker tracker = new FileTailTracker(f.getName(), raf);
			tracker.setLastReadPosition(f.length());
			Files.write(f.toPath(), "l1\nl2\nl3\n".getBytes(StandardCharsets.UTF_8));

			assertFalse("growth is not rotation", tracker.handleRotation(_out));
			assertTrue("nothing to announce", _out.isEmpty());
		}
		finally
		{
			raf.close();
		}
	}





	/**
	 * CTRAIL-13. The backwards scan counted only '\n' while readLine() also breaks
	 * on a lone '\r', so a CR-terminated file found no breaks and replayed whole.
	 */
	@Test
	public void tailNHonoursLoneCarriageReturns() throws Exception
	{
		final File f = write("line1\rline2\rline3\rline4\rline5\r");
		final RandomAccessFile raf = new RandomAccessFile(f, "r");
		try
		{
			final FileTailTracker tracker = new FileTailTracker(f.getName(), raf);
			tracker.seekToLastNLines(2);

			final List<String> lines = new ArrayList<>();
			String line = raf.readLine();
			while (line != null)
			{
				lines.add(line);
				line = raf.readLine();
			}

			assertEquals("only the last two lines are history", 2, lines.size());
			assertEquals("line4", lines.get(0));
			assertEquals("line5", lines.get(1));
		}
		finally
		{
			raf.close();
		}
	}





	@Test
	public void tailNStillHandlesCrLfAsOneTerminator() throws Exception
	{
		final File f = write("a\r\nb\r\nc\r\nd\r\n");
		final RandomAccessFile raf = new RandomAccessFile(f, "r");
		try
		{
			new FileTailTracker(f.getName(), raf).seekToLastNLines(2);

			assertEquals("c", raf.readLine());
			assertEquals("d", raf.readLine());
		}
		finally
		{
			raf.close();
		}
	}





	/**
	 * CTRAIL-15. Nothing closed these, so a multi-file tail held every descriptor
	 * until the JVM exited.
	 */
	@Test
	public void closeReleasesTheHandle() throws Exception
	{
		final File f = write("x\n");
		final RandomAccessFile raf = new RandomAccessFile(f, "r");
		final FileTailTracker tracker = new FileTailTracker(f.getName(), raf);

		tracker.close();

		try
		{
			raf.length();
			fail("the handle should be closed");
		}
		catch (final IOException expected)
		{
			// closed, as intended
		}
	}





	/**
	 * CTRAIL-11. A tracker whose position could not be established used to be
	 * returned anyway, with the file pointer and _lastReadPosition disagreeing -
	 * which either spins the reader at 100% CPU or dumps the whole file.
	 */
	@Test
	public void aTrackerThatCannotBePositionedIsRefused() throws Exception
	{
		final File f = write("l1\nl2\nl3\n");
		final RandomAccessFile raf = new RandomAccessFile(f, "r");
		raf.close();

		try
		{
			new FileTailTracker(f.getName(), raf);
			fail("a tracker that cannot be positioned must not be constructed");
		}
		catch (final IllegalStateException expected)
		{
			assertTrue(expected.getMessage().contains("cannot position file"));
		}
	}
}
