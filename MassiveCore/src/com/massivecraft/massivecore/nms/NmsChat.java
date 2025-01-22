package com.massivecraft.massivecore.nms;

import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.entity.MassiveCoreMConf;
import com.massivecraft.massivecore.mixin.Mixin;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.Txt;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

public class NmsChat extends Mixin
{
	// -------------------------------------------- //
	// DEFAULT
	// -------------------------------------------- //
	
	private static NmsChat d = new NmsChat().setAlternatives(
		NmsChat112R1P.class,
		NmsChat18R2P.class,
		NmsChat18R1.class,
		NmsChatFallback.class
	);
	
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static NmsChat i = d;
	public static NmsChat get() { return i; }
	
	// -------------------------------------------- //
	// CHAT
	// -------------------------------------------- //
	
	public void sendChatMson(Object sendeeObject, Mson mson)
	{
		CommandSender sendee = IdUtil.getSender(sendeeObject);
		if (sendee == null) return;

		Player player = IdUtil.getPlayer(sendee);
		if (player != null)
		{
			BaseComponent[] bc = ComponentSerializer.parse(mson.toRaw());
			player.spigot().sendMessage(bc);
		}
		else
		{
			String plain = mson.toPlain(true);
			sendChatPlain(sendee, plain);
		}
	}
	
	public void sendChatPlain(Object sendeeObject, String plain) {
		CommandSender sendee = IdUtil.getSender(sendeeObject);
		if (sendee == null) return;
		
		MassiveCoreMConf mconf = MassiveCoreMConf.get();
		if (mconf != null && !mconf.consoleColorsEnabled && sendee instanceof ConsoleCommandSender) {
			plain = ChatColor.stripColor(plain);
		}
		
		sendee.sendMessage(plain);
	}
	
	// -------------------------------------------- //
	// TITLE
	// -------------------------------------------- //
	
	public void sendTitleRaw(Object sendeeObject, int ticksIn, int ticksStay, int ticksOut, String rawMain, String rawSub)
	{
		Mson msonMain = MassiveCore.gson.fromJson(rawMain, Mson.class);
		Mson msonSub = MassiveCore.gson.fromJson(rawSub, Mson.class);

		sendTitleMson(sendeeObject, ticksIn, ticksStay, ticksOut, msonMain, msonSub);
	}
	
	public void sendTitleMson(Object sendeeObject, int ticksIn, int ticksStay, int ticksOut, Mson msonMain, Mson msonSub)
	{
		String messageMain = msonMain.toPlain(true);
		String messageSub = msonSub.toPlain(true);

		this.sendTitleRaw(sendeeObject, ticksIn, ticksStay, ticksOut, messageMain, messageSub);
	}
	
	public void sendTitleMessage(Object sendeeObject, int ticksIn, int ticksStay, int ticksOut, String messageMain, String messageSub)
	{
		// If we don't send any message (empty is ok) we might end up displaying old messages.
		if (messageMain == null) messageMain = "";
		if (messageSub == null) messageSub = "";

		Player player = IdUtil.getPlayer(sendeeObject);
		if (sendeeObject == null) return;

		player.sendTitle(messageMain, messageSub, ticksIn, ticksStay, ticksOut);
	}
	
	public void sendTitleMsg(Object sendeeObject, int ticksIn, int ticksStay, int ticksOut, String msgMain, String msgSub)
	{
		String messageMain = Txt.parse(msgMain);
		String messageSub = Txt.parse(msgSub);
		
		this.sendTitleMessage(sendeeObject, ticksIn, ticksStay, ticksOut, messageMain, messageSub);
	}
	
	// -------------------------------------------- //
	// ACTIONBAR
	// -------------------------------------------- //
	
	public void sendActionbarRaw(Object sendeeObject, String raw)
	{
		Player player = IdUtil.getPlayer(sendeeObject);
		if (sendeeObject == null) return;


	}
	
	public void sendActionbarMson(Object sendeeObject, Mson mson)
	{
		String message = mson.toRaw();
		
		this.sendActionbarRaw(sendeeObject, message);
	}
	
	public void sendActionbarMessage(Object sendeeObject, String message)
	{
		message = messageToRaw(message);

		this.sendActionbarRaw(sendeeObject, message);
	}

	public void sendActionbarMsg(Object sendeeObject, String msg)
	{
		String message = Txt.parse(msg);
		
		this.sendActionbarMessage(sendeeObject, message);
	}
	
	// -------------------------------------------- //
	// MESSAGE TO RAW
	// -------------------------------------------- //

	public static String messageToRaw(String message)
	{
		message = JSONObject.escape(message);
		message = "{\"text\": \"" + message + "\"}";
		return message;
	}
	
}
