package com.massivecraft.factions.cmd;

import com.massivecraft.factions.engine.EngineFly;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.MassiveCommandToggle;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.engine.EngineMassiveCorePlayerUpdate;
import com.massivecraft.massivecore.ps.PS;
import org.bukkit.entity.Player;

public class CmdFactionsFly extends MassiveCommandToggle
{
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //

	private static CmdFactionsFly i = new CmdFactionsFly();
	public static CmdFactionsFly get() { return i; }

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsFly()
	{
		this.addRequirements(RequirementIsPlayer.get());
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public String getName()
	{
		return "faction flying";
	}

	@Override
	public boolean getValue() throws MassiveException
	{
		return MPlayer.get(sender).isFlying();
	}

	public void setValue(boolean value) throws MassiveException
	{
		MPlayer mplayer = MPlayer.get(sender);
		Player player = me;
		if (player == null) throw new MassiveException().addMsg("<b>Could not find player.");

		PS ps = PS.valueOf(player);
		if (value) EngineFly.canFlyInTerritoryOrThrow(mplayer, ps);

		mplayer.setFlying(value);
		EngineMassiveCorePlayerUpdate.update(player, false);
	}

}
