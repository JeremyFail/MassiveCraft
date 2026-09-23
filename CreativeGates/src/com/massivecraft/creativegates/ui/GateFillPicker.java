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
import com.massivecraft.massivecore.chestgui.ChestActionAbstract;
import com.massivecraft.massivecore.chestgui.ChestGui;
import com.massivecraft.massivecore.dialog.MDialog;
import com.massivecraft.massivecore.dialog.MDialogBuilder;
import com.massivecraft.massivecore.dialog.MDialogButton;
import com.massivecraft.massivecore.dialog.MDialogResponse;
import com.massivecraft.massivecore.dialog.body.MDialogBodyItem;
import com.massivecraft.massivecore.dialog.input.MDialogInputNumber;
import com.massivecraft.massivecore.dialog.type.MDialogTypeConfirmation;
import com.massivecraft.massivecore.dialog.type.MDialogTypeMultiAction;
import com.massivecraft.massivecore.dialog.type.MDialogTypeNotice;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Multi-step gate fill selection via MassiveCore {@link MDialog} (Paper / Spigot / ChestGui).
 * <p>
 * Shared UI for create ({@link PendingGateCreate}) and edit ({@link UGate}). Particle fills may
 * open an amount step (slider or ChestGui +/-) when the player may set particle count.
 * </p>
 */
