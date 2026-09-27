/****************************************************************************
 * FILE: LogLine.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.unit;





import com.kagr.tools.ctrail.props.FileSearchFilter;



import lombok.Data;





@Data
public class LogLine
{
    private String _origFilename;

    private String _line;

    FileSearchFilter _fileSearchFilters;

    /** true for ctrail's own liveness messages, which bypass keyword coloring */
    private boolean _notice;





    /**
     * Builds an ordinary tailed line.
     *
     * @param origFileName_ the file the line came from, or null to suppress the prefix
     * @param line_         the line text
     * @param fst_          the search filter that was in force, may be null
     */
    public LogLine(final String origFileName_, final String line_, final FileSearchFilter fst_)
    {
        this(origFileName_, line_, fst_, false);
    }





    /**
     * Builds a line, optionally flagged as one of ctrail's own notices.
     *
     * @param origFileName_ the file the line came from, or null to suppress the prefix
     * @param line_         the line text
     * @param fst_          the search filter that was in force, may be null
     * @param notice_       true when this is a ctrail liveness message, not tailed content
     */
    public LogLine(final String origFileName_, final String line_, final FileSearchFilter fst_, final boolean notice_)
    {
        setOrigFilename(origFileName_);
        setLine(line_);
        setFileSearchFilters(fst_);
        setNotice(notice_);
    }

}
