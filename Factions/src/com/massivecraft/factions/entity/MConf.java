package com.massivecraft.factions.entity;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.event.EventFactionsChunkChangeType;
import com.massivecraft.factions.integration.dynmap.DynmapStyle;
import com.massivecraft.factions.integration.dynmap.IntegrationDynmap;
import com.massivecraft.massivecore.collections.BackstringSet;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.collections.WorldExceptionSet;
import com.massivecraft.massivecore.command.editor.annotation.EditorName;
import com.massivecraft.massivecore.command.editor.annotation.EditorType;
import com.massivecraft.massivecore.command.editor.annotation.EditorTypeInner;
import com.massivecraft.massivecore.command.editor.annotation.EditorVisible;
import com.massivecraft.massivecore.command.type.TypeMillisDiff;
import com.massivecraft.massivecore.store.Entity;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.TimeUnit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventPriority;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EditorName("config")
public class MConf extends Entity<MConf>
{
	// -------------------------------------------- //
	// META
	// -------------------------------------------- //

	protected static transient MConf i;
	public static MConf get() { return i; }
	
	// -------------------------------------------- //
	// OVERRIDE: ENTITY
	// -------------------------------------------- //
	
	@Override
	public MConf load(MConf that)
	{
		super.load(that);
		return this;
	}
	
	// -------------------------------------------- //
	// VERSION
	// -------------------------------------------- //
	
	public int version = 6;
	
	// -------------------------------------------- //
	// COMMAND ALIASES
	// -------------------------------------------- //
	
	// Don't you want "f" as the base command alias? Simply change it here.
	public List<String> aliasesF = MUtil.list("f");
	
	// -------------------------------------------- //
	// WORLDS FEATURE ENABLED
	// -------------------------------------------- //
	
	// Use this blacklist/whitelist system to toggle features on a per world basis.
	// Do you only want claiming enabled on the one map called "Hurr"?
	// In such case set standard to false and add "Hurr" as an exeption to worldsClaimingEnabled.
	public WorldExceptionSet worldsClaimingEnabled = new WorldExceptionSet();
	public WorldExceptionSet worldsPowerLossEnabled = new WorldExceptionSet();
	public WorldExceptionSet worldsPowerGainEnabled = new WorldExceptionSet();
	
	public WorldExceptionSet worldsPvpRulesEnabled = new WorldExceptionSet();
	
	// -------------------------------------------- //
	// DERPY OVERRIDES
	// -------------------------------------------- //
	
	// Add player names here who should bypass all protections.
	// Should /not/ be used for admins. There is "/f adminmode" for that.
	// This is for other plugins/mods that use a fake player to take actions, which shouldn't be subject to our protections.
	public Set<String> playersWhoBypassAllProtection = new MassiveSet<>();
	
	// -------------------------------------------- //
	// REMOVE DATA
	// -------------------------------------------- //
	
	// Should players be kicked from their faction and their data erased when banned?
	public boolean removePlayerWhenBanned = true;
	
	// After how many milliseconds should players be automatically kicked from their faction?
	
	// The Default
	@EditorType(TypeMillisDiff.class)
	public long cleanInactivityToleranceMillis = 10 * TimeUnit.MILLIS_PER_DAY; // 10 days
	
	// Player Age Bonus
	@EditorTypeInner({TypeMillisDiff.class, TypeMillisDiff.class})
	public Map<Long, Long> cleanInactivityToleranceMillisPlayerAgeToBonus = MUtil.map(
		2 * TimeUnit.MILLIS_PER_WEEK, 10 * TimeUnit.MILLIS_PER_DAY  // +10 days after 2 weeks
	);
	
	// Faction Age Bonus
	@EditorTypeInner({TypeMillisDiff.class, TypeMillisDiff.class})
	public Map<Long, Long> cleanInactivityToleranceMillisFactionAgeToBonus = MUtil.map(
		4 * TimeUnit.MILLIS_PER_WEEK, 10 * TimeUnit.MILLIS_PER_DAY, // +10 days after 4 weeks
		2 * TimeUnit.MILLIS_PER_WEEK,  5 * TimeUnit.MILLIS_PER_DAY  // +5 days after 2 weeks
	);
	
