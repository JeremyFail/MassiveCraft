package com.massivecraft.creativegates.ui;

import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.engine.EngineGateOverride;
import com.massivecraft.creativegates.engine.PendingGateCreates;
import com.massivecraft.creativegates.engine.create.GateCreate;
import com.massivecraft.creativegates.engine.create.PendingGateCreate;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.creativegates.gate.GateOrientation;
import com.massivecraft.creativegates.gate.fill.GateType;
import com.massivecraft.creativegates.gate.fill.ParticleGateType;
import com.massivecraft.massivecore.dialog.MDialog;
import com.massivecraft.massivecore.dialog.MDialogBuilder;
import com.massivecraft.massivecore.dialog.MDialogButton;
import com.massivecraft.massivecore.dialog.body.MDialogBodyItem;
import com.massivecraft.massivecore.dialog.type.MDialogTypeMultiAction;
import com.massivecraft.massivecore.dialog.type.MDialogTypeNotice;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Multi-step gate fill selection via MassiveCore {@link MDialog} (Paper / Spigot / ChestGui).
 * <p>
 * Shared UI for create ({@link PendingGateCreate}) and edit ({@link UGate}). Flow: kind
 * (blocks vs particles) when both are allowed, then a paged list. Particles are action
 * buttons; blocks stay item links. The kind step has Cancel; a list opened from kind has
 * Back as its footer. Escape still dismisses the dialog.
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
	 * Create-time fill picker (pending snapshot → {@link GateCreate#complete}).
	 *
	 * @param player Creating player.
	 * @param pending Snapshot waiting on a fill choice.
	 */
	public static void open(Player player, PendingGateCreate pending)
	{
		if (player == null || pending == null) return;
		
		open(player, new Session(
			pending.getOrientation(),
			(p, type) -> GateCreate.complete(p, pending, type),
			p -> GateCreate.cancel(p, true),
			p -> PendingGateCreates.get().get(p) != null,
			"Cancel gate creation"
		));
	}
	
	/**
	 * Edit-time fill picker for an existing gate (manage UI).
	 *
	 * @param player Managing player.
	 * @param gate Target gate.
	 */
	public static void open(Player player, UGate gate)
	{
		if (player == null || gate == null) return;
		
		String gateId = gate.getId();
		open(player, new Session(
			gate.getOrientation(),
			(p, type) -> applyEdit(p, gate, type),
			p -> GateManageUi.open(p, gate),
			p -> p.isOnline() && gateId != null && UGateColl.get().getFixed(gateId) != null,
			"Back to manage"
		));
	}
	
	/**
	 * Display name for the gate's current fill (blocks or particles).
	 *
	 * @param gate Target gate.
	 * @return Pretty fill name.
	 */
	public static String currentFillLabel(UGate gate)
	{
		if (gate == null) return "unknown";
		GateType type = gate.getFillType();
		if (type == null) return "unknown";
		return prettyName(type);
	}
	
	/**
	 * Whether this player may change fill on the gate from manage UI.
	 * Requires manage access, plus {@link Perm#SET_GATE_FILL} or ownership bypass / override.
	 *
	 * @param player Viewer.
	 * @param gate Target gate.
	 * @return True if the Gate Fill manage control should show.
	 */
	public static boolean canChangeFill(Player player, UGate gate)
	{
		if (player == null || gate == null) return false;
		if (!EngineGateOverride.canManage(player, gate)) return false;
		if (Perm.SET_GATE_FILL.has(player)) return true;
		return EngineGateOverride.canBypassOwnership(player);
	}
	
	private static void applyEdit(Player player, UGate gate, GateType gateType)
	{
		if (player == null || gate == null || gateType == null) return;
		if (!canChangeFill(player, gate))
		{
			MixinMessage.get().messageOne(player, Txt.parse("<b>You cannot change this gate's fill."));
			return;
		}
		if (!MConf.get().isGateTypeAllowed(gateType, gate.getOrientation()))
		{
			MixinMessage.get().messageOne(player, Txt.parse("<b>That gate fill is no longer allowed."));
			return;
		}
		
		gate.empty();
		gate.setFillType(gateType);
		gate.fill();
		
		MixinMessage.get().messageOne(player, Txt.parse("<g>Gate fill set to <h>%s<g>.", prettyName(gateType)));
		GateManageUi.open(player, gate);
	}
	
	private static void open(Player player, Session session)
	{
		if (player == null || session == null) return;
		
		List<GateType> blocks = MConf.get().getSelectableBlockGateTypes(session.orientation);
		List<GateType> particles = MConf.get().getSelectableParticleGateTypes(session.orientation);
		if (blocks.isEmpty() && particles.isEmpty()) return;
		
		if (!blocks.isEmpty() && !particles.isEmpty())
		{
			openKind(player, session);
			return;
		}
		if (!particles.isEmpty())
		{
			openParticleList(player, session, 0, false);
			return;
		}
		openBlockList(player, session, 0, false);
	}
	
	private static void openKind(Player player, Session session)
	{
		MDialogBuilder dialog = baseDialog(session, "<h>Select Gate Fill", "Choose a block fill or a particle fill.");
		
		dialog.body(MDialogBodyItem.of(named(Material.BRICKS, "Blocks"))
			.description("Blocks")
			.clickId("kind_blocks")
			.onClick((p, response) -> openLater(p, session, () -> openBlockList(p, session, 0, true))));
		
		dialog.body(MDialogBodyItem.of(named(Material.FIREWORK_STAR, "Particles"))
			.description("Particles")
			.clickId("kind_particles")
			.onClick((p, response) -> openLater(p, session, () -> openParticleList(p, session, 0, true))));
		
		dialog.type(MDialogTypeNotice.of(cancelButton(session)));
		MDialog.open(player, dialog.build());
	}
	
	private static void openBlockList(Player player, Session session, int page, boolean canGoBack)
	{
		List<GateType> types = MConf.get().getSelectableBlockGateTypes(session.orientation);
		if (types.isEmpty())
		{
			if (canGoBack) openLater(player, session, () -> openKind(player, session));
			return;
		}
		
		int totalPages = pageCount(types.size());
		int safePage = clampPage(page, totalPages);
		List<GateType> slice = pageSlice(types, safePage);
		
		MDialogBuilder dialog = baseDialog(session, "<h>Select Block Fill", "Choose the fill block for this gate.");
		for (GateType gateType : slice)
		{
			String typeId = gateType.getConfigId();
			String label = prettyName(gateType);
			dialog.body(MDialogBodyItem.of(iconFor(gateType))
				.description(label)
				.clickId(typeId)
				.onClick((p, response) -> session.onSelect.accept(p, gateType)));
		}
		
		applyFooter(dialog, session, canGoBack, safePage, totalPages,
			p -> openLater(p, session, () -> openKind(p, session)),
			p -> openLater(p, session, () -> openBlockList(p, session, safePage - 1, canGoBack)),
			p -> openLater(p, session, () -> openBlockList(p, session, safePage + 1, canGoBack)));
		
		MDialog.open(player, dialog.build());
	}
	
	private static void openParticleList(Player player, Session session, int page, boolean canGoBack)
	{
		List<GateType> types = MConf.get().getSelectableParticleGateTypes(session.orientation);
		if (types.isEmpty())
		{
			if (canGoBack) openLater(player, session, () -> openKind(player, session));
			return;
		}
		
		int totalPages = pageCount(types.size());
		int safePage = clampPage(page, totalPages);
		List<GateType> slice = pageSlice(types, safePage);
		
		MDialogBuilder dialog = baseDialog(session, "<h>Select Particle Fill", "Choose the particle for this gate.");
		
		MDialogTypeMultiAction.Builder multi = MDialogTypeMultiAction.builder().columns(PARTICLE_COLUMNS);
		for (GateType gateType : slice)
		{
			String label = prettyName(gateType);
			multi.action(MDialogButton.of(gateType.getConfigId(), label)
				.tooltip(label)
				.onClick((p, response) -> session.onSelect.accept(p, gateType)));
		}
		appendPageBodies(dialog, safePage, totalPages,
			p -> openLater(p, session, () -> openParticleList(p, session, safePage - 1, canGoBack)),
			p -> openLater(p, session, () -> openParticleList(p, session, safePage + 1, canGoBack)));
		multi.exit(listExitButton(session, canGoBack, p -> openLater(p, session, () -> openKind(p, session))));
		dialog.type(multi.build());
		
		MDialog.open(player, dialog.build());
	}
	
	private static MDialogBuilder baseDialog(Session session, String title, String body)
	{
		return MDialog.builder()
			.title(title)
			.bodyPlain(body)
			.canCloseWithEscape(true)
			.onClose(p -> session.onCancel.accept(p));
	}
	
	private static void applyFooter(MDialogBuilder dialog, Session session, boolean canGoBack, int page, int totalPages,
		Consumer<Player> back, Consumer<Player> prev, Consumer<Player> next)
	{
		appendPageBodies(dialog, page, totalPages, prev, next);
		dialog.type(MDialogTypeNotice.of(listExitButton(session, canGoBack, back)));
	}
	
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
	
	private static MDialogButton listExitButton(Session session, boolean canGoBack, Consumer<Player> back)
	{
		return canGoBack ? backButton(back) : cancelButton(session);
	}
	
	private static MDialogButton backButton(Consumer<Player> back)
	{
		return MDialogButton.of("nav_back", "Back")
			.tooltip("Previous step")
			.icon(new ItemStack(Material.ARROW))
			.onClick((p, response) -> back.accept(p));
	}
	
	private static MDialogButton cancelButton(Session session)
	{
		return MDialogButton.of("cancel", "Cancel")
			.tooltip(session.cancelTooltip)
			.icon(new ItemStack(Material.BARRIER))
			.onClick((p, response) -> session.onCancel.accept(p));
	}
	
	private static void openLater(Player player, Session session, Runnable task)
	{
		Bukkit.getScheduler().runTask(CreativeGates.get(), () ->
		{
			if (player == null || !player.isOnline()) return;
			if (!session.stillValid.test(player)) return;
			task.run();
		});
	}
	
	private static int pageCount(int size)
	{
		if (size <= 0) return 1;
		return (size + PAGE_SIZE - 1) / PAGE_SIZE;
	}
	
	private static int clampPage(int page, int totalPages)
	{
		if (page < 0) return 0;
		if (page >= totalPages) return totalPages - 1;
		return page;
	}
	
	private static List<GateType> pageSlice(List<GateType> types, int page)
	{
		int from = page * PAGE_SIZE;
		int to = Math.min(types.size(), from + PAGE_SIZE);
		return types.subList(from, to);
	}
	
	private static ItemStack iconFor(GateType type)
	{
		ItemStack stack = new ItemStack(iconMaterial(type.getBaseMaterial()));
		ItemMeta meta = stack.getItemMeta();
		if (meta != null)
		{
			meta.setDisplayName(Txt.parse("<h>%s", prettyName(type)));
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
	 * Pretty name for a fill type (block material or particle display name).
	 *
	 * @param type Gate type.
	 * @return Display label.
	 */
	public static String prettyName(GateType type)
	{
		if (type == null) return "unknown";
		if (type instanceof ParticleGateType particle) return particle.getDisplayName();
		return Txt.getMaterialName(type.getBaseMaterial());
	}
	
	/**
	 * Create/edit session: orientation + callbacks, independent of pending vs existing gate.
	 */
	private static final class Session
	{
		private final GateOrientation orientation;
		private final BiConsumer<Player, GateType> onSelect;
		private final Consumer<Player> onCancel;
		private final Predicate<Player> stillValid;
		private final String cancelTooltip;
		
		private Session(GateOrientation orientation, BiConsumer<Player, GateType> onSelect, Consumer<Player> onCancel,
			Predicate<Player> stillValid, String cancelTooltip)
		{
			this.orientation = orientation;
			this.onSelect = onSelect;
			this.onCancel = onCancel;
			this.stillValid = stillValid;
			this.cancelTooltip = cancelTooltip;
		}
	}
}
