package com.massivecraft.creativegates.ui;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.engine.PendingGateCreates;
import com.massivecraft.creativegates.engine.create.GateCreate;
import com.massivecraft.creativegates.engine.create.PendingGateCreate;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.gate.fill.GateType;
import com.massivecraft.creativegates.gate.fill.ParticleGateType;
import com.massivecraft.massivecore.dialog.MDialog;
import com.massivecraft.massivecore.dialog.MDialogBuilder;
import com.massivecraft.massivecore.dialog.MDialogButton;
import com.massivecraft.massivecore.dialog.body.MDialogBodyItem;
import com.massivecraft.massivecore.dialog.type.MDialogTypeMultiAction;
import com.massivecraft.massivecore.dialog.type.MDialogTypeNotice;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.function.Consumer;

/**
 * Multi-step gate fill selection via MassiveCore {@link MDialog} (Paper / Spigot / ChestGui).
 * <p>
 * Flow: kind (blocks vs particles) when both are allowed, then a paged list. Particles are
 * action buttons; blocks stay item links. The kind step has Cancel; a list opened from kind
 * has Back as its footer. Escape still dismisses the dialog.
 * </p>
 */
public final class GateFillPicker
{
	private static final int PAGE_SIZE = 20;
	private static final int PARTICLE_COLUMNS = 2;
	
	private GateFillPicker()
	{
	}
	
	/**
	 * Opens the fill picker at the first applicable step.
	 *
	 * @param player Creating player.
	 * @param pending Snapshot waiting on a fill choice.
	 */
	public static void open(Player player, PendingGateCreate pending)
	{
		if (player == null || pending == null) return;
		
		List<GateType> blocks = MConf.get().getSelectableBlockGateTypes(pending.getOrientation());
		List<GateType> particles = MConf.get().getSelectableParticleGateTypes(pending.getOrientation());
		if (blocks.isEmpty() && particles.isEmpty()) return;
		
		if (!blocks.isEmpty() && !particles.isEmpty())
		{
			openKind(player, pending);
			return;
		}
		if (!particles.isEmpty())
		{
			openParticleList(player, pending, 0, false);
			return;
		}
		openBlockList(player, pending, 0, false);
	}
	
	/**
	 * Opens the kind selection dialog (block or particle).
	 * 
	 * @param player Creating player.
	 * @param pending Snapshot waiting on a fill choice.
	 */
	private static void openKind(Player player, PendingGateCreate pending)
	{
		MDialogBuilder dialog = baseDialog("<h>Select Gate Fill", "Choose a block fill or a particle fill.");
		
		dialog.body(MDialogBodyItem.of(named(Material.BRICKS, "Blocks"))
			.description("Blocks")
			.clickId("kind_blocks")
			.onClick((p, response) -> openLater(p, () -> openBlockList(p, pending, 0, true))));
		
		dialog.body(MDialogBodyItem.of(named(Material.FIREWORK_STAR, "Particles"))
			.description("Particles")
			.clickId("kind_particles")
			.onClick((p, response) -> openLater(p, () -> openParticleList(p, pending, 0, true))));
		
		dialog.type(MDialogTypeNotice.of(cancelButton()));
		MDialog.open(player, dialog.build());
	}
	
	/**
	 * Opens the block fill list dialog.
	 * 
	 * @param player Creating player.
	 * @param pending Snapshot waiting on a fill choice.
	 * @param page Current page number.
	 * @param canGoBack Whether the player can go back to the kind selection dialog.
	 */
	private static void openBlockList(Player player, PendingGateCreate pending, int page, boolean canGoBack)
	{
		List<GateType> types = MConf.get().getSelectableBlockGateTypes(pending.getOrientation());
		if (types.isEmpty())
		{
			if (canGoBack) openLater(player, () -> openKind(player, pending));
			return;
		}
		
		int totalPages = pageCount(types.size());
		int safePage = clampPage(page, totalPages);
		List<GateType> slice = pageSlice(types, safePage);
		
		MDialogBuilder dialog = baseDialog("<h>Select Block Fill", "Choose the fill block for this gate.");
		for (GateType gateType : slice)
		{
			String typeId = gateType.getConfigId();
			String label = prettyBlockName(gateType);
			dialog.body(MDialogBodyItem.of(iconFor(gateType))
				.description(label)
				.clickId(typeId)
				.onClick((p, response) -> GateCreate.complete(p, pending, gateType)));
		}
		
		applyFooter(dialog, canGoBack, safePage, totalPages,
			p -> openLater(p, () -> openKind(p, pending)),
			p -> openLater(p, () -> openBlockList(p, pending, safePage - 1, canGoBack)),
			p -> openLater(p, () -> openBlockList(p, pending, safePage + 1, canGoBack)));
		
		MDialog.open(player, dialog.build());
	}
	
