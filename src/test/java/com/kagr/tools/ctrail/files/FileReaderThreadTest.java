/****************************************************************************
 * FILE: FileReaderThreadTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.files;





import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;



import java.nio.file.Paths;
import java.util.Deque;
import java.util.Hashtable;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;



import org.junit.Test;



import com.kagr.tools.ctrail.IShutdownManager;
import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.props.FileSearchFilter;
import com.kagr.tools.ctrail.unit.LogLine;





public class FileReaderThreadTest
{

	@Test
	public void test()
	{
		System.setProperty(CtrailProps.CTRAIL_CFG_KEY, Paths.get(".", "src", "test", "resources", "configs", "ctrail-file-regex.xml").toString());
		CtrailProps props = CtrailProps.getInstance();
		Hashtable<String, FileSearchFilter> filters = props.getFileSearchFilters();
		String filterName = filters.keySet().iterator().next();
		assertNotNull(filterName);
		FileSearchFilter filter = filters.get(filterName);
		assertNotNull(filter);

		BlockingDeque<FileTailTracker> files = new LinkedBlockingDeque<>();
		Deque<LogLine> strout = new LinkedBlockingDeque<>();
		IShutdownManager mgr = new IShutdownManager()
		{
			@Override
			public void initiateShutdown()
			{
			}
		};


		FileSearchFilter fsf = filters.get("apiws-inst01A\\.log\\.0$");
		String line = "hello world, Timeout";
		assertFalse(fsf.shouldExcludeLineDueToSeachTerms(line));
		line = "hello world, received";
		assertTrue(fsf.shouldExcludeLineDueToSeachTerms(line));
	}


}
