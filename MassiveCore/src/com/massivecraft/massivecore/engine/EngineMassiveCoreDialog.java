package com.massivecraft.massivecore.engine;

import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.dialog.MDialogButton;
import com.massivecraft.massivecore.dialog.MDialogCloseHandler;
import com.massivecraft.massivecore.dialog.MDialogResponse;
import com.massivecraft.massivecore.dialog.MDialogSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Engine that tracks open {@link MDialogSession} instances and routes button clicks / closes.
 * <p>
 * One session is kept per player. Backends call {@link #completeClick} or {@link #completeClose}
 * when the player finishes interacting; this engine invokes the matching handlers and clears state.
 * Sessions are also dropped on quit so handlers are not left dangling.
 * </p>
 */
public class EngineMassiveCoreDialog extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static final EngineMassiveCoreDialog i = new EngineMassiveCoreDialog();
	
	/**
	 * @return Singleton engine instance.
	 */
	public static EngineMassiveCoreDialog get() { return i; }
	
	/**
	 * Active dialog sessions keyed by player UUID.
	 */
	private final Map<UUID, MDialogSession> sessions = new ConcurrentHashMap<>();
	
	// -------------------------------------------- //
	// ACCESS
	// -------------------------------------------- //
	
	/**
	 * Looks up the open session for a player.
	 *
	 * @param player Player to look up; null-safe.
	 * @return Session or null if none / player null.
	 */
	public MDialogSession get(Player player)
	{
		// No player means nothing to look up.
		if (player == null) return null;
		return this.sessions.get(player.getUniqueId());
	}
	
	/**
	 * Looks up the open session by player id.
	 *
	 * @param playerId Player UUID; null-safe.
	 * @return Session or null if none / id null.
	 */
	public MDialogSession get(UUID playerId)
	{
		if (playerId == null) return null;
		return this.sessions.get(playerId);
	}
	
	/**
	 * Registers or replaces the session for its player.
	 *
	 * @param session Session to store; ignored if null.
	 */
	public void put(MDialogSession session)
	{
		if (session == null) return;
		this.sessions.put(session.getPlayerId(), session);
	}
	
	/**
	 * Removes and returns the session for a player.
	 *
	 * @param player Player whose session to remove; null-safe.
	 * @return Removed session, or null if absent.
	 */
	public MDialogSession remove(Player player)
	{
		if (player == null) return null;
		return this.sessions.remove(player.getUniqueId());
	}
	
	/**
	 * Removes and returns the session for a player id.
	 *
	 * @param playerId Player UUID; null-safe.
	 * @return Removed session, or null if absent.
	 */
	public MDialogSession remove(UUID playerId)
	{
		if (playerId == null) return null;
		return this.sessions.remove(playerId);
	}
	
	// -------------------------------------------- //
	// COMPLETION
	// -------------------------------------------- //
	
	/**
	 * Marks the session completed, clears it, and runs the button's click handler.
	 * <p>
	 * Safe to call when there is no session or it was already completed (no-op).
	 * </p>
	 *
	 * @param player Player who clicked.
	 * @param buttonId Id of the {@link MDialogButton} that was activated.
	 */
	public void completeClick(Player player, String buttonId)
	{
		MDialogSession session = this.get(player);
		// Ignore stray clicks after the dialog already finished.
		if (session == null || session.isCompleted()) return;
		
		MDialogButton button = session.getButton(buttonId);
		// Snapshot inputs at click time before clearing the session.
		MDialogResponse response = session.responseFor(buttonId);
		session.setCompleted(true);
		this.remove(player);
		
		if (button != null && button.getClickHandler() != null)
		{
			button.getClickHandler().onClick(player, response);
		}
	}
	
	/**
	 * Marks the session completed via cancel/close (no button), clears it, and runs {@link MDialogCloseHandler}.
	 *
	 * @param player Player who closed the UI without selecting a completing action.
	 */
	public void completeClose(Player player)
	{
		MDialogSession session = this.remove(player);
		// Already finished via a button - do not fire onClose.
		if (session == null || session.isCompleted()) return;
		session.setCompleted(true);
		MDialogCloseHandler closeHandler = session.getSpec().getCloseHandler();
		if (closeHandler != null) closeHandler.onClose(player);
	}
	
	// -------------------------------------------- //
	// LISTENER
	// -------------------------------------------- //
	
	/**
	 * Drops any open dialog session when the player leaves.
	 *
	 * @param event Quit event.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event)
	{
		this.remove(event.getPlayer());
	}
}
