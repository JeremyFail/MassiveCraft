package com.massivecraft.massivecore.nms;

import com.massivecraft.massivecore.Couple;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.item.ContainerGameProfileProperty;
import com.massivecraft.massivecore.particleeffect.ReflectionUtils.PackageType;
import com.massivecraft.massivecore.util.ReflectionUtil;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class NmsSkullMeta120R2P extends NmsSkullMeta
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	@SuppressWarnings("FieldMayBeFinal")
	private static NmsSkullMeta120R2P i = new NmsSkullMeta120R2P();
	public static NmsSkullMeta120R2P get() { return i; }
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	// Authlib classes
	protected Class<?> classGameProfile;
	protected Class<?> classProperty;
	protected Class<?> classPropertyMap;
	
	// Constructor methods
	protected Method constructorGameProfile;
	protected Method constructorProperty;
	protected Method constructorPropertyMap;
	
	// GameProfile methods/fields
	protected Field fieldGameProfilePropertyMap;
	protected Field fieldGameProfileName;
	protected Field fieldGameProfileId;
	protected Method methodGameProfileGetProperties;
	
	// Property methods
	protected Method methodPropertyName;
	protected Method methodPropertyValue;
	protected Method methodPropertySignature;
	
	// PropertyMap methods
	protected Method methodPropertyMapEntries;
	protected Method methodPropertyMapClear;
	protected Method methodPropertyMapPut;
	
	// CraftMetaSkull#profile
	public Field fieldCraftMetaSkullProfile;
	// CraftMetaSkull::setProfile
	public Method methodCraftMetaSkullSetProfile;
	
	// -------------------------------------------- //
	// SETUP
	// -------------------------------------------- //
	
	@Override
	public void setup() throws Throwable
	{
		// Load Authlib classes via reflection (they're not in Bukkit API)
		this.classGameProfile = Class.forName("com.mojang.authlib.GameProfile");
		this.classProperty = Class.forName("com.mojang.authlib.properties.Property");
		this.classPropertyMap = Class.forName("com.mojang.authlib.properties.PropertyMap");
		
		// Get constructors
		this.constructorGameProfile = ReflectionUtil.getMethod(this.classGameProfile, "<init>", UUID.class, String.class);
		this.constructorProperty = ReflectionUtil.getMethod(this.classProperty, "<init>", String.class, String.class, String.class);
		this.constructorPropertyMap = ReflectionUtil.getMethod(this.classPropertyMap, "<init>");
		
		// GameProfile fields and methods
		this.fieldGameProfilePropertyMap = ReflectionUtil.getField(this.classGameProfile, "properties");
		this.fieldGameProfileName = ReflectionUtil.getField(this.classGameProfile, "name");
		this.fieldGameProfileId = ReflectionUtil.getField(this.classGameProfile, "id");
		this.methodGameProfileGetProperties = ReflectionUtil.getMethod(this.classGameProfile, "getProperties");
		
		// Property methods
		this.methodPropertyName = ReflectionUtil.getMethod(this.classProperty, "name");
		this.methodPropertyValue = ReflectionUtil.getMethod(this.classProperty, "value");
		this.methodPropertySignature = ReflectionUtil.getMethod(this.classProperty, "signature");
		
		// PropertyMap methods
		this.methodPropertyMapEntries = ReflectionUtil.getMethod(this.classPropertyMap, "entries");
		this.methodPropertyMapClear = ReflectionUtil.getMethod(this.classPropertyMap, "clear");
		this.methodPropertyMapPut = ReflectionUtil.getMethod(this.classPropertyMap, "put", String.class, this.classProperty);
		
		// CraftMetaSkull
		Class<?> classCraftMetaSkull = PackageType.CRAFTBUKKIT_VERSION_INVENTORY.getClass("CraftMetaSkull");
		this.fieldCraftMetaSkullProfile = ReflectionUtil.getField(classCraftMetaSkull, "profile");
		this.methodCraftMetaSkullSetProfile = ReflectionUtil.getMethod(classCraftMetaSkull, "setProfile", this.classGameProfile);
	}
	
	// -------------------------------------------- //
	// RAW
	// -------------------------------------------- //
	
	@Override
	public UUID getId(SkullMeta meta)
	{
		Object gameProfile = getGameProfile(meta);
		if (gameProfile == null) return null;
		return getGameProfileId(gameProfile);
	}
	
	@Override
	public void set(SkullMeta meta, String name, UUID id)
	{
		final Object gameProfile = createGameProfile(id, name);
		setGameProfile(meta, gameProfile);
	}
	
	// -------------------------------------------- //
	// GAMEPROFILE
	// -------------------------------------------- //
	
	@Override
	public Object createGameProfile(UUID id, String name)
	{
		return ReflectionUtil.invokeMethod(this.constructorGameProfile, null, id, name);
	}
	
	@Override
	public <T> T getGameProfile(SkullMeta meta)
	{
		return ReflectionUtil.getField(this.fieldCraftMetaSkullProfile, meta);
	}
	
	@Override
	public void setGameProfile(SkullMeta meta, Object gameProfile)
	{
		ReflectionUtil.invokeMethod(this.methodCraftMetaSkullSetProfile, meta, gameProfile);
	}
	
	// -------------------------------------------- //
	// GAMEPROFILE > GET
	// -------------------------------------------- //
	
	@Override
	public String getGameProfileName(Object profile)
	{
		if (!this.classGameProfile.isInstance(profile)) return null;
		return ReflectionUtil.getField(this.fieldGameProfileName, profile);
	}
	
	@Override
	public UUID getGameProfileId(Object profile)
	{
		if (!this.classGameProfile.isInstance(profile)) return null;
		return ReflectionUtil.getField(this.fieldGameProfileId, profile);
	}
	
	// -------------------------------------------- //
	// GAMEPROFILE > PROPERTIES
	// -------------------------------------------- //
	
	@Override
	public Object getPropertyMap(Object profile)
	{
		if (!this.classGameProfile.isInstance(profile))
			return ReflectionUtil.invokeMethod(this.constructorPropertyMap, null);
		
		return ReflectionUtil.invokeMethod(this.methodGameProfileGetProperties, profile);
	}
	
	@Override
	public void setPropertyMap(Object profile, Object propertyMap)
	{
		if (!this.classGameProfile.isInstance(profile))
			throw new IllegalArgumentException("profile provided is not an Authlib GameProfile");
		
		ReflectionUtil.setField(this.fieldGameProfilePropertyMap, profile, propertyMap);
	}
	
	// -------------------------------------------- //
	// PROPERTY > GET
	// -------------------------------------------- //
	
	@Override
	public Collection<Map.Entry<String, ContainerGameProfileProperty>> getGameProfileProperties(Object propertyMap)
	{
		if (!this.classPropertyMap.isInstance(propertyMap))
			throw new IllegalArgumentException("propertyMap provided is not an Authlib PropertyMap");
		
		Collection<Map.Entry<String, ContainerGameProfileProperty>> ret = new MassiveList<>();
		
		// Get entries from PropertyMap
		Collection<?> entries = ReflectionUtil.invokeMethod(this.methodPropertyMapEntries, propertyMap);
		
		for (Object entryObj : entries)
		{
			@SuppressWarnings("unchecked")
			Map.Entry<String, ?> entry = (Map.Entry<String, ?>) entryObj;
			ret.add(Couple.valueOf(
				entry.getKey(),
				unsafePropertyToContainer(entry.getValue())
			));
		}
		
		return ret;
	}
	
	private ContainerGameProfileProperty unsafePropertyToContainer(Object property)
	{
		ContainerGameProfileProperty ret = new ContainerGameProfileProperty();
		ret.name = ReflectionUtil.invokeMethod(this.methodPropertyName, property);
		ret.value = ReflectionUtil.invokeMethod(this.methodPropertyValue, property);
		ret.signature = ReflectionUtil.invokeMethod(this.methodPropertySignature, property);
		
		return ret;
	}
	
	// -------------------------------------------- //
	// PROPERTYMAP > SET
	// -------------------------------------------- //
	
	@Override
	public Object createPropertyMap()
	{
		return ReflectionUtil.invokeMethod(this.constructorPropertyMap, null);
	}
	
	@Override
	public void setGameProfileProperties(Object propertyMap, Collection<Map.Entry<String, ContainerGameProfileProperty>> properties)
	{
		if (!this.classPropertyMap.isInstance(propertyMap)) return;
		
		// Clear existing properties
		ReflectionUtil.invokeMethod(this.methodPropertyMapClear, propertyMap);
		
		for (Map.Entry<String, ContainerGameProfileProperty> entry : properties)
		{
			ContainerGameProfileProperty prop = entry.getValue();
			// Create Property instance using reflection
			Object property = ReflectionUtil.invokeMethod(this.constructorProperty, null, prop.name, prop.value, prop.signature);
			// Add to PropertyMap
			ReflectionUtil.invokeMethod(this.methodPropertyMapPut, propertyMap, entry.getKey(), property);
		}
	}
}
