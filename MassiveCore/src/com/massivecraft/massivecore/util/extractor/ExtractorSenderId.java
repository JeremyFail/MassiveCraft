package com.massivecraft.massivecore.util.extractor;

import com.massivecraft.massivecore.util.IdUtil;

public class ExtractorSenderId implements Extractor
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static ExtractorSenderId i = new ExtractorSenderId();
	public static ExtractorSenderId get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE: EXTRACTOR
	// -------------------------------------------- //
	
	@Override
	public Object extract(Object o)
	{
		return IdUtil.getId(o);
	}

}
