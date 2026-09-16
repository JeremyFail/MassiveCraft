package com.massivecraft.massivecore.chestgui;

import com.massivecraft.massivecore.chestgui.backend.ChestGuiBackend;
import com.massivecraft.massivecore.chestgui.backend.SpigotChestGuiBackend;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ChestGui implements InventoryHolder
{
	// -------------------------------------------- //
	// BACKEND
	// -------------------------------------------- //
	
	private static final String PAPER_BACKEND = "com.massivecraft.massivecore.chestgui.backend.PaperChestGuiBackend";
	
	private static volatile ChestGuiBackend cachedBackend;
	
	public static ChestGuiBackend resolveBackend()
	{
		ChestGuiBackend cached = cachedBackend;
		if (cached != null) return cached;
		
		synchronized (ChestGui.class)
		{
			if (cachedBackend != null) return cachedBackend;
			
			ChestGuiBackend paper = tryLoad(PAPER_BACKEND);
			if (paper != null)
			{
				cachedBackend = paper;
				return cachedBackend;
			}
			
			cachedBackend = new SpigotChestGuiBackend();
			return cachedBackend;
		}
	}
	
	private static ChestGuiBackend tryLoad(String className)
	{
		try
		{
			Class<?> clazz = Class.forName(className);
			Object instance = clazz.getDeclaredConstructor().newInstance();
			if (!(instance instanceof ChestGuiBackend)) return null;
			ChestGuiBackend backend = (ChestGuiBackend) instance;
			if (backend instanceof ChestGuiBackend.CapabilityProbe && !((ChestGuiBackend.CapabilityProbe) backend).isAvailable())
			{
				return null;
			}
			return backend;
		}
		catch (Throwable t)
		{
			Bukkit.getLogger().log(Level.FINE, "MassiveCore chest GUI backend unavailable: " + className, t);
			return null;
		}
	}
	
	// -------------------------------------------- //
	// REGISTRY
	// -------------------------------------------- //
	
	protected static final Map<Inventory, ChestGui> inventoryToGui = new MassiveMap<>();
	public static Map<Inventory, ChestGui> getInventoryToGui() { return inventoryToGui; }
	
	public static ChestGui get(Inventory inventory)
	{
		if (inventory == null) return null;
		ChestGui gui = inventoryToGui.get(inventory);
		if (gui != null) return gui;
		
		InventoryHolder holder = inventory.getHolder();
		if (holder instanceof ChestGui)
		{
			ChestGui held = (ChestGui) holder;
			if (held.getInventory() == inventory) return held;
		}
		return null;
	}
	
	public static ChestGui getCreative(Inventory inventory)
	{
		if (inventory == null) throw new NullPointerException("inventory");
		
		ChestGui gui = get(inventory);
		if (gui != null) return gui;
		
		gui = new ChestGui();
		gui.attachInventory(inventory);
		
		return gui;
	}
	
	public static ChestGui get(InventoryEvent event)
	{
		if (event == null) throw new NullPointerException("event");

		Inventory inventory = event.getInventory();
		if (inventory == null) return null;
		return get(inventory);
	}
	
	private static void add(Inventory inventory, ChestGui gui) { inventoryToGui.put(inventory, gui); }
	private static void remove(Inventory inventory) { inventoryToGui.remove(inventory); }
	
	// -------------------------------------------- //
	// ADD & REMOVE
	// -------------------------------------------- //
	// Done through instance for override possibilities.
	
	public void add()
	{
		if (this.inventory == null) return;
		add(this.inventory, this);
	}
	
	public void remove()
	{
		if (this.inventory == null) return;
		remove(this.inventory);
	}
	
	// -------------------------------------------- //
	// INVENTORY
	// -------------------------------------------- //
	// It is useful to provide a link back from the GUI to the inventory.
	// This way we have can look up between Inventory and ChestGui both ways.
	
	private Inventory inventory = null;
	@Override
	public Inventory getInventory() { return this.inventory; }
	public void setInventory(Inventory inventory)
	{
		if (this.inventory != null && this.inventory != inventory)
		{
			remove(this.inventory);
		}
		this.inventory = inventory;
		if (inventory != null) this.add();
	}
	
	public void attachInventory(Inventory inventory)
	{
		this.setInventory(inventory);
		this.applyPendingItems();
		this.applyFiller();
	}
	
	public Inventory ensureInventory()
	{
		if (this.inventory == null) resolveBackend().ensureInventory(this);
		return this.inventory;
	}
	
	public void open(Player player)
	{
		if (player == null) return;
		resolveBackend().open(player, this);
	}
	
	// -------------------------------------------- //
	// LAYOUT & TITLE
	// -------------------------------------------- //
	
	private ChestGuiLayout layout = null;
	public ChestGuiLayout getLayout() { return this.layout; }
	public void setLayout(ChestGuiLayout layout) { this.layout = layout; }
	
	private String title = "";
	public String getTitle() { return this.title; }
	public void setTitle(String title) { this.title = title == null ? "" : title; }
	
	private final Map<Integer, ItemStack> pendingItems = new MassiveMap<>();
	
	private ItemStack filler = null;
	public ItemStack getFiller() { return this.filler; }
	public ChestGui setFiller(ItemStack filler)
	{
		this.filler = filler == null ? null : filler.clone();
		this.applyFiller();
		return this;
	}
	
	// -------------------------------------------- //
	// ACTIONS
	// -------------------------------------------- //
	// Actions are assigned to indexes in the inventory.
	// This means the system does not care about what item is in the slot.
	// It just cares about which slot it is.
	// One could have imagined an approach where we looked at the item instead.
	// That is however not feasible since the Bukkit ItemStack equals method is not reliable.
	
	private Map<Integer, ChestAction> indexToAction = new MassiveMap<>();
	public Map<Integer, ChestAction> getIndexToAction() { return this.indexToAction; }
	public ChestAction setAction(int index, ChestAction action) { return this.indexToAction.put(index, action); }
	public ChestAction setAction(int index, String command) { return this.setAction(index, new ChestActionCommand(command)); }
	public ChestAction getAction(int index) { return this.indexToAction.get(index); }
	public ChestAction getAction(InventoryClickEvent event)
	{
		if (!InventoryUtil.isTopInventory(event)) return null;
		return this.getAction(event.getRawSlot());
	}
	
	public ChestGui setItem(int slot, ItemStack item)
	{
		ItemStack clone = item == null ? null : item.clone();
		if (this.inventory != null)
		{
			this.inventory.setItem(slot, clone);
		}
		else
		{
			if (clone == null) this.pendingItems.remove(slot);
			else this.pendingItems.put(slot, clone);
		}
		return this;
	}
	
	public ChestGui setItem(int slot, ItemStack item, ChestAction action)
	{
		this.setItem(slot, item);
		this.setAction(slot, action);
		return this;
	}
	
	public ChestGui setItem(int row, int column, ItemStack item, ChestAction action)
	{
		return this.setItem(ChestGuiLayout.slot(row, column), item, action);
	}
	
	private void applyPendingItems()
	{
		if (this.inventory == null || this.pendingItems.isEmpty()) return;
		for (Map.Entry<Integer, ItemStack> entry : this.pendingItems.entrySet())
		{
			this.inventory.setItem(entry.getKey(), entry.getValue());
		}
		this.pendingItems.clear();
	}
	
	private void applyFiller()
	{
		if (this.inventory == null || this.filler == null) return;
		int size = this.inventory.getSize();
		for (int i = 0; i < size; i++)
		{
			if (InventoryUtil.isNothing(this.inventory.getItem(i)))
			{
				this.inventory.setItem(i, this.filler.clone());
			}
		}
	}
	
	// -------------------------------------------- //
	// LAST ACTION
	// -------------------------------------------- //
	// The last consumed action is stored here.
	// This can for example be useful in the inventory close task.
	
	private ChestAction lastAction = null;
	public ChestAction getLastAction() { return this.lastAction; }
	public void setLastAction(ChestAction lastAction) { this.lastAction = lastAction; }
	
	// -------------------------------------------- //
	// META DATA
	// -------------------------------------------- //
	// Store your arbitrary stuff here. Might come in handy in the future.
	// I don't think we are currently using this ourselves.
	
	private final Map<String, Object> meta = new MassiveMap<>();
	public Map<String, Object> getMeta() { return this.meta; }
	
	// -------------------------------------------- //
	// RUNNABLES
	// -------------------------------------------- //
	// Runnables to be executed after certain events.
	// They are all delayed with one (or is it zero) ticks.
	// This way we don't bug out if you open a new GUI after close.
	
	private final List<Runnable> runnablesOpen = new MassiveList<>();
	public List<Runnable> getRunnablesOpen() { return this.runnablesOpen; }
	
	private final List<Runnable> runnablesClose = new MassiveList<>();
	public List<Runnable> getRunnablesClose() { return this.runnablesClose; }
	
	// -------------------------------------------- //
	// AUTOCLOSING
	// -------------------------------------------- //
	// Close next tick after a consumed action click (ChestAction.onClick returned true).
	// The action may toggle this flag during onClick. Close is skipped if the player
	// already has a different top inventory.
	
	private boolean autoclosing = true;
	public boolean isAutoclosing() { return this.autoclosing; }
	public void setAutoclosing(boolean autoclosing) { this.autoclosing = autoclosing; }
	
	// -------------------------------------------- //
	// AUTOREMOVING
	// -------------------------------------------- //
	// Should the GUI be automatically removed upon the inventory closing?
	
	private boolean autoremoving = true;
	public boolean isAutoremoving() { return this.autoremoving; }
	public void setAutoremoving(boolean autoremoving) { this.autoremoving = autoremoving; }
	
	// -------------------------------------------- //
	// ALLOWBOTTOMINVENTORY
	// -------------------------------------------- //
	// When true, the player may rearrange their own inventory.
	// Transfers into or out of the GUI (shift-click, collect-to-cursor, drag onto top slots) stay blocked.
	
	private boolean allowBottomInventory = false;
	public boolean isBottomInventoryAllowed() { return this.allowBottomInventory; }
	public void setBottomInventoryAllow(boolean allowBottomInventory) { this.allowBottomInventory = allowBottomInventory; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public ChestGui()
	{
		
	}

	public static ChestGui create(int size, String title)
	{
		return create(ChestGuiLayout.chest(size), title);
	}
	
	public static ChestGui create(ChestGuiLayout layout, String title)
	{
		if (layout == null) throw new NullPointerException("layout");
		ChestGui gui = new ChestGui();
		gui.setLayout(layout);
		gui.setTitle(title);
		return gui;
	}

	public static ChestGui constructFromButtons(List<? extends ChestButton> buttons, String title)
	{
		if (buttons == null) throw new NullPointerException("buttons");
		
		ChestGui gui = create(buttons.size(), title);
		int cap = gui.getLayout().getSize();
		for (int i = 0; i < buttons.size() && i < cap; i++)
		{
			ChestButton button = buttons.get(i);
			if (button == null) continue;
			gui.setItem(i, button.getItem(), button.getAction());
		}
		
		return gui;
	}
	
}