public final class GateFillPicker
{
	private static final int PAGE_SIZE = 20;
	private static final int PARTICLE_COLUMNS = 2;
	private static final String AMOUNT_KEY = "amount";
	
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
			MConf.get().getGateFillParticleAmountDefault(),
			(p, type, amount) -> GateCreate.complete(p, pending, type, amount),
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
			gate.getFillParticleAmount(),
			(p, type, amount) -> applyEdit(p, gate, type, amount),
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
		if (type.isParticleFill())
		{
			return prettyName(type) + " (" + gate.getFillParticleAmount() + ")";
		}
		return prettyName(type);
	}
	
	/**
	 * Whether this player may change fill on the gate from manage UI.
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
	
	/**
	 * Whether the player may choose particle amount (create or edit).
	 * Override / bypass always may; otherwise {@link Perm#SET_FILL_PARTICLE_COUNT}.
	 *
	 * @param player Viewer.
	 * @return True if the amount step should be shown.
	 */
	public static boolean canSetParticleCount(Player player)
	{
		if (player == null) return false;
		if (Perm.SET_FILL_PARTICLE_COUNT.has(player)) return true;
		return EngineGateOverride.canBypassOwnership(player);
	}
	
	private static void applyEdit(Player player, UGate gate, GateType gateType, int particleAmount)
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
		if (gateType.isParticleFill())
		{
			gate.setFillParticleAmount(particleAmount);
		}
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
				.onClick((p, response) -> session.onSelect.accept(p, gateType, session.initialParticleAmount)));
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
				.onClick((p, response) -> selectParticle(p, session, gateType, safePage, canGoBack)));
		}
		if (safePage > 0)
		{
			multi.action(MDialogButton.of("nav_prev", "<aqua><< Previous Page")
				.tooltip("Previous page")
				.onClick((p, response) -> openLater(p, session, () -> openParticleList(p, session, safePage - 1, canGoBack))));
		}
		if (safePage < totalPages - 1)
		{
			multi.action(MDialogButton.of("nav_next", "<aqua>Next Page >>")
				.tooltip("Next page")
				.onClick((p, response) -> openLater(p, session, () -> openParticleList(p, session, safePage + 1, canGoBack))));
		}
		multi.exit(listExitButton(session, canGoBack, p -> openLater(p, session, () -> openKind(p, session))));
		dialog.type(multi.build());
		
		MDialog.open(player, dialog.build());
	}
	
	private static void selectParticle(Player player, Session session, GateType gateType, int listPage, boolean listCanGoBack)
	{
		if (canSetParticleCount(player))
		{
			openLater(player, session, () -> openParticleAmount(player, session, gateType, listPage, listCanGoBack));
			return;
		}
		session.onSelect.accept(player, gateType, session.initialParticleAmount);
	}
	
	private static void openParticleAmount(Player player, Session session, GateType gateType, int listPage, boolean listCanGoBack)
	{
		if (MDialog.isNativeDialogAvailable())
		{
			openParticleAmountDialog(player, session, gateType, listPage, listCanGoBack);
		}
		else
		{
			openParticleAmountChest(player, session, gateType, listPage, listCanGoBack);
		}
	}
	
	private static void openParticleAmountDialog(Player player, Session session, GateType gateType, int listPage, boolean listCanGoBack)
	{
		MConf conf = MConf.get();
		int min = conf.getGateFillParticleAmountMin();
		int max = conf.getGateFillParticleAmountMax();
		int initial = conf.clampParticleAmount(session.initialParticleAmount);
		
		MDialog.open(player, MDialog.builder()
			.title("<h>Particle Amount")
			.bodyPlain(Txt.parse("<i>Fill: <h>%s\n<i>Choose how many particles spawn.", prettyName(gateType)))
			.canCloseWithEscape(true)
			.onClose(p -> session.onCancel.accept(p))
			.input(MDialogInputNumber.of(AMOUNT_KEY, "Amount", min, max)
				.initial(initial)
				.step(1f)
				.width(200))
			.type(MDialogTypeConfirmation.of(
				MDialogButton.of("save", "Save")
					.tooltip("Save this particle amount.")
					.onClick((p, response) -> finishParticleAmount(p, session, gateType, response)),
				MDialogButton.of("back", "Back")
					.tooltip("Back to particle selection.")
					.onClick((p, response) -> openLater(p, session, () -> openParticleList(p, session, listPage, listCanGoBack)))
			))
		);
	}
	
	private static void finishParticleAmount(Player player, Session session, GateType gateType, MDialogResponse response)
	{
		int amount = session.initialParticleAmount;
		if (response != null)
		{
			Float value = response.getNumber(AMOUNT_KEY);
			if (value != null) amount = Math.round(value);
		}
		amount = MConf.get().clampParticleAmount(amount);
		session.onSelect.accept(player, gateType, amount);
	}
	
	private static void openParticleAmountChest(Player player, Session session, GateType gateType, int listPage, boolean listCanGoBack)
	{
		MConf conf = MConf.get();
		int min = conf.getGateFillParticleAmountMin();
		int max = conf.getGateFillParticleAmountMax();
		int[] amount = {conf.clampParticleAmount(session.initialParticleAmount)};
		
		ChestGui gui = ChestGui.create(27, Txt.parse("<h>Particle Amount"));
		gui.setBottomInventoryAllow(false);
		gui.setAutoclosing(false);
		gui.setAutoremoving(true);
		
		Runnable refresh = () -> paintParticleAmountChest(gui, amount[0], min, max, prettyName(gateType));
		
		gui.setAction(11, new ChestActionAbstract()
		{
			@Override
			public boolean onClick(InventoryClickEvent event, Player p)
			{
				if (amount[0] <= min) return false;
				amount[0]--;
				refresh.run();
				return false;
			}
		});
		gui.setAction(15, new ChestActionAbstract()
		{
			@Override
			public boolean onClick(InventoryClickEvent event, Player p)
			{
				if (amount[0] >= max) return false;
				amount[0]++;
				refresh.run();
				return false;
			}
		});
		gui.setAction(21, new ChestActionAbstract()
		{
			@Override
			public boolean onClick(InventoryClickEvent event, Player p)
			{
				gui.setAutoclosing(true);
				openLater(p, session, () -> openParticleList(p, session, listPage, listCanGoBack));
				return true;
			}
		});
		gui.setAction(23, new ChestActionAbstract()
		{
			@Override
			public boolean onClick(InventoryClickEvent event, Player p)
			{
				gui.setAutoclosing(true);
				session.onSelect.accept(p, gateType, amount[0]);
				return true;
			}
		});
		
		refresh.run();
		gui.open(player);
	}
	
	private static void paintParticleAmountChest(ChestGui gui, int amount, int min, int max, String fillName)
	{
		String hover = amountHover(amount, min, max);
		gui.setItem(11, pane(Material.RED_STAINED_GLASS_PANE, "<b>-", hover));
		gui.setItem(13, amountDisplay(amount, min, max, fillName));
		gui.setItem(15, pane(Material.LIME_STAINED_GLASS_PANE, "<g>+", hover));
		gui.setItem(21, named(Material.ARROW, "<i>Back"));
		gui.setItem(23, named(Material.EMERALD, "<g>Save"));
	}
	
	private static String amountHover(int amount, int min, int max)
	{
		if (amount <= min) return Txt.parse("<c>MIN") + "\n" + Txt.parse("<i>Current: <h>%s", amount);
		if (amount >= max) return Txt.parse("<c>MAX") + "\n" + Txt.parse("<i>Current: <h>%s", amount);
		return Txt.parse("<i>Current: <h>%s", amount);
	}
	
	private static ItemStack amountDisplay(int amount, int min, int max, String fillName)
	{
		ItemStack stack = new ItemStack(Material.FIREWORK_STAR);
		ItemMeta meta = stack.getItemMeta();
		if (meta != null)
		{
			meta.setDisplayName(Txt.parse("<h>%s", fillName));
			String status;
			if (amount <= min) status = Txt.parse("<c>MIN");
			else if (amount >= max) status = Txt.parse("<c>MAX");
			else status = Txt.parse("<i>Current: <h>%s", amount);
			meta.setLore(Arrays.asList(status, Txt.parse("<i>Amount: <h>%s", amount)));
			stack.setItemMeta(meta);
		}
		return stack;
	}
	
	private static ItemStack pane(Material material, String name, String loreLine)
	{
		ItemStack stack = new ItemStack(material);
		ItemMeta meta = stack.getItemMeta();
		if (meta != null)
		{
			meta.setDisplayName(Txt.parse(name));
			meta.setLore(Arrays.asList(loreLine.split("\n")));
			stack.setItemMeta(meta);
		}
		return stack;
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
			dialog.body(MDialogBodyItem.of(named(Material.PAPER, "<aqua><< Previous Page"))
				.description(Txt.parse("<aqua><< Previous Page"))
				.clickId("nav_prev")
				.onClick((p, response) -> prev.accept(p)));
		}
		if (page < totalPages - 1)
		{
			dialog.body(MDialogBodyItem.of(named(Material.PAPER, "<aqua>Next Page >>"))
				.description(Txt.parse("<aqua>Next Page >>"))
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
			meta.setDisplayName(Txt.parse(name == null ? "" : name));
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
	
	@FunctionalInterface
	private interface FillSelectHandler
	{
		void accept(Player player, GateType type, int particleAmount);
	}
	
	private static final class Session
	{
		private final GateOrientation orientation;
		private final int initialParticleAmount;
		private final FillSelectHandler onSelect;
		private final Consumer<Player> onCancel;
		private final Predicate<Player> stillValid;
		private final String cancelTooltip;
		
		private Session(GateOrientation orientation, int initialParticleAmount, FillSelectHandler onSelect,
			Consumer<Player> onCancel, Predicate<Player> stillValid, String cancelTooltip)
		{
			this.orientation = orientation;
			this.initialParticleAmount = initialParticleAmount;
			this.onSelect = onSelect;
			this.onCancel = onCancel;
			this.stillValid = stillValid;
			this.cancelTooltip = cancelTooltip;
		}
	}
}
