/****************************************************************************
 * FILE: CtrailPropsTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail;





import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;



import org.junit.Assert;
import org.junit.Test;

import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.unit.LineFormatter;
import com.kagr.tools.ctrail.unit.LogLine;





public class LastWordMatchTest extends StdCtrTest
{
    @Test
    public void testWithLastWordMatch()
    {
        System.setProperty(CtrailProps.CTRAIL_CFG_KEY, Paths.get(".", "src", "test", "resources", "configs", "ctrail-last-word-match.xml").toString());
        CtrailProps props = CtrailProps.getInstance();

        LineFormatter formatter = new LineFormatter();
        String str = "(inf) a test (dbg) sensitive line (err)";
        String filename = "test-file.log";
        LogLine line = new LogLine(filename, str, null);

        String resultStr = formatter.format(line);
        String expected = ConsoleColors.BLUE_UNDERLINED + filename + ":" +
                ConsoleColors.RED + str + ConsoleColors.RESET;

        assertEquals(expected, resultStr);

    }
}
