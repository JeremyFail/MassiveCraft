package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MFlagColl;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.pager.Msonifier;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.function.Predicate;

public class CmdFactionsFlagList extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsFlagList()
	{
		// Parameters
		this.addParameter(Parameter.getPage());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Parameter
		final int page = this.readArg();
		
		// Pager create
		String title = "Flag List for " + msenderFaction.describeTo(msender);
		final boolean override = senderIsConsole || msender.isOverriding();

		final Msonifier<MFlag> msonifier = (mf, i) -> {
			// Light gray (silver) = non-visible; aqua = editable; light purple (pink) = not editable
			String color = !mf.isVisible() ? "<silver>" : (mf.isEditable() ? "<aqua>" : "<pink>");
			String line = Txt.parse(color + mf.getName() + " <i>" + mf.getDesc());
			String tooltip = buildFlagListTooltip(mf, override);
			return Mson.fromParsedMessage(line).tooltipParse(tooltip);
		};
		final Pager<MFlag> pager = new Pager<>(this, title, page, msonifier);
		final Predicate<MFlag> predicate = override ? null : MFlag::isVisible;

		Bukkit.getScheduler().runTaskAsynchronously(Factions.get(), () -> {
			// Get items
			List<MFlag> items = MFlagColl.get().getAll(predicate);

			// Pager items
			pager.setItems(items);

			// Pager message
			pager.message();
		});
	}

	/**
	 * Builds hover tooltip for a flag in the list: name, description, editable. For override, also shows
	 * visible. If the flag is not visible, it will also show a notice that it is not visible to players.
	 *
	 * @param flag the flag to build the tooltip for
	 * @param override if true, the tooltip will also show visible
	 * @return the tooltip
	 */
	private static String buildFlagListTooltip(MFlag flag, boolean override)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<aqua>Name: <yellow>").append(flag.getName()).append("\n");
		sb.append("<aqua>Description: <yellow>").append(flag.getDesc()).append("\n");
		sb.append("<aqua>Editable: ").append(flag.isEditable() ? "<lime>TRUE" : "<rose>FALSE");
		if (override)
		{
			sb.append("\n");
			sb.append("<aqua>Visible: ").append(flag.isVisible() ? "<lime>TRUE" : "<rose>FALSE");

			if (!flag.isVisible())
			{
				sb.append("\n<gray>Not visible to players. Shown because you are in override mode.");
			}
		}
		return sb.toString();
	}
	
}
