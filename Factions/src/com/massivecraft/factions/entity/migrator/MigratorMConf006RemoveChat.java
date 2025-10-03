package com.massivecraft.factions.entity.migrator;

import com.google.gson.JsonObject;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

public class MigratorMConf006RemoveChat extends MigratorRoot
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static MigratorMConf006RemoveChat i = new MigratorMConf006RemoveChat();
	public static MigratorMConf006RemoveChat get() { return i; }
	private MigratorMConf006RemoveChat() { super(MConf.class); }


	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void migrateInner(JsonObject entity)
	{
		// Remove chat related fields - all chat functionality going forward will be handled by FactionsChat
		entity.remove("chatSetFormat");
		entity.remove("chatSetFormatAt");
		entity.remove("chatSetFormatTo");
		entity.remove("chatParseTags");
		entity.remove("chatParseTagsAt");
		entity.remove("ventureChatFactionChannelName");
		entity.remove("ventureChatAllyChannelName");
		entity.remove("ventureChatAllowFactionchatBetweenFactionless");
	}
}
