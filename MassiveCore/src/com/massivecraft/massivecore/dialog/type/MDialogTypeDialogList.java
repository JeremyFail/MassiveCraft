package com.massivecraft.massivecore.dialog.type;

import com.massivecraft.massivecore.dialog.MDialogButton;
import com.massivecraft.massivecore.dialog.MDialogSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dialog-list type: buttons that open nested dialogs.
 * <p>
 * Nested {@link MDialogSpec} entries contribute buttons to the same {@link com.massivecraft.massivecore.dialog.MDialogSession}
 * via {@link com.massivecraft.massivecore.dialog.MDialogButtons#collect}.
 * </p>
 */
public final class MDialogTypeDialogList implements MDialogType
{
	/** Child dialogs shown as list entries. */
	private final List<MDialogSpec> dialogs;
	
	/** Optional back / close button. */
	private final MDialogButton exitAction;
	
	/** Column count for list buttons. */
	private final int columns;
	
	/** Optional uniform button width. */
	private final Integer buttonWidth;
	
	/**
	 * @param dialogs Nested specs.
	 * @param exitAction Exit button or null.
	 * @param columns List columns (at least 1).
	 * @param buttonWidth Button width hint or null.
	 */
	private MDialogTypeDialogList(List<MDialogSpec> dialogs, MDialogButton exitAction, int columns, Integer buttonWidth)
	{
		this.dialogs = Collections.unmodifiableList(new ArrayList<>(dialogs));
		this.exitAction = exitAction;
		this.columns = columns;
		this.buttonWidth = buttonWidth;
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
	 * @return Unmodifiable nested dialog specs.
	 */
	public List<MDialogSpec> getDialogs() { return this.dialogs; }
	
	/**
	 * @return Exit button or null.
	 */
	public MDialogButton getExitAction() { return this.exitAction; }
	
	/**
	 * @return List column count.
	 */
	public int getColumns() { return this.columns; }
	
	/**
	 * @return Shared button width hint or null.
	 */
	public Integer getButtonWidth() { return this.buttonWidth; }
	
	/**
	 * Fluent builder for {@link MDialogTypeDialogList}.
	 */
	public static final class Builder
	{
		/** Nested specs in list order. */
		private final List<MDialogSpec> dialogs = new ArrayList<>();
		
		/** Optional exit button. */
		private MDialogButton exitAction;
		
		/** Column count; default 2. */
		private int columns = 2;
		
		/** Optional button width. */
		private Integer buttonWidth;
		
		/**
		 * Adds a nested dialog spec to the list.
		 *
		 * @param dialog Child spec (title becomes button label on some backends).
		 * @return This builder.
		 */
		public Builder dialog(MDialogSpec dialog)
		{
			this.dialogs.add(dialog);
			return this;
		}
		
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
		 * Sets list column count (minimum 1).
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
		 * Sets a uniform width for list buttons.
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
		 * @return Dialog-list type.
		 */
		public MDialogTypeDialogList build()
		{
			return new MDialogTypeDialogList(this.dialogs, this.exitAction, this.columns, this.buttonWidth);
		}
	}
}
