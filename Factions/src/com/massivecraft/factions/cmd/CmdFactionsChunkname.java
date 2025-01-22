package com.massivecraft.factions.cmd;

import com.massivecraft.factions.TerritoryAccess;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.util.AsciiMap;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.command.type.TypeNullable;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.Txt;

public class CmdFactionsChunkname extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsChunkname()
	{
		// Parameters
		this.addParameter(TypeNullable.get(TypeString.get()), "name", "read");

		this.addRequirements(RequirementIsPlayer.get());
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		PS chunk = PS.valueOf(me.getLocation()).getChunk(true);
		TerritoryAccess ta = BoardColl.get().getTerritoryAccessAt(chunk);

		if (!this.argIsSet(0))
		{
			String name = ta.getChunkName();
			if (name == null)
			{
				msg("<i>This chunk has no name.");
			}
			else
			{
				msg("<i>This chunk is called <h>%s<i>.", name);
			}
			return;
		}

		// MPerm
		if (!MPerm.getPermTerritory().has(msender, msenderFaction, true)) return;

		// Args
		String target = this.readArg();
		if (target != null)
		{
			target = target.trim();
			target = Txt.parse(target);
		}

		String old = ta.getChunkName();

		// NoChange
		if (MUtil.equals(old, target))
		{
			if (old == null)
			{
				throw new MassiveException().addMsg("<b>This chunk already has no name.");
			}
			throw new MassiveException().addMsg("<b>The name for this chunk is already <h>%s<b>.", old);
		}

		ta = ta.withChunkName(target);
		BoardColl.get().setTerritoryAccessAt(chunk, ta);

		String chunkDesc = AsciiMap.getChunkDesc(chunk);
		msg("<i>The chunk name%s<i> is now %s.", chunkDesc, target);
	}
	
}
