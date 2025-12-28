package com.massivecraft.massivecore.nms;

import com.massivecraft.massivecore.Couple;
import com.massivecraft.massivecore.item.ContainerGameProfileProperty;
import com.massivecraft.massivecore.mixin.Mixin;
import com.massivecraft.massivecore.util.IdData;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class NmsSkullMeta extends Mixin
{
	// -------------------------------------------- //
	// DEFAULT
	// -------------------------------------------- //
	
	private static NmsSkullMeta d = new NmsSkullMeta().setAlternatives(
		NmsSkullMeta120R2P.class,
		NmsSkullMetaFallback.class
	);
	
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	@SuppressWarnings("FieldMayBeFinal")
	private static NmsSkullMeta i = d;
	public static NmsSkullMeta get() { return i; }
	
	// -------------------------------------------- //
	// RAW
	// -------------------------------------------- //
	
	public String getName(SkullMeta meta)
	{
		return meta.getOwningPlayer().getName();
	}
	
	public UUID getId(SkullMeta meta)
	{
		throw notImplemented();
	}
	
	public void set(SkullMeta meta, String name, UUID id)
	{
		throw notImplemented();
	}
	
	// -------------------------------------------- //
	// RESOLVE
	// -------------------------------------------- //
	// We resolve the locally best possible information using IdUtil.
	
	public Couple<String, UUID> resolve(SkullMeta meta)
	{
		String name = this.getName(meta);
		UUID id = this.getId(meta);
		return this.resolve(name, id);
	}
	
	public Couple<String, UUID> resolve(String name, UUID id)
	{
		// Create Ret
		// We default to the input.
		String retName = name;
		UUID retId = id;
		
		// Fetch IdData
		// First by name then id. 
		IdData data = null;
		if (name != null) data = IdUtil.getNameToData().get(name);
		if (data == null && id != null) data = IdUtil.getIdToData().get(id.toString());
		
		// Use that data if found
		if (data != null)
		{
			retName = data.getName();
			retId = MUtil.asUuid(data.getId());
		}
		
		// Return Ret
		return new Couple<>(retName, retId);
	}
	
	public <T> T createGameProfile(UUID id, String name)
	{
		return null;
	}
	
	public <T> T getGameProfile(SkullMeta meta)
	{
		return null;
	}
	
	public <T> void setGameProfile(SkullMeta meta, T gameProfile)
	{
		// No-op here
	}
	
	public <T> T getPropertyMap(Object profile)
	{
		return null;
	}
	
	public Collection<Map.Entry<String, ContainerGameProfileProperty>> getGameProfileProperties(Object propertyMap)
	{
		return Collections.emptyList();
	}
	
	public void setGameProfileProperties(Object propertyMap, Collection<Map.Entry<String, ContainerGameProfileProperty>> properties)
	{
		// No-op here
	}
	
	public void setPropertyMap(Object profile, Object propertyMap)
	{
		// No-op hee
	}
	
	public Object createPropertyMap()
	{
		return null;
	}
	
	public UUID getGameProfileId(Object gameprofile)
	{
		throw notImplemented();
	}
	
	public String getGameProfileName(Object gameProfile)
	{
		throw notImplemented();
	}
	
}
