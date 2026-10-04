/****************************************************************************
 * FILE: StdinFallbackTest.java
 * DSCRPT: CTRAIL-3 - stdin is tailed only when no file was named. Naming
 *         files that cannot be read is an error, not a quiet stdin tail.
 ****************************************************************************/





package com.kagr.tools.ctrail;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;



import java.io.File;
import java.nio.file.Paths;



import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;



import com.kagr.tools.ctrail.props.CtrailProps;





public class StdinFallbackTest
{
	@Rule public TemporaryFolder _tmp = new TemporaryFolder();





	@Before
	public void setUp()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, "./etc/ctrail.xml");
		CtrailProps.getInstance().setMaxNbrInputFiles(10);
	}





	@Test
	public void missingFileIsAnErrorNotAStdinTail()
	{
		final String missing = new File(_tmp.getRoot(), "definitely-does-not-exist.log").getPath();

		final NoReadableInputException ex = expectNoReadableInput(new String[] { missing });

		assertTrue(ex.getMessage(), ex.getMessage().contains(missing));
	}





	@Test
	public void directoryArgumentIsAnErrorNotAStdinTail()
	{
		expectNoReadableInput(new String[] { _tmp.getRoot().getPath() });
	}





	/**
	 * One good file among bad ones still tails the good one; only "none
	 * readable" is fatal.
	 */
	@Test
	public void readableFileAmongUnreadableIsStillTailed()
	{
		final String good = Paths.get(".", "src", "test", "resources", "sources", "test.log").toString();
		final String missing = new File(_tmp.getRoot(), "missing.log").getPath();

		final CtrailEntryPoint ep = new CtrailEntryPoint(new String[] { missing, good });

		assertEquals(1, ep.getFileTrackers().size());
		ep.shutdown();
	}





	/**
	 * No file arguments at all is the one case that still reads stdin.
	 */
	@Test
	public void noArgumentsStillTailsStdin()
	{
		final CtrailEntryPoint ep = new CtrailEntryPoint(new String[0]);

		assertTrue(ep.getFileTrackers().isEmpty());
		ep.shutdown();
	}





	private NoReadableInputException expectNoReadableInput(final String[] args_)
	{
		try
		{
			new CtrailEntryPoint(args_);
		}
		catch (final NoReadableInputException ex_)
		{
			return ex_;
		}
		throw new AssertionError("expected NoReadableInputException, ctrail fell back to stdin");
	}
}
