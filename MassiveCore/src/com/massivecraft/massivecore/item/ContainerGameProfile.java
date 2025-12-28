package com.massivecraft.massivecore.item;

import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.editor.annotation.EditorMethods;
import com.massivecraft.massivecore.comparator.ComparatorSmart;
import com.massivecraft.massivecore.util.MUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@EditorMethods(true)
public class ContainerGameProfile implements Comparable<ContainerGameProfile>
{
	// -------------------------------------------- //
	// DEFAULTS
	// -------------------------------------------- //
	
	public static final transient List<Map.Entry<String, ContainerGameProfileProperty>> GAMEPROFILEPROPERTIESDEFAULT = new MassiveList<>();
	public static final transient String UUID_DEFAULT = null; // TODO should there be a default uuid which isn't null?
	public static final transient String NAME_DEFAULT = null;
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private String uuid = null;
	public String getUuid() { return DataItemStack.get(this.uuid, UUID_DEFAULT); }
	public void setUuid(String uuid) { this.uuid = DataItemStack.set(uuid, UUID_DEFAULT); }
	
	private String name = null;
	public String getName() { return DataItemStack.get(this.name, NAME_DEFAULT); }
	public void setName(String name) { this.name = DataItemStack.set(name, NAME_DEFAULT); }
	
	private List<Map.Entry<String, ContainerGameProfileProperty>> gameProfileProperties = null;
	public List<Map.Entry<String, ContainerGameProfileProperty>> getGameProfileProperties() { return DataItemStack.get(this.gameProfileProperties, GAMEPROFILEPROPERTIESDEFAULT); }
	public void setGameProfileProperties(List<Map.Entry<String, ContainerGameProfileProperty>> properties) { this.gameProfileProperties = DataItemStack.set(properties, GAMEPROFILEPROPERTIESDEFAULT); }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public ContainerGameProfile()
	{
		// Construct
	}
	
	// -------------------------------------------- //
	// COMPARE & EQUALS & HASHCODE
	// -------------------------------------------- //
	
	@Override
	public int compareTo(ContainerGameProfile that)
	{
		return ComparatorSmart.get().compare(
			this.getUuid(), that.getUuid(),
			this.getName(), that.getName(),
			this.getGameProfileProperties(), that.getGameProfileProperties()
		);
	}
	
	@Override
	public boolean equals(Object object)
	{
        if (!(object instanceof ContainerGameProfile)) return false;
		ContainerGameProfile that = (ContainerGameProfile) object;
		
		return MUtil.equals(
			this.getUuid(), that.getUuid(),
			this.getName(), that.getName(),
			this.getGameProfileProperties(), that.getGameProfileProperties()
		);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			this.getName(),
			this.getUuid(),
			this.getGameProfileProperties()
		);
	}
	
}
