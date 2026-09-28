/****************************************************************************
 * FILE: CharsetHandlingTest.java
 * DSCRPT: CTRAIL-14 - input is decoded with an explicit, configurable charset
 *         rather than RandomAccessFile's hardwired Latin-1 and the platform
 *         default on stdin.
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;



import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;



import com.kagr.tools.ctrail.props.CtrailProps;





public class CharsetHandlingTest
{
	/** "café" and a Japanese word - both multi-byte in UTF-8 */
	private static final String _accented = "café latté";
	private static final String _japanese = "ログ";

	@Rule public TemporaryFolder _tmp = new TemporaryFolder();





	private void useConfig(final String fileName_)
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", fileName_).toString());
		CtrailProps.getInstance();
	}





	private File write(final String content_, final Charset charset_) throws IOException
	{
		final File f = _tmp.newFile();
		Files.write(f.toPath(), content_.getBytes(charset_));
		return f;
	}





	/**
	 * The regression. RandomAccessFile.readLine() discards the high 8 bits of each
	 * byte, so "café" came back as "cafÃ©".
	 */
	@Test
	public void utf8ContentSurvivesTheReadIntact() throws Exception
	{
		useConfig("ctrail-liveness-disabled.xml");
		final File f = write(_accented + "\n" + _japanese + "\n", StandardCharsets.UTF_8);
		final RandomAccessFile raf = new RandomAccessFile(f, "r");
		try
		{
			final FileTailTracker tracker = new FileTailTracker(f.getName(), raf);
			raf.seek(0);

			assertEquals(_accented, tracker.readLine(StandardCharsets.UTF_8));
			assertEquals(_japanese, tracker.readLine(StandardCharsets.UTF_8));
			assertEquals("end of file", null, tracker.readLine(StandardCharsets.UTF_8));
		}
		finally
		{
			raf.close();
		}
	}





	/**
	 * The knock-on effect that made this more than cosmetic: a non-ASCII filter
	 * keyword compared against mis-decoded text can never match.
	 */
	@Test
	public void aNonAsciiKeywordCanMatchADecodedLine() throws Exception
	{
		useConfig("ctrail-liveness-disabled.xml");
		final File f = write("plain\n" + _accented + "\n", StandardCharsets.UTF_8);
		final RandomAccessFile raf = new RandomAccessFile(f, "r");
		try
		{
			final FileTailTracker tracker = new FileTailTracker(f.getName(), raf);
			raf.seek(0);

			assertFalse(tracker.readLine(StandardCharsets.UTF_8).contains("café"));
			assertTrue("the accented keyword must match the decoded line",
					tracker.readLine(StandardCharsets.UTF_8).contains("café"));
		}
		finally
		{
			raf.close();
		}
	}





	@Test
	public void terminatorsBehaveAsReadLineDid() throws Exception
	{
		useConfig("ctrail-liveness-disabled.xml");
		final File f = write("a\nb\r\nc\rd", StandardCharsets.UTF_8);
		final RandomAccessFile raf = new RandomAccessFile(f, "r");
		try
		{
			final FileTailTracker tracker = new FileTailTracker(f.getName(), raf);
			raf.seek(0);

			assertEquals("a", tracker.readLine(StandardCharsets.UTF_8));
			assertEquals("b", tracker.readLine(StandardCharsets.UTF_8));
			assertEquals("c", tracker.readLine(StandardCharsets.UTF_8));
			assertEquals("a final line with no terminator still counts", "d",
					tracker.readLine(StandardCharsets.UTF_8));
			assertEquals(null, tracker.readLine(StandardCharsets.UTF_8));
		}
		finally
		{
			raf.close();
		}
	}





	@Test
	public void theCharsetIsConfigurableAndLatin1RemainsReachable()
	{
		useConfig("ctrail-charset-latin1.xml");
		assertEquals(StandardCharsets.ISO_8859_1, CtrailProps.getInstance().getCharset());
	}





	@Test
	public void theDefaultIsUtf8()
	{
		useConfig("ctrail-liveness-disabled.xml");
		assertEquals(StandardCharsets.UTF_8, CtrailProps.getInstance().getCharset());
	}





	/**
	 * An unusable charset name must not stop ctrail starting - the rest of the
	 * config degrades the same way.
	 */
	@Test
	public void anUnusableCharsetFallsBackToUtf8()
	{
		useConfig("ctrail-charset-bogus.xml");
		assertEquals(StandardCharsets.UTF_8, CtrailProps.getInstance().getCharset());
	}
}
