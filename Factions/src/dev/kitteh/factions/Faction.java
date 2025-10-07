package dev.kitteh.factions;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;

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
public interface Faction
{
    // TODO: Remove methods that were removed from the legacy API

    Map<String, LazyLocation> getWarps();

    Map<String, LazyLocation> warps();

    LazyLocation getWarp(String name);

    LazyLocation warp(String name);

    void setWarp(String name, LazyLocation loc);

    void createWarp(String name, LazyLocation loc);

    boolean isWarp(String name);

    boolean removeWarp(String name);

    void clearWarps();

    Set<String> getInvites();

    Set<String> invites();

    @Deprecated
    String getId();

    void invite(FPlayer fplayer);

    void deinvite(FPlayer fplayer);

    void deInvite(FPlayer fplayer);

    boolean isInvited(FPlayer fplayer);

    boolean hasInvite(FPlayer fplayer);

    boolean getOpen();

    boolean open();

    void setOpen(boolean isOpen);

    void open(boolean isOpen);

    boolean isPeaceful();

    void setPeaceful(boolean isPeaceful);

    void peaceful(boolean isPeaceful);

    void setPeacefulExplosionsEnabled(boolean val);

    void peacefulExplosionsEnabled(boolean val);

    boolean getPeacefulExplosionsEnabled();

    boolean peacefulExplosionsEnabled();

    boolean noExplosionsInTerritory();

    boolean isPermanent();

    void setPermanent(boolean isPermanent);

    void permanent(boolean isPermanent);

    String getTag();

    String tag();

    String getTag(String prefix);

    String getTag(Faction otherFaction);

    String tag(Faction otherFaction);

    String getTag(FPlayer otherFplayer);

    void setTag(String str);

    void tag(String str);

    String getComparisonTag();

    String getDescription();

    String description();

    void setDescription(String value);

    void description(String value);

    void setHome(Location home);

    void home(Location home);

    void delHome();

    void removeHome();

    boolean hasHome();

    Location getHome();

    Location home();

    long getFoundedDate();

    Instant founded();

    void setFoundedDate(long newDate);

    void founded(Instant instant);

    void confirmValidHome();

    boolean noPvPInTerritory();

    boolean noMonstersInTerritory();

    boolean isNormal();

    /**
     * Players in the wilderness faction are consdiered not in a faction.
     *
     * @return true if wilderness
     * @deprecated use {@link #isWilderness()} instead
     */
    @Deprecated
    default boolean isNone()
    {
        return isWilderness();
    }

    boolean isWilderness();

    boolean isSafeZone();

    boolean isWarZone();

    boolean isPlayerFreeType();

//    /**
//     * Get the access of a selectable for a given chunk.
//     *
//     * @param selectable        selectable
//     * @param permissibleAction permissible
//     * @param location          location
//     * @return player's access
//     */
//    boolean hasAccess(Selectable selectable, PermissibleAction permissibleAction, FLocation location);

    int getLandRounded();

    int getLandRoundedInWorld(String worldName);

    // -------------------------------
    // Relation and relation colors
    // -------------------------------

    // Relation getRelationWish(Faction otherFaction);

    // void setRelationWish(Faction otherFaction, Relation relation);

    // int getRelationCount(Relation relation);

    // ----------------------------------------------//
    // DTR (Deaths 'til Raidable)
    // ----------------------------------------------//
    
    // double getDTR();

    // double getDTRWithoutUpdate();

    // void setDTR(double dtr);

    // long getLastDTRUpdateTime();

    // long getFrozenDTRUntilTime();

    // void setFrozenDTR(long time);

    // boolean isFrozenDTR();

    // ----------------------------------------------//
    // Power
    // ----------------------------------------------//
    double getPower();

    double powerExact();

    double getPowerMax();

    double powerMaxExact();

    int getPowerRounded();

    int power();

    int getPowerMaxRounded();

    int powerMax();

    // Integer getPermanentPower();

    // Integer permanentPower();

    // void setPermanentPower(Integer permanentPower);

    // void permanentPower(Integer permanentPower);

    boolean hasPermanentPower();

    double getPowerBoost();

    double powerBoost();

    void setPowerBoost(double powerBoost);

    void powerBoost(double powerBoost);

    // boolean hasLandInflation();

    boolean isPowerFrozen();

    // -------------------------------
    // FPlayers
    // -------------------------------

    // maintain the reference list of FPlayers in this faction
    void refreshFPlayers();

    boolean addFPlayer(FPlayer fplayer);

    boolean removeFPlayer(FPlayer fplayer);

    int getSize();

    int size();

    Set<FPlayer> getFPlayers();

    Set<FPlayer> members();

    Set<FPlayer> getFPlayersWhereOnline(boolean online);

    Set<FPlayer> membersOnline(boolean online);

    Set<FPlayer> getFPlayersWhereOnline(boolean online, FPlayer viewer);

    Set<FPlayer> membersOnline(boolean online, FPlayer viewer);

    FPlayer getFPlayerAdmin();

    FPlayer admin();

    // List<FPlayer> getFPlayersWhereRole(Role role);

    // List<FPlayer> members(Role role);

    List<Player> getOnlinePlayers();

    List<Player> membersOnlineAsPlayers();

    // slightly faster check than getOnlinePlayers() if you just want to see if
    // there are any players online
    boolean hasPlayersOnline();

    boolean hasMembersOnline();

    void memberLoggedOff();

    void trackMemberLoggedOff();

    // used when current leader is about to be removed from the faction;
    // promotes new leader, or disbands faction if no other members left
    void promoteNewLeader();

    // Role getDefaultRole();

    // Role defaultRole();

    // void setDefaultRole(Role role);

    // void defaultRole(Role role);

    void sendMessage(String message);

    void sendMessage(List<String> messages);

    // ----------------------------------------------//
    // Ownership of specific claims
    // ----------------------------------------------//

    // Map<FLocation, Set<String>> getClaimOwnership();

    // void clearAllClaimOwnership();

    // void clearClaimOwnership(FLocation loc);

    // void clearClaimOwnership(FPlayer player);

    // int getCountOfClaimsWithOwners();

    // boolean doesLocationHaveOwnersSet(FLocation loc);

    // boolean isPlayerInOwnerList(FPlayer player, FLocation loc);

    // void setPlayerAsOwner(FPlayer player, FLocation loc);

    // void removePlayerAsOwner(FPlayer player, FLocation loc);

    // Set<String> getOwnerList(FLocation loc);

    // String getOwnerListString(FLocation loc);

    // boolean playerHasOwnershipRights(FPlayer fplayer, FLocation loc);

    // ----------------------------------------------//
    // Persistance and entity management
    // ----------------------------------------------//
    // void remove();

    // Set<FLocation> getAllClaims();

    // Set<FLocation> claims();

    // int claimCount();

    // int claimCount(World world);
    
}
