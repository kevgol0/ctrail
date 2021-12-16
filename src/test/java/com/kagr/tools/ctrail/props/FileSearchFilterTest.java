/****************************************************************************
 * FILE: FileSearchFilterTest.java
 * DSCRPT: 
 ****************************************************************************/

package com.kagr.tools.ctrail.props;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FileSearchFilterTest
{

    @Test
    public void test()
    {
        FileSearchFilter fsf = new FileSearchFilter("test.log");
        assertTrue(fsf.doesMatchFilename("/tmp/aa/bb/cc/dd/test.log"));
        assertFalse(fsf.doesMatchFilename("/tmp/aa/bb/cc/test.log/tt"));
    }

}
