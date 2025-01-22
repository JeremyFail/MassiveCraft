package com.massivecraft.massivecore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
//import com.google.gson.JsonArray;
//import com.google.gson.JsonNull;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonPrimitive;
import com.massivecraft.massivecore.adapter.AdapterBackstringSet;
import com.massivecraft.massivecore.adapter.AdapterEntityInternalMap;
import com.massivecraft.massivecore.adapter.AdapterEntry;
//import com.massivecraft.massivecore.adapter.AdapterJsonElement;
import com.massivecraft.massivecore.adapter.AdapterMassiveList;
import com.massivecraft.massivecore.adapter.AdapterMassiveMap;
import com.massivecraft.massivecore.adapter.AdapterMassiveSet;
import com.massivecraft.massivecore.adapter.AdapterMassiveTreeMap;
import com.massivecraft.massivecore.adapter.AdapterMassiveTreeSet;
import com.massivecraft.massivecore.adapter.AdapterModdedEnumType;
import com.massivecraft.massivecore.adapter.AdapterMson;
import com.massivecraft.massivecore.adapter.AdapterMsonEvent;
import com.massivecraft.massivecore.adapter.AdapterSound;
import com.massivecraft.massivecore.adapter.AdapterUUID;
import com.massivecraft.massivecore.cmd.CmdMassiveCore;
import com.massivecraft.massivecore.cmd.CmdMassiveCoreBuffer;
import com.massivecraft.massivecore.cmd.CmdMassiveCoreClick;
import com.massivecraft.massivecore.cmd.CmdMassiveCoreCmdurl;
import com.massivecraft.massivecore.cmd.CmdMassiveCoreStore;
import com.massivecraft.massivecore.cmd.CmdMassiveCoreUsys;
import com.massivecraft.massivecore.collections.BackstringSet;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.collections.MassiveListDef;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.collections.MassiveMapDef;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.collections.MassiveSetDef;
import com.massivecraft.massivecore.collections.MassiveTreeMap;
import com.massivecraft.massivecore.collections.MassiveTreeMapDef;
import com.massivecraft.massivecore.collections.MassiveTreeSet;
import com.massivecraft.massivecore.collections.MassiveTreeSetDef;
import com.massivecraft.massivecore.command.type.RegistryType;
import com.massivecraft.massivecore.engine.EngineMassiveCoreChestGui;
import com.massivecraft.massivecore.engine.EngineMassiveCoreClean;
import com.massivecraft.massivecore.engine.EngineMassiveCoreCollTick;
import com.massivecraft.massivecore.engine.EngineMassiveCoreCommandRegistration;
import com.massivecraft.massivecore.engine.EngineMassiveCoreCommandSet;
import com.massivecraft.massivecore.engine.EngineMassiveCoreDatabase;
import com.massivecraft.massivecore.engine.EngineMassiveCoreDestination;
import com.massivecraft.massivecore.engine.EngineMassiveCoreGank;
import com.massivecraft.massivecore.engine.EngineMassiveCoreLorePriority;
import com.massivecraft.massivecore.engine.EngineMassiveCoreMain;
import com.massivecraft.massivecore.engine.EngineMassiveCorePlayerLeave;
import com.massivecraft.massivecore.engine.EngineMassiveCorePlayerState;
import com.massivecraft.massivecore.engine.EngineMassiveCorePlayerUpdate;
import com.massivecraft.massivecore.engine.EngineMassiveCoreScheduledTeleport;
import com.massivecraft.massivecore.engine.EngineMassiveCoreTeleportMixinCause;
import com.massivecraft.massivecore.engine.EngineMassiveCoreVariable;
import com.massivecraft.massivecore.engine.EngineMassiveCoreWorldNameSet;
import com.massivecraft.massivecore.entity.MassiveCoreMConf;
import com.massivecraft.massivecore.entity.MassiveCoreMConfColl;
import com.massivecraft.massivecore.entity.MultiverseColl;
import com.massivecraft.massivecore.entity.migrator.MigratorMassiveCoreMConf001CleanInactivity;
import com.massivecraft.massivecore.integration.vault.IntegrationVault;
import com.massivecraft.massivecore.mixin.MixinActionbar;
import com.massivecraft.massivecore.mixin.MixinActual;
import com.massivecraft.massivecore.mixin.MixinCommand;
import com.massivecraft.massivecore.mixin.MixinDisplayName;
import com.massivecraft.massivecore.mixin.MixinEvent;
import com.massivecraft.massivecore.mixin.MixinGamemode;
import com.massivecraft.massivecore.mixin.MixinKick;
import com.massivecraft.massivecore.mixin.MixinLog;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.mixin.MixinModification;
import com.massivecraft.massivecore.mixin.MixinPlayed;
import com.massivecraft.massivecore.mixin.MixinRecipe;
import com.massivecraft.massivecore.mixin.MixinSenderPs;
import com.massivecraft.massivecore.mixin.MixinTeleport;
import com.massivecraft.massivecore.mixin.MixinTitle;
import com.massivecraft.massivecore.mixin.MixinVisibility;
import com.massivecraft.massivecore.mixin.MixinWorld;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.mson.MsonEvent;
import com.massivecraft.massivecore.nms.NmsBasics;
import com.massivecraft.massivecore.nms.NmsBoard;
import com.massivecraft.massivecore.nms.NmsChat;
import com.massivecraft.massivecore.nms.NmsEntityDamageEvent;
import com.massivecraft.massivecore.nms.NmsEntityGet;
import com.massivecraft.massivecore.nms.NmsItemStackTooltip;
import com.massivecraft.massivecore.nms.NmsPermissions;
import com.massivecraft.massivecore.nms.NmsRecipe;
import com.massivecraft.massivecore.nms.NmsSkullMeta;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.ps.PSAdapter;
import com.massivecraft.massivecore.store.EntityInternalMap;
import com.massivecraft.massivecore.store.ModificationPollerLocal;
import com.massivecraft.massivecore.store.ModificationPollerRemote;
import com.massivecraft.massivecore.util.BoardUtil;
import com.massivecraft.massivecore.util.ContainerUtil;
import com.massivecraft.massivecore.util.EventUtil;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.IntervalUtil;
import com.massivecraft.massivecore.util.InventoryUtil;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.PeriodUtil;
import com.massivecraft.massivecore.util.PlayerUtil;
import com.massivecraft.massivecore.util.RecipeUtil;
import com.massivecraft.massivecore.util.ReflectionUtil;
import com.massivecraft.massivecore.util.SignUtil;
import com.massivecraft.massivecore.util.SmokeUtil;
import com.massivecraft.massivecore.util.TimeDiffUtil;
import com.massivecraft.massivecore.util.TimeUnit;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;
import org.bukkit.Sound;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class MassiveCore extends MassivePlugin
{
	// -------------------------------------------- //
	// COMMON CONSTANTS
	// -------------------------------------------- //
	
	public final static String INSTANCE = "instance";
	public final static String DEFAULT = "default";
	public static final String NONE = Txt.parse("<silver>none");
	
	public final static Set<String> NOTHING = MUtil.treeset("", "none", "null", "nothing");
	public final static Set<String> REMOVE = MUtil.treeset("clear", "c", "delete", "del", "d", "erase", "e", "remove", "rem", "r", "reset", "res");
	public final static Set<String> NOTHING_REMOVE = MUtil.treeset("", "none", "null", "nothing", "clear", "c", "delete", "del", "d", "erase", "e", "remove", "rem", "r", "reset", "res");
	
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static MassiveCore i;
	public static MassiveCore get() { return i; }
	public MassiveCore() { i = this; }
	
	// -------------------------------------------- //
	// STATIC
	// -------------------------------------------- //
	
	public static Random random = new Random();
	public static Gson gson = getMassiveCoreGsonBuilder().create();
	
	public static GsonBuilder getMassiveCoreGsonBuilder()
	{
		// Create
		GsonBuilder ret = new GsonBuilder();
		
		// Basic Behavior
		ret.setPrettyPrinting();
		ret.disableHtmlEscaping();
		ret.excludeFieldsWithModifiers(Modifier.TRANSIENT);
		
		// Raw Adapters
//		ret.registerTypeAdapter(JsonNull.class, AdapterJsonElement.get());
//		ret.registerTypeAdapter(JsonPrimitive.class, AdapterJsonElement.get());
//		ret.registerTypeAdapter(JsonArray.class, AdapterJsonElement.get());
//		ret.registerTypeAdapter(JsonObject.class, AdapterJsonElement.get());
		
		// Enumeration Annotation Dodge
		ret.registerTypeAdapterFactory(AdapterModdedEnumType.ENUM_FACTORY);
		
		// Massive Containers
		ret.registerTypeAdapter(MassiveList.class, AdapterMassiveList.get());
		ret.registerTypeAdapter(MassiveListDef.class, AdapterMassiveList.get());
		ret.registerTypeAdapter(MassiveMap.class, AdapterMassiveMap.get());
		ret.registerTypeAdapter(MassiveMapDef.class, AdapterMassiveMap.get());
		ret.registerTypeAdapter(MassiveSet.class, AdapterMassiveSet.get());
		ret.registerTypeAdapter(MassiveSetDef.class, AdapterMassiveSet.get());
		ret.registerTypeAdapter(MassiveTreeMap.class, AdapterMassiveTreeMap.get());
		ret.registerTypeAdapter(MassiveTreeMapDef.class, AdapterMassiveTreeMap.get());
		ret.registerTypeAdapter(MassiveTreeSet.class, AdapterMassiveTreeSet.get());
		ret.registerTypeAdapter(MassiveTreeSetDef.class, AdapterMassiveTreeSet.get());
		
		// Entries (Is this still needed?)
		ret.registerTypeAdapter(Entry.class, AdapterEntry.get());
		
		// Assorted Custom
		ret.registerTypeAdapter(BackstringSet.class, AdapterBackstringSet.get());
		ret.registerTypeAdapter(PS.class, PSAdapter.get());
		ret.registerTypeAdapter(Sound.class, AdapterSound.get());
		ret.registerTypeAdapter(UUID.class, AdapterUUID.get());

		// Mson
		ret.registerTypeAdapter(Mson.class, AdapterMson.get());
		ret.registerTypeAdapter(MsonEvent.class, AdapterMsonEvent.get());
		
		// Storage
		ret.registerTypeAdapter(EntityInternalMap.class, AdapterEntityInternalMap.get());
		
		// Return
		return ret;
	}
	
	public static String getServerId() { return ConfServer.serverid; }
	public static String getTaskServerId() { return MassiveCoreMConf.get().taskServerId; }
	public static boolean isTaskServer()
	{
		String taskServerId = getTaskServerId();
		if (taskServerId == null) return true;
		if (getServerId().equals(taskServerId)) return true;
		return false;
	}
	
	// -------------------------------------------- //
	// LOAD
	// -------------------------------------------- //
	
	@Override
	public void onLoadInner()
	{
		// These util classes are not automatically loaded/resolved when MassiveCore is being loaded.
		// However they need to be loaded to ensure async safety.
		// This fixes a race condition within the asynchronous class loader (LinkageError).
		ReflectionUtil.forceLoadClasses(
			ContainerUtil.class,
			EventUtil.class,
			IntervalUtil.class,
			InventoryUtil.class,
			PeriodUtil.class,
			RecipeUtil.class,
			SignUtil.class,
			SmokeUtil.class,
			TimeUnit.class,
			TimeDiffUtil.class
		);
	}
	
	// -------------------------------------------- //
	// ENABLE
	// -------------------------------------------- //
	
	@Override
	public void onEnableInner()
	{
		// This is safe since all plugins using Persist should bukkit-depend this plugin.
		// Note this one must be before preEnable. dooh.
		// TODO: Create something like "deinit all" (perhaps a forloop) to readd this.
		// TODO: Test and ensure reload compat.
		// Coll.instances.clear();
		
		// Load Server Config
		ConfServer.get().load();
		
		// Setup IdUtil
		IdUtil.setup();
		
		// Setup RegistryType
		RegistryType.registerAll();
		
		// Activate
		this.activateAuto();

		// These must be activated after nms
		this.activate(
			
			// Util
			PlayerUtil.class,
			BoardUtil.class
		);

		// Start the examine threads
		// Start AFTER initializing the MConf, because they rely on the MConf.
		if (ConfServer.localPollingEnabled) ModificationPollerLocal.get().start();
		ModificationPollerRemote.get().start();
		
		// Delete Files (at once and additionally after all plugins loaded)
		MassiveCoreTaskDeleteFiles.get().run();
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, MassiveCoreTaskDeleteFiles.get());
	}

	// These are overriden because the reflection trick was buggy and didn't work on all systems
	@Override
	public List<Class<?>> getClassesActiveMigrators()
	{
		return MUtil.list(
			MigratorMassiveCoreMConf001CleanInactivity.class
		);
	}

	@Override
	public List<Class<?>> getClassesActiveColls()
	{
		return MUtil.list(
			MassiveCoreMConfColl.class,
			MultiverseColl.class
		);
	}

	@Override
	public List<Class<?>> getClassesActiveNms()
	{
		return MUtil.list(
			NmsBasics.class,
			NmsBoard.class,
			NmsChat.class,
			NmsEntityDamageEvent.class,
			NmsEntityGet.class,
			NmsItemStackTooltip.class,
			NmsPermissions.class,
			NmsSkullMeta.class,
			NmsRecipe.class
		);
	}

	@Override
	public List<Class<?>> getClassesActiveCommands()
	{
		return MUtil.list(
				CmdMassiveCore.class,
				CmdMassiveCoreBuffer.class,
				CmdMassiveCoreClick.class,
				CmdMassiveCoreCmdurl.class,
				CmdMassiveCoreStore.class,
				CmdMassiveCoreUsys.class
		);
	}

	@Override
	public List<Class<?>> getClassesActiveEngines()
	{
		return MUtil.list(
			EngineMassiveCoreChestGui.class,
			EngineMassiveCoreClean.class,
			EngineMassiveCoreCollTick.class,
			EngineMassiveCoreCommandRegistration.class,
			EngineMassiveCoreCommandSet.class,
			EngineMassiveCoreDatabase.class,
			EngineMassiveCoreDestination.class,
			EngineMassiveCoreGank.class,
			EngineMassiveCoreLorePriority.class,
			EngineMassiveCoreMain.class,
			EngineMassiveCorePlayerLeave.class,
			EngineMassiveCorePlayerState.class,
			EngineMassiveCorePlayerUpdate.class,
			EngineMassiveCoreScheduledTeleport.class,
			EngineMassiveCoreTeleportMixinCause.class,
			EngineMassiveCoreVariable.class,
			EngineMassiveCoreWorldNameSet.class
			);
	}

	@Override
	public List<Class<?>> getClassesActiveIntegrations()
	{
		return MUtil.list(
			IntegrationVault.class
		);
	}

	@Override
	public List<Class<?>> getClassesActiveMixins()
	{
		return MUtil.list(
			MixinEvent.class,
			MixinActionbar.class,
			MixinActual.class,
			MixinCommand.class,
			MixinDisplayName.class,
			MixinGamemode.class,
			MixinKick.class,
			MixinLog.class,
			MixinMessage.class,
			MixinModification.class,
			MixinPlayed.class,
			MixinRecipe.class,
			MixinSenderPs.class,
			MixinTeleport.class,
			MixinTitle.class,
			MixinVisibility.class,
			MixinWorld.class
			);
	}

	@Override
	public List<Class<?>> getClassesActiveTests()
	{
		return MUtil.list();
	}

	// -------------------------------------------- //
	// DISABLE
	// -------------------------------------------- //
	
	@Override
	public void onDisable()
	{
		super.onDisable();
		ModificationPollerLocal.get().interrupt();
		ModificationPollerRemote.get().interrupt();
		
		MassiveCoreTaskDeleteFiles.get().run();
		IdUtil.saveCachefileDatas();
	}

}
