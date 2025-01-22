package com.massivecraft.massivecore.util;

import com.massivecraft.massivecore.apachecommons.RandomStringUtils;
import com.massivecraft.massivecore.Lang;
import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.type.Type;
import com.massivecraft.massivecore.command.type.primitive.TypeStringConfirmation;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.mson.Mson;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ConfirmationUtil
{
	// -------------------------------------------- //
	// STORE
	// -------------------------------------------- //

	private static Map<Object, WeakHashMap<CommandSender, String>> objectToConfirmationMap = new HashMap<>();

	// -------------------------------------------- //
	// DERP
	// -------------------------------------------- //

	private static Map<CommandSender, String> getConfirmationMap(Object obj)
	{
		if (obj == null) throw new NullPointerException("obj");

		objectToConfirmationMap.putIfAbsent(obj, new WeakHashMap<>());
		return objectToConfirmationMap.get(obj);
	}

	public static String createConfirmationString(Object obj, CommandSender sender)
	{
		if (obj == null) throw new NullPointerException("obj");
		if (sender == null) throw new NullPointerException("sender");

		String str = RandomStringUtils.randomAlphanumeric(6);
		getConfirmationMap(obj).put(sender, str);

		scheduleRemove(obj, sender, 5 * 60);

		return str;
	}

	public static void scheduleRemove(Object obj, CommandSender sender, long seconds)
	{
		if (obj == null) throw new NullPointerException("obj");
		if (sender == null) throw new NullPointerException("sender");

		Bukkit.getScheduler().runTaskLater(MassiveCore.get(), () -> removeConfirmationString(obj, sender), 20L * seconds);
	}

	public static boolean removeConfirmationString(Object obj, CommandSender sender)
	{
		if (obj == null) throw new NullPointerException("obj");
		if (sender == null) throw new NullPointerException("sender");

		return getConfirmationMap(obj).remove(sender) != null;
	}

	public static String getConfirmationString(Object obj, CommandSender sender)
	{
		if (obj == null) throw new NullPointerException("obj");
		if (sender == null) throw new NullPointerException("sender");

		return getConfirmationMap(obj).get(sender);
	}

	public static boolean isConfirmationString(Object obj, CommandSender sender, String str)
	{
		if (obj == null) throw new NullPointerException("obj");
		if (sender == null) throw new NullPointerException("sender");
		if (str == null) throw new NullPointerException("str");

		return str.equals(getConfirmationString(obj, sender));
	}

	public static boolean hasConfirmationString(Object obj, CommandSender sender)
	{
		if (obj == null) throw new NullPointerException("obj");
		if (sender == null) throw new NullPointerException("sender");

		return getConfirmationMap(obj).containsKey(sender);
	}

	public static void tryConfirm(MassiveCommand object) throws MassiveException
	{
		if (object == null) throw new NullPointerException("object");

		CommandSender sender = object.sender;
		if (sender == null) throw new NullPointerException("sender");

		int idx = getConfirmationIdx(object);

		boolean hasConfirmationString = hasConfirmationString(object, sender);
		boolean hasTypedConfirmationString = object.argIsSet(idx);
		boolean hasTypedOther = idx == 0 || object.argIsSet(idx-1);

		// Assertion should not happen
		if (hasTypedConfirmationString && !hasTypedOther) throw new RuntimeException();

		if (!hasTypedOther)
		{
			MassiveException ex = new MassiveException();
			ex.addMsg(Lang.COMMAND_TOO_FEW_ARGUMENTS);
			ex.addMessage(object.getTemplate(false, true, sender));
			throw ex;
		}
		else if (hasConfirmationString && !hasTypedConfirmationString)
		{
			throw getException(object);
		}
		else if (hasConfirmationString && hasTypedConfirmationString)
		{
			// If they typed the wrong thing
			String typedString = object.argAt(idx);
			if (!isConfirmationString(object, sender, typedString))
			{
				// Make them type the right string
				throw getException(object);
			}
			// Otherwise do nothing, because success
		}
		else if (!hasConfirmationString && hasTypedOther)
		{
			String confirmationString = createConfirmationString(object, sender);
			MixinMessage.get().msgOne(sender, "<i>Created confirmation text <h>%s<i> for you.", confirmationString);

			throw getException(object);
		}

	}

	private static int getConfirmationIdx(MassiveCommand command)
	{
		int idx = -1;
		for (int i = 0; i < command.getParameters().size(); i++)
		{
			Type<?> type = command.getParameterType(i);
			if (!(type instanceof TypeStringConfirmation)) continue;

			idx = i;
		}
		if (idx == -1) throw new IllegalStateException("no confirmation string type");

		return idx;
	}

	private static MassiveException getException(MassiveCommand command)
	{
		CommandSender sender = command.sender;
		if (sender == null) throw new NullPointerException("sender");

		int idx = getConfirmationIdx(command);

		List<String> args = new MassiveList<>(command.getArgs());
		args.set(idx, getConfirmationString(command, sender));
		Mson template = command.getTemplateWithArgs(sender, args);
		MassiveException ex = new MassiveException();
		ex.addMsg("<b>To <h>%s <b>confirm it by typing:", command.getDesc());
		ex.addMessage(template);
		return ex;
	}
	
}
