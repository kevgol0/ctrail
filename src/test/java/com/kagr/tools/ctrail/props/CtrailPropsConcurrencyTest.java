/****************************************************************************
 * FILE: CtrailPropsConcurrencyTest.java
 * DSCRPT: CTRAIL-19 - getInstance() sits on the per-line path of both the
 *         reader and the writer thread, so its common path must not take a
 *         lock.
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;



import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;



import org.junit.Before;
import org.junit.Test;





public class CtrailPropsConcurrencyTest
{
	@Before
	public void setUp()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-liveness-disabled.xml").toString());
		CtrailProps.getInstance();
	}





	/**
	 * The fix is that the common path is lock-free. A behavioural test cannot see
	 * a lock, so the property itself is pinned: if `synchronized` comes back on
	 * getInstance, reader and writer resume contending one class-wide monitor on
	 * every line and this fails.
	 */
	@Test
	public void getInstanceMustNotBeSynchronized() throws Exception
	{
		final Method m = CtrailProps.class.getMethod("getInstance");

		assertFalse("getInstance is on the per-line path of two threads; it must not hold a lock",
				Modifier.isSynchronized(m.getModifiers()));
	}





	/**
	 * Removing the lock must not cost correctness: every caller still gets one
	 * shared instance for a given CTRAIL_CFG.
	 */
	@Test
	public void concurrentCallersAllSeeTheSameInstance() throws Exception
	{
		final int threads = 16;
		final int perThread = 500;
		final List<CtrailProps> seen = new CopyOnWriteArrayList<>();
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch done = new CountDownLatch(threads);

		for (int t = 0; t < threads; t++)
		{
			final Thread worker = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						start.await();
						for (int i = 0; i < perThread; i++)
						{
							seen.add(CtrailProps.getInstance());
						}
					}
					catch (final InterruptedException ex_)
					{
						Thread.currentThread().interrupt();
					}
					finally
					{
						done.countDown();
					}
				}
			});
			worker.setDaemon(true);
			worker.start();
		}

		start.countDown();
		assertTrue("workers must finish", done.await(30, TimeUnit.SECONDS));
		assertEquals(threads * perThread, seen.size());

		final CtrailProps first = seen.get(0);
		assertNotNull(first);
		for (int i = 0; i < seen.size(); i++)
		{
			assertSame("every caller must see one shared instance", first, seen.get(i));
		}
	}





	/**
	 * The rebuild path still works: switching CTRAIL_CFG must produce a different
	 * instance, or a test that swaps config would silently keep the old one.
	 */
	@Test
	public void changingTheOverrideStillRebuilds()
	{
		final CtrailProps before = CtrailProps.getInstance();

		System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
				Paths.get(".", "src", "test", "resources", "configs", "ctrail-charset-latin1.xml").toString());
		final CtrailProps after = CtrailProps.getInstance();

		assertFalse("a changed override must rebuild", before == after);
		assertSame("and then cache again", after, CtrailProps.getInstance());
		assertEquals(Collections.emptyList(), Collections.emptyList());
	}
}
