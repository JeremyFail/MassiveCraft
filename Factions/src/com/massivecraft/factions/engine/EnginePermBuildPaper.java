package com.massivecraft.factions.engine;

import com.massivecraft.factions.util.EnumerationUtil;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.util.MUtil;
import io.papermc.paper.event.entity.EntityBreakByEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

/**
 * Paper-only cushion break protection.
 * <p>
 * On Paper 26.3+, cushions are not {@code Hanging}s, so
 * {@link org.bukkit.event.hanging.HangingBreakByEntityEvent} does not fire.
 * Breaking instead fires {@link EntityBreakByEntityEvent}.
 * <p>
 * Activated from {@link EnginePermBuild} only after that event class is confirmed present,
 * so Spigot never loads this class.
 */
public class EnginePermBuildPaper extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static EnginePermBuildPaper i = new EnginePermBuildPaper();
	public static EnginePermBuildPaper get() { return i; }

	// -------------------------------------------- //
	// LISTENER
	// -------------------------------------------- //

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void cushionBreak(EntityBreakByEntityEvent event)
	{
		Entity entity = event.getEntity();
		if (!EnumerationUtil.isEntityTypeCushion(entity.getType())) return;

		Entity remover = event.getRemover();
		if (MUtil.isntPlayer(remover)) return;

		EnginePermBuild.buildCushion((Player) remover, entity.getLocation().getBlock(), event);
	}

}
