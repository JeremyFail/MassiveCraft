package com.massivecraft.massivebooks.entity;

import com.massivecraft.massivecore.store.Coll;
import com.massivecraft.massivecore.util.Txt;

import java.util.UUID;

public class MBookColl extends Coll<MBook>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static final MBookColl i = new MBookColl();
	public static MBookColl get() { return i; }

	/** Look up MBook by bookId (new format). Returns null if not found. */
	public MBook getById(UUID bookId)
	{
		if (bookId == null) return null;
		String idStr = bookId.toString();
		for (MBook mbook : this.getAll())
		{
			if (mbook.getBookId() != null && mbook.getBookId().toString().equals(idStr)) return mbook;
		}
		return null;
	}

	// -------------------------------------------- //
	// STACK TRACEABILITY
	// -------------------------------------------- //
	
	@Override
	public void onTick()
	{
		super.onTick();
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public String fixId(Object oid)
	{
		String ret = super.fixId(oid);
		if (ret == null) return null;
		return Txt.stripColorLegacy(ret.trim().toLowerCase());
	}
	
}
