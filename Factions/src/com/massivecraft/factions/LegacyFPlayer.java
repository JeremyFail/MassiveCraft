package com.massivecraft.factions;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;

/**
 * This class attempts to provide minimal compatibility with Factions V1/FactionsUUID
 * by providing wrapper classes/methods that convert to the V2/V3 class structure.
 * 
 * <p><strong>DO NOT USE THIS FOR NEW IMPLEMENTATIONS.</strong></p>
 * 
 * @deprecated
 */
@Deprecated
public class LegacyFPlayer implements FPlayer
{
   // TODO: Do we need to do logging or anything that commands do in these methods?
    
    private MPlayer realPlayer;
    
    public LegacyFPlayer(OfflinePlayer player)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage();
        
        this.realPlayer = MPlayer.get(player);
    }

    public LegacyFPlayer(MPlayer player)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage();
        
        this.realPlayer = player;
    }
    
    @Override
    public Faction getFaction()
    {
        return new LegacyFaction(realPlayer.getFaction());
    }

    @Override
    public String getFactionId()
    {
        return realPlayer.getFaction().getId();
    }

    @Override
    public boolean hasFaction()
    {
        return realPlayer.hasFaction();
    }

    @Override
    public void setFaction(Faction faction)
    {
        realPlayer.setFaction(FactionColl.get().getFixed(faction.getId()));
    }

    @Override
    public boolean shouldTakeFallDamage()
    {
        return !realPlayer.isFlying();
    }

