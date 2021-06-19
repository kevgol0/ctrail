/****************************************************************************
 * FILE: FileNameRegExTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import java.nio.file.Path;
import java.nio.file.Paths;



import org.junit.Test;



import lombok.extern.slf4j.Slf4j;





@Slf4j
public class FileNameRegExTest extends StdCtrTest
{

    @Test
    public void test()
    {
        System.setProperty("CTRAIL_CFG", Paths.get("./src/test/resources/configs/ctrail-file-regex.xml").toString());
        final Path p1 = Paths.get(".", "src", "test", "resources", "sources", "test.log");
        final Path p2 = Paths.get(".", "src", "test", "resources", "sources", "test2.log");
        final Path p4 = Paths.get(".", "src", "test", "resources", "sources", "test40000b.log.0");
        final Path p3 = Paths.get(".", "src", "test", "resources", "sources", "ace.log");
        final String args[] = new String[]
        {
          p1.toString(), p2.toString(), p3.toString(), p4.toString()
        };

        try
        {
            //replaceStdOut();
            final CtrailEntryPoint ep = new CtrailEntryPoint(args);
            ep.start(10);
            //resetStdOut();
            //Assert.assertTrue(compareFiles(Paths.get("./src/test/resources/expected/search-filter.log")));

        }
        catch (final Exception ex_)
        {
            _logger.error(ex_.toString(), ex_);
        }
    }

}