	// -------------------------------------------- //
	// DEFAULTS
	// -------------------------------------------- //
	
	// Which faction should new players be followers of?
	// "none" means Wilderness. Remember to specify the id, like "3defeec7-b3b1-48d9-82bb-2a8903df24e3" and not the name.
	public String defaultPlayerFactionId = Factions.ID_NONE;
	
	// What power should the player start with?
	public double defaultPlayerPower = 0.0;
	
	// -------------------------------------------- //
	// MOTD
	// -------------------------------------------- //
	
	// During which event priority should the faction message of the day be displayed?
	// Choose between: LOWEST, LOW, NORMAL, HIGH, HIGHEST and MONITOR.
	// This setting only matters if "motdDelayTicks" is set to -1
	public EventPriority motdPriority = EventPriority.NORMAL;
	
	// How many ticks should we delay the faction message of the day with?
	// -1 means we don't delay at all. We display it at once.
	// 0 means it's deferred to the upcoming server tick.
	// 5 means we delay it yet another 5 ticks.
	public int motdDelayTicks = -1;

	// -------------------------------------------- //
	// POWER
	// -------------------------------------------- //
	
	// What is the maximum player power?
	public double powerMax = 10.0;
	
	// What is the minimum player power?
	// NOTE: Negative minimum values is possible.
	public double powerMin = 0.0;
	
	// How much power should be regained per hour online on the server?
	public double powerPerHour = 2.0;
	
	// How much power should be lost on death?
	public double powerPerDeath = -2.0;
	
	// Can players with negative power leave their faction?
	// NOTE: This only makes sense to set to false if your "powerMin" setting is negative.
	public boolean canLeaveWithNegativePower = true;
	
	// -------------------------------------------- //
	// CORE
	// -------------------------------------------- //

	// Is there a maximum amount of members per faction?
	// 0 means there is not. If you set it to 100 then there can at most be 100 members per faction.
	public int factionMemberLimit = 0;
	
	// Is there a maximum faction power cap?
	// 0 means there is not. Set it to a positive value in case you wan't to use this feature.
	public double factionPowerMax = 0.0;
	
	// Limit the length of faction names here.
	public int factionNameLengthMin = 3;
	public int factionNameLengthMax = 16;

	// -------------------------------------------- //
	// SET LIMITS
	// -------------------------------------------- //
	
	// When using radius setting of faction territory, what is the maximum radius allowed?
	public int setRadiusMax = 30;
	
	// When using fill setting of faction territory, what is the maximum chunk count allowed?
	public int setFillMax = 1000;
	
	// -------------------------------------------- //
	// CLAIMS
	// -------------------------------------------- //
	
	// Must claims be connected to each other?
	// If you set this to false you will allow factions to claim more than one base per world map.
	// That would makes outposts possible but also potentially ugly weird claims messing up your Dynmap and ingame experiance.
	public boolean claimsMustBeConnected = true;

	// Must claims be connected to each other enforced strictly?
	// If this is enabled there is also done a check on
	// unclaim which makes sure you can't make two different bases by unclaiming land.
	public boolean claimsMustBeConnectedStrict = false;
	
	// Would you like to allow unconnected claims when conquering land from another faction?
	// Setting this to true would allow taking over someone elses base even if claims normally have to be connected.
	// Note that even without this you can pillage/unclaim another factions territory in war.
	// You just won't be able to take the land as your own.
	public boolean claimsCanBeUnconnectedIfOwnedByOtherFaction = false;
	
	// Is claiming from other factions even allowed?
	// Set this to false to disable territorial warfare altogether.
	public boolean claimingFromOthersAllowed = true;

	// Is it required for factions to have an inflated land/power ratio in order to have their land conquered by another faction?
 	// Set this to false to allow factions to invade each other without requiring them to have an inflated land/power ratio..
	public boolean claimingFromOthersMustBeInflated = true;
	
