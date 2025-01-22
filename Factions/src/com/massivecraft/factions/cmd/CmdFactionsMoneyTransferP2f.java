package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.cmd.req.ReqBankCommandsEnabled;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeMPlayer;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.primitive.TypeDouble;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;

public class CmdFactionsMoneyTransferP2f extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsMoneyTransferP2f()
	{
		// Fields
		this.setSetupEnabled(false);

		// Aliases
		this.addAliases("pf");

		// Parameters
		this.addParameter(TypeDouble.get(), "amount").setDesc("the amount of money to transfer");
		this.addParameter(TypeMPlayer.get(), "player").setDesc("the player to transfer money from");
		this.addParameter(TypeFaction.get(), "faction").setDesc("the faction to transfer money to");

		// Requirements
		this.addRequirements(ReqBankCommandsEnabled.get());
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		double amount = this.readArg();
		MPlayer from = this.readArg();
		Faction to = this.readArg();
		
		boolean success = Econ.transferMoney(msender, from, to, amount);

		if (success && MConf.get().logMoneyTransactions)
		{
			Factions.get().log(ChatColor.stripColor(Txt.parse("%s transferred %s from the player \"%s\" to the faction \"%s\"", msender.getName(), Money.format(amount), from.describeTo(null), to.describeTo(null))));
		}
	}
	
}