//    @Override
//    public void setTakeFallDamage(boolean fallDamage)
//    {
//        // TODO: Not the right way to do this...
//        realPlayer.setFlying(!fallDamage);
//    }

    @Override
    public double getPowerBoost()
    {
        return realPlayer.getPowerBoost();
    }

    @Override
    public void setPowerBoost(double powerBoost)
    {
        realPlayer.setPowerBoost(powerBoost);
    }

    @Override
    public Faction getAutoClaimFor()
    {
        return new LegacyFaction(realPlayer.getAutoClaimFaction());
    }

    @Override
    public void setAutoClaimFor(Faction faction)
    {
        realPlayer.setAutoClaimFaction(FactionColl.get().getFixed(faction.getId()));
    }

    @Override
    public Faction getAutoUnclaimFor()
    {
        // Not quite correct - auto unclaim works a bit different...
        return new LegacyFaction(realPlayer.getAutoClaimFaction());
    }

    @Override
    public void setAutoUnclaimFor(Faction faction)
    {
        realPlayer.setAutoClaimFaction(FactionColl.get().getNone());
    }
    
    public boolean isAdminBypassing()
    {
        return realPlayer.isOverriding();
    }

    @Override
    public boolean isVanished()
    {
        return realPlayer.isVisible();
    }

    @Override
    public void setIsAdminBypassing(boolean adminBypassing)
    {
        realPlayer.setOverriding(adminBypassing);
    }

    @Override
    public void resetFactionData(boolean doSpoutUpdate)
    {
        realPlayer.resetFactionData();
    }

    @Override
    public void resetFactionData()
    {
        realPlayer.resetFactionData();
    }

    @Override
    public long getLastLoginTime()
    {
        return realPlayer.getLastPlayed();
    }

    @Override
    public boolean isMapAutoUpdating()
    {
        return realPlayer.isMapAutoUpdating();
    }

    @Override
    public void setMapAutoUpdating(boolean mapAutoUpdating)
    {
        realPlayer.setMapAutoUpdating(mapAutoUpdating);
    }
    
    @Override
    public String getTitle()
    {
        return realPlayer.getTitle();
    }

    @Override
    public void setTitle(CommandSender sender, String title)
    {
        // TODO: Check command that calls this for additional logic
        realPlayer.setTitle(title);
    }

    @Override
    public String getName()
    {
        return realPlayer.getName();
    }

    @Override
    public String getTag()
    {
        return realPlayer.hasFaction() ? realPlayer.getFaction().getName() : "";
    }

    @Override
    public String getNameAndSomething(String something)
    {
        if (something != null && !something.isEmpty())
        {
            return realPlayer.getName() + something + " ";
        }
        return realPlayer.getName();
    }

    @Override
    public String getNameAndTitle()
    {
        return this.getNameAndSomething(realPlayer.getTitle());
    }

    @Override
    public String getNameAndTag()
    {
        return this.getNameAndSomething(this.getTag());
    }

    @Override
    public String getNameAndTitle(Faction faction)
    {
        return realPlayer.getColorTo(FactionColl.get().getFixed(faction.getId())) + this.getNameAndTitle();
    }

    @Override
    public String getNameAndTitle(FPlayer fplayer)
    {
        return realPlayer.getColorTo(realPlayer) + this.getNameAndTitle();
    }

    @Override
    public void heal(int amnt)
    {
        realPlayer.heal(amnt);
    }

    @Override
    public double getPower()
    {
        return realPlayer.getPower();
    }

    @Override
    public void alterPower(double delta)
    {
        double power = realPlayer.getPower() + delta;
        if (power > realPlayer.getPowerMax())
        {
            realPlayer.setPower(realPlayer.getPowerMax());
        }
        else if (power < realPlayer.getPowerMin())
        {
            realPlayer.setPower(realPlayer.getPowerMin());
        }
        else
        {
            realPlayer.setPower(power);
        }
    }

    @Override
    public double getPowerMax()
    {
        return realPlayer.getPowerMax();
    }

    @Override
    public double getPowerMin()
    {
        return realPlayer.getPowerMin();
    }

    @Override
    public int getPowerRounded()
    {
        return realPlayer.getPowerRounded();
    }

    @Override
    public int getPowerMaxRounded()
    {
        return realPlayer.getPowerMaxRounded();
    }

    @Override
    public int getPowerMinRounded()
    {
        return realPlayer.getPowerMinRounded();
    }

    @Override
    public void updatePower()
    {
        // TODO: Implement - see https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFPlayer.java
        return;
    }

    @Override
    public void losePowerFromBeingOffline()
    {
        // TODO: Implement - see https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFPlayer.java
        return;
    }

    @Override
    public boolean isInOwnTerritory()
    {
        return realPlayer.isInOwnTerritory();
    }

    @Override
    public boolean isInOthersTerritory()
    {
        return realPlayer.isInOthersTerritory();
    }

    @Override
    public boolean isInAllyTerritory()
    {
        return realPlayer.isInAllyTerritory();
    }

    @Override
    public boolean isInNeutralTerritory()
    {
        return realPlayer.isInNeutralTerritory();
    }

    @Override
    public boolean isInEnemyTerritory()
    {
        return realPlayer.isInEnemyTerritory();
    }

    @Override
    public void sendFactionHereMessage(Faction from)
    {
        // TODO: Implement - see https://github.com/FactionsU/UID/blob/main/src/main/java/com/massivecraft/factions/data/MemoryFPlayer.java
        return;
    }

    @Override
    public void leave(boolean makePay)
    {
        realPlayer.leave();
    }

    @Override
    public boolean canClaimForFaction(Faction forFaction)
    {
        // TODO: Implement
//        com.massivecraft.factions.entity.Faction realFaction = FactionColl.get().getFixed(forFaction.getId());
//        return realPlayer.isOverriding() || 
//                !realFaction.getId().equals(Factions.ID_NONE) && (realFaction == realPlayer.getFaction() && this.getFaction().hasAccess(this, PermissibleActions.TERRITORY, null)) || 
//                (realFaction.getId().equals(Factions.ID_SAFEZONE) && Permission.MANAGE_SAFE_ZONE.has(getPlayer())) || 
//                (realFaction.getId().equals(Factions.ID_WARZONE) && Permission.MANAGE_WAR_ZONE.has(getPlayer()));
        return false;
    }

    @Override
    public boolean canClaimForFactionAtLocation(Faction forFaction, Location location, boolean notifyFailure)
    {
        return canClaimForFactionAtLocation(forFaction, new FLocation(location), notifyFailure);
    }

    @Override
    public boolean canClaimForFactionAtLocation(Faction forFaction, FLocation location, boolean notifyFailure)
    {
        return canClaimForFactionAtLocation(forFaction, location.getLocation(), notifyFailure);
    }

    @Override
    public boolean attemptClaim(Faction forFaction, Location location, boolean notifyFailure)
    {
        // TODO: notifyFailure boolean does nothing
        return attemptClaim(forFaction, new FLocation(location), notifyFailure);
    }

    @Override
    public boolean attemptClaim(Faction forFaction, FLocation location, boolean notifyFailure)
    {
        // TODO: notifyFailure boolean does nothing
        com.massivecraft.factions.entity.Faction realFaction = FactionColl.get().getFixed(forFaction.getId());
        final PS chunk = PS.valueOf(location.getLocation()).getChunk(true);
        Set<PS> chunks = Collections.singleton(chunk);
        
        return realPlayer.tryClaim(realFaction, chunks);
    }

    @Override
    public boolean attemptUnclaim(Faction forFaction, FLocation location, boolean notifyFailure)
    {
        com.massivecraft.factions.entity.Faction realFaction = FactionColl.get().getNone();
        final PS chunk = PS.valueOf(location.getLocation()).getChunk(true);
        Set<PS> chunks = Collections.singleton(chunk);
        
        return realPlayer.tryClaim(realFaction, chunks);
    }

    @Override
    public String getId()
    {
        return realPlayer.getId();
    }

    @Override
    public Player getPlayer()
    {
        return realPlayer.getPlayer();
    }

    @Override
    public boolean isOnline()
    {
        return realPlayer.isOnline();
    }

    @Override
    public void sendMessage(String message)
    {
        realPlayer.msg(message);
    }

    @Override
    public void sendMessage(List<String> messages)
    {
        realPlayer.msg(messages);
    }

    @Override
    public boolean isOnlineAndVisibleTo(Player player)
    {
        Player target = this.getPlayer();
        return target != null && player.canSee(target);
    }

    @Override
    public boolean isOffline()
    {
        return realPlayer.isOffline();
    }

    @Override
    public void flightCheck()
    {
        // TODO: Implement? - fix commented out lines
        //if (FactionsPlugin.getInstance().conf().commands().fly().isEnable() && !this.isAdminBypassing()) {
        if (!realPlayer.isOverriding())
        {
            // boolean canFly = this.canFlyAtLocation(this.getLastStoodAt());
            boolean canFly = false;
            if (this.isFlying() && !canFly) 
            {
                this.setFlying(false, false);
            } 
            //else if (this.isAutoFlying() && !this.isFlying() && canFly)
            else if (!this.isFlying() && canFly)
            {
                this.setFlying(true);
            }
        }
    }

    @Override
    public boolean isFlying()
    {
        return realPlayer.isFlying();
    }

    @Override
    public void setFlying(boolean fly)
    {
        realPlayer.setFlying(fly);
    }

    @Override
    public void setFlying(boolean fly, boolean damage)
    {
        // TODO: damage boolean does nothing
        realPlayer.setFlying(fly);
    }

    @Override
    public boolean canFlyAtLocation()
    {
        // TODO: Can we implement? With current player location?
        return false;
    }

    @Override
    public boolean canFlyAtLocation(FLocation location)
    {
        com.massivecraft.factions.entity.Board realBoardAtLoc = BoardColl.get().get(location.getLocation().getWorld());
        com.massivecraft.factions.entity.Faction realFactionAtLoc = realBoardAtLoc.getFactionAt(PS.valueOf(location.getLocation()).getChunkCoords(true));
        
        // TODO: Check Wilderness, Safezone, Warzone once fly permission can be per player
//      return realPlayer.isOverriding() || 
//              !realFaction.getId().equals(Factions.ID_NONE) && (realFaction == realPlayer.getFaction() && this.getFaction().hasAccess(this, PermissibleActions.TERRITORY, null)) || 
//              (realFaction.getId().equals(Factions.ID_SAFEZONE) && Permission.MANAGE_SAFE_ZONE.has(getPlayer())) || 
//              (realFaction.getId().equals(Factions.ID_WARZONE) && Permission.MANAGE_WAR_ZONE.has(getPlayer()));
        
        if (realPlayer.isOverriding())
        {
            return true;
        }
        
        // TODO: FactionsUUID allows enabling fly permission at player/rank level - we do as well, but how do we check that here?
        return realFactionAtLoc.getFlag(MFlag.ID_FLY) && realFactionAtLoc == realPlayer.getFaction();
    }

    @Override
    public boolean isSeeingChunk()
    {
        return realPlayer.isSeeingChunk();
    }
    
    @Override
    public void setSeeingChunk(boolean seeingChunk)
    {
        realPlayer.setSeeingChunk(seeingChunk);
    }
    
}
