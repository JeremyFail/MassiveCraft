package com.massivecraft.factions.engine;

import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.util.EnumerationUtil;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public enum ProtectCase
{
	// -------------------------------------------- //
	// ENUM
	// -------------------------------------------- //
	
	BUILD,
	USE_BLOCK,
	USE_ITEM,
	USE_ENTITY,
	USE_REDSTONE_BLOCK,
	LEASH_MOB,
	
	// END OF LIST
	;
	
	// -------------------------------------------- //
	// PERM
	// -------------------------------------------- //
	
	public MPerm getPerm(Object object)
	{
		switch (this)
		{
			case BUILD:
				return MPerm.getPermBuild();
			
			case USE_ITEM:
				if (!(object instanceof Material)) return null;
				if (!EnumerationUtil.isMaterialEditTool((Material) object)) return null;
				return MPerm.getPermBuild();
			
			case USE_ENTITY:
				if (!(object instanceof Entity)) return null;
				Entity entity = (Entity) object;
				EntityType type = entity.getType();
				if (EnumerationUtil.isEntityTypeContainer(type)) return MPerm.getPermContainer();
				if (EnumerationUtil.isEntityTypeEditOnInteract(type)) return MPerm.getPermBuild();
				return null;
				
			case USE_BLOCK:
				if (!(object instanceof Material)) return null;
				Material material = (Material) object;
				if (EnumerationUtil.isMaterialEditOnInteract(material)) return MPerm.getPermBuild();
				if (EnumerationUtil.isMaterialContainer(material)) return MPerm.getPermContainer();
				if (EnumerationUtil.isMaterialDoor(material)) return MPerm.getPermDoor();
				if (EnumerationUtil.isMaterialButton(material)) return MPerm.getPermButton();
				if (material == Material.LEVER) return MPerm.getPermLever();
				return null;
			
			case USE_REDSTONE_BLOCK:
				if (!(object instanceof Material)) return null;
				Material steppedMaterial = (Material) object;
				if (EnumerationUtil.isMaterialPressurePlate(steppedMaterial)) return MPerm.getPermPressurePlate();
				return null;

			case LEASH_MOB:
				return MPerm.getPermLeashMob();

			default:
				return null;
		}
	}
	
}
