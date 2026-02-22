package com.massivecraft.massivebooks.cmd;

import com.massivecraft.massivebooks.Perm;
import com.massivecraft.massivebooks.cmd.type.TypeBookAmount;
import com.massivecraft.massivebooks.cmd.type.TypeMBook;
import com.massivecraft.massivebooks.entity.MBook;
import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.type.container.TypeList;
import com.massivecraft.massivecore.command.type.sender.TypePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

// NOTE: This command is identical to CmdBookGive, but does not send messages to the player.
// This is used to give books to players typically from the console or in an automated way.
public class CmdBookGiveSilent extends MassiveBooksCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdBookGiveSilent()
	{
		// Parameters (same as give)
		this.addParameter(TypePlayer.get(), true, "player", "you");
		this.addParameter(1, TypeBookAmount.get(), "amount", "1");
		this.addParameter(TypeList.get(TypeMBook.get()), "title", "*bookandquill*", true);

		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.GIVESILENT));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesBookGiveSilent());
	}

	@Override
	public void perform() throws MassiveException
	{
		Player player = this.readArg(me);
		Integer amount = this.readArg();
		boolean ensure = Objects.equals(amount, TypeBookAmount.ENSURE);
		if (ensure) amount = 1;
		List<MBook> mbooks = this.readArg(new MassiveList<>());
		CmdBookGive.performGive(this.sender, player, amount, ensure, mbooks, false);
	}

}
