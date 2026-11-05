/****************************************************************************
 * FILE: LogCapture.java
 * DSCRPT: collects what a class logs at WARN and above, so tests can assert
 *         on warnings instead of trusting them by eye.
 ****************************************************************************/





package com.kagr.tools.ctrail.testutil;





import java.util.ArrayList;
import java.util.List;



import org.apache.commons.lang3.Strings;
import org.slf4j.LoggerFactory;



import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;





public final class LogCapture implements AutoCloseable
{
	private final Logger _logger;

	private final ListAppender<ILoggingEvent> _appender = new ListAppender<>();





	/**
	 * Starts capturing the given class's log events.
	 *
	 * @param source_ the class whose logger to watch
	 */
	public LogCapture(final Class<?> source_)
	{
		_logger = (Logger) LoggerFactory.getLogger(source_);
		_appender.start();
		_logger.addAppender(_appender);
	}





	/**
	 * @param needle_ text the message must contain
	 * @return the WARN-or-worse messages containing it, formatted
	 */
	public List<String> warningsContaining(final String needle_)
	{
		final List<String> found = new ArrayList<>();
		for (final ILoggingEvent event : _appender.list)
		{
			if (event.getLevel().isGreaterOrEqual(Level.WARN) && Strings.CS.contains(event.getFormattedMessage(), needle_))
			{
				found.add(event.getFormattedMessage());
			}
		}
		return found;
	}





	@Override
	public void close()
	{
		_logger.detachAppender(_appender);
		_appender.stop();
	}
}
