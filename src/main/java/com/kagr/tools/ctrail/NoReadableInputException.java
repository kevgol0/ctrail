/****************************************************************************
 * FILE: NoReadableInputException.java
 * DSCRPT: thrown when file arguments were given but none could be opened.
 *         Falling back to stdin there tailed the wrong thing and said so.
 ****************************************************************************/





package com.kagr.tools.ctrail;





public class NoReadableInputException extends RuntimeException
{
	private static final long serialVersionUID = 1L;





	/**
	 * @param message_ what was asked for and why none of it could be used
	 */
	public NoReadableInputException(final String message_)
	{
		super(message_);
	}
}
