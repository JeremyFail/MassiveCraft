package com.massivecraft.massivecore.item;

import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.nms.NmsSkullMeta;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class WriterItemStackMetaSkullProperties extends WriterAbstractItemStackMetaField<SkullMeta, ContainerGameProfile, Object>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final WriterItemStackMetaSkullProperties i = new WriterItemStackMetaSkullProperties();
	public static WriterItemStackMetaSkullProperties get() { return i; }
	public WriterItemStackMetaSkullProperties()
	{
		super(SkullMeta.class);
		this.setMaterial(Material.PLAYER_HEAD);
		this.setConverterFrom(new Converter<Object, ContainerGameProfile>()
		{
			@Override
			public ContainerGameProfile convert(Object fb)
			{
				if (fb == null) return null;
				return gameProfileToContainer(fb);
			}
		});
		this.setConverterTo(new Converter<ContainerGameProfile, Object>()
		{
			@Override
			public Object convert(ContainerGameProfile fa)
			{
				if (fa == null) return null;
				return dataToGameProfile(fa);
			}
		});
	}
	
	// -------------------------------------------- //
	// ACCESS
	// -------------------------------------------- //
	
	@Override
	public ContainerGameProfile getA(DataItemStack ca, ItemStack d)
	{
		return ca.getContainerGameProfile();
	}
	
	@Override
	public void setA(DataItemStack ca, ContainerGameProfile fa, ItemStack d)
	{
		ca.setContainerGameProfile(fa);
	}

	// This should be getting the GameProfile skull meta
	@Override
	public Object getB(SkullMeta cb, ItemStack d)
	{
		NmsSkullMeta nms = NmsSkullMeta.get();
		return nms.getGameProfile(cb);
	}

	// This should set the game profile for the skull meta
	// fb is the game profile object
	@Override
	public void setB(SkullMeta cb, Object fb, ItemStack d)
	{
		NmsSkullMeta nms = NmsSkullMeta.get();
		nms.setGameProfile(cb, fb);
	}
	
	// Returns a gameprofile object
	private Object dataToGameProfile(ContainerGameProfile container)
	{
		NmsSkullMeta nms = NmsSkullMeta.get();
		
		if (container == null) return nms.createGameProfile(UUID.randomUUID(), "testing");
		
		String uuidRaw = container.getUuid();
		UUID uuid = uuidRaw != null ? UUID.fromString(uuidRaw) : UUID.randomUUID();
		
		String name = container.getName();
		if (name == null) name = String.format("null_%d", MassiveCore.random.nextInt(10_000));
		
		Object gameprofile = nms.createGameProfile(uuid, name);
		Object propertyMap = nms.createPropertyMap();
		nms.setGameProfileProperties(propertyMap, container.getGameProfileProperties());
		nms.setPropertyMap(gameprofile, propertyMap);
		
		return gameprofile;
	}
	
	private ContainerGameProfile gameProfileToContainer(Object gameProfile)
	{
		NmsSkullMeta nms = NmsSkullMeta.get();
		
		ContainerGameProfile ret = new ContainerGameProfile();
		
		UUID uuid = nms.getGameProfileId(gameProfile);
		ret.setUuid(uuid != null ? uuid.toString() : null);
		
		String name = nms.getGameProfileName(gameProfile);
		ret.setName(name);
		
		Object propertyMap = nms.getPropertyMap(gameProfile);
		ret.setGameProfileProperties(new MassiveList<>(nms.getGameProfileProperties(propertyMap)));
		
		return ret;
	}
	
}
