package dev.kitteh.factions;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.massivecraft.factions.LegacyApiWarningManager;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Warp;
import com.massivecraft.massivecore.ps.PS;

import dev.kitteh.factions.util.LazyLocation;

/**
 * This class attempts to provide minimal compatibility with FactionsUUID by
 * providing wrapper classes/methods that convert to the V2/V3 class structure.
 * 
 * <p><strong>DO NOT USE THIS FOR NEW IMPLEMENTATIONS.</strong></p>
 * 
 * @deprecated
 */
@Deprecated
public class LegacyFaction implements Faction
{
    // TODO: Remove methods that were removed from the legacy API
    // TODO: Do we need to do logging or anything that commands do in these methods?
    
    private com.massivecraft.factions.entity.Faction realFaction;
    
    public LegacyFaction(com.massivecraft.factions.entity.Faction realFaction)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);
        
        this.realFaction = realFaction;
    }

    @Override
    public String getId()
    {
        return this.realFaction.getId();
    }

    @Override
    public Map<String, LazyLocation> getWarps()
    {
        Map<String, LazyLocation> warps = new HashMap<>();
        for (Warp warp : realFaction.getWarps().getAll())
        {
            warps.put(warp.getName(), new LazyLocation(warp.getLocation().asBukkitLocation()));
        }
        return warps;
    }
    
    @Override
    public Map<String, LazyLocation> warps()
    {
        return getWarps();
    }

    @Override
    public LazyLocation getWarp(String name)
    {
        Warp warp = realFaction.getWarp(name);
        if (warp != null)
        {
            return new LazyLocation(warp.getLocation().asBukkitLocation());
        }
        return null;
    }

    @Override
    public LazyLocation warp(String name)
    {
        return getWarp(name);
    }

    @Override
    public void setWarp(String name, LazyLocation loc)
    {
        PS ps = PS.valueOf(loc.getLocation());
        Warp warp = realFaction.getWarp(name);
        if (warp != null)
        {
            warp.setLocation(ps);
        }
        else
        {
            warp = new Warp(name, ps);
            realFaction.getWarps().attach(warp);
        }
    }

    @Override
    public void createWarp(String name, LazyLocation loc)
    {
        setWarp(name, loc);
    }

    @Override
    public boolean isWarp(String name)
    {
        return realFaction.getWarp(name) != null;
    }


    @Override
    public boolean removeWarp(String name)
    {
        Warp warp = realFaction.getWarp(name);
        if (warp != null)
        {
            warp.detach();
            return true;
        }
        return false;
    }

    @Override
    public void clearWarps()
    {
        for (Warp warp : realFaction.getWarps().getAll())
        {
            warp.detach();
        }
    }

    @Override
    public Set<String> getInvites()
    {
        // TODO: convert realFaction.getInvitations() into correct format
        return null;
    }

    @Override
    public Set<String> invites()
    {
        return getInvites();
    }

    @Override
    public void invite(FPlayer fplayer)
    {
        // TODO: Look at how to implement
        
    }

    @Override
    public void deinvite(FPlayer fplayer)
    {
        realFaction.uninvite(MPlayer.get(fplayer.getPlayer()));
    }

    @Override
    public void deInvite(FPlayer fplayer)
    {
        deinvite(fplayer);
    }

    @Override
    public boolean isInvited(FPlayer fplayer)
    {
        return realFaction.isInvited(MPlayer.get(fplayer.getPlayer()));
    }

    @Override
    public boolean hasInvite(FPlayer fplayer)
    {
        return isInvited(fplayer);
    }

    @Override
    public boolean getOpen()
    {
        return realFaction.getFlag(MFlag.ID_OPEN);
    }

    @Override
    public boolean open()
    {
        return getOpen();
    }

    @Override
    public void setOpen(boolean isOpen)
    {
        realFaction.setFlag(MFlag.ID_OPEN, isOpen);
    }

    @Override
    public void open(boolean isOpen)
    {
        setOpen(isOpen);
    }

    @Override
    public boolean isPeaceful()
    {
        return realFaction.getFlag(MFlag.ID_PEACEFUL);
    }

    @Override
    public void setPeaceful(boolean isPeaceful)
    {
        realFaction.setFlag(MFlag.ID_PEACEFUL, isPeaceful);
    }

    @Override
    public void peaceful(boolean isPeaceful)
    {
        setPeaceful(isPeaceful);
    }

    @Override
    public boolean getPeacefulExplosionsEnabled()
    {
        // TODO: What is peaceful explosions vs. no explosions?
        return realFaction.getFlag(MFlag.ID_EXPLOSIONS);
    }

    @Override
    public boolean peacefulExplosionsEnabled()
    {
        return getPeacefulExplosionsEnabled();
    }
    
    @Override
    public void setPeacefulExplosionsEnabled(boolean peacefulExplosionsEnabled)
    {
        // TODO: What is peaceful explosions vs. no explosions?
        realFaction.setFlag(MFlag.ID_EXPLOSIONS, peacefulExplosionsEnabled);
    }

    @Override
    public void peacefulExplosionsEnabled(boolean peacefulExplosionsEnabled)
    {
        setPeacefulExplosionsEnabled(peacefulExplosionsEnabled);
    }

    @Override
    public boolean noExplosionsInTerritory()
    {
     // TODO: What is peaceful explosions vs. no explosions?
        return realFaction.getFlag(MFlag.ID_EXPLOSIONS);
    }

    @Override
    public boolean isPermanent()
    {
        return realFaction.getFlag(MFlag.ID_PERMANENT);
    }

    @Override
    public void setPermanent(boolean isPermanent)
    {
        realFaction.setFlag(MFlag.ID_PERMANENT, isPermanent);
    }

    @Override
    public void permanent(boolean isPermanent)
    {
        setPermanent(isPermanent);
    }

    @Override
    public int id()
    {
        // Factions3 does not have numeric IDs, so we return a hash code as a substitute
        return this.realFaction.getId().hashCode();
    }

    @Override
    public String getTag()
    {
        return realFaction.getName();
    }

    @Override
    public String tag()
    {
        return getTag();
    }

    @Override
    public String getTag(String prefix)
    {
        return prefix + this.getTag();
    }

    @Override
    public String getTag(Faction otherFaction)
    {
        return realFaction.getName(com.massivecraft.factions.entity.Faction.get(otherFaction.getId()));
    }

    @Override
    public String tag(Faction otherFaction)
    {
        return getTag(otherFaction);
    }

    @Override
    public String getTag(FPlayer otherFplayer)
    {
        return realFaction.getName(MPlayer.get(otherFplayer.getPlayer()));
    }

    @Override
    public void setTag(String tag)
    {
        realFaction.setName(tag);
    }

    @Override
    public void tag(String tag)
    {
        setTag(tag);
    }

    @Override
    public String getComparisonTag()
    {
        return realFaction.getComparisonName();
    }

    @Override
    public String getDescription()
    {
        return realFaction.getDescription();
    }

    @Override
    public String description()
    {
        return getDescription();
    }

    @Override
    public void setDescription(String description)
    {
        realFaction.setDescription(description);
    }

    @Override
    public void description(String description)
    {
        setDescription(description);
    }

    @Override
    public void setHome(Location home)
    {
        // TODO: not sure if this works, need to test
        PS ps = PS.valueOf(home);
		Warp warp = new Warp(MConf.get().warpsHomeName, ps);
        realFaction.getWarps().attach(warp);
    }

    @Override
    public void home(Location home)
    {
        setHome(home);
    }

    @Override
    public void delHome()
    {
        // TODO: not sure if this works, need to test
        Warp warp = realFaction.getWarp(MConf.get().warpsHomeName);
        if (warp != null)
        {
            warp.detach();
        }
    }

    @Override
    public void removeHome()
    {
        delHome();
    }

    @Override
    public boolean hasHome()
    {
        // TODO: not sure if this works, need to test
        Warp warp = realFaction.getWarp(MConf.get().warpsHomeName);
        if (warp != null)
        {
            return true;
        }
        return false;
    }

    @Override
    public Location getHome()
    {
        // TODO: not sure if this works, need to test
        Warp warp = realFaction.getWarp(MConf.get().warpsHomeName);
        if (warp != null)
        {
            return warp.getLocation().asBukkitLocation();
        }
        return null;
    }

    @Override
    public Location home()
    {
        return getHome();
    }

    @Override
    public long getFoundedDate()
    {
        return realFaction.getCreatedAtMillis();
    }

    @Override
    public Instant founded()
    {
        return Instant.ofEpochMilli(getFoundedDate());
    }

    @Override
    public void setFoundedDate(long newDate)
    {
        realFaction.setCreatedAtMillis(newDate);
    }

    @Override
    public void founded(Instant instant)
    {
        setFoundedDate(instant.toEpochMilli());
    }

    @Override
    public void confirmValidHome()
    {
        Warp warp = realFaction.getWarp(MConf.get().warpsHomeName);
        if (warp != null)
        {
            warp.verifyIsValid();
        }
    }

    @Override
    public boolean noPvPInTerritory()
    {
        return realFaction.getFlag(MFlag.ID_PVP);
    }

    @Override
    public boolean noMonstersInTerritory()
    {
        return realFaction.getFlag(MFlag.ID_MONSTERS);
    }

    @Override
    public boolean isNormal()
    {
        return realFaction.isNormal();
    }

    @Override
    public boolean isWilderness()
    {
        return realFaction.isNone();
    }

    @Override
    public boolean isSafeZone()
    {
        return realFaction.isSafeZone();
    }

    @Override
    public boolean isWarZone()
    {
        return realFaction.isWarZone();
    }

    // is the faction a faction that cannot be joined
    @Override
    public boolean isPlayerFreeType()
    {
        return realFaction.isSafeZone() || realFaction.isWarZone();
    }

    @Override
    public int getLandRounded()
    {
        return realFaction.getLandCount();
    }

    @Override
    public int getLandRoundedInWorld(String worldName)
    {
        return realFaction.getLandCountInWorld(worldName);
    }

    @Override
    public double getPower()
    {
        return realFaction.getPower();
    }

    @Override
    public double powerExact()
    {
        return getPower();
    }

    @Override
    public double getPowerMax()
    {
        return realFaction.getPowerMax();
    }

    @Override
    public double powerMaxExact()
    {
        return getPowerMax();
    }

    @Override
    public int getPowerRounded()
    {
        return realFaction.getPowerRounded();
    }

    @Override
    public int power()
    {
        return getPowerRounded();
    }

    @Override
    public int getPowerMaxRounded()
    {
        return realFaction.getPowerMaxRounded();
    }

    @Override
    public int powerMax()
    {
        return getPowerMaxRounded();
    }

    @Override
    public boolean hasPermanentPower()
    {
        // TODO: Factions3 does not have permanent power except for the permanent flag and a power boost?
        return false;
    }

    @Override
    public double getPowerBoost()
    {
        return realFaction.getPowerBoost();
    }

    @Override
    public double powerBoost()
    {
        return getPowerBoost();
    }

    @Override
    public void setPowerBoost(double powerBoost)
    {
        realFaction.setPowerBoost(powerBoost);
    }

    @Override
    public void powerBoost(double powerBoost)
    {
        setPowerBoost(powerBoost);
    }

    @Override
    public boolean isPowerFrozen()
    {
        // Factions3 does not freeze faction power
        return false;
    }

    @Override
    public void refreshFPlayers()
    {
        // Factions3 does not store players this way - as a result, this does nothing
    }

    @Override
    public boolean addFPlayer(FPlayer fplayer)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return false;
    }

    @Override
    public boolean removeFPlayer(FPlayer fplayer)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return false;
    }

    @Override
    public int getSize()
    {
        return realFaction.getMPlayers().size();
    }

    @Override
    public int size()
    {
        return getSize();
    }

    @Override
    public Set<FPlayer> getFPlayers()
    {
        Set<FPlayer> players = new HashSet<>();
        if (this.isNormal())
        {
            for (MPlayer player : realFaction.getMPlayers())
            {
                players.add(new LegacyFPlayer(player));
            }
        }
        return players;
    }

    @Override
    public Set<FPlayer> members()
    {
        return getFPlayers();
    }

    @Override
    public Set<FPlayer> getFPlayersWhereOnline(boolean online)
    {
        Set<FPlayer> players = new HashSet<>();
        if (this.isNormal())
        {
            for (MPlayer player : realFaction.getMPlayersWhereOnline(online))
            {
                players.add(new LegacyFPlayer(player));
            }
        }
        return players;
    }

    @Override
    public Set<FPlayer> membersOnline(boolean online)
    {
        return getFPlayersWhereOnline(online);
    }

    @Override
    public Set<FPlayer> getFPlayersWhereOnline(boolean online, FPlayer viewer)
    {
        // If we want offline, just return all offline players
        if (viewer == null || !online)
        {
            return getFPlayersWhereOnline(online);
        }
        
        Set<FPlayer> players = new HashSet<>();
        if (this.isNormal()) 
        {
            for (MPlayer viewed : realFaction.getMPlayersWhereOnline(online)) 
            {
                if (viewed.getPlayer() != null
                        && viewer.getPlayer() != null
                        && viewer.getPlayer().canSee(viewed.getPlayer()))
                {
                    players.add(new LegacyFPlayer(viewed));
                }
            }
        }

        return players;
    }

    @Override
    public Set<FPlayer> membersOnline(boolean online, FPlayer viewer)
    {
        return getFPlayersWhereOnline(online, viewer);
    }

    @Override
    public FPlayer getFPlayerAdmin()
    {
        FPlayer leader = null;
        if (this.isNormal())
        {
            for (MPlayer player : realFaction.getMPlayers())
            {
                if (player.getRank().isLeader())
                {
                    leader = new LegacyFPlayer(player);
                    break;
                }
            }
        }
        return leader;
    }

    @Override
    public FPlayer admin()
    {
        return getFPlayerAdmin();
    }

