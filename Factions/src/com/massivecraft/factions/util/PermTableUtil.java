package com.massivecraft.factions.util;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.cmd.CmdFactionsPermSet;
import com.massivecraft.factions.cmd.CmdFactionsPermView;
import com.massivecraft.factions.cmd.CmdFactionsPermViewall;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeMPlayer;
import com.massivecraft.factions.cmd.type.TypeRank;
import com.massivecraft.factions.cmd.type.TypeRelation;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.MPerm.MPermable;
import com.massivecraft.factions.entity.Rank;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility for building and displaying the permission table (YES/NOO grid with optional click-to-toggle).
 * Used by /f perm manage, view, viewall, and by the set command when re-displaying after a change.
 * Permission hierarchy / source resolution lives in {@link PermUtil}.
 */
public final class PermTableUtil
{
	private static final Collection<String> RELATIONSHIPS_ALIASES = Arrays.asList("relationships", "relations", "relation", "rels", "rel");

	/** Maximum number of ranks allowed when managing "ranks" (all ranks) at once. */
	public static final int MAX_RANKS_FOR_MANAGE_ALL = 6;

	private PermTableUtil() { }

	// -------------------------------------------- //
	// PUBLIC API
	// -------------------------------------------- //

	/**
	 * Returns the target argument string for the manage/view/viewall command for this permable
	 * (e.g. player name, rank name, "ally", or "factionName-rankName" for another faction's rank).
	 */
	public static String getTargetArgForPermable(MPermable permable, Faction faction)
	{
		return getPermableArgStatic(permable, faction);
	}

	/**
	 * True if the target string is the bulk "ranks" or "relationships" (and aliases) view — multiple columns like manage.
	 */
	public static boolean isBulkRanksOrRelationshipsTarget(String targetArg)
	{
		if (targetArg == null)
		{
			return false;
		}
		String t = targetArg.trim().toLowerCase();
		if ("ranks".equals(t))
		{
			return true;
		}
		return RELATIONSHIPS_ALIASES.contains(t);
	}

	/**
	 * High-level entry: resolve target from string, compute perms, then render. Used by Manage, Set, and Viewall (bulk).
	 * When management is true, checks mayManage and returns without throwing if denied.
	 *
	 * @param commandForPagination command for prev/next links (e.g. manage command); if null when called from Set, caller should pass manage command
	 * @throws MassiveException if target cannot be resolved or page is invalid
	 */
	public static void displayTable(CommandSender sender, String targetArg, Faction faction, int page,
		boolean management, List<MPerm> permsOverride, MassiveCommand commandForPagination) throws MassiveException
	{
		MPlayer msender = MPlayer.get(sender);

		if (management)
		{
			boolean mayManage = mayManage(sender, faction, msender);
			if (!mayManage)
			{
				String deniedMessage = MPerm.getPermPerms().createDeniedMessage(msender, faction);
				MixinMessage.get().msgOne(sender, deniedMessage);
				return;
			}
		}

		List<MPermable> permables = resolveTarget(targetArg, faction, sender);
		if (permables == null || permables.isEmpty())
		{
			throw new MassiveException().addMsg("<b>Could not resolve target: <h>%s<b>. Use: ranks, relationships, or a specific rank/relation/faction/player name.", targetArg);
		}

		List<MPerm> perms = computePermsList(permsOverride, msender, sender);
		displayTable(sender, targetArg, faction, page, permables, perms, management, commandForPagination);
	}

	/**
	 * High-level entry for a single permable (View/Viewall). Builds targetArg and permables, computes perms if permsOverride is null.
	 *
	 * @param commandForPagination command for prev/next links (e.g. view or viewall command); typically pass {@code this}
	 */
	public static void displayTable(CommandSender sender, MPermable permable, Faction faction, int page,
		boolean management, List<MPerm> permsOverride, MassiveCommand commandForPagination)
	{
		String targetArg = getTargetArgForPermable(permable, faction);
		List<MPermable> permables = Arrays.asList(permable);
		MPlayer msender = MPlayer.get(sender);
		List<MPerm> perms = permsOverride != null && !permsOverride.isEmpty()
			? new MassiveList<>(permsOverride)
			: computePermsList(null, msender, sender);
		displayTable(sender, targetArg, faction, page, permables, perms, management, commandForPagination);
	}