	// Is a minimum distance (measured in chunks) to other factions required?
	// 0 means the feature is disabled.
	// Set the feature to 10 and there must be 10 chunks of wilderness between factions.
	// Factions may optionally allow their allies to bypass this limit by configuring their faction permissions ingame themselves.
	public int claimMinimumChunksDistanceToOthers = 0;
	
	// Do you need a minimum amount of faction members to claim land?
	// 1 means just the faction leader alone is enough.
	public int claimsRequireMinFactionMembers = 1;
	
	// Is there a maximum limit to chunks claimed?
	// 0 means there isn't.
	public int claimedLandsMax = 0;
	
	// The max amount of worlds in which a player can have claims in.
	public int claimedWorldsMax = -1;
	
	// -------------------------------------------- //
	// PROTECTION
	// -------------------------------------------- //
	
	public boolean protectionLiquidFlowEnabled = true;

	// Protects the faction land from piston extending/retracting
	// through the denying of MPerm build
	public boolean handlePistonProtectionThroughDenyBuild = true;
	
	// -------------------------------------------- //
	// WARPS
	// -------------------------------------------- //
	
	// Is the warps feature enabled?
	// If you set this to false players can't set warps or teleport to a warp.
	public boolean warpsEnabled = true;

	// How many warps can they have?
	public int warpsMax = 1;

	// Must warps be located inside the faction's territory?
	// It's usually a wise idea keeping this true.
	// Otherwise players can set their warps inside enemy territory.
	public boolean warpsMustBeInClaimedTerritory = true;

	// And what faction warp should be used when a player types /f home
	public String warpsHomeName = "home";

	// These options can be used to limit rights to warp under different circumstances.
	public boolean warpsTeleportAllowedFromEnemyTerritory = true;
	public boolean warpsTeleportAllowedFromDifferentWorld = true;
	public double warpsTeleportAllowedEnemyDistance = 32.0;
	public boolean warpsTeleportIgnoreEnemiesIfInOwnTerritory = true;
	
	// Should players teleport to faction warp on death?
	// Set this to true to override the default respawn location.
	public boolean warpsTeleportToOnDeathActive = false;

	// And what faction warp should it be? It must have a specific name.
	public String warpsTeleportToOnDeathName = "home";
	
	// This value can be used to tweak compatibility with other plugins altering the respawn location.
	// Choose between: LOWEST, LOW, NORMAL, HIGH, HIGHEST and MONITOR.
	public EventPriority warpsTeleportToOnDeathPriority = EventPriority.NORMAL;

	// -------------------------------------------- //
	// TERRITORY INFO
	// -------------------------------------------- //
	
	public boolean territoryInfoTitlesDefault = true;

	public String territoryInfoTitlesMain = "{relcolor}{name}";
	public String territoryInfoTitlesSub = "<i>{desc}";
	public int territoryInfoTitlesTicksIn = 5;
	public int territoryInfoTitlesTicksStay = 60;
	public int territoryInfoTitleTicksOut = 5;

	public String territoryInfoChat = "<i> ~ {relcolor}{name} <i>{desc}";

	public boolean territoryAccessShowMessage = true;
	
	// -------------------------------------------- //
	// ASSORTED
	// -------------------------------------------- //
	
	// Set this to true if want to block the promotion of new leaders for permanent factions.
	// I don't really understand the user case for this option.
	public boolean permanentFactionsDisableLeaderPromotion = false;
	
	// How much health damage should a player take upon placing or breaking a block in a "pain build" territory?
	// 2.0 means one heart.
	public double actionDeniedPainAmount = 2.0D;
	
	// If you set this option to true then factionless players cant partake in PVP.
	// It works in both directions. Meaning you must join a faction to hurt players and get hurt by players.
	public boolean disablePVPForFactionlessPlayers = false;
	
	// If you set this option to true then factionless players cant damage each other.
	// So two factionless can't PvP, but they can PvP with others if that is allowed.
	public boolean enablePVPBetweenFactionlessPlayers = true;
	
