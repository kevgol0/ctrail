/****************************************************************************
 * FILE: DurationFormatterTest.java
 * DSCRPT:
 ****************************************************************************/





package com.kagr.tools.ctrail.unit;





import static org.junit.Assert.assertEquals;



import java.util.Locale;

import org.junit.After;
import org.junit.Test;





public class DurationFormatterTest
{
    private final Locale _originalDefault = Locale.getDefault();


    @After
    public void restoreDefaultLocale()
    {
        Locale.setDefault(_originalDefault);
    }


    /**
     * The banner and the idle notices must read the same on every machine. Before
     * Locale.ROOT was passed, a JVM defaulting to Devanagari rendered "4m 12s" as
     * "४m १२s" - and this very test class failed on such a machine.
     */
    @Test
    public void digitsAreAsciiRegardlessOfDefaultLocale()
    {
        for (final String tag : new String[] { "hi-IN-u-nu-deva", "ar-EG", "th-TH-u-nu-thai", "bn-IN" })
        {
            Locale.setDefault(Locale.forLanguageTag(tag));
            assertEquals("seconds under " + tag, "45s", DurationFormatter.format(45 * _second));
            assertEquals("minutes under " + tag, "4m 12s", DurationFormatter.format((4 * _minute) + (12 * _second)));
            assertEquals("hours under " + tag, "2h 09m", DurationFormatter.format((2 * _hour) + (9 * _minute)));
            assertEquals("days under " + tag, "3d 04h", DurationFormatter.format((3 * _day) + (4 * _hour)));
        }
    }


    private static final long _second = 1000L;
    private static final long _minute = 60L * _second;
    private static final long _hour = 60L * _minute;
    private static final long _day = 24L * _hour;





    @Test
    public void zeroAndNegativeRenderAsZeroSeconds()
    {
        assertEquals("0s", DurationFormatter.format(0));
        assertEquals("0s", DurationFormatter.format(-5000));
    }





    @Test
    public void subMinuteRendersSecondsOnly()
    {
        assertEquals("0s", DurationFormatter.format(999));
        assertEquals("1s", DurationFormatter.format(_second));
        assertEquals("45s", DurationFormatter.format(45 * _second));
        assertEquals("59s", DurationFormatter.format(59 * _second));
    }





    @Test
    public void minutesArePaddedToTwoDigitSeconds()
    {
        assertEquals("1m 00s", DurationFormatter.format(_minute));
        assertEquals("4m 12s", DurationFormatter.format((4 * _minute) + (12 * _second)));
        assertEquals("59m 59s", DurationFormatter.format((59 * _minute) + (59 * _second)));
    }





    @Test
    public void hoursArePaddedToTwoDigitMinutes()
    {
        assertEquals("1h 00m", DurationFormatter.format(_hour));
        assertEquals("2h 09m", DurationFormatter.format((2 * _hour) + (9 * _minute)));
        assertEquals("23h 59m", DurationFormatter.format((23 * _hour) + (59 * _minute)));
    }





    @Test
    public void daysArePaddedToTwoDigitHours()
    {
        assertEquals("1d 00h", DurationFormatter.format(_day));
        assertEquals("3d 04h", DurationFormatter.format((3 * _day) + (4 * _hour)));
    }
}