	/**
	 * Builds and sends the permission table. Caller is responsible for permission checks, target resolution,
	 * and providing the list of permables and perms to show.
	 *
	 * @param sender                who to send the message to
	 * @param targetArg             target string (for title and pagination args)
	 * @param faction               the faction whose perms we are showing
	 * @param page                  page number (1-based)
	 * @param permables             resolved columns (one or more entities)
	 * @param perms                 permissions to display (will be paginated)
	 * @param management            if true, cells are clickable and header says "click YES/NOO to toggle"
	 * @param commandForPagination  command used for prev/next page links (e.g. manage command)
	 */
	public static void displayTable(CommandSender sender, String targetArg, Faction faction, int page,
		List<MPermable> permables, List<MPerm> perms, boolean management, MassiveCommand commandForPagination)
	{
		MPlayer msender = MPlayer.get(sender);

		if (perms.isEmpty())
		{
			MixinMessage.get().msgOne(sender, "<i>No permissions to display.");
			return;
		}

		// Build table manually (can't use Pager due to complex tooltip logic and custom table logic)
		String title = describeTarget(targetArg, permables, sender, commandForPagination) + 
				" Perms for " + faction.describeTo(msender);

		int pageHeight = (sender instanceof Player) ? Txt.PAGEHEIGHT_PLAYER : Txt.PAGEHEIGHT_CONSOLE;
		int pageCount = Math.max(1, (int) Math.ceil((double) perms.size() / pageHeight));

		if (page < 1 || page > pageCount)
		{
			MixinMessage.get().messageOne(sender, Txt.getMessageInvalid(pageCount));
			return;
		}

		int from = (page - 1) * pageHeight;
		int to = Math.min(from + pageHeight, perms.size());
		List<MPerm> pagePerms = perms.subList(from, to);

		List<Mson> messages = new MassiveList<>();
		List<String> manageArgs = new MassiveList<>(targetArg, faction.getId(), String.valueOf(page));
		Mson titleMson = Txt.titleizeMson(title, pageCount, page, commandForPagination, manageArgs);
		messages.add(titleMson);
		messages.add(buildHeaderRow(permables, faction, sender, management));

		for (int i = 0; i < pagePerms.size(); i++)
		{
			MPerm perm = pagePerms.get(i);
			messages.add(buildPermRow(perm, permables, faction, from + i, manageArgs, sender, msender, management));
		}

		MixinMessage.get().messageOne(sender, messages);
	}

	// -------------------------------------------- //
	// HIGH-LEVEL HELPERS (manage / view / viewall)
	// -------------------------------------------- //

	/**
	 * True if the sender may manage permissions for the faction (console, own faction with perm, bypass, or override).
	 */
	private static boolean mayManage(CommandSender sender, Faction faction, MPlayer msender)
	{
		return !(sender instanceof Player)
			|| (faction == msender.getFaction() && MPerm.getPermPerms().has(msender, faction, false))
			|| Perm.PERM_MANAGE_BYPASS.has(sender)
			|| (msender != null && msender.isOverriding());
	}

	/**
	 * Build perms list: use override if provided, else visible only (or all if console/override).
	 */
	private static List<MPerm> computePermsList(List<MPerm> permsOverride, MPlayer msender, CommandSender sender)
	{
		if (permsOverride != null && !permsOverride.isEmpty())
		{
			return new MassiveList<>(permsOverride);
		}
		boolean showAllPerms = !(sender instanceof Player) || (msender != null && msender.isOverriding());
		return MPerm.getAll().stream()
			.filter(p -> showAllPerms || p.isVisible())
			.collect(Collectors.toList());
	}

