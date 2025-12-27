package com.massivecraft.massivebooks.entity;

import com.massivecraft.massivebooks.Lang;
import com.massivecraft.massivecore.store.SenderEntity;

public class MPlayer extends SenderEntity<MPlayer>
{
	// -------------------------------------------- //
	// META
	// -------------------------------------------- //
	
	public static MPlayer get(Object oid)
	{
		return MPlayerColl.get().get(oid);
	}
	
	//----------------------------------------------//
	// OVERRIDE
	//----------------------------------------------//
	
	@Override
	public MPlayer load(MPlayer that)
	{
		this.usingAutoUpdate = that.usingAutoUpdate;
		
		return this;
	}
	
	@Override
	public boolean isDefault()
	{
		if (!this.usingAutoUpdate) return false;
		return true;
	}
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private boolean usingAutoUpdate = true;
	public boolean isUsingAutoUpdate() { return this.usingAutoUpdate; }
	public void setUsingAutoUpdate(boolean usingAutoUpdate)
	{
		this.setUsingAutoUpdate(usingAutoUpdate, false, false);
	}
	
	public void setUsingAutoUpdate(boolean usingAutoUpdate, boolean verboseChange, boolean verboseSame)
	{
		boolean same = (this.usingAutoUpdate == usingAutoUpdate);
		if (same)
		{
			if (verboseSame)
			{
				if (usingAutoUpdate)
				{
					this.message(Lang.AUTOUPDATE_ALREADY_TRUE);
				}
				else
				{
					this.message(Lang.AUTOUPDATE_ALREADY_FALSE);
				}
			}
			return;
		}
		
		this.usingAutoUpdate = usingAutoUpdate;
		this.changed();
		
		if (verboseChange)
		{
			if (usingAutoUpdate)
			{
				this.message(Lang.AUTOUPDATE_CHANGED_TO_TRUE);
			}
			else
			{
				this.message(Lang.AUTOUPDATE_CHANGED_TO_FALSE);
			}
		}
	}

}