	// Set this option to true to create an exception to the rule above.
	// Players inside their own faction territory can then hurt facitonless players.
	// This way you may "evict" factionless trolls messing around in your home base.
	public boolean enablePVPAgainstFactionlessInAttackersLand = false;
	
	// Inside your own faction territory you take less damage.
	// 0.1 means that you take 10% less damage at home.
	public double territoryShieldFactor = 0.1D;

	// Make faction disbanding a confirmation thing
	public boolean requireConfirmationForFactionDisbanding = true;

	// At what speed can players fly with /f fly?
	public float flySpeed = 0.1f;

	// Will flying be disabled on pvp
	public boolean flyDisableOnPvp = false;

	// -------------------------------------------- //
	// DENY COMMANDS
	// -------------------------------------------- //
	
	// A list of commands to block for members of permanent factions.
	// I don't really understand the user case for this option.
	public List<String> denyCommandsPermanentFactionMember = new ArrayList<>();

	// Lists of commands to deny depending on your relation to the current faction territory.
	// You may for example not type /home (might be the plugin Essentials) in the territory of your enemies.
	public Map<Rel, List<String>> denyCommandsTerritoryRelation = MUtil.map(
		Rel.ENEMY, MUtil.list(
			// Essentials commands
			"home",
			"homes",
			"sethome",
			"createhome",
			"tpahere",
			"tpaccept",
			"tpyes",
			"tpa",
			"call",
			"tpask",
			"warp",
			"warps",
			"spawn",
			// Essentials e-alliases
			"ehome",
			"ehomes",
			"esethome",
			"ecreatehome",
			"etpahere",
			"etpaccept",
			"etpyes",
			"etpa",
			"ecall",
			"etpask",
			"ewarp",
			"ewarps",
			"espawn",
			// Essentials fallback alliases
			"essentials:home",
			"essentials:homes",
			"essentials:sethome",
			"essentials:createhome",
			"essentials:tpahere",
			"essentials:tpaccept",
			"essentials:tpyes",
			"essentials:tpa",
			"essentials:call",
			"essentials:tpask",
			"essentials:warp",
			"essentials:warps",
			"essentials:spawn",
			// Other plugins
			"wtp",
			"uspawn",
			"utp",
			"mspawn",
			"mtp",
			"fspawn",
			"ftp",
			"jspawn",
			"jtp"
		),
		Rel.NEUTRAL, new ArrayList<String>(),
		Rel.TRUCE, new ArrayList<String>(),
		Rel.ALLY, new ArrayList<String>(),
		Rel.FACTION, new ArrayList<String>()
	);
	
	// The distance for denying the following commands. Set to -1 to disable.
	public double denyCommandsDistance = -1;
	
	// Lists of commands to deny depending on your relation to a nearby enemy in the above distance.
	public Map<Rel, List<String>> denyCommandsDistanceRelation = MUtil.map(
		Rel.ENEMY, MUtil.list(
			"home"
		),
		Rel.NEUTRAL, new ArrayList<String>(),
		Rel.TRUCE, new ArrayList<String>(),
		Rel.ALLY, new ArrayList<String>(),
		Rel.FACTION, new ArrayList<String>()
	);
	
	// Allow bypassing the above setting when in these territories.
	public List<Rel> denyCommandsDistanceBypassIn = MUtil.list(
		Rel.FACTION,
		Rel.ALLY
	);
	
	// -------------------------------------------- //
	// COLORS
	// -------------------------------------------- //
	
	// Here you can alter the colors tied to certain faction relations and settings.
	// You probably don't want to edit these to much.
	// Doing so might confuse players that are used to Factions.
	public ChatColor colorMember = ChatColor.GREEN;
	public ChatColor colorAlly = ChatColor.DARK_PURPLE;
	public ChatColor colorTruce = ChatColor.LIGHT_PURPLE;
	public ChatColor colorNeutral = ChatColor.WHITE;
	public ChatColor colorEnemy = ChatColor.RED;
	
	// This one is for example applied to SafeZone since that faction has the pvp flag set to false.
	public ChatColor colorNoPVP = ChatColor.GOLD;
	
