/****************************************************************************
 * FILE: OutputWriterThreadTest.java
 * DSCRPT: regression coverage for writer shutdown and backlog draining
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;



import org.junit.Before;
import org.junit.Test;



import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.unit.LogLine;





public class OutputWriterThreadTest
{
	private BlockingDeque<LogLine>	_output;
	private ByteArrayOutputStream	_bytes;
	private PrintStream				_sout;





	@Before
	public void setUp() throws UnsupportedEncodingException
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-case-sensitive.xml").toString());
		CtrailProps.getInstance();

		_output = new LinkedBlockingDeque<>();
		_bytes = new ByteArrayOutputStream();
		_sout = new PrintStream(_bytes, true, StandardCharsets.UTF_8.name());
	}





	private String written()
	{
		_sout.flush();
		return new String(_bytes.toByteArray(), StandardCharsets.UTF_8);
	}





	/**
	 * shutdown() flips the flag without interrupting. The loop used to park in
	 * take(), so this non-daemon thread never noticed and the JVM hung.
	 */
	@Test
	public void testShutdownWithoutInterruptTerminates() throws Exception
	{
		final OutputWriterThread writer = new OutputWriterThread(_output, _sout);
		writer.setDaemon(true);
		writer.start();

		// let it settle into the poll loop with an empty queue
		Thread.sleep(200);

		writer.setShouldContinue(false, false);
		writer.join(3000);

		assertFalse("writer must exit when shouldContinue is cleared without an interrupt", writer.isAlive());
	}





	/**
	 * The backlog drain sized itself at size()-1, so the final queued line was
	 * never printed. With a single pending line nothing drained at all.
	 */
	@Test
	public void testDrainEmitsEveryPendingLine()
	{
		final OutputWriterThread writer = new OutputWriterThread(_output, _sout);
		writer.setShouldContinue(false, false);

		_output.add(new LogLine(null, "line-one", null));
		_output.add(new LogLine(null, "line-two", null));
		_output.add(new LogLine(null, "line-three", null));

		// shouldContinue is already false, so run() goes straight to the drain
		writer.run();

		final String out = written();
		assertTrue("line-one missing", out.contains("line-one"));
		assertTrue("line-two missing", out.contains("line-two"));
		assertTrue("last queued line must not be dropped", out.contains("line-three"));
		assertEquals("queue should be fully drained", 0, _output.size());
	}





	@Test
	public void testDrainOfSingleLine()
	{
		final OutputWriterThread writer = new OutputWriterThread(_output, _sout);
		writer.setShouldContinue(false, false);

		_output.add(new LogLine(null, "only-line", null));
		writer.run();

		assertTrue("a lone pending line must still be printed", written().contains("only-line"));
	}





	/**
	 * pollLast() drained the backlog in reverse, printing it out of order.
	 */
	@Test
	public void testDrainPreservesOrder()
	{
		final OutputWriterThread writer = new OutputWriterThread(_output, _sout);
		writer.setShouldContinue(false, false);

		_output.add(new LogLine(null, "aaa-first", null));
		_output.add(new LogLine(null, "bbb-second", null));
		_output.add(new LogLine(null, "ccc-third", null));

		writer.run();

		final String out = written();
		assertTrue(out.indexOf("aaa-first") < out.indexOf("bbb-second"));
		assertTrue(out.indexOf("bbb-second") < out.indexOf("ccc-third"));
	}





	@Test
	public void testNormalRunLoopWritesLines() throws Exception
	{
		final OutputWriterThread writer = new OutputWriterThread(_output, _sout);
		writer.setDaemon(true);
		writer.start();

		_output.put(new LogLine(null, "streamed-line", null));
		Thread.sleep(300);

		writer.setShouldContinue(false, false);
		writer.join(3000);

		assertTrue(written().contains("streamed-line"));
	}

}
