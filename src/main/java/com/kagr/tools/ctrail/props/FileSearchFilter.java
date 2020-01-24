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





@Data
@RequiredArgsConstructor
public class FileSearchFilter
{
	@NonNull @Getter @Setter private String _fileName;

	@Getter private List<String> _includeTerms = new LinkedList<String>();

	@Getter private List<String> _excldueTerms = new LinkedList<String>();





	public boolean doesMatchFilename(@NonNull String fname_)
	{
		return fname_.contains(_fileName);
	}
}
