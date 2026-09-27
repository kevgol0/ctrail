/****************************************************************************
 * FILE: NoticeFormattingTest.java
 * DSCRPT: ctrail's own messages must not be dressed up as log content.
 ****************************************************************************/





package com.kagr.tools.ctrail.unit;





import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;



import java.nio.file.Paths;



import org.junit.Before;
import org.junit.Test;



import com.kagr.tools.ctrail.ConsoleColors;
import com.kagr.tools.ctrail.props.CtrailProps;





public class NoticeFormattingTest
{
    private LineFormatter _formatter;





    @Before
    public void setUp()
    {
        //
        // this fixture colors the keyword "error" red, which is what makes the
        // "a notice is not log content" assertions below meaningful. Its notice
        // color is purple, deliberately not the compiled default
        //
        System.setProperty(CtrailProps.CTRAIL_CFG_KEY,
                Paths.get(".", "src", "test", "resources", "configs", "ctrail-liveness.xml").toString());
        CtrailProps.getInstance();
        _formatter = new LineFormatter();
    }





    @Test
    public void noticesAreRenderedInTheNoticeColor()
    {
        final String out = _formatter.format(new LogLine(null, "ctrail: app.log - no movement in 30s", null, true));

        assertTrue(out.startsWith(ConsoleColors.PURPLE));
        assertTrue(out.endsWith(ConsoleColors.RESET));
    }





    @Test
    public void aNoticeMentioningAKeywordDoesNotPickUpItsColor()
    {
        //
        // "error.log - no movement" must not come out looking like an error line
        //
        final String out = _formatter.format(new LogLine(null, "ctrail: error.log - no movement in 30s", null, true));

        assertTrue(out.startsWith(ConsoleColors.PURPLE));
        assertFalse(out.contains(ConsoleColors.RED));
    }





    @Test
    public void aNoticeCarriesNoFilenamePrefix()
    {
        //
        // the filename is already inside the message; prefixing would double it
        //
        final String out = _formatter.format(new LogLine("app.log", "ctrail: app.log - resumed after 4m 12s", null, true));

        assertFalse(out.startsWith(ConsoleColors.PURPLE + "app.log:"));
        assertTrue(out.startsWith(ConsoleColors.PURPLE + "ctrail:"));
    }





    @Test
    public void ordinaryLinesStillPickUpKeywordColoring()
    {
        final String out = _formatter.format(new LogLine(null, "something error happened", null));

        assertTrue(out.contains(ConsoleColors.RED));
        assertFalse(out.startsWith(ConsoleColors.PURPLE));
    }
}