	/**
	 * Resolves the target argument to a list of permables (one or many).
	 * Supports: rank-/relation-/faction-/player- prefixes, "ranks", "relationships" (and aliases),
	 * single relation/rank/faction/player, and factionName-rankName for another faction's rank.
	 *
	 * @return list of permables, or null if unresolved
	 */
	private static List<MPermable> resolveTarget(String targetArg, Faction faction, CommandSender sender) throws MassiveException
	{
		String lower = targetArg.toLowerCase().trim();

		// Prefixed forms (same as TypeMPermable)
		if (lower.startsWith("rank-"))
		{
			String subArg = targetArg.substring("rank-".length());
			try
			{
				Rank rank = TypeRank.get(faction, null).read(subArg, sender);
				return Arrays.asList(rank);
			}
			catch (MassiveException ignored)
			{
				// fall through
			}
		}
		if (lower.startsWith("relation-"))
		{
			String subArg = targetArg.substring("relation-".length());
			try
			{
				Rel rel = TypeRelation.get().read(subArg, sender);
				if (rel != Rel.FACTION)
				{
					return Arrays.asList(rel);
				}
			}
			catch (MassiveException ignored)
			{
				// fall through
			}
		}
		if (lower.startsWith("player-"))
		{
			String subArg = targetArg.substring("player-".length());
			try
			{
				MPlayer mplayer = TypeMPlayer.get().read(subArg, sender);
				return Arrays.asList(mplayer);
			}
			catch (MassiveException ignored)
			{
				// fall through
			}
		}
		if (lower.startsWith("faction-"))
		{
			String subArg = targetArg.substring("faction-".length());
			try
			{
				Faction other = TypeFaction.get().read(subArg, sender);
				if (other != faction)
				{
					return Arrays.asList(other);
				}
			}
			catch (MassiveException ignored)
			{
				// fall through
			}
		}

		// "ranks" -> all ranks (max 6), highest rank first
		if ("ranks".equals(lower))
		{
			List<Rank> ranks = new ArrayList<>(faction.getRanks().getAll());
			ranks.sort(Comparator.comparingInt(Rank::getPriority).reversed());
			if (ranks.size() > MAX_RANKS_FOR_MANAGE_ALL)
			{
				throw new MassiveException().addMsg("<b>This faction has more than %d ranks. Manage a specific rank instead.", MAX_RANKS_FOR_MANAGE_ALL);
			}
			return new MassiveList<>(ranks);
		}

		// "relationships" and aliases -> all relations (Ally, Truce, Neutral, Enemy)
		if (RELATIONSHIPS_ALIASES.contains(lower))
		{
			List<MPermable> rels = new MassiveList<>();
			rels.add(Rel.ALLY);
			rels.add(Rel.TRUCE);
			rels.add(Rel.NEUTRAL);
			rels.add(Rel.ENEMY);
			return rels;
		}

		// Single relation
		try
		{
			Rel rel = TypeRelation.get().read(targetArg, sender);
			if (rel != Rel.FACTION)
			{
				return Arrays.asList(rel);
			}
		}
		catch (MassiveException ignored)
		{
			// fall through
		}

		// Single rank (this faction)
		try
		{
			Rank rank = TypeRank.get(faction, null).read(targetArg, sender);
			return Arrays.asList(rank);
		}
		catch (MassiveException ignored)
		{
			// fall through
		}

		// Single player (before Single faction: TypeFaction accepts player names and returns their faction)
		try
		{
			MPlayer mplayer = TypeMPlayer.get().read(targetArg, sender);
			return Arrays.asList(mplayer);
		}
		catch (MassiveException ignored)
		{
			// fall through
		}

		// Single faction
		try
		{
			Faction other = TypeFaction.get().read(targetArg, sender);
			if (other != faction)
			{
				return Arrays.asList(other);
			}
		}
		catch (MassiveException ignored)
		{
			// fall through
		}

		// faction-rank (other faction's rank)
		if (targetArg.contains("-"))
		{
			int idx = targetArg.indexOf('-');
			String factionPart = targetArg.substring(0, idx);
			String rankPart = targetArg.substring(idx + 1);
			try
			{
				Faction otherFaction = TypeFaction.get().read(factionPart, sender);
				Rank otherRank = TypeRank.get(otherFaction, null).read(rankPart, sender);
				return Arrays.asList(otherRank);
			}
			catch (MassiveException ignored)
			{
				// fall through
			}
		}

		return null;
	}

