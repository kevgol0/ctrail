/****************************************************************************
 * FILE: FileTailTrackerPositionTest.java
 * DSCRPT: CTRAIL-8 - where a newly opened file is positioned for each
 *         <tailLast> outcome: N lines, N bytes, 0 (the end), whole file.
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import static org.junit.Assert.assertEquals;



import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;



import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;



import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.TailUnit;





public class FileTailTrackerPositionTest
{
	/** 5 lines of 6 bytes each: 30 bytes */
	private static final String CONTENT = "line1\nline2\nline3\nline4\nline5\n";

	@Rule public TemporaryFolder _tmp = new TemporaryFolder();

	private File _file;

	private FileTailTracker _tracker;





	@Before
	public void setUp() throws IOException
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-taillast-none.xml").toString());
		_file = _tmp.newFile("position.log");
		Files.write(_file.toPath(), CONTENT.getBytes(StandardCharsets.UTF_8));
	}





	@After
	public void tearDown()
	{
		if (_tracker != null)
		{
			_tracker.close();
		}

		// the singleton outlives this class; leave it at the default
		CtrailProps.getInstance().setTailLast(CtrailProps.DEFAULT_TAIL_LAST_COUNT, TailUnit.LINES);
	}





	@Test
	public void lastNLines() throws IOException
	{
		CtrailProps.getInstance().setTailLast(2, TailUnit.LINES);

		assertEquals(18, open());
	}





	@Test
	public void lastNBytes() throws IOException
	{
		CtrailProps.getInstance().setTailLast(7, TailUnit.BYTES);

		assertEquals(23, open());
	}





	@Test
	public void moreBytesThanTheFileStartsAtZero() throws IOException
	{
		CtrailProps.getInstance().setTailLast(1000, TailUnit.BYTES);

		assertEquals(0, open());
	}





	@Test
	public void zeroLinesStartsAtTheEnd() throws IOException
	{
		CtrailProps.getInstance().setTailLast(0, TailUnit.LINES);

		assertEquals(CONTENT.length(), open());
	}





	@Test
	public void zeroBytesStartsAtTheEnd() throws IOException
	{
		CtrailProps.getInstance().setTailLast(0, TailUnit.BYTES);

		assertEquals(CONTENT.length(), open());
	}





	@Test
	public void entireFileStartsAtZero() throws IOException
	{
		CtrailProps.getInstance().setTailLast(2, TailUnit.LINES);
		CtrailProps.getInstance().setReadEntireFile(true);

		assertEquals(0, open());
	}





	/**
	 * @return the position the tracker settled on, checked against the real file pointer
	 */
	private long open() throws IOException
	{
		_tracker = new FileTailTracker("position.log", new RandomAccessFile(_file, "r"));
		assertEquals("file pointer and recorded position must agree",
				_tracker.getLastReadPosition(), _tracker.getFile().getFilePointer());
		return _tracker.getLastReadPosition();
	}
}
