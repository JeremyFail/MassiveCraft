package com.massivecraft.factions.entity;


import com.massivecraft.factions.Factions;
import com.massivecraft.factions.FactionsIndex;
import com.massivecraft.factions.FactionsParticipator;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.RelationParticipator;
import com.massivecraft.factions.entity.MPerm.MPermable;
import com.massivecraft.factions.predicate.PredicateCommandSenderFaction;
import com.massivecraft.factions.predicate.PredicateMPlayerRank;
import com.massivecraft.factions.util.MiscUtil;
import com.massivecraft.factions.util.RelationUtil;
import com.massivecraft.massivecore.Couple;
import com.massivecraft.massivecore.Identified;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.collections.MassiveMapDef;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.predicate.PredicateAnd;
import com.massivecraft.massivecore.predicate.PredicateVisibleTo;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.store.Entity;
import com.massivecraft.massivecore.store.EntityInternalMap;
import com.massivecraft.massivecore.store.SenderColl;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Faction extends Entity<Faction> implements FactionsParticipator, MPerm.MPermable
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //
	
	public static final transient String NODESCRIPTION = Txt.parse("<em><silver>no description set");
	public static final transient String NOMOTD = Txt.parse("<em><silver>no message of the day set");
	
	// -------------------------------------------- //
	// META
	// -------------------------------------------- //
	
	public static Faction get(Object oid)
	{
		return FactionColl.get().get(oid);
	}
	
	// -------------------------------------------- //
	// OVERRIDE: ENTITY
	// -------------------------------------------- //
	
	@Override
	public Faction load(Faction that)
	{
		this.setName(that.name);
		this.setDescription(that.description);
		this.setMotd(that.motd);
		this.setCreatedAtMillis(that.createdAtMillis);
		this.warps.load(that.warps);
		this.setPowerBoost(that.powerBoost);
		this.money = that.money;
		this.invitations.load(that.invitations);
		this.ranks.load(that.ranks);
		this.votes.load(that.votes);
		this.setRelationWishes(that.relationWishes);
		this.setFlagIds(that.flags);
		this.perms = that.perms;
		this.tax = that.tax;
		
		return this;
	}
	
	@Override
	public void preDetach(String id)
	{
		if (!this.isLive()) return;
		
		// NOTE: Existence check is required for compatibility with some plugins.
		// If they have money ...
		if (Money.exists(this))
		{
			// ... remove it.
			Money.set(this, null, 0, "Factions");
		}
	}
	
	// -------------------------------------------- //
	// VERSION
	// -------------------------------------------- //
	
	public int version = 4;
	
	// -------------------------------------------- //
	// FIELDS: RAW
	// -------------------------------------------- //
	// In this section of the source code we place the field declarations only.
	// Each field has it's own section further down since just the getter and setter logic takes up quite some place.
	
	// The actual faction id looks something like "54947df8-0e9e-4471-a2f9-9af509fb5889" and that is not too easy to remember for humans.
	// Thus we make use of a name. Since the id is used in all foreign key situations changing the name is fine.
	// Null should never happen. The name must not be null.
	private String name = null;
	
	// Factions can optionally set a description for themselves.
	// This description can for example be seen in territorial alerts.
	// Null means the faction has no description.
	private String description = null;
	
	// Factions can optionally set a message of the day.
	// This message will be shown when logging on to the server.
	// Null means the faction has no motd
	private String motd = null;
	
	// We store the creation date for the faction.
	// It can be displayed on info pages etc.
	private long createdAtMillis = System.currentTimeMillis();

	// Factions can set a few significant locations (warps)
	private EntityInternalMap<Warp> warps = new EntityInternalMap<>(this, Warp.class);
	
	// Factions usually do not have a powerboost. It defaults to 0.
	// The powerBoost is a custom increase/decrease to default and maximum power.
	// Null means the faction has powerBoost (0).
	private Double powerBoost = null;

	// The money a Faction has
	// null means 0.0
	private Double money = null;
	
	// This is the ids of the invited players.
	// They are actually "senderIds" since you can invite "@console" to your faction.
	private EntityInternalMap<Invitation> invitations = new EntityInternalMap<>(this, Invitation.class);

	// This is where all the ranks are, they are faction specific
	private EntityInternalMap<Rank> ranks = this.createRankMap();

	// This is the votes currently open in the faction
	private EntityInternalMap<Vote> votes = new EntityInternalMap<>(this, Vote.class);

	// The keys in this map are factionIds.
	// Null means no special relation whishes.
	private MassiveMapDef<String, Rel> relationWishes = new MassiveMapDef<>();
	
	// The flag overrides are modifications to the default values.
	// Null means default.
	private MassiveMapDef<String, Boolean> flags = new MassiveMapDef<>();

	private Map<String, Set<String>> perms = this.createNewPermMap();

	// What is the base tax on members of the faction?
	// Specific taxes on ranks or players.
	public static transient String IDENTIFIER_TAX_BASE = "base";

	private Map<String, Double> tax = new MassiveMap<>();
	
	// -------------------------------------------- //
	// FIELD: id
	// -------------------------------------------- //
	
	// FINER
	
	public boolean isNone()
	{
		return this.getId().equals(Factions.ID_NONE);
	}
	
	public boolean isNormal()
	{
		return ! this.isNone();
	}
	
	// -------------------------------------------- //
	// FIELD: name
	// -------------------------------------------- //
	
	// RAW
	
	@Override
	public String getName()
	{
		return this.name;
	}
	
	public void setName(String name)
	{
		// Clean input
		String target = name;
		
		// Detect Nochange
		if (MUtil.equals(this.name, target)) return;

		// Apply
		this.name = target;
		
		// Mark as changed
		this.changed();
	}
	
	// FINER
	
	public String getComparisonName()
	{
		return MiscUtil.getComparisonString(this.getName());
	}
	
	public String getName(String prefix)
	{
		return prefix + this.getName();
	}
	
	public String getName(RelationParticipator observer)
	{
		if (observer == null) return getName();
		return this.getName(this.getColorTo(observer).toString());
	}
	
	// -------------------------------------------- //
	// FIELD: description
	// -------------------------------------------- //
	
	// RAW
	
	public boolean hasDescription()
	{
		return this.description != null;
	}
	
	public String getDescription()
	{
		return this.description;
	}
	
	public void setDescription(String description)
	{
		// Clean input
		String target = clean(description);
		
		// Detect Nochange
		if (MUtil.equals(this.description, target)) return;

		// Apply
		this.description = target;
		
		// Mark as changed
		this.changed();
	}
	
	// FINER
	
	public String getDescriptionDesc()
	{
		String motd = this.getDescription();
		if (motd == null) motd = NODESCRIPTION;
		return motd;
	}
	
	// -------------------------------------------- //
	// FIELD: motd
	// -------------------------------------------- //
	
	// RAW
	
	public boolean hasMotd()
	{
		return this.motd != null;
	}
	
	public String getMotd()
	{
		return this.motd;
	}
	
	public void setMotd(String motd)
	{
		// Clean input
		String target = clean(motd);
		
		// Detect Nochange
		if (MUtil.equals(this.motd, target)) return;

		// Apply
		this.motd = target;
		
		// Mark as changed
		this.changed();
	}
	
	// FINER
	
	public String getMotdDesc()
	{
		return getMotdDesc(this.getMotd());
	}
	
	private static String getMotdDesc(String motd)
	{
		if (motd == null) motd = NOMOTD;
		return motd;
	}
	
	public List<Object> getMotdMessages()
	{
		// Create
		List<Object> ret = new MassiveList<>();
		
		// Fill
		Object title = this.getName() + " - Message of the Day";
		title = Txt.titleize(title);
		ret.add(title);
		
		String motd = this.getMotdDesc();
		List<String> motds = Arrays.asList(motd.split("\\\\n"));
		motds = motds.stream().map(s -> Txt.parse("<i>") + s).collect(Collectors.toList());
		ret.addAll(motds);
		
		ret.add("");
		
		// Return
		return ret;
	}
	
	// -------------------------------------------- //
	// FIELD: createdAtMillis
	// -------------------------------------------- //
	
	public long getCreatedAtMillis()
	{
		return this.createdAtMillis;
	}
	
	public void setCreatedAtMillis(long createdAtMillis)
	{
		// Detect Nochange
		if (MUtil.equals(this.createdAtMillis, createdAtMillis)) return;

		// Apply
		this.createdAtMillis = createdAtMillis;
		
		// Mark as changed
		this.changed();
	}
	
	public long getAge()
	{
		return this.getAge(System.currentTimeMillis());
	}
	
	public long getAge(long now)
	{
		return now - this.getCreatedAtMillis();
	}
	
	// -------------------------------------------- //
	// FIELD: warp
	// -------------------------------------------- //

	public EntityInternalMap<Warp> getWarps() { return this.warps; }

	public Warp getWarp(Object oid)
	{
		if (oid == null) throw new NullPointerException("oid");
		Warp warp = this.getWarps().get(oid);
		if (warp == null) return null;

		if (!warp.verifyIsValid()) return null;
		return warp;
	}

	public PS getWarpPS(Object oid)
	{
		if (oid == null) throw new NullPointerException("oid");
		Warp warp = this.getWarp(oid);
		if (warp == null) return null;
		return warp.getLocation();
	}
	
	public String addWarp(Warp warp)
	{
		return this.getWarps().attach(warp);
	}

	public Warp removeWarp(Warp warp)
	{
		return this.getWarps().detachEntity(warp);
	}
	
	// -------------------------------------------- //
	// FIELD: powerBoost
	// -------------------------------------------- //
	
	// RAW
	@Override
	public double getPowerBoost()
	{
		Double ret = this.powerBoost;
		if (ret == null) ret = 0D;
		return ret;
	}
	
	@Override
	public void setPowerBoost(Double powerBoost)
	{
		// Clean input
		Double target = powerBoost;
		
		if (target == null || target == 0) target = null;
		
		// Detect Nochange
		if (MUtil.equals(this.powerBoost, target)) return;
		
		// Apply
		this.powerBoost = target;
		
		// Mark as changed
		this.changed();
	}

	// -------------------------------------------- //
	// FIELD: money
	// -------------------------------------------- //

	public double getMoney()
	{
		if (!MConf.get().econEnabled) throw new UnsupportedOperationException("econ not enabled");
		if (!MConf.get().bankEnabled) throw new UnsupportedOperationException("bank not enabled");
		if (!MConf.get().useNewMoneySystem) throw new UnsupportedOperationException("this server does not use the new econ system");

		return this.convertGet(this.money, 0D);
	}

	public void setMoney(Double money)
	{
		if (!MConf.get().econEnabled) throw new UnsupportedOperationException("econ not enabled");
		if (!MConf.get().bankEnabled) throw new UnsupportedOperationException("bank not enabled");
		if (!MConf.get().useNewMoneySystem) throw new UnsupportedOperationException("this server does not use the new econ system");
		
		this.money = this.convertSet(money, this.money, 0D);
	}
	
	// -------------------------------------------- //
	// FIELD: invitations
	// -------------------------------------------- //
	
	// RAW
	
	public EntityInternalMap<Invitation> getInvitations() { return this.invitations; }
	
	// FINER
	
	public boolean isInvited(String playerId)
	{
		return this.getInvitations().containsKey(playerId);
	}
	
	public boolean isInvited(MPlayer mplayer)
	{
		return this.isInvited(mplayer.getId());
	}
	
	public boolean uninvite(String playerId)
	{
		return this.getInvitations().detachId(playerId) != null;
	}
	
	public boolean uninvite(MPlayer mplayer)
	{
		return uninvite(mplayer.getId());
	}
	
	public void invite(String playerId, Invitation invitation)
	{
		uninvite(playerId);
		this.invitations.attach(invitation, playerId);
	}

	// -------------------------------------------- //
	// FIELD: ranks
	// -------------------------------------------- //

	// RAW

	public EntityInternalMap<Rank> getRanks() { return this.ranks; }

	// FINER

	public boolean hasRank(Rank rank)
	{
		return this.getRanks().containsKey(rank.getId());
	}

	public Rank getRank(String rankId)
	{
		if (rankId == null) throw new NullPointerException("rankId");
		return this.getRanks().getFixed(rankId);
	}

	private EntityInternalMap<Rank> createRankMap()
	{
		EntityInternalMap<Rank> ret = new EntityInternalMap<>(this, Rank.class);

		MConf.get().defaultRanks.stream()
			.map(Rank::copy)
			.forEach(ret::attach);

		return ret;
	}

	public Rank getLeaderRank()
	{
		Rank ret = null;
		for (Rank rank : this.getRanks().getAll())
		{
			if (ret != null && ret.isMoreThan(rank)) continue;

			ret = rank;
		}
		return ret;
	}

	public Rank getLowestRank()
	{
		Rank ret = null;
		for (Rank rank : this.getRanks().getAll())
		{
			if (ret != null && ret.isLessThan(rank)) continue;

			ret = rank;
		}
		return ret;
	}

	// -------------------------------------------- //
	// FIELD: votes
	// -------------------------------------------- //

	// RAW

	public EntityInternalMap<Vote> getVotes() { return this.votes; }

	public void addVote(Vote vote)
	{
		if (vote == null) throw new NullPointerException("vote");
		this.getVotes().attach(vote);
	}

	public Optional<Vote> getVoteByName(String name)
	{
		if (name == null) throw new NullPointerException("name");
		return this.getVotes().getAll().stream().filter(vote -> vote.getName().equalsIgnoreCase(name)).findFirst();
	}

	// -------------------------------------------- //
	// FIELD: relationWish
	// -------------------------------------------- //
	
	// RAW
	
	public Map<String, Rel> getRelationWishes()
	{
		return this.relationWishes;
	}
	
	public void setRelationWishes(Map<String, Rel> relationWishes)
	{
		// Clean input
		MassiveMapDef<String, Rel> target = new MassiveMapDef<>(relationWishes);
		
		// Detect Nochange
		if (MUtil.equals(this.relationWishes, target)) return;
		
		// Apply
		this.relationWishes = target;
		
		// Mark as changed
		this.changed();
	}
	
	// FINER
	
	public Rel getRelationWish(String factionId)
	{
		Rel ret = this.getRelationWishes().get(factionId);
		if (ret == null) ret = Rel.NEUTRAL;
		return ret;
	}
	
	public Rel getRelationWish(Faction faction)
	{
		return this.getRelationWish(faction.getId());
	}
	
	public void setRelationWish(String factionId, Rel rel)
	{
		Map<String, Rel> relationWishes = this.getRelationWishes();
		if (rel == null || rel == Rel.NEUTRAL)
		{
			relationWishes.remove(factionId);
		}
		else
		{
			relationWishes.put(factionId, rel);
		}
		this.setRelationWishes(relationWishes);
	}
	
	public void setRelationWish(Faction faction, Rel rel)
	{
		this.setRelationWish(faction.getId(), rel);
	}
	
	// -------------------------------------------- //
	// FIELD: flagOverrides
	// -------------------------------------------- //
	
	// RAW
	
	public Map<MFlag, Boolean> getFlags()
	{
		// We start with default values ...
		Map<MFlag, Boolean> ret = new MassiveMap<>();
		for (MFlag mflag : MFlag.getAll())
		{
			ret.put(mflag, mflag.isStandard());
		}
		
		// ... and if anything is explicitly set we use that info ...
		Iterator<Map.Entry<String, Boolean>> iter = this.flags.entrySet().iterator();
		while (iter.hasNext())
		{
			// ... for each entry ...
			Map.Entry<String, Boolean> entry = iter.next();
			
			// ... extract id and remove null values ...
			String id = entry.getKey();					
			if (id == null)
			{
				iter.remove();
				this.changed();
				continue;
			}
			
			// ... resolve object and skip unknowns ...
			MFlag mflag = MFlag.get(id);
			if (mflag == null) continue;
			
			ret.put(mflag, entry.getValue());
		}
		
		return ret;
	}
	
	public void setFlags(Map<MFlag, Boolean> flags)
	{
		Map<String, Boolean> flagIds = new MassiveMap<>();
		for (Map.Entry<MFlag, Boolean> entry : flags.entrySet())
		{
			flagIds.put(entry.getKey().getId(), entry.getValue());
		}
		setFlagIds(flagIds);
	}
	
	public void setFlagIds(Map<String, Boolean> flagIds)
	{
		// Clean input
		MassiveMapDef<String, Boolean> target = new MassiveMapDef<>();
		for (Map.Entry<String, Boolean> entry : flagIds.entrySet())
		{
			String key = entry.getKey();
			if (key == null) continue;
			key = key.toLowerCase(); // Lowercased Keys Version 2.6.0 --> 2.7.0
			
			Boolean value = entry.getValue();
			if (value == null) continue;
			
			target.put(key, value);
		}

		// Detect Nochange
		if (MUtil.equals(this.flags, target)) return;
		
		// Apply
		this.flags = new MassiveMapDef<>(target);
		
		// Mark as changed
		this.changed();
	}
	
	// FINER
	
	public boolean getFlag(String flagId)
	{
		if (flagId == null) throw new NullPointerException("flagId");
		
		Boolean ret = this.flags.get(flagId);
		if (ret != null) return ret;
		
		MFlag flag = MFlag.get(flagId);
		if (flag == null) throw new NullPointerException("flag");
		
		return flag.isStandard();
	}
	
	public boolean getFlag(MFlag flag)
	{
		if (flag == null) throw new NullPointerException("flag");
		
		String flagId = flag.getId();
		if (flagId == null) throw new NullPointerException("flagId");
		
		Boolean ret = this.flags.get(flagId);
		if (ret != null) return ret;
		
		return flag.isStandard();
	}
	
	public Boolean setFlag(String flagId, boolean value)
	{
		if (flagId == null) throw new NullPointerException("flagId");
		
		Boolean ret = this.flags.put(flagId, value);
		if (ret == null || ret != value) this.changed();
		return ret;
	}
	
	public Boolean setFlag(MFlag flag, boolean value)
	{
		if (flag == null) throw new NullPointerException("flag");
		
		String flagId = flag.getId();
		if (flagId == null) throw new NullPointerException("flagId");
		
		Boolean ret = this.flags.put(flagId, value);
		if (ret == null || ret != value) this.changed();
		return ret;
	}
	
	// -------------------------------------------- //
	// FIELD: perms
	// -------------------------------------------- //

	public Map<String, Set<String>> getPerms()
	{
		return this.perms;
	}

	public Map<String, Set<String>> createNewPermMap()
	{
		Map<String, Set<String>> ret = new MassiveMap<>();
		for (MPerm mperm : MPerm.getAll())
		{
			Set<String> granteds = mperm2granteds(mperm);

			ret.put(mperm.getId(), granteds);
		}
		return ret;
	}

	private Set<String> mperm2granteds(MPerm mperm)
	{
		String permId = mperm.getId();
		Set<String> value = new MassiveSet<>(MConf.get().perm2default.get(permId));
		Set<String> additions = new MassiveSet<>();
		outer:
		for (Iterator<String> it = value.iterator(); it.hasNext();)
		{
			String granted = it.next();
			for (Rank rank : this.getRanks().getAll())
			{
				if (granted.equalsIgnoreCase(rank.getName()))
				{
					it.remove();
					additions.add(rank.getId());
					continue outer;
				}
			}
		}
		value.addAll(additions);
		return value;
	}

	// IS PERMITTED

	public boolean isPlayerPermitted(MPlayer mplayer, String permId)
	{
		if (isPermitted(mplayer.getId(), permId)) return true;
		if (isPermitted(mplayer.getFaction().getId(), permId)) return true;
		if (isPermitted(mplayer.getRank().getId(), permId)) return true;
		if (isPermitted(RelationUtil.getRelationOfThatToMe(mplayer, this).toString(), permId)) return true;

		return false;
	}

	public boolean isPlayerPermitted(MPlayer mplayer, MPerm mperm)
	{
		return isPlayerPermitted(mplayer, mperm.getId());
	}

	public boolean isFactionPermitted(Faction faction, String permId)
	{
		if (isPermitted(faction.getId(), permId)) return true;
		if (isPermitted(RelationUtil.getRelationOfThatToMe(faction, this).toString(), permId)) return true;

		return false;
	}

	public boolean isFactionPermitted(Faction faction, MPerm mperm)
	{
		return isFactionPermitted(faction, mperm.getId());
	}

	public Set<String> getPermitted(String permId)
	{
		if (permId == null) throw new NullPointerException("permId");
		Set<String> permables = this.perms.get(permId);

		if (permables == null)
		{
			// No perms was found, but likely this is just a new MPerm.
			// So if this does not exist in the database, throw an error.
			if (!doesPermExist(permId)) throw new NullPointerException(permId + " caused null");

			permables = new MassiveSet<>();
			this.perms.put(permId, permables);
		}

		return permables;
	}

	public Set<String> getPermitted(MPerm mperm)
	{
		return getPermitted(mperm.getId());
	}

	public Set<MPermable> getPermittedPermables(String permId)
	{
		return MPerm.idsToMPermables(getPermitted(permId));
	}

	public Set<MPermable> getPermittedPermables(MPerm mperm)
	{
		return getPermittedPermables(mperm.getId());
	}

	public boolean isPermitted(String permableId, String permId)
	{
		if (permableId == null) throw new NullPointerException("permableId");
		if (permId == null) throw new NullPointerException("permId");

		// TODO: Isn't this section redundant and just a copy of that from getPermitted?
		Set<String> permables = this.perms.get(permId);
		if (permables == null)
		{
			// No perms was found, but likely this is just a new MPerm.
			// So if this does not exist in the database, throw an error.
			if (!doesPermExist(permId)) throw new NullPointerException(permId + " caused null");

			// Otherwise handle it
			return false;
		}

		return getPermitted(permId).contains(permableId);
	}

	// SET PERMITTED

	public boolean setPermitted(MPerm.MPermable mpermable, String permId, boolean permitted)
	{
		boolean changed = false;

		Set<String> perms = this.perms.get(permId);
		if (perms == null)
		{
			// No perms was found, but likely this is just a new MPerm.
			// So if this does not exist in the database, throw an error.
			if (!doesPermExist(permId)) throw new NullPointerException(permId + " caused null");

			// Otherwise handle it
			Set<String> permables = new MassiveSet<>();
			this.perms.put(permId, permables);
			changed = true;
		}

		if (permitted)
		{
			changed = this.getPerms().get(permId).add(mpermable.getId()) | changed;
		}
		else
		{
			changed = this.getPerms().get(permId).remove(mpermable.getId()) | changed;
		}
		if (changed) this.changed();
		return changed;
	}

	public boolean setPermitted(MPerm.MPermable mpermable, MPerm mperm, boolean permitted)
	{
		return setPermitted(mpermable, mperm.getId(), permitted);
	}

	public void setPermittedRelations(String permId, Collection<MPerm.MPermable> permables)
	{
		Set<String> ids = permables.stream().map(MPerm.MPermable::getId).collect(Collectors.toSet());
		this.getPerms().put(permId, ids);
	}

	public void setPermittedRelations(MPerm perm, Collection<MPerm.MPermable> permables)
	{
		setPermittedRelations(perm.getId(), permables);
	}


	private boolean doesPermExist(String permId)
	{
		return MPermColl.get().getFixed(permId) != null;
	}

	// -------------------------------------------- //
	// FIELD: tax
	// -------------------------------------------- //

	// RAW
	public Map<String, Double> getTax() { return this.tax; }

	 // FINER GET
	public Double getTaxFor(String id)
	{
		if (id == null) throw new NullPointerException("id");
		return this.tax.get(id);
	}

	public Double getTaxFor(Identified identified)
	{
		if (identified == null) throw new NullPointerException("identified");
		return this.getTaxFor(identified.getId());
	}

	public double getTaxForPlayer(MPlayer mplayer)
	{
		return getTaxAndReasonForPlayer(mplayer).map(Entry::getValue).orElse(0D);
	}

	public Optional<Entry<String, Double>> getTaxAndReasonForPlayer(MPlayer mplayer)
	{
		if (mplayer == null) throw new NullPointerException("mplayer");

		if (mplayer.getFaction() != this) throw new IllegalArgumentException("Player " + mplayer.getId() + " not in " + this.getId());

		Double ret = null;

		ret = this.getTaxFor(mplayer);
		if (ret != null) return Optional.of(new Couple<>(mplayer.getId(), ret));

		ret = this.getTaxFor(mplayer.getRank());
		if (ret != null) return Optional.of(new Couple<>(mplayer.getRank().getId(), ret));

		ret = this.getTaxFor(IDENTIFIER_TAX_BASE);
		if (ret != null) return Optional.of(new Couple<>(IDENTIFIER_TAX_BASE, ret));

		return Optional.empty();
	}

	// FINER SET
	public Double setTaxFor(String id, Double value)
	{
		this.changed();
		return this.tax.put(id, value);
	}

	public Double setTaxFor(Identified identified, Double value)
	{
		return this.setTaxFor(identified.getId(), value);
	}

	// -------------------------------------------- //
	// OVERRIDE: RelationParticipator
	// -------------------------------------------- //
	
	@Override
	public String describeTo(RelationParticipator observer, boolean ucfirst)
	{
		return RelationUtil.describeThatToMe(this, observer, ucfirst);
	}
	
	@Override
	public String describeTo(RelationParticipator observer)
	{
		return RelationUtil.describeThatToMe(this, observer);
	}
	
	@Override
	public Rel getRelationTo(RelationParticipator observer)
	{
		return RelationUtil.getRelationOfThatToMe(this, observer);
	}
	
	@Override
	public Rel getRelationTo(RelationParticipator observer, boolean ignorePeaceful)
	{
		return RelationUtil.getRelationOfThatToMe(this, observer, ignorePeaceful);
	}
	
	@Override
	public ChatColor getColorTo(RelationParticipator observer)
	{
		return RelationUtil.getColorOfThatToMe(this, observer);
	}

	// -------------------------------------------- //
	// OVERRIDE: permable
	// -------------------------------------------- //

	@Override
	public String getDisplayName(Object senderObject)
	{
		MPlayer mplayer = MPlayer.get(senderObject);
		if (mplayer == null) return this.getName();

		return this.describeTo(mplayer);
	}

	// -------------------------------------------- //
	// POWER
	// -------------------------------------------- //
	// TODO: Implement a has enough feature.
	
	public double getPower()
	{
		if (this.getFlag(MFlag.getFlagInfpower())) return 999999;
		
		double ret = 0;
		for (MPlayer mplayer : this.getMPlayers())
		{
			ret += mplayer.getPower();
		}
		
		ret = this.limitWithPowerMax(ret);
		ret += this.getPowerBoost();
		
		return ret;
	}
	
	public double getPowerMax()
	{
		if (this.getFlag(MFlag.getFlagInfpower())) return 999999;
	
		double ret = 0;
		for (MPlayer mplayer : this.getMPlayers())
		{
			ret += mplayer.getPowerMax();
		}
		
		ret = this.limitWithPowerMax(ret);
		ret += this.getPowerBoost();
		
		return ret;
	}
	
	private double limitWithPowerMax(double power)
	{
		// NOTE: 0.0 powerMax means there is no max power
		double powerMax = MConf.get().factionPowerMax;
		
		return powerMax <= 0 || power < powerMax ? power : powerMax;
	}
	
	public int getPowerRounded()
	{
		return (int) Math.round(this.getPower());
	}
	
	public int getPowerMaxRounded()
	{
		return (int) Math.round(this.getPowerMax());
	}
	
	public int getLandCount()
	{
		return BoardColl.get().getCount(this);
	}
	public int getLandCountInWorld(String worldName)
	{
		return Board.get(worldName).getCount(this);
	}
	
	public boolean hasLandInflation()
	{
		return this.getLandCount() > this.getPowerRounded();
	}
	
	// -------------------------------------------- //
	// WORLDS
	// -------------------------------------------- //
	
	public Set<String> getClaimedWorlds()
	{
		return BoardColl.get().getClaimedWorlds(this);
	}
	
	// -------------------------------------------- //
	// FOREIGN KEY: MPLAYER
	// -------------------------------------------- //
	
	public List<MPlayer> getMPlayers()
	{
		return new MassiveList<>(FactionsIndex.get().getMPlayers(this));
	}
	
	public List<MPlayer> getMPlayers(java.util.function.Predicate<? super MPlayer> where, Comparator<? super MPlayer> orderby, Integer limit, Integer offset)
	{
		return MUtil.transform(this.getMPlayers(), where, orderby, limit, offset);
	}
	
	public List<MPlayer> getMPlayersWhere(java.util.function.Predicate<? super MPlayer> predicate)
	{
		return this.getMPlayers(predicate, null, null, null);
	}
	
	public List<MPlayer> getMPlayersWhereOnline(boolean online)
	{
		return this.getMPlayersWhere(online ? SenderColl.PREDICATE_ONLINE : SenderColl.PREDICATE_OFFLINE);
	}

	public List<MPlayer> getMPlayersWhereOnlineTo(Object senderObject)
	{
		return this.getMPlayersWhere(PredicateAnd.get(SenderColl.PREDICATE_ONLINE, PredicateVisibleTo.get(senderObject)));
	}
	
	public List<MPlayer> getMPlayersWhereRank(Rank rank)
	{
		return this.getMPlayersWhere(PredicateMPlayerRank.get(rank));
	}
	
	public MPlayer getLeader()
	{
		List<MPlayer> ret = this.getMPlayersWhereRank(this.getLeaderRank());
		if (ret.size() == 0) return null;
		return ret.get(0);
	}

	public Set<String> getMPlayerIds()
	{
		return this.getMPlayers().stream().map(MPlayer::getId).collect(Collectors.toSet());
	}

	public List<CommandSender> getOnlineCommandSenders()
	{
		// Create Ret
		List<CommandSender> ret = new MassiveList<>();
		
		// Fill Ret
		for (CommandSender sender : IdUtil.getLocalSenders())
		{
			if (MUtil.isntSender(sender)) continue;
			
			MPlayer mplayer = MPlayer.get(sender);
			if (mplayer.getFaction() != this) continue;
			
			ret.add(sender);
		}
		
		// Return Ret
		return ret;
	}
	
	public List<Player> getOnlinePlayers()
	{
		// Create Ret
		List<Player> ret = new MassiveList<>();
		
		// Fill Ret
		for (Player player : Bukkit.getOnlinePlayers())
		{
			if (MUtil.isntPlayer(player)) continue;
			
			MPlayer mplayer = MPlayer.get(player);
			if (mplayer.getFaction() != this) continue;
			
			ret.add(player);
		}
		
		// Return Ret
		return ret;
	}

	// used when current leader is about to be removed from the faction; promotes new leader, or disbands faction if no other members left
	public void promoteNewLeader()
	{
		if ( ! this.isNormal()) return;
		if (this.getFlag(MFlag.getFlagPermanent()) && MConf.get().permanentFactionsDisableLeaderPromotion) return;

		MPlayer oldLeader = this.getLeader();
		Rank leaderRank = oldLeader.getRank();

		List<MPlayer> replacements = Collections.emptyList();
		for (Rank rank = leaderRank; rank != null; rank = rank.getRankBelow())
		{
			//Skip first
			if (rank == leaderRank) continue;

			replacements = this.getMPlayersWhereRank(rank);
			if (!replacements.isEmpty()) break;
		}

		// if we found a replacement
		if (replacements == null || replacements.isEmpty())
		{
			// faction leader is the only member; one-man faction
			if (this.getFlag(MFlag.getFlagPermanent()))
			{
				if (oldLeader != null)
				{
					oldLeader.setRank(this.getLeaderRank().getRankBelow());
				}
				return;
			}

			// no members left and faction isn't permanent, so disband it
			if (MConf.get().logFactionDisband)
			{
				Factions.get().log("The faction "+this.getName()+" ("+this.getId()+") has been disbanded since it has no members left.");
			}

			for (MPlayer mplayer : MPlayerColl.get().getAllOnline())
			{
				mplayer.msg("<i>The faction %s<i> was disbanded.", this.getName(mplayer));
			}

			this.detach();
		}
		else
		{
			// promote new faction leader
			if (oldLeader != null)
			{
				oldLeader.setRank(this.getLeaderRank().getRankBelow());
			}
				
			replacements.get(0).setRank(this.getLeaderRank());
			this.msg("<i>Faction leader <h>%s<i> has been removed. %s<i> has been promoted as the new faction leader.", oldLeader == null ? "" : oldLeader.getName(), replacements.get(0).getName());
			Factions.get().log("Faction "+this.getName()+" ("+this.getId()+") leader was removed. Replacement leader: "+replacements.get(0).getName());
		}
	}
	
	// -------------------------------------------- //
	// FACTION ONLINE STATE
	// -------------------------------------------- //

	public boolean isAllMPlayersOffline()
	{
		return this.getMPlayersWhereOnline(true).size() == 0;
	}
	
	public boolean isAnyMPlayersOnline()
	{
		return !this.isAllMPlayersOffline();
	}
	
	public boolean isFactionConsideredOffline()
	{
		return this.isAllMPlayersOffline();
	}
	
	public boolean isFactionConsideredOnline()
	{
		return !this.isFactionConsideredOffline();
	}
	
	public boolean isExplosionsAllowed()
	{
		boolean explosions = this.getFlag(MFlag.getFlagExplosions());
		boolean offlineexplosions = this.getFlag(MFlag.getFlagOfflineexplosions());

		if (explosions && offlineexplosions) return true;
		if ( ! explosions && ! offlineexplosions) return false;

		boolean online = this.isFactionConsideredOnline();
		
		return (online && explosions) || (!online && offlineexplosions);
	}
	
	// -------------------------------------------- //
	// MESSAGES
	// -------------------------------------------- //
	// These methods are simply proxied in from the Mixin.
	
	// CONVENIENCE SEND MESSAGE
	
	public boolean sendMessage(Object message)
	{
		return MixinMessage.get().messagePredicate(new PredicateCommandSenderFaction(this), message);
	}
	
	public boolean sendMessage(Object... messages)
	{
		return MixinMessage.get().messagePredicate(new PredicateCommandSenderFaction(this), messages);
	}
	
	public boolean sendMessage(Collection<Object> messages)
	{
		return MixinMessage.get().messagePredicate(new PredicateCommandSenderFaction(this), messages);
	}
	
	// CONVENIENCE MSG
	
	public boolean msg(String msg)
	{
		return MixinMessage.get().msgPredicate(new PredicateCommandSenderFaction(this), msg);
	}
	
	public boolean msg(String msg, Object... args)
	{
		return MixinMessage.get().msgPredicate(new PredicateCommandSenderFaction(this), msg, args);
	}
	
	public boolean msg(Collection<String> msgs)
	{
		return MixinMessage.get().msgPredicate(new PredicateCommandSenderFaction(this), msgs);
	}
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	// FIXME this probably needs to be moved elsewhere
	public static String clean(String message)
	{
		String target = message;
		if (target == null) return null;
		
		target = target.trim();
		if (target.isEmpty()) target = null;
		
		return target;
	}
	
}
