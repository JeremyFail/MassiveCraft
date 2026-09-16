# MDialog

MassiveCore wrapper for **Minecraft dialogs**: notices, confirmations, multi-action pickers, and form-style inputs.

`MDialog` is the single entry point. MassiveCore picks a backend at runtime (Minecraft **1.21.6+** required for native dialogs):

1. **Paper** (`PaperMDialogBackend`) — Paper Dialog API + Adventure.
2. **Spigot** (`SpigotMDialogBackend`) — Bungee Dialog API + custom click events.
3. **ChestGui** (`ChestGuiMDialogBackend`) — inventory fallback on older servers or when dialog classes are missing.

You do not register listeners. Session routing lives in `EngineMassiveCoreDialog`. Backend classes are loaded with `Class.forName` so older servers never link Paper/Spigot dialog types.

Strings accept MassiveCore `Txt` color codes (e.g. `<h>Title`); backends convert them for Adventure / Bungee.

## Classes

| Class | Role |
| --- | --- |
| `MDialog` | Entry point: `builder()`, `open(player, …)`. Resolves and caches the backend. |
| `MDialogBuilder` / `MDialogSpec` | Fluent builder → immutable dialog definition. |
| `MDialogButton` | Action button (`id`, label, tooltip, icon, `onClick`). |
| `MDialogResponse` | Snapshot of button id + input values at click time. |
| `MDialogClickHandler` / `MDialogCloseHandler` | Button click vs cancel/close (no completing button). |
| `MDialogAfterAction` | `CLOSE` / `NONE` / `WAIT_FOR_RESPONSE` (native Dialog APIs). |
| `body.*` | Body content: plain message, item. |
| `input.*` | Inputs: text, bool, number, single-option. |
| `type.*` | Bottom chrome: notice, confirmation, multi-action, dialog-list, server-links. |
| `backend.*` | Paper / Spigot / ChestGui renderers. |
| `EngineMassiveCoreDialog` | Open sessions, click/close completion, quit cleanup. |

## Preferred usage

Build a spec, attach handlers on buttons (and optionally `onClose`), then `MDialog.open`.

### Notice (OK)

```java
MDialog.open(player, MDialog.builder()
	.title("<h>Saved")
	.bodyPlain("Your settings were written.")
	.type(MDialogTypeNotice.of(
		MDialogButton.of("ok", "OK").onClick((p, response) -> {
			// acknowledge
		})
	))
	.build());
```

If you omit `type(...)`, the builder defaults to a simple OK notice.

### Confirmation (yes / no)

```java
MDialog.open(player, MDialog.builder()
	.title("<h>Delete?")
	.bodyPlain("This cannot be undone.")
	.type(MDialogTypeConfirmation.of(
		MDialogButton.of("yes", "Delete").onClick((p, response) -> doDelete(p)),
		MDialogButton.of("no", "Cancel").onClick((p, response) -> { /* noop */ })
	))
	.onClose(p -> { /* Escape / inventory close without a button */ })
	.build());
```

### Multi-action (picker)

```java
MDialogTypeMultiAction.Builder type = MDialogTypeMultiAction.builder()
	.columns(3)
	.exit(MDialogButton.of("cancel", "Cancel")
		.onClick((p, response) -> cancel(p)));

for (Option option : options)
{
	type.action(MDialogButton.of(option.id(), option.label())
		.tooltip(option.tooltip())
		.icon(option.icon())
		.onClick((p, response) -> apply(p, option)));
}

MDialog.open(player, MDialog.builder()
	.title("<h>Choose")
	.bodyPlain("Pick one.")
	.type(type.build())
	.onClose(p -> cancel(p))
	.build());
```

In-tree example: CreativeGates `GateFillPicker`.

### Inputs (form)

Keys on inputs become keys on `MDialogResponse`. Single-option stores the **selected option id** as text.

