package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeMFlag;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.event.EventFactionsFlagChange;
import com.massivecraft.factions.util.FlagTableUtil;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.command.type.primitive.TypeBooleanYes;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdFactionsFlagSet extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsFlagSet()
	{
		// Parameters: flag, value, faction, [manage], [page] - last two optional to re-display table after set
		this.addParameter(TypeMFlag.get(), "flag").setDesc("the faction flag to set a value for");
		this.addParameter(TypeBooleanYes.get(), "yes/no").setDesc("should the flag be on or off?");
		this.addParameter(TypeFaction.get(), "faction", "you").setDesc("the faction to set the flag for (per default your own)");
		this.addParameter(TypeString.get(), "manage", "");
		this.addParameter(Parameter.getPage());
	}

	/**
	 * Builds the full command line for /f flag set (used by FlagTableUtil for click-to-toggle).
	 */
	public static String buildSetCommandLine(String flagId, String value, String factionId, String manage, String page)
	{
		return CmdFactions.get().cmdFactionsFlag.cmdFactionsFlagSet.getCommandLine(flagId, value, factionId, manage, page);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		MFlag flag = this.readArgAt(0);
		boolean value = this.readArgAt(1);
		Faction faction = this.readArgAt(2, msenderFaction);
		
		// Does the sender have the right to change flags for this faction?
		if (!canChangeFlags(sender, msender, faction))
		{
			MPerm.getPermFlags().has(msender, faction, true);
			return;
		}
		
		// Is this flag editable?
		if (!msender.isOverriding() && ! flag.isEditable())
		{
			throw new MassiveException().addMsg("<b>The flag <h>%s <b>is not editable.", flag.getName());
		}
		
		// Event
		EventFactionsFlagChange event = new EventFactionsFlagChange(sender, faction, flag, value);
		event.run();
		if (event.isCancelled()) return;
		value = event.isNewValue();
		
		// No change 
		if (faction.getFlag(flag) == value)
		{
			throw new MassiveException().addMsg("%s <i>already has %s <i>set to %s<i>.", faction.describeTo(msender), flag.getStateDesc(value, false, true, true, false, true), flag.getStateDesc(value, true, true, false, false, false));
		}
		
		// Apply
		faction.setFlag(flag, value);
		
		// Inform
		String stateInfo = flag.getStateDesc(faction.getFlag(flag), true, false, true, true, true);
		if (msender.getFaction() != faction)
		{
			// Send message to sender
			msg("<h>%s <i>set a flag for <h>%s<i>.", msender.describeTo(msender, true), faction.describeTo(msender, true));
			message(stateInfo);
		}
		faction.msg("<h>%s <i>set a flag for <h>%s<i>.", msender.describeTo(faction, true), faction.describeTo(faction, true));
		faction.sendMessage(stateInfo);

		// Re-display manage table if "manage" and page were provided (avoids multi-command click)
		String manageOpt = this.readArgAt(3, "");
		Integer pageOpt = this.readArgAt(4, 1);
		if (manageOpt != null && "manage".equalsIgnoreCase(manageOpt.trim()) && pageOpt != null && pageOpt >= 1)
		{
			FlagTableUtil.displayTable(sender, faction, pageOpt, true, CmdFactions.get().cmdFactionsFlag.cmdFactionsFlagManage);
		}
	}
	
	private static boolean canChangeFlags(CommandSender sender, MPlayer msender, Faction faction)
	{
		if ( ! (sender instanceof Player)) return true;
		if (Perm.FLAG_MANAGE_BYPASS.has(sender)) return true;
		return MPerm.getPermFlags().has(msender, faction, false);
	}
	
}
