/****************************************************************************
 * FILE: StdinReaderThreadTest.java
 * DSCRPT: regression coverage for the std-in read loop
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;



import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;



import org.junit.Before;
import org.junit.Test;



import com.kagr.tools.ctrail.IShutdownManager;
import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.FileSearchFilter;
import com.kagr.tools.ctrail.unit.LogLine;





public class StdinReaderThreadTest
{
	private BlockingDeque<LogLine>	_output;
	private boolean					_shutdownCalled;



	private final IShutdownManager _mgr = new IShutdownManager()
	{
		@Override
		public void initiateShutdown()
		{
			_shutdownCalled = true;
		}
	};





	@Before
	public void setUp()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-file-search-filter.xml").toString());
		CtrailProps.getInstance();
		_output = new LinkedBlockingDeque<>();
		_shutdownCalled = false;
	}





	private List<String> drain()
	{
		final List<String> lines = new ArrayList<>();
		LogLine ll;
		while ((ll = _output.poll()) != null)
		{
			lines.add(ll.getLine());
		}
		return lines;
	}





	private InputStream stream(final String text_)
	{
		return new ByteArrayInputStream(text_.getBytes(StandardCharsets.UTF_8));
	}





	/**
	 * The read loop used to branch filter-or-match and read nothing at all when
	 * neither was supplied, so "cat file | ctr" emitted no output.
	 */
	@Test
	public void testNoMatchAndNoFilterEmitsEveryLine()
	{
		final StdinReaderThread reader = new StdinReaderThread(stream("alpha\nbravo\ncharlie\n"),
				_output, null, _mgr, null);
		reader.run();

		assertEquals(3, _output.size());
		final List<String> lines = drain();
		assertEquals("alpha", lines.get(0));
		assertEquals("bravo", lines.get(1));
		assertEquals("charlie", lines.get(2));
		assertTrue("shutdown must be signalled", _shutdownCalled);
	}





	@Test
	public void testMatchFiltersLines()
	{
		final StdinReaderThread reader = new StdinReaderThread(stream("alpha\nbravo\nalphabet\n"),
				_output, "alpha", _mgr, null);
		reader.run();

		final List<String> lines = drain();
		assertEquals(2, lines.size());
		assertEquals("alpha", lines.get(0));
		assertEquals("alphabet", lines.get(1));
	}





	/**
	 * -m used String.contains() directly, ignoring useCaseSensitiveSarch=false.
	 */
	@Test
	public void testMatchHonorsCaseInsensitiveConfig()
	{
		final StdinReaderThread reader = new StdinReaderThread(stream("ALPHA line\nbravo line\n"),
				_output, "alpha", _mgr, null);
		reader.run();

		final List<String> lines = drain();
		assertEquals(1, lines.size());
		assertEquals("ALPHA line", lines.get(0));
	}





	/**
	 * When a std-in filter was configured, -m was skipped entirely.
	 */
	@Test
	public void testMatchIsAppliedAlongsideFilter()
	{
		final FileSearchFilter filter = new FileSearchFilter("stdin", true);
		filter.getExcldueTerms().add("noisy");

		final StdinReaderThread reader = new StdinReaderThread(stream("keep alpha\nkeep bravo\nnoisy alpha\n"),
				_output, "alpha", _mgr, filter);
		reader.run();

		final List<String> lines = drain();
		assertEquals(1, lines.size());
		assertEquals("keep alpha", lines.get(0));
	}





	@Test
	public void testFilterIncludeAndExclude()
	{
		final FileSearchFilter filter = new FileSearchFilter("stdin", false);
		filter.getIncludeTerms().add("keep");
		filter.getExcldueTerms().add("drop");

		final StdinReaderThread reader = new StdinReaderThread(stream("keep me\ndrop me\nkeep and drop\nneither\n"),
				_output, null, _mgr, filter);
		reader.run();

		final List<String> lines = drain();
		assertEquals(1, lines.size());
		assertEquals("keep me", lines.get(0));
	}





	/**
	 * The source name was hardcoded, so piped input carried a "stdin:" prefix
	 * even with prependFilenameToLine=false. The file reader always honored it.
	 */
	@Test
	public void testPrependFilenameToLineIsHonored()
	{
		final CtrailProps props = CtrailProps.getInstance();

		props.setPrependFilenameToLine(true);
		new StdinReaderThread(stream("alpha\n"), _output, null, _mgr, null).run();
		assertEquals(CtrailProps.STDIN_FILTER_NAME, _output.poll().getOrigFilename());

		props.setPrependFilenameToLine(false);
		new StdinReaderThread(stream("bravo\n"), _output, null, _mgr, null).run();
		assertNull("no source prefix when prependFilenameToLine is false", _output.poll().getOrigFilename());

		props.setPrependFilenameToLine(true);
	}





	/**
	 * End-to-end: a <stdinfilter> declared in the config reaches the reader via
	 * resolveStdinFilter() and filters the piped stream.
	 */
	@Test
	public void testConfiguredStdinFilterIsApplied()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-stdin-filter.xml").toString());
		final CtrailProps props = CtrailProps.getInstance();

		final StdinReaderThread reader = new StdinReaderThread(
				stream("keepme one\nnoise two\nalsokeep three\ndropme keepme four\n"),
				_output, null, _mgr, props.resolveStdinFilter());
		reader.run();

		final List<String> lines = drain();
		assertEquals(2, lines.size());
		assertEquals("keepme one", lines.get(0));
		assertEquals("alsokeep three", lines.get(1));
	}





	/**
	 * With filtering switched off, resolveStdinFilter() hands back nothing and
	 * every line is shown.
	 */
	@Test
	public void testFilteringDisabledShowsEveryLine()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-stdin-filter.xml").toString());
		final CtrailProps props = CtrailProps.getInstance();
		props.setEnabledFileFiltering(false);

		try
		{
			final StdinReaderThread reader = new StdinReaderThread(stream("keepme one\nnoise two\n"),
					_output, null, _mgr, props.resolveStdinFilter());
			reader.run();

			assertEquals("-f false must show everything", 2, drain().size());
		}
		finally
		{
			props.setEnabledFileFiltering(true);
		}
	}

}
