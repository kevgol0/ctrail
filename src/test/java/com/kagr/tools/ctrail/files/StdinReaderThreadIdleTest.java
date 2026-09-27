/****************************************************************************
 * FILE: StdinReaderThreadIdleTest.java
 * DSCRPT: stdin must count only the lines it actually emits as movement, and
 *         must stop being watched once the pipe closes.
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;



import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;



import org.junit.Before;
import org.junit.Test;



import com.kagr.tools.ctrail.IShutdownManager;
import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.FileSearchFilter;
import com.kagr.tools.ctrail.unit.LogLine;





public class StdinReaderThreadIdleTest
{
	/** an epoch-1970 marker, so any real activity timestamp is unmistakably newer */
	private static final long _marker = 1000L;

	private Deque<LogLine> _out;
	private IShutdownManager _noopShutdown;





	@Before
	public void setUp()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-liveness.xml").toString());
		CtrailProps.getInstance();

		_out = new LinkedBlockingDeque<>();
		_noopShutdown = new IShutdownManager()
		{
			@Override
			public void initiateShutdown()
			{
			}
		};
	}





	@Test
	public void anEmittedLineCountsAsMovement()
	{
		final StdinReaderThread reader = readerFor("keep me\n", "keep");
		final ActivityState state = reader.getActivityState();
		state.setLastActivityMillis(_marker);

		reader.run();

		assertEquals(1, _out.size());
		assertTrue("an emitted line must refresh the activity clock", state.getLastActivityMillis() > _marker);
	}





	@Test
	public void aLineDroppedByTheMatchIsNotMovement()
	{
		//
		// output you cannot see is not movement - a filter that drops everything
		// should still let the source go idle
		//
		final StdinReaderThread reader = readerFor("drop this\nand this too\n", "keep");
		final ActivityState state = reader.getActivityState();
		state.setLastActivityMillis(_marker);

		reader.run();

		assertTrue(_out.isEmpty());
		assertEquals(_marker, state.getLastActivityMillis());
	}





	@Test
	public void passthroughLinesCountAsMovement()
	{
		final StdinReaderThread reader = readerFor("one\ntwo\n", null);
		final ActivityState state = reader.getActivityState();
		state.setLastActivityMillis(_marker);

		reader.run();

		assertEquals(2, _out.size());
		assertTrue(state.getLastActivityMillis() > _marker);
	}





	@Test
	public void endOfPipeMarksTheSourceFinished()
	{
		//
		// without this the monitor would keep announcing silence that can never
		// end, long after the producer went away
		//
		final StdinReaderThread reader = readerFor("one\n", null);
		assertTrue(!reader.getActivityState().isFinished());

		reader.run();

		assertTrue(reader.getActivityState().isFinished());
	}





	@Test
	public void stdinIsNamedForItsNotices()
	{
		assertEquals("stdin", readerFor("", null).getActivityState().getName());
	}





	@Test
	public void aFilteredLineThatSurvivesCountsAsMovement()
	{
		//
		// the filtered read loop is a separate code path from the match loop and
		// the passthrough loop; all three have to record movement
		//
		final FileSearchFilter filter = new FileSearchFilter("stdin$", false);
		filter.getIncludeTerms().add("keep");

		final StdinReaderThread reader = filteredReaderFor("keep me\n", filter);
		final ActivityState state = reader.getActivityState();
		state.setLastActivityMillis(_marker);

		reader.run();

		assertEquals(1, _out.size());
		assertTrue(state.getLastActivityMillis() > _marker);
	}





	@Test
	public void aLineDroppedByTheFilterIsNotMovement()
	{
		final FileSearchFilter filter = new FileSearchFilter("stdin$", false);
		filter.getIncludeTerms().add("keep");

		final StdinReaderThread reader = filteredReaderFor("nothing of interest\n", filter);
		final ActivityState state = reader.getActivityState();
		state.setLastActivityMillis(_marker);

		reader.run();

		assertTrue(_out.isEmpty());
		assertEquals(_marker, state.getLastActivityMillis());
	}





	private StdinReaderThread readerFor(final String content_, final String match_)
	{
		return new StdinReaderThread(new ByteArrayInputStream(content_.getBytes(StandardCharsets.UTF_8)),
				_out,
				match_,
				_noopShutdown,
				null);
	}





	private StdinReaderThread filteredReaderFor(final String content_, final FileSearchFilter filter_)
	{
		return new StdinReaderThread(new ByteArrayInputStream(content_.getBytes(StandardCharsets.UTF_8)),
				_out,
				null,
				_noopShutdown,
				filter_);
	}
}