```java
MDialog.open(player, MDialog.builder()
	.title("<h>Settings")
	.bodyPlain("Edit and confirm.")
	.inputText("name", "Name")
	.inputBool("enabled", "Enabled")
	.inputNumber("scale", "Scale", 0f, 10f)
	.input(MDialogInputSingleOption.builder("mode", "Mode")
		.option(MDialogInputOption.of("fast", "Fast").initial(true))
		.option(MDialogInputOption.of("slow", "Slow"))
		.build())
	.type(MDialogTypeConfirmation.of(
		MDialogButton.of("save", "Save").onClick((p, response) -> {
			String name = response.getText("name");
			Boolean enabled = response.getBoolean("enabled");
			Float scale = response.getNumber("scale");
			String mode = response.getText("mode"); // option id
			save(p, name, enabled, scale, mode);
		}),
		MDialogButton.of("cancel", "Cancel")
	))
	.build());
```

Builder shortcuts: `inputBool`, `inputText`, `inputNumber`, `inputSingleOption` / `input(...)`.

## Builder options

| Option | Default | Meaning |
| --- | --- | --- |
| `title` | `""` | Main dialog title. |
| `externalTitle` | `null` | Label when this dialog appears as a child in a dialog-list. |
| `canCloseWithEscape` | `true` | Escape may dismiss (Dialog backends). |
| `pause` | `false` | Client pause flag (Dialog backends). |
| `afterAction` | `CLOSE` | After a button click: close, stay open, or wait for another dialog. |
| `body` / `bodyPlain` / `bodyItem` | empty | Ordered body elements. |
| `input…` | empty | Ordered input fields. |
| `type` | notice OK | Bottom button layout. |
| `onClose` | `null` | Fired when the UI closes **without** a completing button click. |

## Click vs close

- **Button click** → `MDialogClickHandler.onClick(player, response)`. Session is completed and removed first.
- **Close without a completing button** (Escape, inventory close on ChestGui fallback, etc.) → `MDialogCloseHandler.onClose(player)` if set. A prior button click does **not** also fire `onClose`.
- Quit clears any open session without calling handlers.

One open session is kept per player (`EngineMassiveCoreDialog`).

## Types

| Type | Use when |
| --- | --- |
| `MDialogTypeNotice` | Single acknowledge / OK. |
| `MDialogTypeConfirmation` | Yes / no. |
| `MDialogTypeMultiAction` | Grid of actions + optional exit. |
| `MDialogTypeDialogList` | Nested list of child `MDialogSpec`s. |
| `MDialogTypeServerLinks` | Server-links style chrome (native Dialog). |

## Backends

| Backend | When |
| --- | --- |
| Paper | Minecraft ≥ 1.21.6 (`ReflectionUtil.isAtLeastMinecraft`) **and** Paper Dialog classes present. |
| Spigot | Same version gate **and** Bungee Dialog / `PlayerCustomClickEvent` present. |
| ChestGui | Everything else: older MC, missing APIs, or both dialog backends failed their capability probe. |

You should not import `backend.*` from plugin code. Define an `MDialogSpec` and call `MDialog.open`; the wrapper owns rendering.

ChestGui fallback approximates bodies, inputs (click-to-cycle), and actions as inventory slots. Prefer native Dialog when available; use ChestGui menus directly (`chestgui/README.md`) when you need a custom virtual chest outside this wrapper.

## What this is not

MDialog does not replace raw Paper/Spigot Dialog builders if you need API surface that is not modeled here.

It also **does not** _currently_ paginate large option lists. A multi-action dialog shows every `action(...)` you add in one screen (subject to client/layout limits). If you have dozens of choices (e.g. every material), you must build paging yourself — for example Next/Previous buttons that `MDialog.open` a new spec for the next slice of items, or a dialog-list of category dialogs. MassiveCore will not invent pages for you.

For pure clickable item menus on every server (and when you want full control of chest slots), use `ChestGui` directly — see `chestgui/README.md`.
