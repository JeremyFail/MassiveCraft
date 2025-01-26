package com.massivecraft.factions;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.util.LazyLocation;

/**
 * This class attempts to provide minimal compatibility with Factions V1/FactionsUUID
 * by providing wrapper classes/methods that convert to the V2/V3 class structure.
 * 
 * <p><strong>DO NOT USE THIS FOR NEW IMPLEMENTATIONS.</strong></p>
 * 
 * @deprecated
 */
@Deprecated
public class LegacyFaction implements Faction
{
    
    // TODO: Do we need to do logging or anything that commands do in these methods?
    
    private com.massivecraft.factions.entity.Faction realFaction;
    
    public LegacyFaction(com.massivecraft.factions.entity.Faction realFaction)
    {
        this.realFaction = realFaction;
    }
    
    public String getId()
    {
        return this.realFaction.getId();
    }

    @Override
    public Map<String, LazyLocation> getWarps()
    {
        // TODO: convert realFaction.getWarps() into correct format
        return null;
    }

    @Override
    public LazyLocation getWarp(String name)
    {
        // TODO: need to look at how we can get a single warp
        return null;
    }

    @Override
    public void setWarp(String name, LazyLocation loc)
    {
        // TODO: Look at how to implement
    }

    @Override
    public boolean isWarp(String name)
    {
        // TODO: Look at how to implement
        return false;
    }


    @Override
    public boolean removeWarp(String name)
    {
        // TODO: Look at how to implement
        return false;
    }

    @Override
    public void clearWarps()
    {
        // TODO: Look at how to implement
        
    }

    @Override
    public Set<String> getInvites()
    {
        // TODO: convert realFaction.getInvitations() into correct format
        return null;
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
    public boolean isInvited(FPlayer fplayer)
    {
        return realFaction.isInvited(MPlayer.get(fplayer.getPlayer()));
    }

    @Override
    public boolean getOpen()
    {
        return realFaction.getFlag(MFlag.ID_OPEN);
    }

    @Override
    public void setOpen(boolean isOpen)
    {
        realFaction.setFlag(MFlag.ID_OPEN, isOpen);
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
    public boolean getPeacefulExplosionsEnabled()
    {
        // TODO: What is peaceful explosions vs. no explosions?
        return realFaction.getFlag(MFlag.ID_EXPLOSIONS);
    }
    
    @Override
    public void setPeacefulExplosionsEnabled(boolean peacefulExplosionsEnabled)
    {
        // TODO: What is peaceful explosions vs. no explosions?
        realFaction.setFlag(MFlag.ID_EXPLOSIONS, peacefulExplosionsEnabled);
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
    public String getTag()
    {
        return realFaction.getName();
    }

    @Override
    public String getTag(String prefix)
    {
        return prefix + this.getTag();
    }

    @Override
    public String getTag(Faction otherFaction)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return null;
    }

    @Override
    public String getTag(FPlayer otherFplayer)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return null;
    }

    @Override
    public void setTag(String tag)
    {
        realFaction.setName(tag);
    }

    @Override
    public String getComparisonTag()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return null;
    }

    @Override
    public String getDescription()
    {
        return realFaction.getDescription();
    }

    @Override
    public void setDescription(String description)
    {
        realFaction.setDescription(description);
    }

    @Override
    public void setHome(Location home)
    {
        // TODO: Look at how to implement with warps
    }

    @Override
    public void delHome()
    {
        // TODO: Look at how to implement with warps
        
    }

    @Override
    public boolean hasHome()
    {
        // TODO: Look at how to implement with warps
        return false;
    }

    @Override
    public Location getHome()
    {
        // TODO: Look at how to implement with warps
        return null;
    }

    @Override
    public long getFoundedDate()
    {
        // TODO: not sure if this value is the same...
        return realFaction.getCreatedAtMillis();
    }

    @Override
    public void setFoundedDate(long newDate)
    {
        // TODO: Can we implement?
    }

    @Override
    public void confirmValidHome()
    {
        // TODO: Look at how to implement with warps
        
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
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return false;
    }

