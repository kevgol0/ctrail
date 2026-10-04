/****************************************************************************
 * FILE: TailUnit.java
 * DSCRPT: what <tailLast><count> counts - lines, like tail -n, or bytes,
 *         like tail -c.
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import org.apache.commons.lang3.StringUtils;





public enum TailUnit
{
	LINES,
	BYTES;





	/**
	 * Resolves a config value, ignoring case and surrounding whitespace.
	 *
	 * @param value_ the raw &lt;unit&gt; text, may be null
	 * @return the matching unit, or null when the value is blank or unrecognised
	 */
	public static TailUnit fromConfigValue(final String value_)
	{
		final String trimmed = StringUtils.trimToEmpty(value_);
		for (final TailUnit unit : values())
		{
			if (StringUtils.equalsIgnoreCase(unit.name(), trimmed))
			{
				return unit;
			}
		}
		return null;
	}





	/**
	 * @return the config spelling, e.g. "lines"
	 */
	public String configName()
	{
		return StringUtils.lowerCase(name());
	}
}
