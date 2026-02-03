package com.massivecraft.factions.entity;

import com.massivecraft.factions.AccessStatus;
import com.massivecraft.factions.Perm;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.TerritoryAccess;
import com.massivecraft.factions.cmd.CmdFactions;
import com.massivecraft.factions.event.EventFactionsCreatePerms;
import com.massivecraft.massivecore.Identified;
import com.massivecraft.massivecore.Named;
import com.massivecraft.massivecore.Prioritized;
import com.massivecraft.massivecore.Registerable;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.comparator.ComparatorSmart;
import com.massivecraft.massivecore.predicate.PredicateIsRegistered;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.store.Entity;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MPerm extends Entity<MPerm> implements Prioritized, Registerable, Named
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //
	
	public final static transient String ID_BUILD = "build";
	public final static transient String ID_PAINBUILD = "painbuild";
	public final static transient String ID_DOOR = "door";
	public final static transient String ID_BUTTON = "button";
	public final static transient String ID_PRESSURE_PLATE = "pressureplate";
	public final static transient String ID_LEVER = "lever";
	public final static transient String ID_CONTAINER = "container";
	public final static transient String ID_LEASH = "lead";
	
	public final static transient String ID_NAME = "name";
	public final static transient String ID_DESC = "desc";
	public final static transient String ID_MOTD = "motd";
	public final static transient String ID_COLOR = "color";
	public final static transient String ID_INVITE = "invite";
	public final static transient String ID_KICK = "kick";
	public final static transient String ID_RANK = "rank";
	public final static transient String ID_TITLE = "title";
	public final static transient String ID_WARP = "warp";
	public final static transient String ID_SETWARP = "setwarp";
	public final static transient String ID_DEPOSIT = "deposit";
	public final static transient String ID_WITHDRAW = "withdraw";
	public final static transient String ID_TERRITORY = "territory";
	public final static transient String ID_ACCESS = "access";
	public final static transient String ID_VOTE = "VOTE"; // Why is this capitalised? Can that be easily changed?
	public final static transient String ID_CREATEVOTE = "createvote";
	public final static transient String ID_CLAIMNEAR = "claimnear";
	public final static transient String ID_TAX = "tax";
	public final static transient String ID_REL = "rel";
	public final static transient String ID_DISBAND = "disband";
	public final static transient String ID_FLAGS = "flags";
	public final static transient String ID_PERMS = "perms";

	public final static transient int PRIORITY_BUILD = 1000;
	public final static transient int PRIORITY_PAINBUILD = 2000;
	public final static transient int PRIORITY_DOOR = 3000;
	public final static transient int PRIORITY_BUTTON = 4000;
	public final static transient int PRIORITY_PRESSURE_PLATE = 4500;
	public final static transient int PRIORITY_LEVER = 5000;
	public final static transient int PRIORITY_CONTAINER = 6000;
	public final static transient int PRIORITY_LEASH = 6500;

	public final static transient int PRIORITY_NAME = 7000;
	public final static transient int PRIORITY_DESC = 8000;
	public final static transient int PRIORITY_MOTD = 9000;
	public final static transient int PRIORITY_COLOR = 8500;
	public final static transient int PRIORITY_INVITE = 10000;
	public final static transient int PRIORITY_KICK = 11000;
	public final static transient int PRIORITY_RANK = 11500;
	public final static transient int PRIORITY_TITLE = 12000;
	public final static transient int PRIORITY_WARP = 13000;
	public final static transient int PRIORITY_SETWARP = 14000;
	public final static transient int PRIORITY_DEPOSIT = 15000;
	public final static transient int PRIORITY_WITHDRAW = 16000;
	public final static transient int PRIORITY_TERRITORY = 17000;
	public final static transient int PRIORITY_ACCESS = 18000;
	public final static transient int PRIORITY_VOTE = 18200;
	public final static transient int PRIORITY_CREATEVOTE = 18600;
	public final static transient int PRIORITY_CLAIMNEAR = 19000;
	public final static transient int PRIORITY_TAX = 19500;
	public final static transient int PRIORITY_REL = 20000;
	public final static transient int PRIORITY_DISBAND = 21000;
	public final static transient int PRIORITY_FLAGS = 22000;
	public final static transient int PRIORITY_PERMS = 23000;
	
	// -------------------------------------------- //
	// META: CORE
	// -------------------------------------------- //
	
	public static MPerm get(Object oid)
	{
		return MPermColl.get().get(oid);
	}
	
	public static List<MPerm> getAll()
	{
		return getAll(false);
	}
	
	public static List<MPerm> getAll(boolean isAsync)
	{
		setupStandardPerms();
		new EventFactionsCreatePerms().run();
		
		return MPermColl.get().getAll(PredicateIsRegistered.get(), ComparatorSmart.get());
	}
	
	public static void setupStandardPerms()
	{
		getPermBuild();
		getPermPainbuild();
		getPermDoor();
		getPermButton();
		getPermPressurePlate();
		getPermLever();
		getPermContainer();
		getPermLeashMob();
		
		getPermName();
		getPermDesc();
		getPermMotd();
		getPermColor();
		getPermInvite();
		getPermKick();
		getPermRank();
		getPermTitle();
		getPermWarp();
		getPermSetwarp();
		getPermDeposit();
		getPermWithdraw();
		getPermTerritory();
		getPermAccess();
		getPermVote();
		getPermCreateVote();
		getPermClaimnear();
		getPermTax();
		getPermRel();
		getPermDisband();
		getPermFlags();
		getPermPerms();
	}
	
	public static MPerm getPermBuild() { return getCreative(PRIORITY_BUILD, ID_BUILD, ID_BUILD, "edit the terrain", true, true, true); }
	public static MPerm getPermPainbuild() { return getCreative(PRIORITY_PAINBUILD, ID_PAINBUILD, ID_PAINBUILD, "edit, take damage", true, true, true); }
	public static MPerm getPermDoor() { return getCreative(PRIORITY_DOOR, ID_DOOR, ID_DOOR, "use doors", true, true, true); }
	public static MPerm getPermButton() { return getCreative(PRIORITY_BUTTON, ID_BUTTON, ID_BUTTON, "use buttons", true, true, true); }
	public static MPerm getPermPressurePlate() { return getCreative(PRIORITY_PRESSURE_PLATE, ID_PRESSURE_PLATE, ID_PRESSURE_PLATE, "use pressure plates", true, true, true); }
	public static MPerm getPermLever() { return getCreative(PRIORITY_LEVER, ID_LEVER, ID_LEVER, "use levers", true, true, true); }
	public static MPerm getPermContainer() { return getCreative(PRIORITY_CONTAINER, ID_CONTAINER, ID_CONTAINER, "use containers", true, true, true); }
	public static MPerm getPermLeashMob() { return getCreative(PRIORITY_LEASH, ID_LEASH, ID_LEASH, "attach/detach leads", true, true, true); }

	public static MPerm getPermName() { return getCreative(PRIORITY_NAME, ID_NAME, ID_NAME, "set name", false, true, true); }
	public static MPerm getPermDesc() { return getCreative(PRIORITY_DESC, ID_DESC, ID_DESC, "set description", false, true, true); }
	public static MPerm getPermMotd() { return getCreative(PRIORITY_MOTD, ID_MOTD, ID_MOTD, "set motd", false, true, true); }
	public static MPerm getPermColor() { return getCreative(PRIORITY_COLOR, ID_COLOR, ID_COLOR, "set color", false, true, true); }
	public static MPerm getPermInvite() { return getCreative(PRIORITY_INVITE, ID_INVITE, ID_INVITE, "invite players", false, true, true); }
	public static MPerm getPermKick() { return getCreative(PRIORITY_KICK, ID_KICK, ID_KICK, "kick members", false, true, true); }
	public static MPerm getPermRank() { return getCreative(PRIORITY_RANK, ID_RANK, ID_RANK, "change ranks", false, true, true); }
	public static MPerm getPermTitle() { return getCreative(PRIORITY_TITLE, ID_TITLE, ID_TITLE, "set titles", false, true, true); }
	public static MPerm getPermWarp() { return getCreative(PRIORITY_WARP, ID_WARP, ID_WARP, "teleport to warp", false, true, true); }
	public static MPerm getPermSetwarp() { return getCreative(PRIORITY_SETWARP, ID_SETWARP, ID_SETWARP, "set warps", false, true, true); }
	public static MPerm getPermDeposit() { return getCreative(PRIORITY_DEPOSIT, ID_DEPOSIT, ID_DEPOSIT, "deposit money", false, false, false); } // non editable, non visible.
	public static MPerm getPermWithdraw() { return getCreative(PRIORITY_WITHDRAW, ID_WITHDRAW, ID_WITHDRAW, "withdraw money", false, true, true); }
	public static MPerm getPermTerritory() { return getCreative(PRIORITY_TERRITORY, ID_TERRITORY, ID_TERRITORY, "claim or unclaim", false, true, true); }
	public static MPerm getPermAccess() { return getCreative(PRIORITY_ACCESS, ID_ACCESS, ID_ACCESS, "grant territory", false, true, true); }
	public static MPerm getPermVote() { return getCreative(PRIORITY_VOTE, ID_VOTE, ID_VOTE, "vote", false, true, true); }
	public static MPerm getPermCreateVote() { return getCreative(PRIORITY_CREATEVOTE, ID_CREATEVOTE, ID_CREATEVOTE, "manage votes", false, true, true); }
	public static MPerm getPermClaimnear() { return getCreative(PRIORITY_CLAIMNEAR, ID_CLAIMNEAR, ID_CLAIMNEAR, "claim nearby", false, false, false); } // non editable, non visible.
	public static MPerm getPermTax() { return getCreative(PRIORITY_TAX, ID_TAX, ID_TAX, "set taxes", false, true, true); }
	public static MPerm getPermRel() { return getCreative(PRIORITY_REL, ID_REL, ID_REL, "change relations", false, true, true); }
	public static MPerm getPermDisband() { return getCreative(PRIORITY_DISBAND, ID_DISBAND, ID_DISBAND, "disband the faction", false, true, true); }
	public static MPerm getPermFlags() { return getCreative(PRIORITY_FLAGS, ID_FLAGS, ID_FLAGS, "manage flags", false, true, true); }
	public static MPerm getPermPerms() { return getCreative(PRIORITY_PERMS, ID_PERMS, ID_PERMS, "manage permissions", false, true, true); }
	
	public static MPerm getCreative(int priority, String id, String name, String desc, boolean territory, boolean editable, boolean visible)
	{
		MPerm ret = MPermColl.get().get(id, false);
		if (ret != null)
		{
			ret.setRegistered(true);
			return ret;
		}
		
		ret = new MPerm(priority, name, desc, territory, editable, visible);
		MPermColl.get().attach(ret, id);
		ret.setRegistered(true);
		ret.sync();
		
		return ret;
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public MPerm load(MPerm that)
	{
		this.priority = that.priority;
		this.name = that.name;
		this.desc = that.desc;
		this.territory = that.territory;
		this.editable = that.editable;
		this.visible = that.visible;

		return this;
	}
	
	// -------------------------------------------- //
	// TRANSIENT FIELDS (Registered)
	// -------------------------------------------- //
	
	private transient boolean registered = false;
	public boolean isRegistered() { return this.registered; }
	public void setRegistered(boolean registered) { this.registered = registered; }

	// -------------------------------------------- //
	// VERSION
	// -------------------------------------------- //

	public int version = 3;

	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	// The sort priority. Low values appear first in sorted lists.
	// 1 is high up, 99999 is far down.
	// Standard Faction perms use "thousand values" like 1000, 2000, 3000 etc to allow adding new perms inbetween.
	// So 1000 might sound like a lot but it's actually the priority for the first perm.
	private int priority = 0;
	@Override public int getPriority() { return this.priority; }
	public MPerm setPriority(int priority) { this.priority = priority; this.changed(); return this; }
	
	// The name of the perm. According to standard it should be fully lowercase just like the perm id.
	// In fact the name and the id of all standard perms are the same.
	// I just added the name in case anyone feel like renaming their perms for some reason.
	// Example: "build"
	private String name = "defaultName";
	@Override public String getName() { return this.name; }
	public MPerm setName(String name) { this.name = name; this.changed(); return this; }
	
	// The perm function described as an "order".
	// The desc should match the format:
	// "You are not allowed to X."
	// "You are not allowed to edit the terrain."
	// Example: "edit the terrain"
	private String desc = "defaultDesc";
	public String getDesc() { return this.desc; }
	public MPerm setDesc(String desc) { this.desc = desc; this.changed(); return this; }
	
	// What is the standard (aka default) perm value?
	// This value will be set for factions from the beginning.
	// Example: ... set of relations ...
	//@Deprecated
	//private Set<String> standard = null;
	//@Deprecated public Set<String> getStandard() { return this.standard; }
	//@Deprecated public MPerm setStandard(Set<String> standard) { this.standard = standard; this.changed(); return this; }
	
	// Is this a territory perm meaning it has to do with territory construction, modification or interaction?
	// True Examples: build, container, door, lever etc.
	// False Examples: name, invite, home, sethome, deposit, withdraw etc.
	private boolean territory = false;
	public boolean isTerritory() { return this.territory; }
	public MPerm setTerritory(boolean territory) { this.territory = territory; this.changed(); return this; }
	
	// Is this perm editable by players?
	// With this we mean standard non administrator players.
	// All perms can be changed using /f override.
	// Example: true (all perms are editable by default)
	private boolean editable = false;
	public boolean isEditable() { return this.editable; }
	public MPerm setEditable(boolean editable) { this.editable = editable; this.changed(); return this; }
	
	// Is this perm visible to players?
	// With this we mean standard non administrator players.
	// All perms can be seen using /f override.
	// Some perms can be rendered meaningless by settings in Factions or external plugins.
	// Say we set "editable" to false.
	// In such case we might want to hide the perm by setting "visible" false.
	// If it can't be changed, why bother showing it?
	// Example: true (yeah we need to see this permission)
	private boolean visible = true;
	public boolean isVisible() { return this.visible; }
	public MPerm setVisible(boolean visible) { this.visible = visible; this.changed(); return this; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public MPerm()
	{
		// No argument constructor for GSON
	}
	
	public MPerm(int priority, String name, String desc, boolean territory, boolean editable, boolean visible)
	{
		this.priority = priority;
		this.name = name;
		this.desc = desc;
		this.territory = territory;
		this.editable = editable;
		this.visible = visible;
	}
	
	// -------------------------------------------- //
	// EXTRAS
	// -------------------------------------------- //
	
	public String createDeniedMessage(MPlayer mplayer, Faction hostFaction)
	{
		// Null Check
		if (mplayer == null) throw new NullPointerException("mplayer");
		if (hostFaction == null) throw new NullPointerException("hostFaction");
		
		String ret = Txt.parse("%s<b> does not allow you to %s<b>.", hostFaction.describeTo(mplayer, true), this.getDesc());
		
		Player player = mplayer.getPlayer();
		if (player != null && Perm.OVERRIDE.has(player))
		{
			ret += Txt.parse("\n<i>You can bypass by using " + CmdFactions.get().cmdFactionsOverride.getTemplate(false).toPlain(true));
		}
		
		return ret;
	}
	
	public String getDesc(boolean withName, boolean withDesc)
	{
		List<String> parts = new ArrayList<>();
		
		if (withName)
		{
			String nameFormat;
			if ( ! this.isVisible())
			{
				nameFormat = "<silver>%s";
			}
			else if (this.isEditable())
			{
				nameFormat = "<pink>%s";
			}
			else
			{
				nameFormat = "<aqua>%s";
			}
			String name = this.getName();
			String nameDesc = Txt.parse(nameFormat, name);
			parts.add(nameDesc);
		}
		
		if (withDesc)
		{
			String desc = this.getDesc();
			
			String descDesc = Txt.parse("<i>%s", desc);
			parts.add(descDesc);
		}
		
		return Txt.implode(parts, " ");
	}
	
	public boolean has(Faction faction, Faction hostFaction)
	{
		// Null Check
		if (faction == null) throw new NullPointerException("faction");
		if (hostFaction == null) throw new NullPointerException("hostFaction");

		return hostFaction.isFactionPermitted(faction, this);
	}
	
	public boolean has(MPlayer mplayer, Faction hostFaction, boolean verboose)
	{
		// Null Check
		if (mplayer == null) throw new NullPointerException("mplayer");
		if (hostFaction == null) throw new NullPointerException("hostFaction");
		
		if (mplayer.isOverriding()) return true;
		
		if (hostFaction.isPlayerPermitted(mplayer, this)) return true;
		
		if (verboose) mplayer.message(this.createDeniedMessage(mplayer, hostFaction));
		
		return false;
	}
	
	public boolean has(MPlayer mplayer, PS ps, boolean verboose)
	{
		// Null Check
		if (mplayer == null) throw new NullPointerException("mplayer");
		if (ps == null) throw new NullPointerException("ps");
		
		if (mplayer.isOverriding()) return true;
		
		TerritoryAccess ta = BoardColl.get().getTerritoryAccessAt(ps);
		Faction hostFaction = ta.getHostFaction();
		
		if (this.isTerritory())
		{
			AccessStatus accessStatus = ta.getTerritoryAccess(mplayer);
			if (accessStatus != AccessStatus.STANDARD)
			{
				if (verboose && accessStatus == AccessStatus.DECREASED)
				{
					mplayer.message(this.createDeniedMessage(mplayer, hostFaction));
				}
				
				return accessStatus.hasAccess();
			}
		}

		return this.has(mplayer, hostFaction, verboose);
	}

	// -------------------------------------------- //
	// PERMABLES
	// -------------------------------------------- //

	public static List<MPermable> getPermables(Faction faction)
	{
		if (faction == null) throw new NullPointerException("faction");

		List<MPermable> list = new MassiveList<>();

		list.addAll(Arrays.asList(Rel.values()));
		list.remove(Rel.FACTION);

		list.addAll(faction.getRanks().getAll(Comparator.comparingInt(Rank::getPriority)));

		return list;
	}

	public interface MPermable extends Named, Identified
	{
		default boolean isRelation()
		{
			return this instanceof Rel;
		}

		default boolean isRank()
		{
			return this instanceof Rank;
		}

		default String getShortName()
		{
			return this.getName().substring(0, 3).toUpperCase();
		}

		String getDisplayName(Object senderObject);
	}

	public static Set<MPermable> idsToMPermables(Collection<String> ids)
	{
		return ids.stream()
			.map(MPerm::idToMPermable)
			.collect(Collectors.toSet());
	}

	public static MPermable idToMPermable(String id)
	{
		return idToMPermableOptional(id).orElseThrow(() -> new RuntimeException(id));
	}

	public static Optional<MPermable> idToMPermableOptional(String id)
	{
		MPlayer mplayer = MPlayerColl.get().get(id, false);
		if (mplayer != null) return Optional.of(mplayer);

		// Workaround for registered senders
		// Players ussually have a power, which makes sure they are in the coll
		if (IdUtil.getRegistryIdToSender().containsKey(id)) return Optional.of(MPlayerColl.get().get(id, true));

		Faction faction = Faction.get(id);
		if (faction != null) return Optional.of(faction);

		for (Faction f : FactionColl.get().getAll())
		{
			Rank rank = f.getRank(id);
			if (rank != null) return Optional.of(rank);
		}

		if (Rel.ALLY.name().equalsIgnoreCase(id)) return Optional.of(Rel.ALLY);
		if (Rel.TRUCE.name().equalsIgnoreCase(id)) return Optional.of(Rel.TRUCE);
		if (Rel.NEUTRAL.name().equalsIgnoreCase(id)) return Optional.of(Rel.NEUTRAL);
		if (Rel.ENEMY.name().equalsIgnoreCase(id)) return Optional.of(Rel.ENEMY);

		return Optional.empty();
	}
	
}