	/**
	 * Opens the particle fill list dialog.
	 * 
	 * @param player Creating player.
	 * @param pending Snapshot waiting on a fill choice.
	 * @param page Current page number.
	 * @param canGoBack Whether the player can go back to the kind selection dialog.
	 */
	private static void openParticleList(Player player, PendingGateCreate pending, int page, boolean canGoBack)
	{
		List<GateType> types = MConf.get().getSelectableParticleGateTypes(pending.getOrientation());
		if (types.isEmpty())
		{
			if (canGoBack) openLater(player, () -> openKind(player, pending));
			return;
		}
		
		int totalPages = pageCount(types.size());
		int safePage = clampPage(page, totalPages);
		List<GateType> slice = pageSlice(types, safePage);
		
		MDialogBuilder dialog = baseDialog("<h>Select Particle Fill", "Choose the particle for this gate.");
		
		MDialogTypeMultiAction.Builder multi = MDialogTypeMultiAction.builder().columns(PARTICLE_COLUMNS);
		for (GateType gateType : slice)
		{
			String label = prettyParticleName(gateType);
			multi.action(MDialogButton.of(gateType.getConfigId(), label)
				.tooltip(label)
				.onClick((p, response) -> GateCreate.complete(p, pending, gateType)));
		}
		appendPageBodies(dialog, safePage, totalPages,
			p -> openLater(p, () -> openParticleList(p, pending, safePage - 1, canGoBack)),
			p -> openLater(p, () -> openParticleList(p, pending, safePage + 1, canGoBack)));
		multi.exit(listExitButton(canGoBack, p -> openLater(p, () -> openKind(p, pending))));
		dialog.type(multi.build());
		
		MDialog.open(player, dialog.build());
	}
	
	/**
	 * Creates a base dialog builder with the given title and body.
	 * 
	 * @param title Dialog title.
	 * @param body Dialog body.
	 * @return Base dialog builder.
	 */
	private static MDialogBuilder baseDialog(String title, String body)
	{
		return MDialog.builder()
			.title(title)
			.bodyPlain(body)
			.canCloseWithEscape(true)
			.onClose(p -> GateCreate.cancel(p, true));
	}
	
	/**
	 * Page links stay in the body so the footer can be a single exit button
	 * (Back after the kind step, otherwise Cancel).
	 */
	private static void applyFooter(MDialogBuilder dialog, boolean canGoBack, int page, int totalPages,
		Consumer<Player> back, Consumer<Player> prev, Consumer<Player> next)
	{
		appendPageBodies(dialog, page, totalPages, prev, next);
		dialog.type(MDialogTypeNotice.of(listExitButton(canGoBack, back)));
	}
	
	/**
	 * Appends page links to the dialog body.
	 * 
	 * @param dialog Dialog builder.
	 * @param page Current page number.
	 * @param totalPages Total number of pages.
	 * @param prev Consumer for previous page.
	 * @param next Consumer for next page.
	 */
	private static void appendPageBodies(MDialogBuilder dialog, int page, int totalPages,
		Consumer<Player> prev, Consumer<Player> next)
	{
		if (page > 0)
		{
			dialog.body(MDialogBodyItem.of(named(Material.SPECTRAL_ARROW, "Previous"))
				.description("Previous")
				.clickId("nav_prev")
				.onClick((p, response) -> prev.accept(p)));
		}
		if (page < totalPages - 1)
		{
			dialog.body(MDialogBodyItem.of(named(Material.COMPASS, "Next"))
				.description("Next")
				.clickId("nav_next")
				.onClick((p, response) -> next.accept(p)));
		}
	}
	
	/**
	 * Creates an exit button for the list dialog.
	 * 
	 * @param canGoBack Whether the player can go back to the kind selection dialog.
	 * @param back Consumer for going back to the kind selection dialog.
	 * @return Exit button.
	 */
	private static MDialogButton listExitButton(boolean canGoBack, Consumer<Player> back)
	{
		return canGoBack ? backButton(back) : cancelButton();
	}
	
	/**
	 * Creates a back button for the list dialog.
	 * 
	 * @param back Consumer for going back to the kind selection dialog.
	 * @return Back button.
	 */
	private static MDialogButton backButton(Consumer<Player> back)
	{
		return MDialogButton.of("nav_back", "Back")
			.tooltip("Previous step")
			.icon(new ItemStack(Material.ARROW))
			.onClick((p, response) -> back.accept(p));
	}
	
