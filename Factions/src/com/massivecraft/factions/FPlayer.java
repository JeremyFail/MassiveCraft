package com.massivecraft.factions;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This class attempts to provide minimal compatibility with Factions V1/FactionsUUID
 * by providing wrapper classes/methods that convert to the V2/V3 class structure.
 * 
 * <p><strong>DO NOT USE THIS FOR NEW IMPLEMENTATIONS.</strong></p>
 * 
 * @deprecated
 */
@Deprecated
public interface FPlayer
{
       
    public Faction getFaction();

    boolean hasFaction();

    void setFaction(Faction faction);
    
    public boolean shouldTakeFallDamage();
    
//    public void setTakeFallDamage(boolean fallDamage);

    double getPowerBoost();

    void setPowerBoost(double powerBoost);

    Faction getAutoClaimFor();

    void setAutoClaimFor(Faction faction);

    Faction getAutoUnclaimFor();

    void setAutoUnclaimFor(Faction faction);

    boolean isAdminBypassing();

    boolean isVanished();

    void setIsAdminBypassing(boolean val);

    void resetFactionData(boolean doSpoutUpdate);

    void resetFactionData();

    long getLastLoginTime();

    boolean isMapAutoUpdating();

    void setMapAutoUpdating(boolean mapAutoUpdating);

    String getTitle();
    
    void setTitle(CommandSender sender, String title);

    String getName();

    String getTag();

    // Base concatenations:

    String getNameAndSomething(String something);

    String getNameAndTitle();

    String getNameAndTag();

    // Colored concatenations:
    // These are used in information messages

    String getNameAndTitle(Faction faction);

    String getNameAndTitle(FPlayer fplayer);

    //----------------------------------------------//
    // Health
    //----------------------------------------------//
    void heal(int amnt);


    //----------------------------------------------//
    // Power
    //----------------------------------------------//
    double getPower();

    void alterPower(double delta);

    double getPowerMax();

    double getPowerMin();

    int getPowerRounded();

    int getPowerMaxRounded();

    int getPowerMinRounded();

    void updatePower();

    void losePowerFromBeingOffline();

    //----------------------------------------------//
    // Territory
    //----------------------------------------------//
    boolean isInOwnTerritory();

    boolean isInOthersTerritory();

    boolean isInAllyTerritory();

    boolean isInNeutralTerritory();

    boolean isInEnemyTerritory();

    void sendFactionHereMessage(Faction from);

    // -------------------------------
    // Actions
    // -------------------------------

    void leave(boolean makePay);

    boolean canClaimForFaction(Faction forFaction);

    @Deprecated
    boolean canClaimForFactionAtLocation(Faction forFaction, Location location, boolean notifyFailure);

    boolean canClaimForFactionAtLocation(Faction forFaction, FLocation location, boolean notifyFailure);

    boolean attemptClaim(Faction forFaction, Location location, boolean notifyFailure);

    boolean attemptClaim(Faction forFaction, FLocation location, boolean notifyFailure);

    boolean attemptUnclaim(Faction forFaction, FLocation flocation, boolean notifyFailure);

    String getId();

    Player getPlayer();

    boolean isOnline();

    void sendMessage(String message);

    void sendMessage(List<String> messages);

    boolean isOnlineAndVisibleTo(Player me);

    boolean isOffline();

    void flightCheck();

    boolean isFlying();

    void setFlying(boolean fly);

    void setFlying(boolean fly, boolean damage);

    boolean canFlyAtLocation();

    boolean canFlyAtLocation(FLocation location);
    
}
