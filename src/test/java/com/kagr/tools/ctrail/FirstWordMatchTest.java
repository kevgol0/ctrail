/****************************************************************************
 * FILE: CtrailPropsTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.nio.file.Path;
import java.nio.file.Paths;



import org.junit.Assert;
import org.junit.Test;





public class FirstWordMatchTest extends StdCtrTest
{
    @Test
    public void testWithLastWordMatch()
    {
        System.setProperty("CTRAIL_CFG", Paths.get(".", "src", "test", "resources", "configs", "ctrail-first-word-match.xml").toString());
        final Path p = Paths.get(".", "src", "test", "resources", "sources", "test.log");
        final String args[] = new String[]
        {
          p.toString(),
        };

        replaceStdOut();
        final CtrailEntryPoint ep = new CtrailEntryPoint(args);
        ep.start(10);
        resetStdOut();
        Assert.assertTrue(compareFiles(Paths.get(".", "src/test/resources/expected/first-word-match.log")));
    }
}
