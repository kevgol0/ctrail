/****************************************************************************
 * FILE: ShutdownWaitTest.java
 * DSCRPT: CTRAIL-1 - the main thread's wait for shutdown must not lose a
 *         request made before it waited, nor end on a spurious wakeup.
 ****************************************************************************/





package com.kagr.tools.ctrail;





import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;



import org.junit.After;
import org.junit.Before;
import org.junit.Test;



import com.kagr.tools.ctrail.props.CtrailProps;





public class ShutdownWaitTest
{
	private CtrailEntryPoint _entryPoint;





	@Before
	public void setUp()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, "./etc/ctrail.xml");
		final String log = Paths.get(".", "src", "test", "resources", "sources", "test.log").toString();
		_entryPoint = new CtrailEntryPoint(new String[] { log });
	}





	@After
	public void tearDown()
	{
		_entryPoint.shutdown();
	}





	/**
	 * The race itself, made deterministic: the reader hits EOF and asks for
	 * shutdown before main reaches wait(). The old notifyAll() found no waiter,
	 * so this blocked forever; the timeout turns that hang into a failure.
	 */
	@Test(timeout = 5000)
	public void shutdownRequestedBeforeWaitIsNotLost()
	{
		_entryPoint.initiateShutdown();

		_entryPoint.awaitShutdownInstruction();

		assertTrue(_entryPoint.isShutdownRequested());
	}





	/**
	 * A notify with no request behind it - what a spurious wakeup looks like
	 * to the waiter - must not end the wait and tear down a live tail.
	 */
	@Test(timeout = 5000)
	public void wakeupWithoutRequestKeepsWaiting() throws Exception
	{
		// start an indefinite wait on another thread
		final CountDownLatch returned = new CountDownLatch(1);
		final Thread waiter = new Thread(() -> {
			_entryPoint.awaitShutdownInstruction();
			returned.countDown();
		});
		waiter.start();
		waitUntilWaiting(waiter);


		// wake it with no request: it must go back to waiting
		final Object holder = runtimeHolder();
		synchronized (holder)
		{
			holder.notifyAll();
		}
		assertFalse("woke without a shutdown request", returned.await(300, TimeUnit.MILLISECONDS));


		// a real request still ends it
		_entryPoint.initiateShutdown();
		assertTrue("did not wake on a real request", returned.await(2, TimeUnit.SECONDS));
	}





	/**
	 * The timed variant keeps its contract: it returns once the timeout passes,
	 * even with no request.
	 */
	@Test(timeout = 5000)
	public void timedWaitReturnsAfterTimeoutWithoutRequest()
	{
		final long start = System.currentTimeMillis();

		_entryPoint.awaitShutdownInstruction(200);

		assertTrue(System.currentTimeMillis() - start >= 200);
		assertFalse(_entryPoint.isShutdownRequested());
	}





	private void waitUntilWaiting(final Thread thread_) throws InterruptedException
	{
		while (thread_.getState() != Thread.State.WAITING)
		{
			Thread.sleep(5);
		}
	}





	private Object runtimeHolder() throws ReflectiveOperationException
	{
		final Field field = CtrailEntryPoint.class.getDeclaredField("_runtimeHolder");
		field.setAccessible(true);
		return field.get(_entryPoint);
	}
}
