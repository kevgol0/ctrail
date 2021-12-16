/****************************************************************************
 * FILE: MaxFilesTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Ignore;
import org.junit.Test;

import com.kagr.tools.ctrail.props.CtrailProps;





public class MaxFilesTest
{
    @Test
    public void test()
    {
        System.setProperty(CtrailProps.CTRAIL_CFG_KEY, "./etc/ctrail.xml");
        CtrailProps.getInstance();
        final CtrailProps props = CtrailProps.getInstance();
        props.setMaxNbrInputFiles(1);
        final Path p = Paths.get(".", "src", "test", "resources", "sources", "test.log");
        final Path p2 = Paths.get(".", "src", "test", "resources", "sources", "test2.log");
        final String args[] = new String[]
        {
          p.toString(),
          p2.toString()
        };

        final CtrailEntryPoint ep = new CtrailEntryPoint(args);
        assertEquals(1, ep.getFileTrackers().size(), 0);
    }

}
