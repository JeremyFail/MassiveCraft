package com.massivecraft.massivecore.util.extractor;

import com.massivecraft.massivecore.mixin.MixinSenderPs;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerEvent;

public class ExtractorLogic
{
	// -------------------------------------------- //
	// WORLD
	// -------------------------------------------- //
	
	public static World world(Block o) { return o.getWorld(); }
	public static World world(Location o) { return o.getWorld(); }
	public static World world(Entity o) { return o.getWorld(); }
	public static World world(PlayerEvent o) { return world(o.getPlayer()); }
	public static World world(PS o) { try { return o.asBukkitWorld(true); } catch (Exception e) { return null; }}
	
	public static World worldFromObject(Object o)
	{
		if (o instanceof World) return (World)o;
		if (o instanceof Block) return world((Block)o);
		if (o instanceof Location) return world((Location)o);
		if (o instanceof Entity) return world((Entity)o);
		if (o instanceof PlayerEvent) return world((PlayerEvent)o);
		if (o instanceof PS) return world((PS)o);
		
		return null;
	}
	
	// -------------------------------------------- //
	// WORLD NAME
	// -------------------------------------------- //
	
	public static String worldNameFromObject(Object o)
	{
		if (o instanceof String)
		{
			String string = (String)o;
			if (MUtil.isUuid(string))
			{
				String ret = worldNameViaPsMixin(string);
				if (ret != null) return ret;
			}
			return string;
		}
		
		if (o instanceof PS) return ((PS)o).getWorld();
		
		World world = worldFromObject(o);
		if (world != null) return world.getName();
		
		String ret = worldNameViaPsMixin(o);
		if (ret != null) return ret;

		return null;
	}
	
	public static String worldNameViaPsMixin(Object senderObject)
	{
		if (senderObject == null) return null;
		
		PS ps = MixinSenderPs.get().getSenderPs(senderObject);
		if (ps == null) return null;
		
		return ps.getWorld();
	}
	
}