	// This one is for example applied to WarZone since that faction has the friendly fire flag set to true.
	public ChatColor colorFriendlyFire = ChatColor.DARK_RED;

	// -------------------------------------------- //
	// EXPLOITS
	// -------------------------------------------- //
	
	public boolean handleExploitObsidianGenerators = true;
	public boolean handleExploitEnderPearlClipping = true;
	public boolean handleNetherPortalTrap = true;
	
	// -------------------------------------------- //
	// UNSTUCK
	// -------------------------------------------- //
	
	public int unstuckSeconds = 30;
	public int unstuckChunkRadius = 10;

	// -------------------------------------------- //
	// ENDER PEARL AND CHORUS FRUIT
	// -------------------------------------------- //

	public boolean allowEnderPearlInEnemyTerritory = true;
	public boolean allowEnderPearlInWildernessTerritory = true;
	public boolean allowEnderPearlInOwnTerritory = true;
	public boolean allowEnderPearlInOtherTerritory = true;

	public boolean allowChorusFruitInEnemyTerritory = true;
	public boolean allowChorusFruitInWildernessTerritory = true;
	public boolean allowChorusFruitInOwnTerritory = true;
	public boolean allowChorusFruitInOtherTerritory = true;

	// -------------------------------------------- //
	// LOGGING
	// -------------------------------------------- //
	
	// Here you can disable logging of certain events to the server console.
	
	public boolean logFactionCreate = true;
	public boolean logFactionDisband = true;
	public boolean logFactionJoin = true;
	public boolean logFactionKick = true;
	public boolean logFactionLeave = true;
	public boolean logLandClaims = true;
	public boolean logMoneyTransactions = true;

	// -------------------------------------------- //
	// TAX
	// -------------------------------------------- //

	// Should the tax system be enabled?
	public boolean taxEnabled = false;

	// How much can you tax a player?
	public double taxPlayerMinimum = -10;
	public double taxPlayerMaximum = 10;

	// How much should Factions be taxed?
	//public double taxUpkeepBase = 0;
	//public double taxUpkeepPerChunk = 1;

	// When is a player inactive?
	public int taxInactiveDays = 3;

	// When the last run time was (in unix time)
	// 0 means never
	public long taxTaskLastMillis = 0;

	// Tax run when?
	// 0 means at midnight UTC it can be offset by a certain millis
	public long taxTaskInvocationOffsetMillis = 0;

	// How often should the task be run?
	public long taxTaskPeriodMillis = TimeUnit.MILLIS_PER_DAY;

	// -------------------------------------------- //
	// RANKS
	// -------------------------------------------- //

	@EditorVisible(false)
	public List<Rank> defaultRanks = MUtil.list(
		new Rank("Leader", 400, "**"),
		new Rank("Officer", 300, "*"),
		new Rank("Member", 200, "+"),
		new Rank("Recruit", 100, "-")
	);

	// -------------------------------------------- //
	// PERMISSIONS
	// -------------------------------------------- //
	
