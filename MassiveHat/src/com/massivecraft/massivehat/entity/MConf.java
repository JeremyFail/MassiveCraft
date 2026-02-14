package com.massivecraft.massivehat.entity;

import com.massivecraft.massivecore.collections.ExceptionSet;
import com.massivecraft.massivecore.command.editor.annotation.EditorName;
import com.massivecraft.massivecore.store.Entity;
import com.massivecraft.massivecore.util.MUtil;

import java.util.Set;

@SuppressWarnings("CanBeFinal")
@EditorName("config")
public class MConf extends Entity<MConf>
{
	// -------------------------------------------- //
	// META
	// -------------------------------------------- //
	
	protected static transient MConf i;
	public static MConf get() { return i; }
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	// Aliases for the main base command (e.g. /massivehat)
	private Set<String> aliasesMassiveHat = MUtil.set("massivehat");
	public Set<String> getAliasesMassiveHat() { return this.aliasesMassiveHat; }
	public void setAliasesMassiveHat(Set<String> aliasesMassiveHat) { this.aliasesMassiveHat = aliasesMassiveHat; }

	// Aliases for the "use" shortcut command (e.g. /mhat = /massivehat use). Add /hat etc. in config if desired.
	private Set<String> aliasesMhat = MUtil.set("mhat", "usehat", "blockhat", "hat");
	public Set<String> getAliasesMhat() { return this.aliasesMhat; }
	public void setAliasesMhat(Set<String> aliasesMhat) { this.aliasesMhat = aliasesMhat; }

	// Subcommand aliases for "use" under the base (e.g. /massivehat use, /massivehat wear)
	private Set<String> aliasesHatUse = MUtil.set("use", "wear", "apply");
	public Set<String> getAliasesHatUse() { return this.aliasesHatUse; }
	public void setAliasesHatUse(Set<String> aliasesHatUse) { this.aliasesHatUse = aliasesHatUse; }
	
	// Aliases for the /massivehat config sub-command
	private Set<String> aliasesHatConfig = MUtil.set("config");
	public Set<String> getAliasesHatConfig() { return this.aliasesHatConfig; }
	public void setAliasesHatConfig(Set<String> aliasesHatConfig) { this.aliasesHatConfig = aliasesHatConfig; }

	// Aliases for the /massivehat version sub-command
	private Set<String> aliasesHatVersion = MUtil.set("v", "version");
	public Set<String> getAliasesHatVersion() { return this.aliasesHatVersion; }
	public void setAliasesHatVersion(Set<String> aliasesHatVersion) { this.aliasesHatVersion = aliasesHatVersion; }
	
	// This specifies which non block items may be treated as a hat.
	// Defaults to banners only.
	public ExceptionSet itemHats = new ExceptionSet(false,
		"BANNER"
	);
	
	// This specifies what block types may be used as a hat.
	// Defaults to all
	public ExceptionSet blockHats = new ExceptionSet(true);
	
	// These materials are already considered headwear in vanilla, so we will
	// let these be handled normally by the server.
	public ExceptionSet normalHelmets = new ExceptionSet(false,
		"LEATHER_HELMET",
		"CHAINMAIL_HELMET",
		"COPPER_HELMET",
		"IRON_HELMET",
		"GOLD_HELMET",
		"DIAMOND_HELMET",
		"NETHERITE_HELMET",
		"CARVED_PUMPKIN",
		"SKELETON_SKULL",
		"WITHER_SKELETON_SKULL",
		"PLAYER_HEAD",
		"ZOMBIE_HEAD",
		"CREEPER_HEAD",
		"PIGLIN_HEAD",
		"DRAGON_HEAD"
	);
	
	// When players try to place an item in their helmet slot but don't have permission,
	// should we give them a message telling them they don't have permission?
	public boolean hatPlacePermDenyVerbose = true;
	
}
