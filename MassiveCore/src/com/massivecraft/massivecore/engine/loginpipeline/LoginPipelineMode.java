package com.massivecraft.massivecore.engine.loginpipeline;

/**
 * Which login pipeline listener set {@link LoginPipeline} registered at enable.
 */
public enum LoginPipelineMode
{
	/** Pure Spigot (or fork without Paper APIs). Uses {@code PlayerLoginEvent} via {@link LoginPipelineSpigotListener}. */
	SPIGOT,
	
	/**
	 * Paper 1.21.4–1.21.6 soft-support: {@code PlayerConnectionValidateLoginEvent} is unavailable, so we fall back to
	 * {@link LoginPipelineSpigotListener}. Not actively tested; may show deprecation warnings on Paper 1.21.6+.
	 */
	PAPER_LEGACY,
	
	/** Paper 1.21.7+: modern connection pipeline via {@link LoginPipelinePaperListener}. */
	PAPER_MODERN,
	
	// END OF LIST
	;
	
}