	public Map<String, Set<String>> perm2default = MUtil.map(
		MPerm.ID_BUILD, MUtil.set("LEADER", "OFFICER", "MEMBER"),
		MPerm.ID_PAINBUILD, MUtil.set(),
		MPerm.ID_DOOR, MUtil.set("LEADER", "OFFICER", "MEMBER", "RECRUIT", "ALLY"),
		MPerm.ID_BUTTON, MUtil.set("LEADER", "OFFICER", "MEMBER", "RECRUIT", "ALLY"),
		MPerm.ID_PRESSURE_PLATE, MUtil.set("LEADER", "OFFICER", "MEMBER", "RECRUIT", "ALLY"),
		MPerm.ID_LEVER, MUtil.set("LEADER", "OFFICER", "MEMBER", "RECRUIT", "ALLY"),
		MPerm.ID_CONTAINER, MUtil.set("LEADER", "OFFICER", "MEMBER"),
		MPerm.ID_LEASH, MUtil.set("LEADER", "OFFICER", "MEMBER", "RECRUIT", "ALLY"),

		MPerm.ID_NAME, MUtil.set("LEADER"),
		MPerm.ID_DESC, MUtil.set("LEADER", "OFFICER"),
		MPerm.ID_MOTD, MUtil.set("LEADER", "OFFICER"),
		MPerm.ID_INVITE, MUtil.set("LEADER", "OFFICER"),
		MPerm.ID_KICK, MUtil.set("LEADER", "OFFICER"),
		MPerm.ID_RANK, MUtil.set("LEADER", "OFFICER"),
		MPerm.ID_TITLE, MUtil.set("LEADER", "OFFICER"),
		MPerm.ID_WARP, MUtil.set("LEADER", "OFFICER", "MEMBER", "RECRUIT", "ALLY"),
		MPerm.ID_SETWARP, MUtil.set("LEADER", "OFFICER"),
		MPerm.ID_DEPOSIT, MUtil.set("LEADER", "OFFICER", "MEMBER", "RECRUIT", "ALLY", "TRUCE", "NEUTRAL", "ENEMY"),
		MPerm.ID_WITHDRAW, MUtil.set("LEADER"),
		MPerm.ID_TERRITORY, MUtil.set("LEADER", "OFFICER"),
		MPerm.ID_ACCESS, MUtil.set("LEADER", "OFFICER"),
		MPerm.ID_VOTE, MUtil.set("LEADER", "OFFICER", "MEMBER", "RECRUIT"),
		MPerm.ID_CREATEVOTE, MUtil.set("LEADER", "OFFICER"),
		MPerm.ID_CLAIMNEAR, MUtil.set("LEADER", "OFFICER", "MEMBER", "RECRUIT", "ALLY"),
		MPerm.ID_TAX, MUtil.set("LEADER"),
		MPerm.ID_REL, MUtil.set("LEADER", "OFFICER"),
		MPerm.ID_DISBAND, MUtil.set("LEADER"),
		MPerm.ID_FLAGS, MUtil.set("LEADER"),
		MPerm.ID_PERMS, MUtil.set("LEADER")
	);
	
	// -------------------------------------------- //
	// ENUMERATIONS
	// -------------------------------------------- //
	// In this configuration section you can add support for Forge mods that add new Materials and EntityTypes.
	// This way they can be protected in Faction territory.
	// Use the "UPPER_CASE_NAME" for the Material or EntityType in question.
	// If you are running a regular Spigot server you don't have to edit this section.
	// In fact all of these sets can be empty on regular Spigot servers without any risk.
	
	// Interacting with these materials when they are already placed in the terrain results in an edit.
	public BackstringSet<Material> materialsEditOnInteract = new BackstringSet<>(Material.class);
	
	// Interacting with the the terrain holding this item in hand results in an edit.
	// There's no need to add all block materials here. Only special items other than blocks.
	public BackstringSet<Material> materialsEditTools = new BackstringSet<>(Material.class);
	
	// Interacting with these materials placed in the terrain results in door toggling.
	public BackstringSet<Material> materialsDoor = new BackstringSet<>(Material.class);
	
	// Interacting with these materials placed in the terrain results in opening a container.
	public BackstringSet<Material> materialsContainer = new BackstringSet<>(Material.class);

	// Interacting with these materials placed in the terrain results in button toggling.
	public BackstringSet<Material> materialsButton = new BackstringSet<>(Material.class);

	// Interacting with these materials placed in the terrain results in pressure plate activation.
	public BackstringSet<Material> materialsPressurePlate = new BackstringSet<>(Material.class);
	
	// Interacting with these entities results in an edit.
	public BackstringSet<EntityType> entityTypesEditOnInteract = new BackstringSet<>(EntityType.class);
	
	// Damaging these entities results in an edit.
	public BackstringSet<EntityType> entityTypesEditOnDamage = new BackstringSet<>(EntityType.class);
	
	// Interacting with these entities results in opening a container.
	public BackstringSet<EntityType> entityTypesContainer = new BackstringSet<>(EntityType.class);