	// -------------------------------------------- //
	// PERMABLE ARG (for set command and target arg)
	// -------------------------------------------- //

	private static String getPermableArgStatic(MPermable permable, Faction contextFaction)
	{
		if (permable instanceof Rank)
		{
			Rank rank = (Rank) permable;
			Faction rankFaction = rank.getFaction();
			if (rankFaction != null && rankFaction != contextFaction)
			{
				return rankFaction.getName() + "-" + rank.getName();
			}
			return rank.getName();
		}
		if (permable instanceof Rel)
		{
			return ((Rel) permable).name();
		}
		if (permable instanceof MPlayer)
		{
			return ((MPlayer) permable).getName();
		}
		if (permable instanceof Faction)
		{
			return ((Faction) permable).getName();
		}
		return permable.getId();
	}

	// -------------------------------------------- //
	// TITLE / DESCRIBE
	// -------------------------------------------- //

	private static String describeTarget(String targetArg, List<MPermable> permables, CommandSender sender, MassiveCommand commandForPagination)
	{
		String title = "Manage";
		if (commandForPagination != null)
		{
			if (commandForPagination instanceof CmdFactionsPermView)
			{
				title = "View";
			}
			else if (commandForPagination instanceof CmdFactionsPermViewall)
			{
				title = "View All";
			}
		}
		
		if ("ranks".equalsIgnoreCase(targetArg.trim()))
		{
			title += " Rank";
		}
		if (RELATIONSHIPS_ALIASES.contains(targetArg.toLowerCase().trim()))
		{
			title += " Relationship";
		}
		return title;
	}

	private static String getEntityLabel(MPermable permable, CommandSender sender)
	{
		if (permable instanceof MPlayer)
		{
			return "Player: " + ((MPlayer) permable).getDisplayName(sender);
		}
		if (permable instanceof Rank)
		{
			return "Rank: " + permable.getDisplayName(sender);
		}
		if (permable instanceof Faction)
		{
			return "Faction: " + ((Faction) permable).getDisplayName(sender);
		}
		if (permable instanceof Rel)
		{
			return "Relation: " + permable.getDisplayName(sender);
		}
		return permable.getDisplayName(sender);
	}

	private static String getEntityLabelPrefix(MPermable permable)
	{
		if (permable instanceof MPlayer) return "Player: ";
		if (permable instanceof Rank) return "Rank: ";
		if (permable instanceof Faction) return "Faction: ";
		if (permable instanceof Rel) return "Relation: ";
		return null;
	}

	// -------------------------------------------- //
	// COLUMN HEADER / COLOR
	// -------------------------------------------- //

	private static String getColumnHeader(MPermable permable)
	{
		if (permable instanceof Rel)
		{
			Rel rel = (Rel) permable;
			String name = rel.name();
			return name.length() >= 3 ? name.substring(0, 3) : name;
		}
		if (permable instanceof Rank)
		{
			Rank rank = (Rank) permable;
			String name = rank.getName();
			return name.length() >= 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase();
		}
		String name = permable.getName();
		return name != null && name.length() >= 3 ? name.substring(0, 3).toUpperCase() : (name != null ? name.toUpperCase() : "???");
	}

	private static ChatColor getColumnColor(MPermable permable, Faction hostFaction)
	{
		if (permable instanceof Rel)
		{
			return ((Rel) permable).getColor();
		}
		if (permable instanceof Rank)
		{
			return ChatColor.GREEN;
		}
		if (permable instanceof Faction && hostFaction != null)
		{
			return RelationUtil.getRelationOfThatToMe((Faction) permable, hostFaction).getColor();
		}
		return ChatColor.WHITE;
	}

	// -------------------------------------------- //
	// PERMISSION CHECK (hierarchy-aware)
	// -------------------------------------------- //

