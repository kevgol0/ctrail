/****************************************************************************
 * FILE: DurationFormatter.java
 * DSCRPT: renders an elapsed-millis value as a short, human readable age,
 *         shared by the startup banner and the idle/resumed notices.
 ****************************************************************************/





package com.kagr.tools.ctrail.unit;





import java.util.Locale;





public final class DurationFormatter
{
    private static final long _millisPerSecond = 1000L;
    private static final long _secondsPerMinute = 60L;
    private static final long _minutesPerHour = 60L;
    private static final long _hoursPerDay = 24L;





    private DurationFormatter()
    {
        // static helper, never instantiated
    }





    /**
     * Renders an elapsed duration as a short age string: {@code 45s}, {@code 4m 12s},
     * {@code 2h 09m} or {@code 3d 04h}. Values at or below zero render as {@code 0s}.
     *
     * Always renders ASCII digits: the default locale is not consulted, so a JVM
     * defaulting to Devanagari or Arabic-Indic numerals still produces "4m 12s".
     *
     * @param millis_ the elapsed time in milliseconds
     * @return the formatted age, never null
     */
    public static String format(final long millis_)
    {
        if (millis_ <= 0)
        {
            return "0s";
        }


        //
        // break the duration down into whole units; each branch below shows the
        // two most significant units, which is as much precision as a liveness
        // message ever needs
        //
        final long totalSeconds = millis_ / _millisPerSecond;
        final long totalMinutes = totalSeconds / _secondsPerMinute;
        final long totalHours = totalMinutes / _minutesPerHour;
        final long totalDays = totalHours / _hoursPerDay;

        if (totalSeconds < _secondsPerMinute)
        {
            return totalSeconds + "s";
        }

        if (totalMinutes < _minutesPerHour)
        {
            return String.format(Locale.ROOT, "%dm %02ds", totalMinutes, totalSeconds % _secondsPerMinute);
        }

        if (totalHours < _hoursPerDay)
        {
            return String.format(Locale.ROOT, "%dh %02dm", totalHours, totalMinutes % _minutesPerHour);
        }

        return String.format(Locale.ROOT, "%dd %02dh", totalDays, totalHours % _hoursPerDay);
    }
}
