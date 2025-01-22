package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeMPerm;
import com.massivecraft.factions.cmd.type.TypeMPermable;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.event.EventFactionsPermChange;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeBooleanYes;
import com.massivecraft.massivecore.util.Txt;

public class CmdFactionsPermSet extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsPermSet()
	{
		// Parameters
		this.addParameter(TypeMPerm.get(), "perm");
		this.addParameter(TypeMPermable.get(), "rank/rel/player/faction");
		this.addParameter(TypeBooleanYes.get(), "yes/no");
		this.addParameter(TypeFaction.get(), "faction", "you");
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		MPerm perm = this.readArgAt(0);
		Boolean value = this.readArgAt(2);
		Faction faction = this.readArgAt(3, msenderFaction);

		MPerm.MPermable permable = TypeMPermable.get(faction).read(this.argAt(1), sender);
		
		// Do the sender have the right to change perms for this faction?
		if ( ! MPerm.getPermPerms().has(msender, faction, true)) return;
		
		// Is this perm editable?
		if ( ! msender.isOverriding() && ! perm.isEditable())
		{
			throw new MassiveException().addMsg("<b>The perm <h>%s <b>is not editable.", perm.getName());
		}

		if (permable == faction)
		{
			throw new MassiveException().addMsg("<b>A faction can't have perms for itself. Perhaps try ranks.");
		}
		
		// Event
		EventFactionsPermChange event = new EventFactionsPermChange(sender, faction, perm, permable, value);
		event.run();
		if (event.isCancelled()) return;
		value = event.getNewValue();

		// Apply
		boolean change = faction.setPermitted(permable, perm, value);
		
		// No change
		if (!change)
		{
			throw new MassiveException().addMsg("%s <i>already has %s <i>set to %s <i>for %s<i>.", faction.describeTo(msender), perm.getDesc(true, false), Txt.parse(value ? "<g>YES" : "<b>NOO"), permable.getDisplayName(msender));
		}
		
		// The following is to make sure the leader always has the right to change perms if that is our goal.
		if (perm == MPerm.getPermPerms() && MConf.get().perm2default.get(MPerm.ID_PERMS).contains("LEADER"))
		{
			faction.setPermitted( faction.getLeaderRank(), MPerm.getPermPerms(), true);
		}

		// Inform sender
		String yesNo = Txt.parse(value ? "<g>YES" : "<b>NOO");
		msg("<i>Set perm <h>%s <i>to <h>%s <i>for <reset>%s<i> in <reset>%s<i>.", perm.getName(), yesNo, permable.getDisplayName(msender), faction.describeTo(msender));
	}
	
}
