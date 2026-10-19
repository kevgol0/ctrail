/****************************************************************************
 * FILE: CtrailUsageException.java
 * DSCRPT: the command line could not be used: an unknown option, a missing
 *         value, or a -q pattern that is not a valid regex. main() prints
 *         the message to stderr and exits 2.
 ****************************************************************************/





package com.kagr.tools.ctrail;





public class CtrailUsageException extends RuntimeException
{
	private static final long serialVersionUID = 1L;





	/**
	 * @param message_ what was wrong with the arguments, shown to the user as-is
	 */
	public CtrailUsageException(final String message_)
	{
		super(message_);
	}
}