	// The complete list of entities considered to be boats.
	public BackstringSet<EntityType> entityTypesBoats = new BackstringSet<>(EntityType.class);
	
	// The complete list of entities considered to be monsters.
	public BackstringSet<EntityType> entityTypesMonsters = new BackstringSet<>(EntityType.class);
	
	// List of entities considered to be animals.
	public BackstringSet<EntityType> entityTypesAnimals = new BackstringSet<>(EntityType.class);

	// -------------------------------------------- //
	// INTEGRATION: LWC
	// -------------------------------------------- //
	
	// Do you need faction build rights in the territory to create an LWC protection there?
	public boolean lwcMustHaveBuildRightsToCreate = true;
	
	// The config option above does not handle situations where a player creates an LWC protection in Faction territory and then leaves the faction.
	// The player would then have an LWC protection in a territory where they can not build.
	// Set this config option to true to enable an automatic removal feature.
	// LWC protections that couldn't be created will be removed on an attempt to open them by any player.
	public boolean lwcRemoveIfNoBuildRights = false;
	
	// WARN: Experimental and semi buggy.
	// If you change this to true: alien LWC protections will be removed upon using /f set.
	public Map<EventFactionsChunkChangeType, Boolean> lwcRemoveOnChange = MUtil.map(
		EventFactionsChunkChangeType.BUY, false, // when claiming from wilderness
		EventFactionsChunkChangeType.SELL, false, // when selling back to wilderness
		EventFactionsChunkChangeType.CONQUER, false, // when claiming from another player faction
		EventFactionsChunkChangeType.PILLAGE, false // when unclaiming (to wilderness) from another player faction
	);
	
	// -------------------------------------------- //
	// INTEGRATION: WorldGuard
	// -------------------------------------------- //
	
	// Global WorldGuard Integration Switch
	public boolean worldguardCheckEnabled = false;
	
	// Enable the WorldGuard check per-world 
	// Specify which worlds the WorldGuard Check can be used in
	public WorldExceptionSet worldguardCheckWorldsEnabled = new WorldExceptionSet();

	// -------------------------------------------- //
	// INTEGRATION: ECONOMY
	// -------------------------------------------- //
	
	// Should economy features be enabled?
	// This requires that you have the external plugin called "Vault" installed.
	public boolean econEnabled = true;
	
	// When paying a cost you may specify an account that should receive the money here.
	// Per default "" the money is just destroyed.
	public String econUniverseAccount = "";
	
	// What is the price per chunk when using /f set?
	public Map<EventFactionsChunkChangeType, Double> econChunkCost = MUtil.map(
		EventFactionsChunkChangeType.BUY, 1.0, // when claiming from wilderness
		EventFactionsChunkChangeType.SELL, 0.0, // when selling back to wilderness
		EventFactionsChunkChangeType.CONQUER, 0.0, // when claiming from another player faction
		EventFactionsChunkChangeType.PILLAGE, 0.0 // when unclaiming (to wilderness) from another player faction
	);
	
	// What is the price to create a faction?
	public double econCostCreate = 100.0;
	
	// And so on and so forth ... you get the idea.
	public double econCostWarpAdd = 0.0;
	public double econCostWarpRemove = 0.0;
	public double econCostJoin = 0.0;
	public double econCostLeave = 0.0;
	public double econCostKick = 0.0;
	public double econCostInvite = 0.0;
	public double econCostDeinvite = 0.0;
	public double econCostWarpGo = 0.0;
	public double econCostName = 0.0;
	public double econCostDescription = 0.0;
	public double econCostTitle = 0.0;
	public double econCostFlag = 0.0;
	
	public Map<Rel, Double> econRelCost = MUtil.map(
		Rel.ENEMY, 0.0,
		Rel.ALLY, 0.0,
		Rel.TRUCE, 0.0,
		Rel.NEUTRAL, 0.0
	);
	
	// Should the faction bank system be enabled?
	// This enables the command /f money.
	public boolean bankEnabled = true;
	