	/**
	 * Creates a cancel button for the list dialog.
	 * 
	 * @return Cancel button.
	 */
	private static MDialogButton cancelButton()
	{
		return MDialogButton.of("cancel", "Cancel")
			.tooltip("Cancel gate creation")
			.icon(new ItemStack(Material.BARRIER))
			.onClick((p, response) -> GateCreate.cancel(p, true));
	}
	
	/**
	 * Opens a task later on the main server thread.
	 * 
	 * @param player Creating player.
	 * @param task Task to run.
	 */
	private static void openLater(Player player, Runnable task)
	{
		Bukkit.getScheduler().runTask(CreativeGates.get(), () -> {
			if (player == null || !player.isOnline()) return;
			if (PendingGateCreates.get().get(player) == null) return;
			task.run();
		});
	}
	
	/**
	 * Calculates the total number of pages for a list of items.
	 * 
	 * @param size Total number of items.
	 * @return Total number of pages.
	 */
	private static int pageCount(int size)
	{
		if (size <= 0) return 1;
		return (size + PAGE_SIZE - 1) / PAGE_SIZE;
	}
	
	/**
	 * Clamps a page number to a valid range.
	 * 
	 * @param page Current page number.
	 * @param totalPages Total number of pages.
	 * @return Clamped page number.
	 */
	private static int clampPage(int page, int totalPages)
	{
		if (page < 0) return 0;
		if (page >= totalPages) return totalPages - 1;
		return page;
	}
	
	/**
	 * Creates a sublist of items for the current page.
	 * 
	 * @param types List of items.
	 * @param page Current page number.
	 * @return Sublist of items for the current page.
	 */
	private static List<GateType> pageSlice(List<GateType> types, int page)
	{
		int from = page * PAGE_SIZE;
		int to = Math.min(types.size(), from + PAGE_SIZE);
		return types.subList(from, to);
	}
	
	/**
	 * Creates an item stack for the given gate type.
	 * 
	 * @param type Gate type.
	 * @return Item stack.
	 */
	private static ItemStack iconFor(GateType type)
	{
		ItemStack stack = new ItemStack(iconMaterial(type.getBaseMaterial()));
		ItemMeta meta = stack.getItemMeta();
		if (meta != null)
		{
			meta.setDisplayName(Txt.parse("<h>%s", prettyBlockName(type)));
			stack.setItemMeta(meta);
		}
		return stack;
	}
	
	/**
	 * Dialog item bodies need an {@link ItemStack}; Paper 26+ rejects non-item materials
	 * ({@code NETHER_PORTAL}, fluids, {@code END_GATEWAY}, …). Those fills have no item form,
	 * so we show a related proxy item.
	 */
	private static Material iconMaterial(Material material)
	{
		if (material != null && material.isItem()) return material;
		if (material == null) return Material.BARRIER;
		switch (material)
		{
			case WATER:
			case BUBBLE_COLUMN:
				return Material.WATER_BUCKET;
			case LAVA:
				return Material.LAVA_BUCKET;
			case POWDER_SNOW:
				return Material.POWDER_SNOW_BUCKET;
			case NETHER_PORTAL:
				return Material.OBSIDIAN;
			case END_GATEWAY:
			case END_PORTAL:
				return Material.END_PORTAL_FRAME;
			case FROSTED_ICE:
				return Material.ICE;
			case FIRE:
				return Material.CAMPFIRE;
			case SOUL_FIRE:
				return Material.SOUL_CAMPFIRE;
			default:
				return Material.BARRIER;
		}
	}
	
	/**
	 * Creates an item stack with the given material and name.
	 * 
	 * @param material Material.
	 * @param name Item name.
	 * @return Item stack.
	 */
	private static ItemStack named(Material material, String name)
	{
		ItemStack stack = new ItemStack(material == null ? Material.PAPER : material);
		ItemMeta meta = stack.getItemMeta();
		if (meta != null)
		{
			meta.setDisplayName(Txt.parse("<h>%s", name));
			stack.setItemMeta(meta);
		}
		return stack;
	}
	
	/**
	 * Creates a pretty name for the given block gate type.
	 * 
	 * @param type Block gate type.
	 * @return Pretty name.
	 */
	private static String prettyBlockName(GateType type)
	{
		return Txt.getMaterialName(type.getBaseMaterial());
	}
	
	/**
	 * Creates a pretty name for the given particle gate type.
	 * 
	 * @param type Particle gate type.
	 * @return Pretty name.
	 */
	private static String prettyParticleName(GateType type)
	{
		if (type instanceof ParticleGateType particle) return particle.getDisplayName();
		return type.getConfigId();
	}
}
