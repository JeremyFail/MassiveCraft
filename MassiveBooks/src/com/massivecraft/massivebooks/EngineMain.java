package com.massivecraft.massivebooks;

import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivebooks.entity.MPlayer;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.mixin.MixinActual;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.mixin.MixinPlayed;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.InventoryUtil;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EngineMain extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final EngineMain i = new EngineMain();
	public static EngineMain get() { return i; }
	
	// -------------------------------------------- //
	// NEW PLAYER COMMANDS
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void newPlayerCommands(PlayerJoinEvent event)
	{
		// If a player is joining the server for the first time ...
		final Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		if (MixinPlayed.get().hasPlayedBefore(player)) return;
		
		// ... and we are using new player commands ...
		if (!MConf.get().usingNewPlayerCommands) return;
		
		// ... prepare a task ...
		Runnable task = () -> {
			final ConsoleCommandSender consoleSender = Bukkit.getConsoleSender();
			MassiveBooks.get().log(Lang.getNewPlayerCommandsForX(player, consoleSender));
			for (String cmd : MConf.get().newPlayerCommands)
			{
				cmd = Txt.removeLeadingCommandDust(cmd);
				cmd = cmd.replace("{p}", player.getName());
				cmd = cmd.replace("{player}", player.getName());
				Bukkit.getServer().dispatchCommand(consoleSender, cmd);
			}
		};
		
		// ... and run it either now or later.
		if (MConf.get().usingNewPlayerCommandsDelayTicks)
		{
			Bukkit.getScheduler().scheduleSyncDelayedTask(MassiveBooks.get(), task, MConf.get().newPlayerCommandsDelayTicks);
		}
		else
		{
			task.run();
		}
	}
	
	// -------------------------------------------- //
	// ITEM FRAME: LOAD AND DISPLAYNAME
	// -------------------------------------------- //
	
	// Can be cancelled but we don't care :P
	// NOTE: Placed at low so that the content update on LOWEST will run before.
	@EventHandler(priority = EventPriority.LOW)
	public void itemFrameLoadAndDisplayname(PlayerInteractEntityEvent event)
	{
		// Ignore Off Hand
		if (isOffHand(event)) return;
		
		// If a player is interacting with an item frame ...
		Entity entity = event.getRightClicked();
		if (!(entity instanceof ItemFrame)) return;
		ItemFrame itemFrame = (ItemFrame)entity;
		
		// ... and that item frame contains something ...
		ItemStack item = itemFrame.getItem();
		if (item.getType() == Material.AIR) return;
		
		// ... check it the player is sneaking ...
		final Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		final boolean sneaking = player.isSneaking();
		
		if (this.itemFrameLoad(event, item, player, sneaking)) return;
		if (this.itemFrameDisplayname(event, item, player, sneaking)) return;
		
		// Add more here if you want to in the future :P
	}
	
	public boolean itemFrameLoad(PlayerInteractEntityEvent event, ItemStack itemInFrame, Player player, boolean sneaking)
	{
		// If loading is allowed ...
		if (sneaking && !MConf.get().itemFrameLoadIfSneakTrue) return false;
		if (!sneaking && !MConf.get().itemFrameLoadIfSneakFalse) return false;
		
		// ... and there is something with BookMeta in the item frame ...
		if (!BookUtil.hasBookMeta(itemInFrame)) return false;
		
		// ... then cancel to stop rotation ...
		event.setCancelled(true);
		
		// ... now do different stuff depending on what item the player is holding ...
		ItemStack itemInHand = InventoryUtil.getMainHand(player);
		if (itemInHand == null) return true;
	
		// ... if the player is holding a similar item ...
		if (itemInHand.isSimilar(itemInFrame))
		{
			// ... do unload ...
			ItemStack target = new ItemStack(itemInHand);
			BookUtil.clear(target);
			InventoryUtil.setMainHand(player, target);
			
			// ... and inform.
			MixinMessage.get().messageOne(player, Lang.getFrameUnload(itemInHand));
		}
		// ... else if the player is holding a clear book and quill ...
		else if (BookUtil.isCleared(itemInHand))
		{
			// Has right to copy?
			if (!BookUtil.hasCopyPerm(itemInFrame, player, true)) return true;
			
			// ... do load ...
			ItemStack target = new ItemStack(itemInFrame);
			target.setAmount(itemInHand.getAmount());
			InventoryUtil.setMainHand(player, target);
			
			// ... and inform.
			MixinMessage.get().messageOne(player, Lang.getFrameLoad(target));
		}
		// ... else if the player is holding an educated but invalid guess ...
		else if (itemInHand.getType() == Material.WRITTEN_BOOK || itemInHand.getType() == Material.WRITABLE_BOOK || itemInHand.getType() == Material.BOOK)
		{
			// ... do help.
			MixinMessage.get().messageOne(player, Lang.getFrameHelp());
		}
		
		// ... and return true which means that no displayname info should be sent.
		return true;
	}
	
	public boolean itemFrameDisplayname(PlayerInteractEntityEvent event, ItemStack itemInFrame, Player player, boolean sneaking)
	{
		// If displayname is allowed ...
		if (sneaking && !MConf.get().itemFrameDisplaynameIfSneakTrue) return false;
		if (!sneaking && !MConf.get().itemFrameDisplaynameIfSneakFalse) return false;
		
		// ... and there is something with displayname in the item frame ...
		if (!itemInFrame.hasItemMeta()) return false;
		ItemMeta meta = InventoryUtil.createMeta(itemInFrame);
		if (!meta.hasDisplayName()) return false;
		String displayname = meta.getDisplayName();
		
		// ... and inform on what the frame contains.
		MixinMessage.get().messageOne(player, Lang.getFrameContains(displayname));
		return true;
	}
	
	// -------------------------------------------- //
	// ITEM FRAME: ROTATE
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void itemFrameRotate(PlayerInteractEntityEvent event)
	{
		// Ignore Off Hand
		if (isOffHand(event)) return;
		
		// If a player is interacting with an item frame ...
		Entity entity = event.getRightClicked();
		if (!(entity instanceof ItemFrame)) return;
		ItemFrame itemFrame = (ItemFrame)entity;
		
		// ... and that item frame contains something ...
		ItemStack item = itemFrame.getItem();
		if (item.getType() == Material.AIR) return;
		
		// ... possibly ...
		final Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		final boolean sneaking = player.isSneaking();
		if (sneaking && MConf.get().itemFrameRotateIfSneakTrue) return;
		if (!sneaking && MConf.get().itemFrameRotateIfSneakFalse) return;
		
		// ... stop rotation.
		event.setCancelled(true);
	}
	
	// -------------------------------------------- //
	// AUTOUPDATE: JOIN WARN OR TOGGLE 
	// -------------------------------------------- //
	
	// Can't be cancelled
	@EventHandler(priority = EventPriority.NORMAL)
	public void autoupdateJoinWarnOrToggle(PlayerJoinEvent event)
	{
		if ( ! MixinActual.get().isActualJoin(event)) return;
		
		final Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		MPlayer mplayer = MPlayer.get(player);
		if (mplayer.isUsingAutoUpdate()) return;
		
		if (Perm.AUTOUPDATE.has(player, false))
		{
			// Warn
			MixinMessage.get().messageOne(player, Lang.AUTOUPDATE_JOINWARN);
		}
		else
		{
			// Toggle
			mplayer.setUsingAutoUpdate(true, true, false);
		}
		
	}
	
	// -------------------------------------------- //
	// AUTOUPDATE: PERFORM 
	// -------------------------------------------- //
	// MassiveBooks auto-updates books. What does that mean?
	// The item displayname is updated to contain the display name of the author player.
	// Since the player displayname may change so must the displayname of the book.
	// Additionally books that have the same title as a saved book receives complete content updates.
	// This way you can always be sure those special books of yours never exists in multiple versions.
	// Saved a new version of the server rule book? All old instances will be updated on the fly.
	
	// Can be cancelled but we don't care :P
	@EventHandler(priority = EventPriority.LOWEST)
	public void updatePerform(PlayerInteractEntityEvent event)
	{
		// Ignore Off Hand
		if (isOffHand(event)) return;
		
		Entity entity = event.getRightClicked();
		if (!(entity instanceof ItemFrame)) return;
		ItemFrame itemFrame = (ItemFrame)entity;
		
		final Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		MPlayer mplayer = MPlayer.get(player);
		if (!mplayer.isUsingAutoUpdate()) return;
		
		BookUtil.updateBook(itemFrame);
	}
	
	// Can't be cancelled
	@EventHandler(priority = EventPriority.LOWEST)
	public void updatePerform(PlayerItemHeldEvent event)
	{
		final Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		MPlayer mplayer = MPlayer.get(player);
		if (!mplayer.isUsingAutoUpdate()) return;
		
		BookUtil.updateBooks(player);
	}
	
	// Can be cancelled but we don't care :P
	@EventHandler(priority = EventPriority.LOWEST)
	public void updatePerform(EntityPickupItemEvent event)
	{
		Entity entity = event.getEntity();
		if (MUtil.isntPlayer(entity)) return;
		final Player player = (Player) entity;
		if (MUtil.isntPlayer(player)) return;
		MPlayer mplayer = MPlayer.get(player);
		if (!mplayer.isUsingAutoUpdate()) return;
		
		BookUtil.updateBook(event.getItem());
	}
	
	// Can be cancelled but we don't care :P
	@EventHandler(priority = EventPriority.LOWEST)
	public void updatePerform(InventoryClickEvent event)
	{
		final Player player = IdUtil.getAsPlayer(event.getWhoClicked());
		if (MUtil.isntPlayer(player)) return;
		
		MPlayer mplayer = MPlayer.get(player);
		if (!mplayer.isUsingAutoUpdate()) return;
		
		BookUtil.updateBooks(event.getInventory());
		BookUtil.updateBooks(event.getWhoClicked());
	}
	
	// Can be cancelled but we don't care :P
	@EventHandler(priority = EventPriority.LOWEST)
	public void updatePerform(InventoryOpenEvent event)
	{
		final Player player = IdUtil.getAsPlayer(event.getPlayer());
		if (MUtil.isntPlayer(player)) return;
		
		MPlayer mplayer = MPlayer.get(player);
		if (!mplayer.isUsingAutoUpdate()) return;
		
		BookUtil.updateBooks(event.getInventory());
		BookUtil.updateBooks(player);
	}
	
}
