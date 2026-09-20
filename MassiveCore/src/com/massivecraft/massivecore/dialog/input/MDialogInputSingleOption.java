package com.massivecraft.massivecore.dialog.input;

import com.massivecraft.massivecore.dialog.MDialogText;
import com.massivecraft.massivecore.mson.Mson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Single-option (cycling) dialog input. Selected option id is returned as text.
 * <p>
 * Build with {@link #builder(String, String)} or {@link #builder(String, MDialogText)};
 * selected {@link MDialogInputOption#getId()} is stored under {@link #getKey()} in
 * {@link com.massivecraft.massivecore.dialog.MDialogResponse}.
 * </p>
 */
public final class MDialogInputSingleOption implements MDialogInput
{
	/** Response key. */
	private final String key;
	
	/** Field label. */
	private final MDialogText label;
	
	/** Optional width hint. */
	private final Integer width;
	
	/** Whether the label is shown. */
	private final boolean labelVisible;
	
	/** Choices in display order. */
	private final List<MDialogInputOption> options;
	
	private MDialogInputSingleOption(String key, MDialogText label, Integer width, boolean labelVisible, List<MDialogInputOption> options)
	{
		this.key = key;
		this.label = label == null ? MDialogText.txt("") : label;
		this.width = width;
		this.labelVisible = labelVisible;
		this.options = Collections.unmodifiableList(new ArrayList<>(options));
	}
	
	/**
	 * Starts a fluent builder for a single-option input.
	 *
	 * @param key Response key.
	 * @param label Field label (Txt tags allowed).
	 * @return New builder.
	 */
	public static Builder builder(String key, String label)
	{
		return new Builder(key, MDialogText.txt(label));
	}
	
	/**
	 * Starts a fluent builder with rich label text.
	 *
	 * @param key Response key.
	 * @param label Field label.
	 * @return New builder.
	 */
	public static Builder builder(String key, MDialogText label)
	{
		return new Builder(key, label);
	}
	
	/**
	 * Starts a fluent builder with an {@link Mson} label.
	 *
	 * @param key Response key.
	 * @param label Field label.
	 * @return New builder.
	 */
	public static Builder builder(String key, Mson label)
	{
		return new Builder(key, MDialogText.mson(label));
	}
	
	@Override
	public String getKey() { return this.key; }
	
	/**
	 * @return Rich label text.
	 */
	public MDialogText getLabelText() { return this.label; }
	
	/**
	 * Plain / legacy label for ChestGui and string callers.
	 *
	 * @return Styled plain label.
	 */
	public String getLabel() { return this.label.toPlain(); }
	
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
		private final String key;
		private final MDialogText label;
		private Integer width;
		private boolean labelVisible = true;
		private final List<MDialogInputOption> options = new ArrayList<>();
		
		private Builder(String key, MDialogText label)
		{
			this.key = key;
			this.label = label;
		}
		
		/**
		 * Sets layout width (same width on every row aligns the cycling controls).
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
		 * Sets whether the label is visible / incorporated into the button.
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
		 * @param display Display text (Txt tags allowed).
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
		 * @param display Display text (Txt tags allowed).
		 * @param initial True to mark as default selection.
		 * @return This builder.
		 */
		public Builder option(String id, String display, boolean initial)
		{
			this.options.add(MDialogInputOption.of(id, display).initial(initial));
			return this;
		}
		
		/**
		 * Adds an option with rich display text.
		 *
		 * @param id Option id.
		 * @param display Display text.
		 * @param initial True to mark as default selection.
		 * @return This builder.
		 */
		public Builder option(String id, MDialogText display, boolean initial)
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
