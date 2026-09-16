package com.massivecraft.massivecore.dialog.type;

import com.massivecraft.massivecore.dialog.MDialogButton;

/**
 * Server-links dialog (uses the server's configured {@link org.bukkit.ServerLinks}).
 * <p>
 * Renders link entries from the server; optional exit button and layout hints mirror multi-action types.
 * </p>
 */
public final class MDialogTypeServerLinks implements MDialogType
{
	/** Optional close / back button. */
	private final MDialogButton exitAction;
	
	/** Column count for link buttons. */
	private final int columns;
	
	/** Optional uniform button width. */
	private final Integer buttonWidth;
	
	/**
	 * @param exitAction Exit button or null.
	 * @param columns Column count (at least 1).
	 * @param buttonWidth Width hint or null.
	 */
	private MDialogTypeServerLinks(MDialogButton exitAction, int columns, Integer buttonWidth)
	{
		this.exitAction = exitAction;
		this.columns = columns;
		this.buttonWidth = buttonWidth;
	}
	
	/**
	 * Starts a fluent builder.
	 *
	 * @return New builder defaulting to one column.
	 */
	public static Builder builder()
	{
		return new Builder();
	}
	
	/**
	 * @return Exit button or null.
	 */
	public MDialogButton getExitAction() { return this.exitAction; }
	
	/**
	 * @return Link grid column count.
	 */
	public int getColumns() { return this.columns; }
	
	/**
	 * @return Button width hint or null.
	 */
	public Integer getButtonWidth() { return this.buttonWidth; }
	
	/**
	 * Fluent builder for {@link MDialogTypeServerLinks}.
	 */
	public static final class Builder
	{
		/** Optional exit button. */
		private MDialogButton exitAction;
		
		/** Column count; default 1 for link lists. */
		private int columns = 1;
		
		/** Optional button width. */
		private Integer buttonWidth;
		
		/**
		 * Sets the optional exit button.
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
		 * Sets column count (minimum 1).
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
		 * Sets uniform button width for link rows.
		 *
		 * @param buttonWidth Width hint.
		 * @return This builder.
		 */
		public Builder buttonWidth(int buttonWidth)
		{
			this.buttonWidth = buttonWidth;
			return this;
		}
		
		/**
		 * Builds the immutable type.
		 *
		 * @return Server-links dialog type.
		 */
		public MDialogTypeServerLinks build()
		{
			return new MDialogTypeServerLinks(this.exitAction, this.columns, this.buttonWidth);
		}
	}
}
