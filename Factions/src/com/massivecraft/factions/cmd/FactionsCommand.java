package com.massivecraft.factions.cmd;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.MassiveCommandHelp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FactionsCommand extends MassiveCommand
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public MPlayer msender;
	public Faction msenderFaction;

	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public FactionsCommand()
	{

	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void senderFields(boolean set)
	{
		this.msender = set ? MPlayer.get(sender) : null;
		this.msenderFaction = set ? this.msender.getFaction() : null;
	}

	// -------------------------------------------- //
	// CHILDREN
	// -------------------------------------------- //

	/**
	 * Orders child commands by their primary alias, case-insensitively.
	 * The help command stays first, since command lookup expects it at index 0.
	 * Nested faction commands are sorted the same way.
	 */
	protected void sortChildrenAlphabetically()
	{
		List<MassiveCommand> help = new ArrayList<>();
		List<MassiveCommand> rest = new ArrayList<>();
		for (MassiveCommand child : this.getChildren())
		{
			if (child instanceof MassiveCommandHelp)
			{
				help.add(child);
			}
			else
			{
				rest.add(child);
			}
		}

		rest.sort(Comparator.comparing(FactionsCommand::primaryAlias, String.CASE_INSENSITIVE_ORDER));

		List<MassiveCommand> sorted = new ArrayList<>(help.size() + rest.size());
		sorted.addAll(help);
		sorted.addAll(rest);
		this.children = Collections.unmodifiableList(sorted);

		for (MassiveCommand child : rest)
		{
			if (child instanceof FactionsCommand)
			{
				((FactionsCommand) child).sortChildrenAlphabetically();
			}
		}
	}

	private static String primaryAlias(MassiveCommand command)
	{
		List<String> aliases = command.getAliases();
		if (aliases.isEmpty()) return command.getClass().getSimpleName();
		return aliases.get(0);
	}

}
