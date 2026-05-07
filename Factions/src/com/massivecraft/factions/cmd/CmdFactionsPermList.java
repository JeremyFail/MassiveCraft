package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPermColl;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.pager.Msonifier;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.function.Predicate;

public class CmdFactionsPermList extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsPermList()
	{
		this.addParameter(Parameter.getPage());
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void perform() throws MassiveException
	{
		int page = this.readArg();
		String title = "Available Perms list";
		final boolean override = senderIsConsole || msender.isOverriding();

		final Msonifier<MPerm> msonifier = (mp, i) -> {
			// Light gray (silver) = non-visible; aqua = editable; light purple (pink) = not editable
			String color = !mp.isVisible() ? "<silver>" : (mp.isEditable() ? "<aqua>" : "<pink>");
			String line = Txt.parse(color + mp.getName() + " <i>" + mp.getDesc());
			String tooltip = buildPermListTooltip(mp, override);
			return Mson.fromParsedMessage(line).tooltipParse(tooltip);
		};
		final Pager<MPerm> pager = new Pager<>(this, title, page, msonifier);
		final Predicate<MPerm> predicate = override ? null : MPerm::isVisible;

		Bukkit.getScheduler().runTaskAsynchronously(Factions.get(), () -> {
			List<MPerm> items = MPermColl.get().getAll(predicate);
			pager.setItems(items);
			pager.message();
		});
	}

	/**
	 * Builds hover tooltip for a perm in the list: name, description. For override, also shows 
	 * territory, editable, visible. If the perm is not visible, it will also show a notice that it is
	 * not visible to players.
	 * 
	 * @param perm the perm to build the tooltip for
	 * @param override if true, the tooltip will also show territory, editable, visible
	 * @return the tooltip
	 */
	private static String buildPermListTooltip(MPerm perm, boolean override)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<aqua>Name: <yellow>").append(perm.getName()).append("\n");
		sb.append("<aqua>Description: <yellow>").append(perm.getDesc()).append("\n");
		sb.append("<aqua>Editable: ").append(perm.isEditable() ? "<lime>TRUE" : "<rose>FALSE");
		if (override)
		{
			sb.append("\n");
			sb.append("<aqua>Territory: ").append(perm.isTerritory() ? "<lime>TRUE" : "<rose>FALSE").append("\n");
			sb.append("<aqua>Visible: ").append(perm.isVisible() ? "<lime>TRUE" : "<rose>FALSE");

			if (!perm.isVisible())
			{
				sb.append("\n<gray>Not visible to players. Shown because you are in override mode.");
			}
		}
		return sb.toString();
	}
}
