/****************************************************************************
 * FILE: CtrailPropsTest.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.unit;





import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;

import org.junit.Test;

import com.kagr.tools.ctrail.ConsoleColors;
import com.kagr.tools.ctrail.StdCtrTest;
import com.kagr.tools.ctrail.props.CtrailProps;
import com.kagr.tools.ctrail.unit.LineFormatter;
import com.kagr.tools.ctrail.unit.LogLine;





public class CaseSensitiveTest extends StdCtrTest
{
    @Test
    public void testNullLogLine()
    {
        System.setProperty(CtrailProps.CTRAIL_CFG_KEY, Paths.get(".", "src", "test", "resources", "configs", "ctrail-case-sensitive.xml").toString());
        CtrailProps.getInstance();

        LineFormatter formatter = new LineFormatter();
        LogLine line = null;
        String resultStr = formatter.format(line);
        String expected = "";
        assertEquals("expected empty line", expected, resultStr);
    }





    @Test
    public void testEmptyLine()
    {
        System.setProperty(CtrailProps.CTRAIL_CFG_KEY, Paths.get(".", "src", "test", "resources", "configs", "ctrail-case-sensitive.xml").toString());
        CtrailProps.getInstance();

        LineFormatter formatter = new LineFormatter();
        String str = "";
        String filename = "test-file.log";
        LogLine line = new LogLine(filename, str, null);

        String resultStr = formatter.format(line);
        String expected = "";
        assertEquals("expected empty line", expected, resultStr);
    }





    @Test
    public void testWithCaseSensitiveColor()
    {
        System.setProperty(CtrailProps.CTRAIL_CFG_KEY, Paths.get(".", "src", "test", "resources", "configs", "ctrail-case-sensitive.xml").toString());
        CtrailProps.getInstance();

        LineFormatter formatter = new LineFormatter();
        String str = "(INF) a test sensitive LINE";
        String filename = "test-file.log";
        LogLine line = new LogLine(filename, str, null);

        String resultStr = formatter.format(line);
        String expected = ConsoleColors.BLUE_UNDERLINED + filename + ":" +
                ConsoleColors.YELLOW + str + ConsoleColors.RESET;
        assertEquals(expected, resultStr);
    }





    @Test
    public void testWithCaseSensitiveMissColor()
    {
        System.setProperty(CtrailProps.CTRAIL_CFG_KEY, Paths.get(".", "src", "test", "resources", "configs", "ctrail-case-sensitive.xml").toString());
        CtrailProps props = CtrailProps.getInstance();

        LineFormatter formatter = new LineFormatter();
        String str = "(inf) a test sensitive LINE";
        String filename = "test-file.log";
        LogLine line = new LogLine(filename, str, null);

        String resultStr = formatter.format(line);
        String expected = props.getDefaultFgColor() + filename + ":" +
                ConsoleColors.WHITE + str + ConsoleColors.RESET;
        assertEquals(expected, resultStr);
    }





    @Test
    public void testWithCaseSensitiveNoColor()
    {
        System.setProperty(CtrailProps.CTRAIL_CFG_KEY, Paths.get(".", "src", "test", "resources", "configs", "ctrail-case-sensitive.xml").toString());
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