//    @Override
//    public List<FPlayer> getFPlayersWhereRole(Role role)
//    {
//        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
//        return null;
//    }

    @Override
    public List<Player> getOnlinePlayers()
    {
        return realFaction.getOnlinePlayers();
    }

    @Override
    public List<Player> membersOnlineAsPlayers()
    {
        return getOnlinePlayers();
    }

    @Override
    public boolean hasPlayersOnline()
    {
        return realFaction.getOnlinePlayers().size() > 0;
    }

    @Override
    public boolean hasMembersOnline()
    {
        return hasPlayersOnline();
    }

    @Override
    public void memberLoggedOff()
    {
        // Factions3 does not track last online time - this does nothing.
    }

    @Override
    public void trackMemberLoggedOff()
    {
        // Factions3 does not track last online time - this does nothing.
    }

    @Override
    public void promoteNewLeader()
    {
        realFaction.promoteNewLeader();
    }

//    @Override
//    public Role getDefaultRole()
//    {
//        // TODO: Can we convert Factions3 Ranks to Legacy Roles?
//        return null;
//    }

//    @Override
//    public void setDefaultRole(Role role)
//    {
//        // TODO: Factions3 doesn't have configurable default roles?
//    }

    @Override
    public void sendMessage(String message)
    {
        realFaction.msg(message);
    }

    @Override
    public void sendMessage(List<String> messages)
    {
        realFaction.msg(messages);
    }

    // @Override
    // public Map<FLocation, Set<String>> getClaimOwnership()
    // {
    //     // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
    //     return null;
    // }

    // @Override
    // public void clearAllClaimOwnership()
    // {
    //     // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
    // }

    // @Override
    // public void clearClaimOwnership(FLocation loc)
    // {
    //     // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    // }

    // @Override
    // public void clearClaimOwnership(FPlayer player)
    // {
    //     // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    // }

    // @Override
    // public int getCountOfClaimsWithOwners()
    // {
    //     // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
    //     return 0;
    // }

    // @Override
    // public boolean doesLocationHaveOwnersSet(FLocation loc)
    // {
    //     // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
    //     return false;
    // }

    // @Override
    // public boolean isPlayerInOwnerList(FPlayer player, FLocation loc)
    // {
    //     // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
    //     return false;
    // }

    // @Override
    // public void setPlayerAsOwner(FPlayer player, FLocation loc)
    // {
    //     // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    // }

    // @Override
    // public void removePlayerAsOwner(FPlayer player, FLocation loc)
    // {
    //     // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    // }

    // @Override
    // public Set<String> getOwnerList(FLocation loc)
    // {
    //     // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
    //     return null;
    // }

    // @Override
    // public String getOwnerListString(FLocation loc)
    // {
    //     // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
    //     return null;
    // }

    // @Override
    // public boolean playerHasOwnershipRights(FPlayer fplayer, FLocation loc)
    // {
    //     // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
    //     return false;
    // }

    // @Override
    // public void remove()
    // {
    //     // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    // }

    // @Override
    // public Set<FLocation> getAllClaims()
    // {
    //     // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
    //     return null;
    // }
    
}
