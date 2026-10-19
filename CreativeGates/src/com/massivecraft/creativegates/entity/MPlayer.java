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
		this.createScope = that.createScope;
		return this;
	}
	
	@Override
	public boolean isDefault()
	{
		if (this.overriding != null && this.overriding) return false;
		if (this.toolsEnabled != null && !this.toolsEnabled) return false;
		if (this.createScope != null) return false;
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
	
	/** Null means use {@link MConf#getDefaultCreateScope()}. */
	private String createScope = null;
	
	/**
	 * Preferred create scope for this player, or the server default when unset.
	 *
	 * @return Preferred create scope.
	 */
	public GateScopeType getCreateScope()
	{
		GateScopeType fallback = MConf.get().getDefaultCreateScope();
		GateScopeType scope = this.createScope == null
			? fallback
			: GateScopeType.parse(this.createScope, fallback);
		// Do not keep FACTION selected when no team bridge is present.
		if (scope == GateScopeType.FACTION && !com.massivecraft.creativegates.util.GateScopeUtil.isFactionScopeAvailable())
		{
			scope = fallback == GateScopeType.FACTION ? GateScopeType.PLAYER : fallback;
		}
		if (!MConf.get().isScopeEnabled(scope))
		{
			return fallback;
		}
		return scope;
	}
	
	/**
	 * Sets preferred create scope. Passing null clears the override (use MConf default).
	 *
	 * @param createScope Preferred scope, or null for server default.
	 */
	public void setCreateScope(GateScopeType createScope)
	{
		String target = createScope == null ? null : createScope.name();
		if (MUtil.equals(this.createScope, target)) return;
		this.createScope = target;
		this.changed();
	}
	
}