    @Override
    public boolean isWilderness()
    {
        return realFaction.getId() == Factions.ID_NONE;
    }

    @Override
    public boolean isSafeZone()
    {
        return realFaction.getId() == Factions.ID_SAFEZONE;
    }

    @Override
    public boolean isWarZone()
    {
        return realFaction.getId() == Factions.ID_WARZONE;
    }

    @Override
    public boolean isPlayerFreeType()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return false;
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
    public double getDTR()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return 0;
    }

    @Override
    public double getDTRWithoutUpdate()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return 0;
    }

    @Override
    public void setDTR(double dtr)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    }

    @Override
    public long getLastDTRUpdateTime()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return 0;
    }

    @Override
    public long getFrozenDTRUntilTime()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return 0;
    }

    @Override
    public void setFrozenDTR(long time)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    }

    @Override
    public boolean isFrozenDTR()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return false;
    }

    @Override
    public double getPower()
    {
        return realFaction.getPower();
    }

    @Override
    public double getPowerMax()
    {
        return realFaction.getPowerMax();
    }

    @Override
    public int getPowerRounded()
    {
        return realFaction.getPowerRounded();
    }

    @Override
    public int getPowerMaxRounded()
    {
        return realFaction.getPowerMaxRounded();
    }

    @Override
    public Integer getPermanentPower()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        // realFaction.getPowerBoost()?
        return 0;
    }

    @Override
    public void setPermanentPower(Integer permanentPower)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    }

    @Override
    public boolean hasPermanentPower()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return false;
    }

    @Override
    public double getPowerBoost()
    {
        return realFaction.getPowerBoost();
    }

    @Override
    public void setPowerBoost(double powerBoost)
    {
        realFaction.setPowerBoost(powerBoost);
    }

    @Override
    public boolean hasLandInflation()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return false;
    }

    @Override
    public boolean isPowerFrozen()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return false;
    }

    @Override
    public void refreshFPlayers()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
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
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return 0;
    }

    @Override
    public Set<FPlayer> getFPlayers()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return null;
    }

    @Override
    public Set<FPlayer> getFPlayersWhereOnline(boolean online)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return null;
    }

    @Override
    public Set<FPlayer> getFPlayersWhereOnline(boolean online, FPlayer viewer)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return null;
    }

    @Override
    public FPlayer getFPlayerAdmin()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return null;
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
    public boolean hasPlayersOnline()
    {
        return realFaction.getOnlinePlayers().size() > 0;
    }

    @Override
    public void memberLoggedOff()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    }

    @Override
    public void promoteNewLeader()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    }

//    @Override
//    public Role getDefaultRole()
//    {
//        // TODO Auto-generated method stub
//        return null;
//    }

//    @Override
//    public void setDefaultRole(Role role)
//    {
//        // TODO Auto-generated method stub
//        
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

    @Override
    public Map<FLocation, Set<String>> getClaimOwnership()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return null;
    }

    @Override
    public void clearAllClaimOwnership()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
    }

    @Override
    public void clearClaimOwnership(FLocation loc)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    }

    @Override
    public void clearClaimOwnership(FPlayer player)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    }

    @Override
    public int getCountOfClaimsWithOwners()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return 0;
    }

    @Override
    public boolean doesLocationHaveOwnersSet(FLocation loc)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return false;
    }

    @Override
    public boolean isPlayerInOwnerList(FPlayer player, FLocation loc)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return false;
    }

    @Override
    public void setPlayerAsOwner(FPlayer player, FLocation loc)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    }

    @Override
    public void removePlayerAsOwner(FPlayer player, FLocation loc)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    }

    @Override
    public Set<String> getOwnerList(FLocation loc)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return null;
    }

    @Override
    public String getOwnerListString(FLocation loc)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return null;
    }

    @Override
    public boolean playerHasOwnershipRights(FPlayer fplayer, FLocation loc)
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return false;
    }

    @Override
    public void remove()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        
    }

    @Override
    public Set<FLocation> getAllClaims()
    {
        // TODO: Look at how this is used in FactionsUUID - https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFaction.java
        return null;
    }
    
}
