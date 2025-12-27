package com.massivecraft.massivehat.entity;

import com.massivecraft.massivecore.collections.ExceptionSet;
import com.massivecraft.massivecore.command.editor.annotation.EditorName;
import com.massivecraft.massivecore.store.Entity;
import com.massivecraft.massivecore.util.MUtil;

import java.util.List;

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
	
	// Aliases
	public List<String> aliasesHat = MUtil.list("massivehat");
	public List<String> aliasesHatUseInner = MUtil.list("use", "wear", "apply");
	public List<String> aliasesHatUseOuter = MUtil.list("usehat", "hat", "blockhat", "mhat");
	public List<String> aliasesHatConfig = MUtil.list("config");
	public List<String> aliasesHatVersion = MUtil.list("v", "version");
	
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
