package dev.kitteh.factions;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This class attempts to provide minimal compatibility with FactionsUUID by
 * providing wrapper classes/methods that convert to the V2/V3 class structure.
 * 
 * <p><strong>DO NOT USE THIS FOR NEW IMPLEMENTATIONS.</strong></p>
 * 
 * @deprecated
 */
@Deprecated
public interface FPlayer
{
    UUID uniqueId();

    String name();

    Faction getFaction();

    Faction faction();

    String getFactionId();

    boolean hasFaction();

    void setFaction(Faction faction);

    void faction(Faction faction);

    // boolean autoLeaveExempt();

    // void autoLeaveExempt(boolean autoLeave);

    // long lastFrostwalkerMessageTime();

    // void updateLastFrostwalkerMessageTime();

    // void monitorJoins(boolean monitor);

    // boolean monitorJoins();
    
    boolean shouldTakeFallDamage();

    boolean takeFallDamage();
    
    // public void setTakeFallDamage(boolean fallDamage);

    double getPowerBoost();

    void setPowerBoost(double powerBoost);

    Faction getAutoClaimFor();

    Faction autoClaim();

    void setAutoClaimFor(Faction faction);

    void autoClaim(Faction faction);

    Faction getAutoUnclaimFor();

    Faction autoUnclaim();

    void setAutoUnclaimFor(Faction faction);

    void autoUnclaim(Faction faction);

    boolean isAdminBypassing();

    boolean adminBypass();

    void setIsAdminBypassing(boolean val);

    void adminBypass(boolean val);

    boolean isVanished();

    // ChatTarget chatTarget();

    // void chatTarget(ChatTarget chatTarget);

    // boolean ignoreAllianceChat();

    // void ignoreAllianceChat(boolean ignore);

    // boolean ignoreTruceChat();

    // void ignoreTruceChat(boolean ignore);

    // boolean spyingChat();

    // void spyingChat(boolean spying);

    // boolean showScoreboard();

    // void showScoreboard(boolean show);

    void resetFactionData(boolean doSpoutUpdate);

    void resetFactionData();

    long getLastLoginTime();

    long lastLogin();

    boolean isMapAutoUpdating();

    boolean mapAutoUpdating();

    void setMapAutoUpdating(boolean mapAutoUpdating);

    void mapAutoUpdating(boolean mapAutoUpdating);

    // boolean loginPVPDisabled();

    // FLocation lastStoodAt();

    // void lastStoodAt(FLocation flocation);

    String getTitle();

    // Component title();
    
    void setTitle(CommandSender sender, String title);

    // void title(Component title);

    String getName();

    String getTag();

    // Base concatenations:

    // Component nameWithTitle();

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

    double power();

    void alterPower(double delta);

    void power(double delta);

    double getPowerMax();

    double powerMax();

    double getPowerMin();

    double powerMin();

    int getPowerRounded();

    int powerRounded();

    int getPowerMaxRounded();

    int powerMaxRounded();

    int getPowerMinRounded();

    int powerMinRounded();

    void updatePower();

    void losePowerFromBeingOffline();

    // void onDeath();

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

    Player asPlayer();

    boolean isOnline();

    void sendMessage(String message);

    void sendMessage(List<String> messages);

    boolean isOnlineAndVisibleTo(Player me);

    boolean isOffline();

    void flightCheck();

    boolean isFlying();

    boolean flying();

    void setFlying(boolean fly);

    void flying(boolean fly);

    void setFlying(boolean fly, boolean damage);

    void flying(boolean fly, boolean damage);

    boolean canFlyAtLocation();

    boolean canFlyAtLocation(FLocation location);

    // boolean flyTrail();

    // void flyTrail(boolean state);

    // void warmingUp();

    // Warmup warmup();

    // void addWarmup(Warmup warmup, int taskId);

    // void cancelWarmup();
    
    boolean seeChunk();

    void seeChunk(boolean seeingChunk);
    
}