	private static boolean hasPermission(Faction faction, MPermable permable, MPerm perm)
	{
		if (permable instanceof MPlayer)
		{
			return faction.isPlayerPermitted((MPlayer) permable, perm);
		}
		if (permable instanceof Faction)
		{
			return faction.isFactionPermitted((Faction) permable, perm);
		}
		if (permable instanceof Rank)
		{
			return faction.isRankPermitted((Rank) permable, perm.getId());
		}
		return faction.isPermitted(permable.getId(), perm.getId());
	}

	private static Object[] resolveSetPermableAndDesc(MPermable permable, Faction faction, MPerm perm, boolean value)
	{
		MPermable setPermable = permable;
		String toggleTargetDesc = null;

		if (permable instanceof MPlayer)
		{
			MPlayer playerCol = (MPlayer) permable;
			MPermable source = PermUtil.getPlayerPermissionSourcePermable(faction, playerCol, perm.getId());
			if (source != null)
			{
				setPermable = source;
				toggleTargetDesc = source instanceof MPlayer ? "player" : source instanceof Rank ? "rank" : source instanceof Rel ? "relation" : "faction";
				if (source instanceof MPlayer && ((MPlayer) source).getId().equals(playerCol.getId()))
				{
					toggleTargetDesc = null;
				}
			}
		}
		else if (permable instanceof Rank && value)
		{
			Rank rankCol = (Rank) permable;
			MPermable source = PermUtil.getRankPermissionSourcePermable(faction, rankCol, perm.getId());
			if (source != null)
			{
				setPermable = source;
				toggleTargetDesc = source instanceof Rel ? "relation" : source instanceof Faction ? "faction" : "rank";
				if (source instanceof Rank && ((Rank) source).getId().equals(rankCol.getId()))
				{
					toggleTargetDesc = null;
				}
			}
		}
		else if (permable instanceof Faction && value)
		{
			Faction facCol = (Faction) permable;
			MPermable source = PermUtil.getFactionPermissionSourcePermable(faction, facCol, perm.getId());
			if (source != null)
			{
				setPermable = source;
				toggleTargetDesc = source instanceof Rel ? "relation" : "faction";
				if (source instanceof Faction && ((Faction) source).getId().equals(facCol.getId()))
				{
					toggleTargetDesc = null;
				}
			}
		}

		return new Object[] { setPermable, toggleTargetDesc };
	}

	// -------------------------------------------- //
	// HEADER ROW
	// -------------------------------------------- //

	private static Mson buildHeaderRow(List<MPermable> permables, Faction faction, CommandSender sender, boolean management)
	{
		List<Mson> parts = new MassiveList<>();
		if (permables.size() == 1)
		{
			MPermable p = permables.get(0);
			String prefix = getEntityLabelPrefix(p);
			if (prefix != null)
			{
				parts.add(Mson.mson(prefix).color(ChatColor.WHITE));
				parts.add(Mson.mson(p.getDisplayName(sender)).color(getColumnColor(p, faction)));
			}
			else
			{
				parts.add(Mson.mson(getEntityLabel(p, sender)).color(getColumnColor(p, faction)));
			}
			parts.add(Mson.SPACE);
		}
		else
		{
			for (MPermable p : permables)
			{
				parts.add(Mson.mson(getColumnHeader(p)).color(getColumnColor(p, faction)));
				parts.add(Mson.SPACE);
			}
		}
		if (sender instanceof Player)
		{
			String hint = management ? "(click YES/NOO to toggle)" : "(hover over YES or NOO for details)";
			parts.add(Mson.mson(hint).color(ChatColor.GRAY));
		}
		return Mson.mson(parts);
	}

	// -------------------------------------------- //
	// PERM ROW
	// -------------------------------------------- //

