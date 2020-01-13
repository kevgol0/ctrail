/****************************************************************************
 * FILE: FileSearchTerms.java
 * DSCRPT: 
 ****************************************************************************/





package com.kagr.tools.ctrail.props;





import java.util.LinkedList;
import java.util.List;



import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;





@Data
@RequiredArgsConstructor
public class FileSearchFilter
{
	@NonNull @Getter @Setter private String _fileName;

	@Delegate private List<String> _searchTerms = new LinkedList<String>();





	public boolean doesMatchFilename(@NonNull String fname_)
	{
		return fname_.contains(_fileName);
	}
}
