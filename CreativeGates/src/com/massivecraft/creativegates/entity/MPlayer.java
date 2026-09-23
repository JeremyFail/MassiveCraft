package com.massivecraft.creativegates.entity;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.massivecore.store.SenderEntity;
import com.massivecraft.massivecore.util.MUtil;

/**
 * Per-player CreativeGates data (persisted).
 */
public class MPlayer extends SenderEntity<MPlayer>
{
	// -------------------------------------------- //
	// META
	// -------------------------------------------- //
	
	/**
	 * Gets the player data for the given object id.
	 * 
	 * @param oid Object id.
	 * @return Player data.
	 */
	public static MPlayer get(Object oid)
	{
		return MPlayerColl.get().get(oid);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public MPlayer load(MPlayer that)
	{
		this.overriding = that.overriding;
		this.toolsEnabled = that.toolsEnabled;
		return this;
	}
	
	@Override
	public boolean isDefault()
	{
		if (this.overriding != null && this.overriding) return false;
		if (this.toolsEnabled != null && !this.toolsEnabled) return false;
		return true;
	}
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	/** Null means false (not overriding). */
	private Boolean overriding = null;
	
	/**
	 * Gets the current override mode state.
	 * 
	 * @return True if override mode is on and the player still has {@link Perm#CG_OVERRIDE}.
	 */
	public boolean isOverriding()
	{
		if (this.overriding == null) return false;
		if (!this.overriding) return false;
		
		if (!this.hasPermission(Perm.CG_OVERRIDE, true))
		{
			this.setOverriding(false);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Sets override mode. {@code false} is stored as {@code null}.
	 *
	 * @param overriding True to enable override mode.
	 */
	public void setOverriding(Boolean overriding)
	{
		Boolean target = overriding;
		if (MUtil.equals(target, false)) target = null;
		
		if (MUtil.equals(this.overriding, target)) return;
		
		this.overriding = target;
		this.changed();
	}
	
	/** Null means true (inspect/manage tools enabled). */
	private Boolean toolsEnabled = null;
	
	/**
	 * Whether inspect/manage tools should respond for this player.
	 * Defaults to {@code true}.
	 * 
	 * @return True if tools are enabled.
	 */
	public boolean isToolsEnabled()
	{
		if (this.toolsEnabled == null) return true;
		return this.toolsEnabled;
	}
	
	/**
	 * Sets whether inspect/manage tools are enabled. {@code true} is stored as {@code null}.
	 *
	 * @param toolsEnabled True to enable tools.
	 */
	public void setToolsEnabled(Boolean toolsEnabled)
	{
		Boolean target = toolsEnabled;
		if (MUtil.equals(target, true)) target = null;
		
		if (MUtil.equals(this.toolsEnabled, target)) return;
		
		this.toolsEnabled = target;
		this.changed();
	}
	
}