	// That costs should the faciton bank take care of?
	// If you set this to false the player executing the command will pay instead.
	public boolean bankFactionPaysCosts = true;

	public boolean useNewMoneySystem = false;

	// -------------------------------------------- //
	// INTEGRATION: DYNMAP
	// -------------------------------------------- //

	// Should the dynmap intagration be used?
	public boolean dynmapEnabled = true;

	// Should the dynmap updates be logged to console output?
	public boolean dynmapLogTimeSpent = false;

	// Name of the Factions layer
	public String dynmapLayerName = "Factions";

	// Should the layer be visible per default
	public boolean dynmapLayerHiddenByDefault = false;

	// Ordering priority in layer menu (low goes before high - default is 0)
	public int dynmapLayerPriority = 2;

	// (optional) set minimum zoom level before layer is visible (0 = defalt, always visible)
	public int dynmapLayerMinimumZoom = 0;

	// Format for popup - substitute values for macros
	//public String dynmapInfowindowFormat = "<div class=\"infowindow\"><span style=\"font-size:120%;\">%regionname%</span><br />Flags<br /><span style=\"font-weight:bold;\">%flags%</span></div>";
	public String dynmapFactionDescription =
		"<div class=\"infowindow\">\n" +
		"<span style=\"font-weight: bold; font-size: 150%;\">%name%</span></br>\n" +
		"<span style=\"font-style: italic; font-size: 110%;\">%description%</span></br>\n" +
		"</br>\n" +
		"<span style=\"font-weight: bold;\">Leader:</span> %players.leader%</br>\n" +
		"<span style=\"font-weight: bold;\">Members:</span> %players%</br>\n" +
		"</br>\n" +
		"<span style=\"font-weight: bold;\">Age:</span> %age%</br>\n" +
		"<span style=\"font-weight: bold;\">Bank:</span> %money%</br>\n" +
		"</br>\n" +
		"<span style=\"font-weight: bold;\">Flags:</span></br>\n" +
		"%flags.table3%\n" +
		"</div>";

	// Enable the %money% macro. Only do this if you know your economy manager is thread safe.
	public boolean dynmapShowMoneyInDescription = false;

	// Allow players in faction to see one another on Dynmap (only relevant if Dynmap has 'player-info-protected' enabled)
	//public boolean dynmapVisibilityByFaction = true;

	// Optional setting to limit which regions to show.
	// If empty all regions are shown.
	// Specify Faction either by name or UUID.
	// To show all regions on a given world, add 'world:<worldname>' to the list.
	public Set<String> dynmapVisibleFactions = new MassiveSet<>();

	// Optional setting to hide specific Factions.
	// Specify Faction either by name or UUID.
	// To hide all regions on a given world, add 'world:<worldname>' to the list.
	public Set<String> dynmapHiddenFactions = new MassiveSet<>();

	@EditorVisible(false)
	public DynmapStyle dynmapDefaultStyle = new DynmapStyle(
		IntegrationDynmap.DYNMAP_STYLE_LINE_COLOR,
		IntegrationDynmap.DYNMAP_STYLE_LINE_OPACITY,
		IntegrationDynmap.DYNMAP_STYLE_LINE_WEIGHT,
		IntegrationDynmap.DYNMAP_STYLE_FILL_COLOR,
		IntegrationDynmap.DYNMAP_STYLE_FILL_OPACITY,
		IntegrationDynmap.DYNMAP_STYLE_HOME_MARKER,
		IntegrationDynmap.DYNMAP_STYLE_BOOST
	);

	// Optional per Faction style overrides. Any defined replace those in dynmapDefaultStyle.
	// Specify Faction either by name or UUID.
	@EditorVisible(false)
	public Map<String, DynmapStyle> dynmapFactionStyles = MUtil.map(
		"SafeZone", new DynmapStyle().withLineColor("#FF00FF").withFillColor("#FF00FF").withBoost(false),
		"WarZone", new DynmapStyle().withLineColor("#FF0000").withFillColor("#FF0000").withBoost(false)
	);
}