	private static Mson buildPermRow(MPerm perm, List<MPermable> permables, Faction faction, int index,
		List<String> manageArgs, CommandSender sender, MPlayer msender, boolean management)
	{
		List<Mson> parts = new MassiveList<>();
		boolean canEdit = management && (perm.isEditable() || (msender != null && msender.isOverriding()));
		String page = manageArgs.get(2);

		for (MPermable permable : permables)
		{
			boolean value = hasPermission(faction, permable, perm);
			String yesNo = value ? "YES" : "NOO";
			ChatColor color = value ? ChatColor.GREEN : ChatColor.RED;

			Object[] resolved = resolveSetPermableAndDesc(permable, faction, perm, value);
			String toggleTargetDesc = (String) resolved[1];

			boolean singleEntityView = (permables.size() == 1);
			String tooltip = buildCellTooltip(perm, permable, faction, value, canEdit, toggleTargetDesc, singleEntityView, management, msender);

			Mson cell = Mson.mson(yesNo).color(color).tooltipParse(tooltip);
			if (canEdit)
			{
				String permableArg = getPermableArgStatic((MPermable) resolved[0], faction);
				String setValue = value ? "no" : "yes";
				String clickCommandLine = CmdFactionsPermSet.buildSetCommandLine(perm.getId(), permableArg, setValue, faction.getId(), "manage", page);
				cell = cell.command(clickCommandLine);
			}
			parts.add(cell);
			parts.add(Mson.SPACE);
		}

		// Silver (light gray) for non-visible; aqua = editable, light purple = not editable
		ChatColor nameColor = !perm.isVisible() ? ChatColor.GRAY : (perm.isEditable() ? ChatColor.AQUA : ChatColor.LIGHT_PURPLE);
		parts.add(Mson.mson(perm.getName()).color(nameColor));
		parts.add(Mson.SPACE);
		parts.add(Mson.mson(perm.getDesc()).color(ChatColor.YELLOW));

		return Mson.mson(parts);
	}

	private static String buildCellTooltip(MPerm perm, MPermable permable, Faction faction, boolean value, boolean canEdit,
		String toggleTargetDesc, boolean singleEntityView, boolean management, MPlayer msender)
	{
		StringBuilder sb = new StringBuilder();
		if (value)
		{
			sb.append("<g>Granted");
		}
		else
		{
			sb.append("<b>Not granted");
		}

		String sourceDesc = null;
		if (permable instanceof MPlayer)
		{
			MPlayer playerCol = (MPlayer) permable;
			MPermable src = PermUtil.getPlayerPermissionSourcePermable(faction, playerCol, perm.getId());
			sourceDesc = PermUtil.formatPermissionSourceDesc(src);
			if (sourceDesc != null && src instanceof MPlayer && ((MPlayer) src).getId().equals(playerCol.getId()))
			{
				sourceDesc = null;
			}
		}
		else if (permable instanceof Rank)
		{
			Rank rankCol = (Rank) permable;
			MPermable src = PermUtil.getRankPermissionSourcePermable(faction, rankCol, perm.getId());
			sourceDesc = PermUtil.formatPermissionSourceDesc(src);
			if (sourceDesc != null && src instanceof Rank && ((Rank) src).getId().equals(rankCol.getId()))
			{
				sourceDesc = null;
			}
		}
		else if (permable instanceof Rel)
		{
			sourceDesc = null;
		}
		else if (permable instanceof Faction)
		{
			Faction facCol = (Faction) permable;
			MPermable src = PermUtil.getFactionPermissionSourcePermable(faction, facCol, perm.getId());
			sourceDesc = PermUtil.formatPermissionSourceDesc(src);
			if (sourceDesc != null && src instanceof Faction && ((Faction) src).getId().equals(facCol.getId()))
			{
				sourceDesc = null;
			}
		}

		if (sourceDesc != null)
		{
			sb.append("\n<i>From: ").append(sourceDesc);
		}
		if (management && canEdit)
		{
			if (value)
			{
				sb.append("\n<aqua>Click to revoke");
				if (!singleEntityView && toggleTargetDesc != null)
				{
					sb.append(" <teal><em>from ").append(toggleTargetDesc);
				}
			}
			else
			{
				sb.append("\n<aqua>Click to grant");
			}
		}

		if (!perm.isEditable())
		{
			if (canEdit)
			{
				sb.append("\n<gold>Editable in override mode");
			}
			else
			{
				sb.append("\n<pink>Not editable");
			}
		}

		if (!perm.isVisible())
		{
			sb.append("\n<silver>Not visible to players");
		}

		return sb.toString();
	}
}
