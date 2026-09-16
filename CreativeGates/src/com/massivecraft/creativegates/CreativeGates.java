package com.massivecraft.creativegates;

import com.massivecraft.creativegates.cmd.CmdCg;
import com.massivecraft.creativegates.engine.EngineGateClientVisual;
import com.massivecraft.creativegates.engine.EngineMain;
import com.massivecraft.creativegates.engine.PendingGateCreates;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.MConfColl;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.entity.migrator.MigratorMConf001GateTypes;
import com.massivecraft.creativegates.gate.GateOrientation;
import com.massivecraft.creativegates.gate.fill.GateType;
import com.massivecraft.creativegates.gate.fill.SupportedGateType;
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
		
		// Activate (migrator before MConfColl so version upgrades run on load)
		this.activate(
			MigratorMConf001GateTypes.class,
			
			// Coll
			MConfColl.class,
			UGateColl.class,
		
			// Engine
			EngineMain.class,
			PendingGateCreates.class,
			EngineGateClientVisual.class,
			
			// Command
			CmdCg.class
		);
	
		// Schedule a permission update.
		// Possibly it will be useful due to the way Bukkit loads permissions.
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> MConf.get().updatePerms());
		
		// Resync client-visual gate overlays after load.
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
			for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers())
			{
				EngineGateClientVisual.get().syncPlayer(player);
			}
		}, 40L);
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
	 * Materials that can appear as real server-side gate interiors.
	 */
	public static boolean isGateFillMaterial(Material material)
	{
		return SupportedGateType.fromServerMaterial(material) != null;
	}
	
	/**
	 * True for void or known gate fill materials (used when replacing interior blocks).
	 */
	public static boolean isGateFillOrVoid(Material material)
	{
		return isVoid(material) || isGateFillMaterial(material);
	}
	
	/**
	 * Resolves the server block to place for a gate type in a world.
	 *
	 * @param gateType Selected gate type; null falls back to config default.
	 * @param world World the gate is in.
	 * @param orientation Gate orientation for default selection / compatibility.
	 * @return Material to place, never null.
	 */
	public static Material getFillMaterial(GateType gateType, World world, GateOrientation orientation)
	{
		GateType type = gateType;
		if (type == null)
		{
			type = MConf.get().resolveDefaultGateType(orientation, world);
		}
		if (type == null)
		{
			if (orientation != null && orientation.isHorizontal()) return Material.WATER;
			return Material.NETHER_PORTAL;
		}
		return type.getServerFillMaterial(world);
	}
	
}
