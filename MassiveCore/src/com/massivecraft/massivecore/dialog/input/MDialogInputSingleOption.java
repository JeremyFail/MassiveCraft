package com.massivecraft.massivecore.dialog.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Single-option (dropdown / cycling) dialog input. Selected option id is returned as text.
 * <p>
 * Build with {@link #builder(String, String)}; selected {@link MDialogInputOption#getId()}
 * is stored under {@link #getKey()} in {@link com.massivecraft.massivecore.dialog.MDialogResponse}.
 * </p>
 */
public final class MDialogInputSingleOption implements MDialogInput
{
	/** Response key. */
	private final String key;
	
	/** Field label. */
	private final String label;
	
	/** Optional width hint. */
	private final Integer width;
	
	/** Whether the label is shown. */
	private final boolean labelVisible;
	
	/** Choices in display order. */
	private final List<MDialogInputOption> options;
	
	/**
	 * @param key Input key.
	 * @param label Label text.
	 * @param width Width hint or null.
	 * @param labelVisible Label visibility.
	 * @param options Option list; copied defensively.
	 */
	private MDialogInputSingleOption(String key, String label, Integer width, boolean labelVisible, List<MDialogInputOption> options)
	{
		this.key = key;
		this.label = label;
		this.width = width;
		this.labelVisible = labelVisible;
		this.options = Collections.unmodifiableList(new ArrayList<>(options));
	}
	
	/**
	 * Starts a fluent builder for a single-option input.
	 *
	 * @param key Response key.
	 * @param label Field label.
	 * @return New builder.
	 */
	public static Builder builder(String key, String label)
	{
		return new Builder(key, label);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getKey() { return this.key; }
	
	/**
	 * @return Label text.
	 */
	public String getLabel() { return this.label; }
	
	/**
	 * @return Width hint or null.
	 */
	public Integer getWidth() { return this.width; }
	
	/**
	 * @return True if the label is shown.
	 */
	public boolean isLabelVisible() { return this.labelVisible; }
	
	/**
	 * @return Unmodifiable option list.
	 */
	public List<MDialogInputOption> getOptions() { return this.options; }
	
	/**
	 * Fluent builder for {@link MDialogInputSingleOption}.
	 */
	public static final class Builder
	{
		/** Target key. */
		private final String key;
		
		/** Target label. */
		private final String label;
		
		/** Optional width. */
		private Integer width;
		
		/** Label visibility; default true. */
		private boolean labelVisible = true;
		
		/** Accumulated options. */
		private final List<MDialogInputOption> options = new ArrayList<>();
		
		/**
		 * @param key Input key.
		 * @param label Label text.
		 */
		private Builder(String key, String label)
		{
			this.key = key;
			this.label = label;
		}
		
		/**
		 * Sets layout width.
		 *
		 * @param width Width hint.
		 * @return This builder.
		 */
		public Builder width(int width)
		{
			this.width = width;
			return this;
		}
		
		/**
		 * Sets whether the label is visible.
		 *
		 * @param labelVisible Label visibility.
		 * @return This builder.
		 */
		public Builder labelVisible(boolean labelVisible)
		{
			this.labelVisible = labelVisible;
			return this;
		}
		
		/**
		 * Adds a non-initial option.
		 *
		 * @param id Option id.
		 * @param display Display text.
		 * @return This builder.
		 */
		public Builder option(String id, String display)
		{
			this.options.add(MDialogInputOption.of(id, display));
			return this;
		}
		
		/**
		 * Adds an option with explicit initial flag.
		 *
		 * @param id Option id.
		 * @param display Display text.
		 * @param initial True to mark as default selection.
		 * @return This builder.
		 */
		public Builder option(String id, String display, boolean initial)
		{
			this.options.add(MDialogInputOption.of(id, display).initial(initial));
			return this;
		}
		
		/**
		 * Adds a pre-built option.
		 *
		 * @param option Option instance.
		 * @return This builder.
		 */
		public Builder option(MDialogInputOption option)
		{
			this.options.add(option);
			return this;
		}
		
		/**
		 * Builds the immutable input.
		 *
		 * @return New single-option input.
		 */
		public MDialogInputSingleOption build()
		{
			return new MDialogInputSingleOption(this.key, this.label, this.width, this.labelVisible, this.options);
		}
	}
}
