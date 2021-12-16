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





public class IgnoreCaseTest extends StdCtrTest
{
    @Test
    public void testWithNoCaseSensitiveColor()
    {
        System.setProperty(CtrailProps.CTRAIL_CFG_KEY, Paths.get(".", "src", "test", "resources", "configs", "ctrail-case-ignore.xml").toString());
        CtrailProps.getInstance();

        LineFormatter formatter = new LineFormatter();
        String str = "(inf) a test sensitive LINE";
        String filename = "test-file.log";
        LogLine line = new LogLine(filename, str, null);

        String resultStr = formatter.format(line);
        String expected = ConsoleColors.BLUE_UNDERLINED + filename + ":" +
                ConsoleColors.YELLOW + str + ConsoleColors.RESET;
        assertEquals(expected, resultStr);
    }





    @Test
    public void testWithNoCaseSensitiveMissColor()
    {
        System.setProperty(CtrailProps.CTRAIL_CFG_KEY, Paths.get(".", "src", "test", "resources", "configs", "ctrail-case-sensitive.xml").toString());
        CtrailProps props = CtrailProps.getInstance();

        LineFormatter formatter = new LineFormatter();
        String str = "(inf) a test sensitive LINE";
        String filename = "test-file.log";
        LogLine line = new LogLine(filename, str, null);

        String resultStr = formatter.format(line);
        String expected = ConsoleColors.BLUE_UNDERLINED + filename + ":" +
                ConsoleColors.YELLOW + str + ConsoleColors.RESET;
        assertEquals(expected, resultStr);
    }





    @Test
    public void testWithNoCaseSensitiveNoColor()
    {
        System.setProperty(CtrailProps.CTRAIL_CFG_KEY, Paths.get(".", "src", "test", "resources", "configs", "ctrail-case-ignore.xml").toString());
        CtrailProps props = CtrailProps.getInstance();

        LineFormatter formatter = new LineFormatter();
        String str = "a test sensitive LINE";
        String filename = "test-file.log";
        LogLine line = new LogLine("test-file.log", str, null);

        String resultStr = formatter.format(line);
        String expected = props.getDefaultFgColor() + filename + ":" +
                ConsoleColors.WHITE + str + ConsoleColors.RESET;
        assertEquals(expected, resultStr);
    }
}
