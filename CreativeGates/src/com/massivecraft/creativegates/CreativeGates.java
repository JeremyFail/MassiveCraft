package com.massivecraft.creativegates;

import com.massivecraft.creativegates.cmd.CmdCg;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.MConfColl;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.index.IndexCombined;
import com.massivecraft.massivecore.MassivePlugin;
import com.massivecraft.massivecore.command.type.RegistryType;
import com.massivecraft.massivecore.command.type.enumeration.TypePermissionDefault;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.permissions.PermissionDefault;

import java.util.EnumSet;
import java.util.Set;

public class CreativeGates extends MassivePlugin
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //
	
	public final static Set<Material> VOID_MATERIALS = EnumSet.of(Material.AIR); 
	
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static CreativeGates i;
	public static CreativeGates get() { return i; }
	public CreativeGates()
	{
		CreativeGates.i = this;
		this.setVersionSynchronized(false);
	}
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	// Index
	private final IndexCombined index = new IndexCombined();
	public IndexCombined getIndex() { return this.index; }
	
	// Filling
	private boolean filling = false;
	public boolean isFilling() { return this.filling; }
	public void setFilling(boolean filling) { this.filling = filling; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void onEnableInner()
	{
		// Index
		this.getIndex().clear();
		
		// types
		RegistryType.register(PermissionDefault.class, TypePermissionDefault.get());
		
		// Activate
		this.activate(
			// Coll
			MConfColl.class,
			UGateColl.class,
		
			// Engine
			EngineMain.class,
			
			// Command
			CmdCg.class
		);
	
		// Schedule a permission update.
		// Possibly it will be useful due to the way Bukkit loads permissions.
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> MConf.get().updatePerms());
	}
	
	@Override
	public void onDisable()
	{
		this.getIndex().clear();
		super.onDisable();
	}
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	public static boolean isVoid(Material material)
	{
		return VOID_MATERIALS.contains(material);
	}
	
	public static boolean isVoid(Block block)
	{
		return isVoid(block.getType());
	}
	
	public static boolean isFluidFillMaterial(Material material)
	{
		return material == Material.WATER || material == Material.LAVA;
	}
	
	/**
	 * The material used to fill a gate in the given world.
	 * Water mode uses lava in the nether when {@link MConf#isUseLavaInNether()} is enabled.
	 * Horizontal gates always use water/lava because nether portal blocks do not work reliably on floors.
	 */
	public static Material getFillMaterial(World world)
	{
		return getFillMaterial(world, null);
	}
	
	public static Material getFillMaterial(World world, GateOrientation orientation)
	{
		MConf mconf = MConf.get();
		if (orientation != null && orientation.isHorizontal())
		{
			if (world.getEnvironment() == World.Environment.NETHER && mconf.isUseLavaInNether()) return Material.LAVA;
			return Material.WATER;
		}
		if (!mconf.isUsingWater()) return Material.NETHER_PORTAL;
		if (world.getEnvironment() == World.Environment.NETHER && mconf.isUseLavaInNether()) return Material.LAVA;
		return Material.WATER;
	}
	
}
