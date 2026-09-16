# ChestGui

MassiveCore wrapper for **virtual chest menus**: an `Inventory` whose slots are buttons, not real storage.

`ChestGui` is the single entry point. MassiveCore picks a backend at runtime, same idea as `MDialog`:

1. **Paper** (`PaperChestGuiBackend`) — Adventure `Component` titles and, when the inventory is created at `open(player)`, Paper `MenuType` views (`GENERIC_9xN`, hopper, 3×3).
2. **Spigot** (`SpigotChestGuiBackend`) — `Bukkit.createInventory` with parsed string titles.

The click engine (`EngineMassiveCoreChestGui`) is shared. You do not register listeners.

## Classes

| Class | Role |
| --- | --- |
| `ChestGui` | Entry point, registry, per-slot actions, open/close options. Implements `InventoryHolder`. |
| `ChestGuiLayout` | Chest 1–6 rows, hopper (5), dropper (3×3). |
| `ChestAction` | `boolean onClick(InventoryClickEvent)`. `true` = consumed (`lastAction` + autoclose). `false` = ignore those; the click stays cancelled either way. |
| `ChestActionAbstract` | Resolves the clicker to a `Player` and no-ops for non-players. |
| `ChestActionCommand` | Runs a command line (must start with `/`) as the clicking player. |
| `ChestButton` / `ChestButtonSimple` | Pair of `ItemStack` + `ChestAction` for `constructFromButtons`. |
| `backend.PaperChestGuiBackend` | Paper titles + `MenuType`. Loaded via reflection; never imported from Spigot-only code. |
| `backend.SpigotChestGuiBackend` | Fallback inventory factory. |
| `EngineMassiveCoreChestGui` | Click / drag / hopper / open / close / quit. Lives in `com.massivecraft.massivecore.engine`. |

## Preferred usage

Create through `ChestGui`, set items on the GUI, then `open(player)`. That lets Paper use `MenuType` (player-bound view). Calling `getInventory()` or `ensureInventory()` before open forces a shared `Bukkit.createInventory` instead.

```java
ChestGui gui = ChestGui.create(27, "<h>Example");
gui.setBottomInventoryAllow(false);
gui.setAutoclosing(true);

gui.setItem(13, icon, (InventoryClickEvent event) -> {
    // already cancelled; return true to consume (autoclose / lastAction)
    return true;
});

gui.open(player);
```

Hopper or 3×3:

```java
ChestGui gui = ChestGui.create(ChestGuiLayout.HOPPER, "<h>Pick one");
gui.setItem(2, icon, action);
gui.open(player);
```

Or build from a list (size rounded up to a chest row count, **capped at 54**; extra buttons are omitted). Does not create the inventory until `open(player)`, so Paper can still use `MenuType`:

```java
ChestGui gui = ChestGui.constructFromButtons(buttons, "Title");
gui.open(player);
```

`getCreative(inventory)` still wraps an inventory you created yourself. Prefer `create` + `open` so the backend can own title and type.

Lookups are **by `Inventory` identity** (and `InventoryHolder` when the backend set the GUI as holder). Actions are **by slot index**, not by `ItemStack`.

## Options

| Option | Default | Meaning |
| --- | --- | --- |
| `autoclosing` | `true` | Close **next tick** after a **consumed** action click (`onClick` returned `true`). The action may toggle this flag during `onClick`. Empty slots and `false` returns do not close. Close is skipped if the player already has a different top inventory. |
| `autoremoving` | `true` | Drop the registry entry 20 ticks after close. |
| `allowBottomInventory` | `false` | If false, clicks/drags in the player's own inventory are cancelled with “exit the GUI to edit your items.” If true, they may **rearrange their own inventory only**. Shift-click, collect-to-cursor, drag onto GUI slots, and other transfers into/out of the GUI stay blocked (with “you can't move items into this menu”). |
| `runnablesOpen` / `runnablesClose` | empty | Run **next tick** after open/close. Close is the usual place to detect "clicked nothing and backed out". |
| `lastAction` | `null` | Last **consumed** action. Useful from a close runnable. |
| `meta` | empty map | Arbitrary extras. MassiveCore itself does not use this. |
| `filler` | `null` | Optional item cloned into empty slots when the inventory is attached. |

## Click handling (engine)

Virtual GUI items are never given, taken, dragged, hoppered, or creative-middle-clicked out.

On `InventoryClickEvent` (priority `LOW`, then `HIGHEST` / `MONITOR` re-deny):

1. Ignore inventories that are not registered.
2. Cancel the click (`setCancelled(true)` + `Result.DENY`).
3. Bottom (player) inventory: allow only if `allowBottomInventory` and the click would not give/take relative to the GUI; otherwise keep cancelled and warn.
4. Top slot with a `ChestAction`: run `onClick`. If it returns `true`, store `lastAction` and autoclose **next tick** if still looking at this GUI.

`InventoryDragEvent` is cancelled unless it is entirely in the bottom inventory and bottom editing is allowed. `InventoryMoveItemEvent` (hoppers) is cancelled if source or destination is a registered GUI. `ChestAction.onClick` returning `false` does **not** un-cancel the click; it only skips lastAction and autoclose.

## In-tree example

CreativeGates gate fill selection goes through `MDialog`, which uses ChestGui automatically when Paper/Spigot dialogs are unavailable (`ChestGuiMDialogBackend`).

## What this is not

ChestGui is still a **virtual icon menu**, not a replacement for Paper Dialogs (forms, text, confirmations) or for real-item editors (`CommandEditItemStacksOpen`). It does not paginate, animate, or wrap anvil/smithing/enchanting screens — those have their own mechanics. Use `MDialog` for forms; use ChestGui when you need clickable items on every supported server.
