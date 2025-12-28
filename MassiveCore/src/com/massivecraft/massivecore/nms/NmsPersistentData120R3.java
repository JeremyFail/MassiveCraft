package com.massivecraft.massivecore.nms;

import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.particleeffect.ReflectionUtils.PackageType;
import com.massivecraft.massivecore.util.ReflectionUtil;
import org.bukkit.persistence.PersistentDataContainer;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;

public class NmsPersistentData120R3 extends NmsPersistentData
{
	
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	@SuppressWarnings("FieldMayBeFinal")
	private static NmsPersistentData120R3 i = new NmsPersistentData120R3();
	public static NmsPersistentData120R3 get () { return i; }
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	protected Class<?> classCraftPersistentDataContainer;
	protected Class<?> classCraftNBTTagConfigSerializer;
	protected Class<?> classCompoundTag;
	protected Class<?> classTag;
	
	protected Method methodSerialize;
	protected Method methodIsEmpty;
	protected Method methodClear;
	protected Method methodDeserialize;
	protected Method methodDeserializeMap;
	protected Method methodPutAll;
	protected Method methodSerializeTag;
	
	// -------------------------------------------- //
	// SETUP
	// -------------------------------------------- //
	
	@Override
	public void setup() throws Throwable
	{
		this.classCraftPersistentDataContainer = PackageType.CRAFTBUKKIT_VERSION_PERSISTENCE.getClass("CraftPersistentDataContainer");
		this.classCraftNBTTagConfigSerializer = PackageType.CRAFTBUKKIT_VERSION_UTIL.getClass("CraftNBTTagConfigSerializer");
		this.classCompoundTag = PackageType.MINECRAFT_SERVER.getClass("nbt.CompoundTag");
		this.classTag = PackageType.MINECRAFT_SERVER.getClass("nbt.Tag");
		
		this.methodSerialize = ReflectionUtil.getMethod(this.classCraftPersistentDataContainer, "serialize");
		this.methodIsEmpty = ReflectionUtil.getMethod(this.classCraftPersistentDataContainer, "isEmpty");
		this.methodClear = ReflectionUtil.getMethod(this.classCraftPersistentDataContainer, "clear");
		this.methodDeserialize = ReflectionUtil.getMethod(this.classCraftNBTTagConfigSerializer, "deserialize", String.class);
		this.methodDeserializeMap = ReflectionUtil.getMethod(this.classCraftNBTTagConfigSerializer, "deserialize", Map.class);
		this.methodPutAll = ReflectionUtil.getMethod(this.classCraftPersistentDataContainer, "putAll", this.classCompoundTag);
		this.methodSerializeTag = ReflectionUtil.getMethod(this.classCraftNBTTagConfigSerializer, "serialize", this.classCompoundTag);
	}
	
	// -------------------------------------------- //
	// PERSISTENT DATA
	// -------------------------------------------- //
	
	@Override
	public String getPersistentData(PersistentDataContainer persistentDataContainer) {
		if (!this.classCraftPersistentDataContainer.isInstance(persistentDataContainer)) {
			MassiveCore.get().log(Level.WARNING, "Failed to getPersistentData - Not CraftPDC");
			return null;
		}
		
		Boolean isEmpty = ReflectionUtil.invokeMethod(this.methodIsEmpty, persistentDataContainer);
		if (isEmpty != null && isEmpty) return null;
		
		return ReflectionUtil.invokeMethod(this.methodSerialize, persistentDataContainer);
	}
	
	@Override
	public void setPersistentData(PersistentDataContainer persistentDataContainer, String data) {
		if (!this.classCraftPersistentDataContainer.isInstance(persistentDataContainer)) {
			MassiveCore.get().log(Level.WARNING, "Failed to setPersistentData - Not CraftPDC");
			return;
		}
		
		if (data == null) {
			ReflectionUtil.invokeMethod(this.methodClear, persistentDataContainer);
			return;
		}
		
		Object deserialized = ReflectionUtil.invokeMethod(this.methodDeserialize, null, data);
		if (this.classCompoundTag.isInstance(deserialized)) {
			ReflectionUtil.invokeMethod(this.methodPutAll, persistentDataContainer, deserialized);
		}
	}
	
	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public String mapToString(Map<String, Object> data) {
		Object deserialized = ReflectionUtil.invokeMethod(this.methodDeserializeMap, null, data);
		if (this.classCompoundTag.isInstance(deserialized)) {
			return ReflectionUtil.invokeMethod(this.methodSerializeTag, null, deserialized);
		}
		
		MassiveCore.get().log(Level.WARNING, "Failed to mapToString - Deserialized Tag is not CompoundTag");
		return null;
	}
	
}
