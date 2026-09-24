package com.massivecraft.massivecore.dialog;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One open dialog for a player (used by backends for click / close routing).
 * <p>
 * Holds working copies of input values updated as the player edits fields,
 * and a flat map of all {@link MDialogButton} ids reachable from the spec (including nested dialog lists).
 * </p>
 */
public final class MDialogSession
{
	/** Player who owns this session. */
	private final UUID playerId;
	
	/** Spec shown when the session was opened. */
	private final MDialogSpec spec;
	
	/** All buttons indexed by id for O(1) click lookup. */
	private final Map<String, MDialogButton> buttonsById = new HashMap<>();
	
	/** Current text / single-option values keyed by input key. */
	private final Map<String, String> workingTexts = new HashMap<>();
	
	/** Current boolean input values keyed by input key. */
	private final Map<String, Boolean> workingBooleans = new HashMap<>();
	
	/** Current number input values keyed by input key. */
	private final Map<String, Float> workingNumbers = new HashMap<>();
	
	/** Set when the engine finishes click or close handling. */
	private boolean completed;
	
	/**
	 * Creates a session and seeds button / input state from the spec.
	 *
	 * @param playerId Viewer id.
	 * @param spec Dialog being shown.
	 */
	public MDialogSession(UUID playerId, MDialogSpec spec)
	{
		this.playerId = playerId;
		this.spec = spec;
		// Flatten type tree so nested dialog-list buttons are clickable.
		MDialogButtons.collect(spec, this.buttonsById);
		MDialogButtons.seedWorkingValues(spec, this.workingTexts, this.workingBooleans, this.workingNumbers);
	}
	
	/**
	 * @return Player UUID for this session.
	 */
	public UUID getPlayerId() { return this.playerId; }
	
	/**
	 * @return Original dialog spec.
	 */
	public MDialogSpec getSpec() { return this.spec; }
	
	/**
	 * @return True after {@link com.massivecraft.massivecore.engine.EngineMassiveCoreDialog} completes the session.
	 */
	public boolean isCompleted() { return this.completed; }
	
	/**
	 * Marks whether click/close handling has finished.
	 *
	 * @param completed New completed flag.
	 */
	public void setCompleted(boolean completed) { this.completed = completed; }
	
	/**
	 * Looks up a button by exact id.
	 *
	 * @param id Button id.
	 * @return Button or null if unknown.
	 */
	public MDialogButton getButton(String id)
	{
		return this.buttonsById.get(id);
	}
	
	/**
	 * Looks up a button by id, falling back to case-insensitive match.
	 * <p>
	 * Some backends normalize ids differently; this avoids missed handlers.
	 * </p>
	 *
	 * @param id Button id from the client.
	 * @return Matching button or null.
	 */
	public MDialogButton findButtonIgnoreCase(String id)
	{
		if (id == null) return null;
		MDialogButton exact = this.buttonsById.get(id);
		if (exact != null) return exact;
		// Slow path: scan keys when casing differs.
		for (Map.Entry<String, MDialogButton> entry : this.buttonsById.entrySet())
		{
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(id)) return entry.getValue();
		}
		return null;
	}
	
	/**
	 * @return Unmodifiable view of all collected buttons.
	 */
	public Map<String, MDialogButton> getButtonsById()
	{
		return java.util.Collections.unmodifiableMap(this.buttonsById);
	}
	
	/**
	 * @return Live map of text / option values (backends mutate this).
	 */
	public Map<String, String> getWorkingTexts() { return this.workingTexts; }
	
	/**
	 * @return Live map of boolean values (backends mutate this).
	 */
	public Map<String, Boolean> getWorkingBooleans() { return this.workingBooleans; }
	
	/**
	 * @return Live map of number values (backends mutate this).
	 */
	public Map<String, Float> getWorkingNumbers() { return this.workingNumbers; }
	
	/**
	 * Builds a response snapshot for a button click.
	 *
	 * @param buttonId Id of the activated button.
	 * @return Immutable response carrying current working values.
	 */
	public MDialogResponse responseFor(String buttonId)
	{
		return new MDialogResponse(buttonId, this.workingTexts, this.workingBooleans, this.workingNumbers);
	}
}
