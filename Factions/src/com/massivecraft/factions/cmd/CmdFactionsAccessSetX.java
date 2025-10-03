package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeMPermable;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm.MPermable;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.ps.PS;

import java.util.Set;

public abstract class CmdFactionsAccessSetX extends CmdFactionsAccessAbstract
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	private boolean grant = true;
	public boolean isGranting() { return this.grant; }
	public void setGranting(boolean grant) { this.grant = grant; }

	private int mpermableArgIndex = 0;
	public int getMPermableArgIndex() { return this.mpermableArgIndex; }
	public void setMPermableArgIndex(int mpermableArgIndex) { this.mpermableArgIndex = mpermableArgIndex; }

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsAccessSetX(boolean grant)
	{
		this.setGranting(grant);
		this.setSetupEnabled(false);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{	
		// Args
		final MPermable mpermable = this.getMPermable(hostFaction);

		final Set<PS> chunks = this.getChunks();
		if (chunks == null) throw new NullPointerException("chunks");

		// Apply / Inform
		setAccess(chunks, mpermable, this.isGranting());
	}
	
	// -------------------------------------------- //
	// ABSTRACT
	// -------------------------------------------- //
	
	public abstract Set<PS> getChunks() throws MassiveException;
	
	// -------------------------------------------- //
	// EXTRAS
	// -------------------------------------------- //
	
	public MPermable getMPermable(Faction faction) throws MassiveException
	{
		String arg = this.argAt(this.getMPermableArgIndex());
		return TypeMPermable.get(faction).read(arg, sender);
	}
	
}
