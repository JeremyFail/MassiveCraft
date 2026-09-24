package com.massivecraft.massivecore.dialog.type;

import com.massivecraft.massivecore.dialog.MDialogButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Multi-action dialog: grid of action buttons plus optional exit.
 */
public final class MDialogTypeMultiAction implements MDialogType
{
	/** Primary action buttons in order. */
	private final List<MDialogButton> actions;
	
	/** Optional cancel / back button. */
	private final MDialogButton exitAction;
	
	/** Column count for the action grid. */
	private final int columns;
	
	/**
	 * @param actions Action buttons.
	 * @param exitAction Exit button or null.
	 * @param columns Grid columns (at least 1).
	 */
	private MDialogTypeMultiAction(List<MDialogButton> actions, MDialogButton exitAction, int columns)
	{
		this.actions = Collections.unmodifiableList(new ArrayList<>(actions));
		this.exitAction = exitAction;
		this.columns = columns;
	}
	
	/**
	 * Starts a fluent builder.
	 *
	 * @return New builder with default two columns.
	 */
	public static Builder builder()
	{
		return new Builder();
	}
	
	/**
	 * @return Unmodifiable action buttons.
	 */
	public List<MDialogButton> getActions() { return this.actions; }
	
	/**
	 * @return Exit button or null.
	 */
	public MDialogButton getExitAction() { return this.exitAction; }
	
	/**
	 * @return Grid column count.
	 */
	public int getColumns() { return this.columns; }
	
	/**
	 * Fluent builder for {@link MDialogTypeMultiAction}.
	 */
	public static final class Builder
	{
		/** Action buttons added in order. */
		private final List<MDialogButton> actions = new ArrayList<>();
		
		/** Optional exit button. */
		private MDialogButton exitAction;
		
		/** Column count; default 2. */
		private int columns = 2;
		
		/**
		 * Adds an action button to the grid.
		 *
		 * @param button Button to add.
		 * @return This builder.
		 */
		public Builder action(MDialogButton button)
		{
			this.actions.add(button);
			return this;
		}
		
		/**
		 * Sets the optional exit / cancel button.
		 *
		 * @param exitAction Exit button or null.
		 * @return This builder.
		 */
		public Builder exit(MDialogButton exitAction)
		{
			this.exitAction = exitAction;
			return this;
		}
		
		/**
		 * Sets grid column count (minimum 1).
		 *
		 * @param columns Desired columns.
		 * @return This builder.
		 */
		public Builder columns(int columns)
		{
			this.columns = Math.max(1, columns);
			return this;
		}
		
		/**
		 * Builds the immutable type.
		 *
		 * @return Multi-action dialog type.
		 */
		public MDialogTypeMultiAction build()
		{
			return new MDialogTypeMultiAction(this.actions, this.exitAction, this.columns);
		}
	}
}
