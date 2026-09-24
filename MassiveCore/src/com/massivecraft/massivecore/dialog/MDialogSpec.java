package com.massivecraft.massivecore.dialog;

import com.massivecraft.massivecore.dialog.body.MDialogBody;
import com.massivecraft.massivecore.dialog.input.MDialogInput;
import com.massivecraft.massivecore.dialog.type.MDialogType;
import com.massivecraft.massivecore.dialog.type.MDialogTypeNotice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable dialog definition shown via {@link MDialog#open}.
 * <p>
 * Built by {@link MDialogBuilder}; backends read fields to render UI and
 * {@link com.massivecraft.massivecore.engine.EngineMassiveCoreDialog} uses {@link #getCloseHandler()} for dismiss routing.
 * </p>
 */
public final class MDialogSpec
{
	/** In-dialog title (may differ from {@link #externalTitle}). */
	private final String title;
	
	/** Title shown outside the dialog chrome when supported. */
	private final String externalTitle;
	
	/** Whether the client may dismiss with Escape. */
	private final boolean canCloseWithEscape;
	
	/** Whether the dialog pauses the game (single-player style). */
	private final boolean pause;
	
	/** Client behavior after a button click. */
	private final MDialogAfterAction afterAction;
	
	/** Body lines / items shown above inputs. */
	private final List<MDialogBody> bodies;
	
	/** Input widgets whose values appear in {@link MDialogResponse}. */
	private final List<MDialogInput> inputs;
	
	/** Bottom button layout (notice, confirmation, etc.). */
	private final MDialogType type;
	
	/** Invoked when the dialog closes without a successful button action. */
	private final MDialogCloseHandler closeHandler;
	
	/**
	 * Package-private constructor used by {@link MDialogBuilder#build()}.
	 *
	 * @param title Dialog title.
	 * @param externalTitle External title or null.
	 * @param canCloseWithEscape Escape key closes when true.
	 * @param pause Pause flag.
	 * @param afterAction Post-click action; defaults to {@link MDialogAfterAction#CLOSE} when null.
	 * @param bodies Body elements; copied defensively.
	 * @param inputs Input fields; copied defensively.
	 * @param type Dialog type; defaults to {@link MDialogTypeNotice#of()} when null.
	 * @param closeHandler Close callback or null.
	 */
	MDialogSpec(String title, String externalTitle, boolean canCloseWithEscape, boolean pause, MDialogAfterAction afterAction, List<MDialogBody> bodies, List<MDialogInput> inputs, MDialogType type, MDialogCloseHandler closeHandler)
	{
		this.title = title;
		this.externalTitle = externalTitle;
		this.canCloseWithEscape = canCloseWithEscape;
		this.pause = pause;
		// Mirror vanilla default when builder omits afterAction.
		this.afterAction = afterAction == null ? MDialogAfterAction.CLOSE : afterAction;
		this.bodies = Collections.unmodifiableList(new ArrayList<>(bodies));
		this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
		// Empty type becomes a simple OK notice.
		this.type = type == null ? MDialogTypeNotice.of() : type;
		this.closeHandler = closeHandler;
	}
	
	/**
	 * @return In-dialog title.
	 */
	public String getTitle() { return this.title; }
	
	/**
	 * @return External title, or null if unset.
	 */
	public String getExternalTitle() { return this.externalTitle; }
	
	/**
	 * @return True if Escape may close the dialog.
	 */
	public boolean canCloseWithEscape() { return this.canCloseWithEscape; }
	
	/**
	 * @return True if the dialog should pause the game.
	 */
	public boolean isPause() { return this.pause; }
	
	/**
	 * @return Post-click client action.
	 */
	public MDialogAfterAction getAfterAction() { return this.afterAction; }
	
	/**
	 * @return Unmodifiable body elements.
	 */
	public List<MDialogBody> getBodies() { return this.bodies; }
	
	/**
	 * @return Unmodifiable input definitions.
	 */
	public List<MDialogInput> getInputs() { return this.inputs; }
	
	/**
	 * @return Button / layout type for the dialog footer.
	 */
	public MDialogType getType() { return this.type; }
	
	/**
	 * @return Handler for non-button closes, or null.
	 */
	public MDialogCloseHandler getCloseHandler() { return this.closeHandler; }
}
