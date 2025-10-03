package com.massivecraft.massivecore.util;

import com.massivecraft.massivecore.MassiveCore;
import com.massivecraft.massivecore.collections.ExceptionSet;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.collections.MassiveTreeSet;
import com.massivecraft.massivecore.comparator.ComparatorCaseInsensitive;
import com.massivecraft.massivecore.engine.EngineMassiveCoreDatabase;
import com.massivecraft.massivecore.engine.EngineMassiveCoreMain;
import com.massivecraft.massivecore.engine.EngineMassiveCoreWorldNameSet;
import com.massivecraft.massivecore.integration.liability.IntegrationLiabilityAreaEffectCloud;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.nms.NmsEntityGet;
import com.massivecraft.massivecore.predicate.PredicateElementGarbage;
import com.massivecraft.massivecore.predicate.PredicateElementSignificant;
import com.massivecraft.massivecore.util.extractor.Extractor;
import com.massivecraft.massivecore.util.extractor.ExtractorSenderId;
import com.massivecraft.massivecore.util.extractor.ExtractorWorld;
import com.massivecraft.massivecore.util.extractor.ExtractorWorldName;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.Metadatable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class MUtil
{
	// -------------------------------------------- //
	// GET SERVER-RELATED INFORMATION
	// -------------------------------------------- //
	public static boolean isPaper()
	{
		try 
		{
			Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
			return true;
		} 
		catch (ClassNotFoundException e) 
		{
			return false;
		}
	}

	// -------------------------------------------- //
	// GET ONLINE PLAYERS
	// -------------------------------------------- //
	// Old thing that had to do with the conversion from 1.17 to 1.8

	@Deprecated
	public static Collection<Player> getOnlinePlayers()
	{
		Collection<? extends Player>coll = Bukkit.getOnlinePlayers();
		List<Player> ret = new MassiveList<>(coll.size());
		return ret;
	}
	
	// -------------------------------------------- //
	// GET NEARBY PLAYERS
	// -------------------------------------------- //
	
	public static Set<Player> getNearbyPlayers(Entity entity, double raidus, boolean includeSelf)
	{
		Set<Player> ret = getNearbyPlayers(entity.getLocation(), raidus);
		if (isPlayer(entity) && !includeSelf) ret.remove(entity);
		return ret;
	}
	
	public static Set<Player> getNearbyPlayers(Location location, double radius)
	{
		// Create
		Set<Player> ret = new MassiveSet<>();
		
		// Fill
		final World world = location.getWorld();
		final double radiusSquared = radius * radius;
		for (Player player : Bukkit.getOnlinePlayers())
		{
			Location playerLocation = player.getLocation();
			World playerWorld = playerLocation.getWorld();
			if ( ! world.equals(playerWorld)) continue;
			double distanceSquared = location.distanceSquared(playerLocation);
			if (distanceSquared > radiusSquared) continue;
			ret.add(player);
		}
		
		// Return
		return ret;
	}
	
	// -------------------------------------------- //
	// IS SYNCHRONOUS
	// -------------------------------------------- //
	
	public static boolean isSynchronous()
	{
		return Thread.holdsLock(Bukkit.getPluginManager()) || Bukkit.isPrimaryThread();
	}
	
	// -------------------------------------------- //
	// GET ENTITY
	// -------------------------------------------- //
	
	public static Entity getEntity(World world, UUID uuid)
	{
		return NmsEntityGet.get().getEntity(world, uuid);
	}
	
	public static Entity getEntity(UUID uuid)
	{
		return NmsEntityGet.get().getEntity(uuid);
	}
	
	// -------------------------------------------- //
	// IS VALID PLAYER NAME
	// -------------------------------------------- //
	
	// The regex for a valid minecraft player name.
	public final static Pattern PATTERN_PLAYER_NAME = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");
	
	public static boolean isValidPlayerName(String string)
	{
		return PATTERN_PLAYER_NAME.matcher(string).matches();
	}
	
	// -------------------------------------------- //
	// UUID
	// -------------------------------------------- //
	
	public static UUID asUuid(String string)
	{
		// Null
		if (string == null) throw new NullPointerException("string");
		
		// Avoid Exception
		if (string.length() != 36) return null;
		
		// Try
		try
		{
			return UUID.fromString(string);
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	public static boolean isUuid(String string)
	{
		return asUuid(string) != null;
	}
	
	// -------------------------------------------- //
	// CONTAINS COMMAND
	// -------------------------------------------- //
	
	public static boolean containsCommand(String needle, ExceptionSet haystack)
	{
		boolean ret = haystack.isStandard();
		if (containsCommand(needle, haystack.exceptions)) ret = !ret; 
		return ret;
	}
	
	public static boolean containsCommand(String needle, Iterable<String> haystack)
	{
		if (needle == null) throw new NullPointerException("needle");
		needle = prepareCommand(needle);
		
		for (String straw : haystack)
		{
			if (straw == null) continue;
			straw = prepareCommand(straw);
			
			// If it starts with then it is possibly a subject.
			if ( ! needle.startsWith(straw)) continue;
			
			// Get the remainder.
			String remainder = needle.substring(straw.length());
			
			// If they were equal, definitely true.
			if (remainder.isEmpty()) return true;
			
			// If the next is a space, the space is used as separator for sub commands or arguments.
			// Otherwise it might just have been another command coincidentally starting with the first command.
			// The old behaviour was if (needle.startsWith(straw)) return true;
			// If "s" was block, then all commands starting with "s" was, now it isn't.
			if (remainder.startsWith(" ")) return true;
		}
		
		return false;
	}
	
	private static String prepareCommand(String string)
	{
		if (string == null) throw new NullPointerException("string");
		string = Txt.removeLeadingCommandDust(string);
		string = string.toLowerCase();
		string = string.trim();
		return string;
	}
	
	// -------------------------------------------- //
	// IP
	// -------------------------------------------- //
	
	public static String getIp(CommandSender sender)
	{
		if (!(sender instanceof Player)) return null;
		Player player = (Player)sender;
		
		InetSocketAddress address = player.getAddress();
		if (address != null) return getIp(address);
		
		String id = IdUtil.getId(player);
		PlayerLoginEvent event = EngineMassiveCoreDatabase.idToPlayerLoginEvent.get(id);
		if (event != null) return getIp(event);
		
		return null;
	}
	
	public static String getIp(InetSocketAddress address)
	{
		if (address == null) return null;
		
		String ret = address.toString();
		String[] parts = ret.split("/");
		
		ret = parts[1];
		parts = ret.split(":");
		
		ret = parts[0];
		return ret;
	}
	
	public static String getIp(PlayerLoginEvent event)
	{
		InetAddress address = event.getAddress();
		return getIp(address);
	}
	
	public static String getIp(InetAddress address)
	{
		if (address == null) return null;
		
		String ret = address.toString();
		String[] parts = ret.split("/");
		
		ret = parts[1];
		return ret;
	}
	
	public static Pattern PATTERN_IPV4 = Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
	public static boolean isIp(String string)
	{
		if (string == null) return false;
		return PATTERN_IPV4.matcher(string).matches();
	}
	
	// -------------------------------------------- //
	// IS(NT) NPC, SENDER, PLAYER
	// -------------------------------------------- //
	
	public static boolean isNpc(Object object)
	{
		if (object == null) throw new NullPointerException("object");

		if ( ! (object instanceof Metadatable)) return false;
		Metadatable metadatable = (Metadatable)object;
		try
		{
			return metadatable.hasMetadata("NPC");
		}
		catch (UnsupportedOperationException e)
		{
			// ProtocolLib
			// UnsupportedOperationException: The method hasMetadata is not supported for temporary players.
			return false;
		}
		
	}
	public static boolean isntNpc(Object object)
	{
		return !isNpc(object);
	}
	
	public static boolean isSender(Object object)
	{
		if (object == null) throw new NullPointerException("object");

		if (!(object instanceof CommandSender)) return false;
		if (isNpc(object)) return false;
		return true;
	}
	public static boolean isntSender(Object object)
	{
		return !isSender(object);
	}
	
	public static boolean isPlayer(Object object)
	{
		if (object == null) throw new NullPointerException("object");

		if (!(object instanceof Player)) return false;
		if (isNpc(object)) return false;
		return true;
	}
	public static boolean isntPlayer(Object object)
	{
		return !isPlayer(object);
	}
	
	// -------------------------------------------- //
	// STACK TRACE: GET
	// -------------------------------------------- //
	
	public static List<StackTraceElement> getStackTrace(Thread thread, int skip)
	{
		// Cut away this method.
		// Cut away system line.
		// We must check if it's the current thread.
		if (thread.equals(Thread.currentThread())) skip += 2;
		
		StackTraceElement[] elements = thread.getStackTrace();
		elements = Arrays.copyOfRange(elements, skip, elements.length);
		return new MassiveList<>(Arrays.asList(elements));
	}
	
	public static List<StackTraceElement> getStackTrace(Thread thread)
	{
		int skip = 0;
		
		// Cut away this method.
		// We must check if it's the current thread.
		if (thread.equals(Thread.currentThread())) skip += 1;
		
		return getStackTrace(thread, skip);
	}
	
	public static List<StackTraceElement> getStackTrace(int skip)
	{
		// Cut away this method.
		// We already know it's going to be the current thread.
		skip++;
		
		Thread thread = Thread.currentThread();
		
		return getStackTrace(thread, skip);
	}
	
	public static List<StackTraceElement> getStackTrace()
	{
		// Cut away this method.
		// We already know it's going to be the current thread.
		int skip = 1;
		
		Thread thread = Thread.currentThread();
		
		return getStackTrace(thread, skip);
	}
	
	// -------------------------------------------- //
	// STACK TRACE: CUT
	// -------------------------------------------- //
	
	public static void cutStackTrace(List<StackTraceElement> trace)
	{
		// Cut Significant
		int index = 0;
		while (index < trace.size())
		{
			StackTraceElement element = trace.get(index);
			if (PredicateElementSignificant.get().apply(element)) break;
			index++;
		}
		trace.subList(index, trace.size()).clear();
		
		// Reverse
		Collections.reverse(trace);
		
		// Eat Garbage
		trace.removeIf(PredicateElementGarbage.get());
		
		// Unreverse
		Collections.reverse(trace);
	}
	
	// -------------------------------------------- //
	// STACK TRACE STRING: MANY
	// -------------------------------------------- //
	
	public static List<String> getStackTraceStrings(List<StackTraceElement> stackTrace, boolean color)
	{
		List<String> ret = new MassiveList<>();
		
		for (StackTraceElement element : stackTrace)
		{
			ret.add(getStackTraceString(element, color));
		}
		
		return ret;
	}
	public static List<String> getStackTraceStrings(int skip, boolean color)
	{
		skip++;
		return getStackTraceStrings(getStackTrace(skip), color);
	}
	public static List<String> getStackTraceStrings(boolean color)
	{
		return getStackTraceStrings(0, color);
	}
	
	public static String getStackTraceString(List<StackTraceElement> stackTrace, boolean color)
	{
		return Txt.implode(getStackTraceStrings(stackTrace, color), "\n");
	}
	public static String getStackTraceString(int skip, boolean color)
	{
		skip++;
		return getStackTraceString(getStackTrace(skip), color);
	}
	public static String getStackTraceString(boolean color)
	{
		return getStackTraceString(0, color);
	}
	
	// -------------------------------------------- //
	// STACK TRACE STRING: ONE
	// -------------------------------------------- //
	
	// Same as the Java8 source but with color option.
	public static String getStackTraceString(StackTraceElement element, boolean color)
	{
		ChatColor separatorColor = ChatColor.GRAY;
		ChatColor classColor = ChatColor.YELLOW;
		ChatColor methodColor = ChatColor.GREEN;
		ChatColor fileColor = ChatColor.AQUA;
		ChatColor lineColor = ChatColor.LIGHT_PURPLE;
		
		String className = element.getClassName();
		String methodName = element.getMethodName();
		boolean nativeMethod = element.isNativeMethod();
		String fileName = element.getFileName();
		int lineNumber = element.getLineNumber();
		
		StringBuilder ret = new StringBuilder();
		
		if (color) ret.append(classColor);
		ret.append(className);
		
		if (color) ret.append(separatorColor);
		ret.append(".");
		
		if (color) ret.append(methodColor);
		ret.append(methodName);
		
		if (color) ret.append(separatorColor);
		ret.append("(");
		
		if (color) ret.append(fileColor);
		if (nativeMethod)
		{
			ret.append("Native Method");
		}
		else
		{
			if (fileName != null && lineNumber >= 0)
			{
				ret.append(fileName);
				
				if (color) ret.append(separatorColor);
				ret.append(":");
				
				if (color) ret.append(lineColor);
				ret.append(lineNumber);
			}
			else
			{
				if (fileName != null)
				{
					ret.append(fileName);
				}
				else
				{
					ret.append("Unknown Source");
				}
			}
		}
		
		if (color) ret.append(separatorColor);
		ret.append(")");
		
		return ret.toString();
	}
	
	// -------------------------------------------- //
	// STACK TRACE: DEBUG
	// -------------------------------------------- //
	
	public static void stackTraceDebug(String title)
	{
		// Create
		List<String> messages = new MassiveList<>();
		
		// Fill
		messages.add(Txt.titleize(title).toPlain(true));
		messages.addAll(MUtil.getStackTraceStrings(1, true));
		
		// Message
		MixinMessage.get().messageOne(IdUtil.CONSOLE_ID, messages);
	}
	
	// -------------------------------------------- //
	// LIST OPERATIONS
	// -------------------------------------------- //
	
	public static <T> List<T> repeat(T object, int times)
	{
		List<T> ret = new ArrayList<>(times);
		for (int i = 1; i <= times; i++)
		{
			ret.add(object);
		}
		return ret;
	}
	public static List<Integer> range(int from, int to)
	{
		List<Integer> ret = new MassiveList<>(to - from);
		for (int i = from; i < to; i++)
		{
			ret.add(i);
		}
		return ret;
	}
	
	public static void keepLeft(List<?> list, int maxlength)
	{
		if (list.size() <= maxlength) return;
		list.subList(maxlength, list.size()).clear();
	}
	
	public static void keepRight(List<?> list, int maxlength)
	{
		if (list.size() <= maxlength) return;
		list.subList(0, maxlength).clear();
	}
	
	public static <T> void padLeft(List<T> list, T object, int length)
	{
		if (list.size() >= length) return;
		list.addAll(0, repeat(object, length - list.size()));
	}
	
	public static <T> void padRight(List<T> list, T object, int length)
	{
		if (list.size() >= length) return;
		list.addAll(repeat(object, length - list.size()));
	}
	
	// -------------------------------------------- //
	// MAP OPERATIONS
	// -------------------------------------------- //
	
	public static void keepLeft(Map<?, ?> map, int maxSize)
	{
		int i = 0;
		Iterator<?> iter = map.entrySet().iterator();
		while (iter.hasNext())
		{
			iter.next();
			i++;
			if (i > maxSize) iter.remove();
		}
	}
	
	// -------------------------------------------- //
	// ITERABLE MATH
	// -------------------------------------------- //
	
	public static <T extends Number> double getSum(Iterable<T> numbers)
	{
		if (numbers == null) throw new NullPointerException("numbers");
		
		double sum = 0;
		for (T number : numbers)
		{
			sum += number.doubleValue();
		}
		
		return sum;
	}
	
	public static <T extends Number> double getAverage(Iterable<T> numbers)
	{
		if (numbers == null) throw new NullPointerException("numbers");
		
		double sum = 0;
		int count = 0;
		for (T number : numbers)
		{
			sum += number.doubleValue();
			count++;
		}
		
		if (count == 0) throw new IllegalArgumentException("numbers empty");
		
		return sum / count;
	}
	
	// -------------------------------------------- //
	// TABLE OPERATIONS
	// -------------------------------------------- //
	
	public static <T> List<List<T>> rotateLeft(List<List<T>> rows)
	{
		List<List<T>> ret = transpose(rows);
		flipVertically(ret);
		return ret;
	}
	
	public static <T> List<List<T>> rotateRight(List<List<T>> rows)
	{
		List<List<T>> ret = transpose(rows);
		flipHorizontally(ret);
		return ret;
	}
	
	public static <T> List<List<T>> transpose(List<List<T>> rows)
	{
		List<List<T>> ret = new ArrayList<>();
		
		final int n = rows.get(0).size();
		
		for (int i = 0; i < n; i++)
		{
			List<T> col = new ArrayList<>();
			for (List<T> row : rows)
			{
				col.add(row.get(i));
			}
			ret.add(col);
		}
		
		return ret;
	}
	
	public static <T> void flipHorizontally(List<List<T>> rows)
	{
		for (List<T> row : rows)
		{
			Collections.reverse(row);
		}
	}
	
	public static <T> void flipVertically(List<List<T>> rows)
	{
		Collections.reverse(rows);
	}
	
	// -------------------------------------------- //
	// COLOR INT CODE
	// -------------------------------------------- //
	
	public static int getChatColorCode(ChatColor chatColor)
	{
		switch (chatColor)
		{
			case BLACK: return 0x00;
			case DARK_BLUE: return 0x1;
			case DARK_GREEN: return 0x2;
			case DARK_AQUA: return 0x3;
			case DARK_RED: return 0x4;
			case DARK_PURPLE: return 0x5;
			case GOLD: return 0x6;
			case GRAY: return 0x7;
			case DARK_GRAY: return 0x8;
			case BLUE: return 0x9;
			case GREEN: return 0xA;
			case AQUA: return 0xB;
			case RED: return 0xC;
			case LIGHT_PURPLE: return 0xD;
			case YELLOW: return 0xE;
			case WHITE: return 0xF;
			case MAGIC: return 0x10;
			case BOLD: return 0x11;
			case STRIKETHROUGH: return 0x12;
			case UNDERLINE: return 0x13;
			case ITALIC: return 0x14;
			case RESET: return 0x15;
		}
		throw new IllegalArgumentException("The chat color " + chatColor.name() + " is not yet supported!");
	}
	
	public static ChatColor getChatColor(int chatColorCode)
	{
		switch (chatColorCode)
		{
			case 0x00: return ChatColor.BLACK;
			case 0x1: return ChatColor.DARK_BLUE;
			case 0x2: return ChatColor.DARK_GREEN;
			case 0x3: return ChatColor.DARK_AQUA;
			case 0x4: return ChatColor.DARK_RED;
			case 0x5: return ChatColor.DARK_PURPLE;
			case 0x6: return ChatColor.GOLD;
			case 0x7: return ChatColor.GRAY;
			case 0x8: return ChatColor.DARK_GRAY;
			case 0x9: return ChatColor.BLUE;
			case 0xA: return ChatColor.GREEN;
			case 0xB: return ChatColor.AQUA;
			case 0xC: return ChatColor.RED;
			case 0xD: return ChatColor.LIGHT_PURPLE;
			case 0xE: return ChatColor.YELLOW;
			case 0xF: return ChatColor.WHITE;
			case 0x10: return ChatColor.MAGIC;
			case 0x11: return ChatColor.BOLD;
			case 0x12: return ChatColor.STRIKETHROUGH;
			case 0x13: return ChatColor.UNDERLINE;
			case 0x14: return ChatColor.ITALIC;
			case 0x15: return ChatColor.RESET;
		}
		throw new IllegalArgumentException("The chat color code " + chatColorCode + " is not yet supported!");
	}
	
	@SuppressWarnings("deprecation")
	public static ChatColor getChatColor(DyeColor dyeColor)
	{
		int woolColorCode = dyeColor.getWoolData();
		
		switch (woolColorCode)
		{
			case 0x0: return ChatColor.WHITE;
			case 0x1: return ChatColor.GOLD;
			case 0x2: return ChatColor.LIGHT_PURPLE;
			case 0x3: return ChatColor.AQUA;
			case 0x4: return ChatColor.YELLOW;
			case 0x5: return ChatColor.GREEN;
			case 0x6: return ChatColor.LIGHT_PURPLE;
			case 0x7: return ChatColor.DARK_GRAY;
			case 0x8: return ChatColor.GRAY;
			case 0x9: return ChatColor.DARK_AQUA;
			case 0xA: return ChatColor.DARK_PURPLE;
			case 0xB: return ChatColor.BLUE;
			case 0xC: return ChatColor.GRAY;
			case 0xD: return ChatColor.DARK_GREEN;
			case 0xE: return ChatColor.RED;
			case 0xF: return ChatColor.BLACK;
		}
		throw new IllegalArgumentException("The dye color " + dyeColor + " is not yet supported!");
	}
	
	// isFinite check recreation to be compatible with java 1.7/1.6
	public static boolean isFinite(double d)
	{
		 return Math.abs(d) <= Double.MAX_VALUE;
	}
	
	// Damage Events
//	public static void scaleDamage(EntityDamageEvent event, double factor)
//	{
//		// Clean Input
//		if ( ! isFinite(factor)) throw new IllegalStateException("not finite factor: " + factor);
//		
//		// No Change?
//		if (equalsishNumber(factor, 1)) return;
//		
//		for (DamageModifier modifier : DamageModifier.values())
//		{
//			// Is this modifier used in the event?
//			if ( ! event.isApplicable(modifier)) continue;
//			
//			// Apply Factor
//			event.setDamage(modifier, factor * event.getDamage(modifier));
//		}
//	}
	// -------------------------------------------- //
	// ENTITY DAMAGE EVENT
	// -------------------------------------------- //
	
	public static void scaleDamage(EntityDamageEvent event, double factor)
	{ 
		// Clean Input
		if (!isFinite(factor)) 
		{
			// No Change?
			throw new IllegalStateException("not finite factor: " + factor); 
		}
		// Scale the final damage
		if (equalsishNumber(factor, 1)) return;
		
		double originalDamage = event.getDamage();
		double scaledDamage = originalDamage * factor;
		event.setDamage(scaledDamage);
	}
	
	private static boolean equalsishNumber(double a, double b)
	{
		return Math.abs(a - b) < 1e-9;
	}
	
	// -------------------------------------------- //
	// PICK
	// -------------------------------------------- //
	
	public static <T> T regexPickFirstVal(String input, Map<String, T> regex2val)
	{
		if (regex2val == null) throw new NullPointerException("regex2val");
		T ret = null;
		
		for (Entry<String, T> entry : regex2val.entrySet())
		{
			ret = entry.getValue();
			if (input == null) continue;
			String regex = entry.getKey();
			if (Pattern.matches(regex, input)) break;
		}
		
		return ret;
	}
	
	public static <E, T> T equalsPickFirstVal(E input, Map<E, T> thing2val)
	{
		if (thing2val == null) throw new NullPointerException("thing2val");
		T ret = null;
		
		for (Entry<E, T> entry : thing2val.entrySet())
		{
			ret = entry.getValue();
			if (input == null) continue;
			E thing = entry.getKey();
			if (MUtil.equals(input, thing)) break;
		}
		
		return ret;
	}
	
	public static <T> T recurseResolveMap(T input, Map<T, T> map)
	{
		T output = map.get(input);
		if (output == null) return input;
		return recurseResolveMap(output, map);
	}
	
	// -------------------------------------------- //
	// GET BLOCKS
	// -------------------------------------------- //
	
	public static List<Block> getBlocks(Location location, int halfWidth)
	{
		return getBlocks(location.getBlock(), halfWidth);
	}
	
	public static List<Block> getBlocks(Block block, int halfWidth)
	{
		int xmin = block.getX() - halfWidth;
		int ymin = block.getY() - halfWidth;
		int zmin = block.getZ() - halfWidth;
		
		int xmax = block.getX() + halfWidth;
		int ymax = block.getY() + halfWidth;
		int zmax = block.getZ() + halfWidth;
		
		return getBlocks(block.getWorld(), xmin, ymin, zmin, xmax, ymax, zmax);
	}
	
	public static List<Block> getBlocks(World world, int xmin, int ymin, int zmin, int xmax, int ymax, int zmax)
	{
		List<Block> blocks = new ArrayList<>();
		
		for (int x = xmin; x <= xmax; x++)
		{
			for (int y = ymin; y <= ymax; y++)
			{
				for (int z = zmin; z <= zmax; z++)
				{
					blocks.add(world.getBlockAt(x, y, z));
				}	
			}
		}
		
		return blocks;
	}
	
	// -------------------------------------------- //
	// LOCATIONS COMPARISON
	// -------------------------------------------- //
	
	public static boolean isSameBlock(PlayerMoveEvent event)
	{
		return isSameBlock(event.getFrom(), event.getTo());
	}
	
	public static boolean isSameBlock(Location one, Location two)
	{
		if (one.getBlockX() != two.getBlockX()) return false;
		if (one.getBlockZ() != two.getBlockZ()) return false;
		if (one.getBlockY() != two.getBlockY()) return false;
		return one.getWorld().equals(two.getWorld());
	}
	
	public static boolean isSameChunk(PlayerMoveEvent event)
	{
		return isSameChunk(event.getFrom(), event.getTo());
	}
	
	public static boolean isSameChunk(Location one, Location two)
	{
		if (one.getBlockX() >> 4 != two.getBlockX() >> 4) return false;
		if (one.getBlockZ() >> 4 != two.getBlockZ() >> 4) return false;
		if (one.getWorld() != two.getWorld()) return false;
		return true;
	}
	
	// -------------------------------------------- //
	// FACE AND YAW
	// -------------------------------------------- //
	
	public static Float getYaw(BlockFace face)
	{
		switch (face)
		{
			case NORTH: return 0f;
			case EAST: return 90f;
			case SOUTH: return 180f;
			case WEST: return 270f;
			case UP: return null;
			case DOWN: return null;
			case NORTH_EAST: return 45f;
			case NORTH_WEST: return 315f;
			case SOUTH_EAST: return 135f;
			case SOUTH_WEST: return 225f;
			case WEST_NORTH_WEST: return 292.5f;
			case NORTH_NORTH_WEST: return 337.5f;
			case NORTH_NORTH_EAST: return 22.5f;
			case EAST_NORTH_EAST: return 67.5f;
			case EAST_SOUTH_EAST: return 112.5f;
			case SOUTH_SOUTH_EAST: return 157.5f;
			case SOUTH_SOUTH_WEST: return 202.5f;
			case WEST_SOUTH_WEST: return 247.5f;
			case SELF: return null;
		}
		return null;
	}
		
	public static boolean isCombatEvent(EntityDamageEvent event)
	{
		if (event.getCause() != DamageCause.ENTITY_ATTACK && event.getCause() != DamageCause.PROJECTILE) return false;
		return event instanceof EntityDamageByEntityEvent;
	}
	
	public static boolean isCloseCombatEvent(EntityDamageEvent event)
	{
		if (event.getCause() != DamageCause.ENTITY_ATTACK) return false;
		return event instanceof EntityDamageByEntityEvent;
	}
	
	public static Entity getLiableDamager(EntityDamageEvent event)
	{
		if (!(event instanceof EntityDamageByEntityEvent)) return null;
		EntityDamageByEntityEvent edbeEvent = (EntityDamageByEntityEvent)event;
		Entity ret = edbeEvent.getDamager();
		if (ret instanceof Projectile)
		{
			Projectile projectile = (Projectile)ret;
			ProjectileSource projectileSource = projectile.getShooter();
			if (projectileSource instanceof Entity) ret = (Entity)projectileSource;
		}
		
		Entity cloudBasedDamager = IntegrationLiabilityAreaEffectCloud.get().getLiableDamager(edbeEvent);
		if (cloudBasedDamager != null) ret = cloudBasedDamager;
		
		return ret;
	}
	
	
	public static String kickReason(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return null;
		UUID uuid = player.getUniqueId();
		
		return EngineMassiveCoreMain.kickedPlayerReasons.get(uuid);
	}
	
	public static boolean causedByKick(PlayerQuitEvent event)
	{
		return kickReason(event) != null;
	}
	
	// -------------------------------------------- //
	// POTION DERP
	// -------------------------------------------- //
	
	public static List<PotionEffect> getPotionEffects(ItemStack itemStack)
	{
		if (itemStack == null) return null;
		if (itemStack.getType() != Material.POTION) return null;

		List<PotionEffect> ret = new ArrayList<>();
		
		PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
		if (meta != null)
		{
			PotionType potionType = meta.getBasePotionType();
			if (potionType == PotionType.WATER)
			{
				return ret;
			}
			else
			{
				ret.addAll(potionType.getPotionEffects());
			}
		}
		
		return ret;
	}
	
	public static final Set<PotionEffectType> HARMFUL_POTION_EFFECTS = Collections.unmodifiableSet(MUtil.set(
		PotionEffectType.BAD_OMEN,
		PotionEffectType.BLINDNESS,
		PotionEffectType.DARKNESS,
		PotionEffectType.HUNGER,
		PotionEffectType.INFESTED,
		PotionEffectType.INSTANT_DAMAGE,
		PotionEffectType.MINING_FATIGUE,
		PotionEffectType.NAUSEA,
		PotionEffectType.OOZING,
		PotionEffectType.POISON,
		PotionEffectType.RAID_OMEN,
		PotionEffectType.SLOWNESS,
		PotionEffectType.TRIAL_OMEN,
		PotionEffectType.UNLUCK,
		PotionEffectType.WEAKNESS,
		PotionEffectType.WEAVING,
		PotionEffectType.WITHER
	));
	
	public static boolean isHarmfulPotion(PotionEffectType potionEffectType)
	{
		return HARMFUL_POTION_EFFECTS.contains(potionEffectType);
	}
	
	public static boolean isHarmfulPotion(PotionEffect potionEffect)
	{
		if (potionEffect == null) return false;
		return isHarmfulPotion(potionEffect.getType());
	}
	
	public static boolean isHarmfulPotion(ItemStack itemStack)
	{
		List<PotionEffect> potionEffects = getPotionEffects(itemStack);
		if (potionEffects == null) return false;
		
		for (PotionEffect potionEffect : potionEffects)
		{
			if (isHarmfulPotion(potionEffect)) return true;
		}
		return false;
	}
	
	public static boolean isHarmfulPotion(ThrownPotion thrownPotion)
	{
		return isHarmfulPotion(thrownPotion.getItem());
	}
	
	// -------------------------------------------- //
	// WORLD DERP
	// -------------------------------------------- //
	
	public static Set<String> getLoadedWorldNames()
	{
		return EngineMassiveCoreWorldNameSet.get().getWorldNames();
	}
	
	// -------------------------------------------- //
	// TRANSFORM
	// -------------------------------------------- //

	public static <T> List<T> transform(Iterable<T> items, Predicate<? super T> where, Comparator<? super T> orderby, Integer limit, Integer offset)
	{
		// Collection
		Collection<T> collection = null;
		if (items instanceof Collection<?>) collection = (Collection<T>)items;
		
		// WHERE
		List<T> ret;
		if (where == null)
		{
			if (collection != null)
			{
				ret = new ArrayList<>(collection);
			}
			else
			{
				ret = new ArrayList<>();
				for (T item : items)
				{
					ret.add(item);
				}
			}
		}
		else
		{
			if (collection != null)
			{
				ret = new ArrayList<>(collection.size());
			}
			else
			{
				ret = new ArrayList<>();
			}
			
			for (T item : items)
			{
				if (where.test(item))
				{
					ret.add(item);
				}
			}
		}
		
		// ORDERBY
		if (orderby != null)
		{
			ret.sort(orderby);
		}
		
		// LIMIT AND OFFSET
		// Parse args
		int fromIndex = 0;
		if (offset != null)
		{
			fromIndex = offset;
		}
		
		int toIndex = ret.size()-1;
		if (limit != null)
		{
			toIndex = offset+limit;
		}
		
		// Clean args
		if (fromIndex <= 0)
		{
			fromIndex = 0;
		}
		else if (fromIndex > ret.size()-1)
		{
			fromIndex = ret.size()-1;
		}
		
		if (toIndex < fromIndex)
		{
			toIndex = fromIndex;
		}
		else if (toIndex > ret.size()-1)
		{
			toIndex = ret.size()-1;
		}
		
		// No limit?
		if (fromIndex == 0 && toIndex == ret.size()-1) return ret;
		
		return new ArrayList<>(ret.subList(fromIndex, toIndex));
	}
	public static <T> List<T> transform(Iterable<T> items, Predicate<? super T> where) { return transform(items, where, null, null, null); }
	public static <T> List<T> transform(Iterable<T> items, Predicate<? super T> where, Comparator<? super T> orderby) { return transform(items, where, orderby, null, null); }
	public static <T> List<T> transform(Iterable<T> items, Predicate<? super T> where, Comparator<? super T> orderby, Integer limit) { return transform(items, where, orderby, limit, null); }
	public static <T> List<T> transform(Iterable<T> items, Predicate<? super T> where, Integer limit) { return transform(items, where, null, limit, null); }
	public static <T> List<T> transform(Iterable<T> items, Predicate<? super T> where, Integer limit, Integer offset) { return transform(items, where, null, limit, offset); }
	public static <T> List<T> transform(Iterable<T> items, Comparator<? super T> orderby) { return transform(items, null, orderby, null, null); }
	public static <T> List<T> transform(Iterable<T> items, Comparator<? super T> orderby, Integer limit) { return transform(items, null, orderby, limit, null); }
	public static <T> List<T> transform(Iterable<T> items, Comparator<? super T> orderby, Integer limit, Integer offset) { return transform(items, null, orderby, limit, offset); }
	public static <T> List<T> transform(Iterable<T> items, Integer limit) { return transform(items, null, null, limit, null); }
	public static <T> List<T> transform(Iterable<T> items, Integer limit, Integer offset) { return transform(items, null, null, limit, offset); }
	
	// -------------------------------------------- //
	// SIMPLE CONSTRUCTORS
	// -------------------------------------------- //
	
	@SafeVarargs
	public static <T> List<T> list(T... items)
	{
		List<T> ret = new MassiveList<>(items.length);
		Collections.addAll(ret, items);
		return ret;
	}
	
	@SafeVarargs
	public static <T> Set<T> set(T... items)
	{
		Set<T> ret = new MassiveSet<>(items.length);
		Collections.addAll(ret, items);
		return ret;
	}
	
	public static Set<String> treeset(String... items)
	{
		return new MassiveTreeSet<String, ComparatorCaseInsensitive>(ComparatorCaseInsensitive.get(), Arrays.asList(items));
	}
	
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> map(K key1, V value1, Object... objects)
	{
		Map<K, V> ret = new MassiveMap<>();
		
		ret.put(key1, value1);
		
		Iterator<Object> iter = Arrays.asList(objects).iterator();
		while (iter.hasNext())
		{
			K key = (K) iter.next();
			V value = (V) iter.next();
			ret.put(key, value);
		}
		
		return ret;
	}
	
	public static <K, V> Map<V, K> flippedMap(Map<K, V> map)
	{
		Map<V, K> ret = new MassiveMap<>();
		
		for(Entry<K, V> entry : map.entrySet())
		{
			V value = entry.getValue();
			K key = entry.getKey();
			
			if (value == null) continue;
			ret.put(value, key);
		}
		
		return ret;
	}
	
	public static <K, V> Map<V, Set<K>> reverseIndex(Map<K, V> map)
	{
		Map<V, Set<K>> ret = new MassiveMap<>();
		
		for (Entry<K, V> entry : map.entrySet())
		{
			K key = entry.getKey();
			V value = entry.getValue();

			Set<K> set = ret.computeIfAbsent(value, k -> new HashSet<>());
			set.add(key);
		}
		
		return ret;
	}
	
	// -------------------------------------------- //
	// COLLECTION MANIPULATION
	// -------------------------------------------- //
	
	public static <T> T removeByIndex(Collection<T> coll, int index)
	{
		if (coll == null) throw new NullPointerException("coll");
		
		if (coll instanceof List<?>)
		{
			return ((List<T>)coll).remove(index);
		}
		
		if (index < 0) throw new IndexOutOfBoundsException("index < 0");
		if (index >= coll.size()) throw new IndexOutOfBoundsException("index > collection size");
		
		int i = -1;
		Iterator<T> iter = coll.iterator();
		while (iter.hasNext())
		{
			i++;
			T ret = iter.next();
			if (i != index) continue;
			iter.remove();
			return ret;
		}
		
		return null;
	}
	
	// -------------------------------------------- //
	// LE NICE RANDOM
	// -------------------------------------------- //
	
	public static <T> T random(Collection<T> coll)
	{
		if (coll.size() == 0) return null;
		if (coll.size() == 1) return coll.iterator().next();
		
		List<T> list = null;
		if (coll instanceof List<?>)
		{
			list = (List<T>)coll;
		}
		else
		{
			list = new ArrayList<>(coll);
		}
		
		int index = MassiveCore.random.nextInt(list.size());
		return list.get(index);
	}
	
	public static <T> List<T> randomSubset(Collection<T> coll, int count)
	{
		// Clean Input
		if (count < 0) count = 0;
		
		// Create Ret
		List<T> ret = new ArrayList<>(coll);
		while (ret.size() > count)
		{
			int index = MassiveCore.random.nextInt(ret.size());
			ret.remove(index);
		}
		
		// Return Ret
		return ret;
	}
	
	public static <E> List<E> random(List<E> list, int count)
	{
		// Create Ret
		List<E> ret = new MassiveList<>();
		
		// Empty
		if (list.isEmpty()) return ret;
		
		// Fill Ret
		for (int i = 0; i < count; i++)
		{
			ret.add(MUtil.random(list));
		}
		
		// Return Ret
		return ret;
	}
	
	// -------------------------------------------- //
	// EQUALS
	// -------------------------------------------- //
	
	public static boolean equals(Object object1, Object object2)
	{
		if (object1 == null) return object2 == null;
		if (object2 == null) return false;
		
		return object1.equals(object2);
	}


	
	public static boolean equals(Object... objects)
	{
		if (objects == null) throw new NullPointerException("objects");
		if (objects.length % 2 != 0) throw new IllegalArgumentException("objects length not even");
		
		int index = 1;
		while (index < objects.length)
		{
			Object object1 = objects[index - 1];
			Object object2 = objects[index];
			
			if ( ! equals(object1, object2)) return false;
			
			index += 2;
		}
		
		return true;
	}
	
	// -------------------------------------------- //
	// EQUALSISH
	// -------------------------------------------- //
	
	public static final double EQUALSISH_EPSILON = 0.0001;

	public static boolean equalsishObject(Object o1, Object o2)
	{
		if (o1 instanceof Number && o2 instanceof Number)
		{
			return equalsishNumber((Number) o1, (Number) o2);
		}

		return equals(o1, o2);
	}

	public static boolean equalsishNumber(Number number1, Number number2)
	{
		if (number1 == null) return number2 == null;
		if (number2 == null) return false;

		return Math.abs(number1.doubleValue() - number2.doubleValue()) < EQUALSISH_EPSILON;
	}

	/**
	 * @deprecated use equalsishNumber
	 */
	@Deprecated
	public static boolean equalsish(Number number1, Number number2)
	{
		return equalsishNumber(number1, number2);
	}

	// -------------------------------------------- //
	// SET IF DIFFERENT
	// -------------------------------------------- //

	public static <T> boolean setIfDifferent(T value, Supplier<T> getter, Consumer<T> setter)
	{
		T currentVal = getter.get();
		if (currentVal == value) return false;
		setter.accept(value);
		return true;
	}
	
	// -------------------------------------------- //
	// SORTING
	// -------------------------------------------- //
	
	//http://stackoverflow.com/questions/2864840/treemap-sort-by-value
	/*public static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
		SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
			new Comparator<Map.Entry<K,V>>() {
				@Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
					int res = e1.getValue().compareTo(e2.getValue());
					return res != 0 ? res : 1; // Special fix to preserve items with equal values
				}
			}
		);
		sortedEntries.addAll(map.entrySet());
		return sortedEntries;
	}*/
	
	// http://stackoverflow.com/questions/2864840/treemap-sort-by-value
	public static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map)
	{
		return entriesSortedByValues(map, true);
	}
	
	public static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map, final boolean ascending)
	{
		SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<>(
			(e1, e2) -> {
				int res;
				if (ascending)
				{
					res = e1.getValue().compareTo(e2.getValue());
				}
				else
				{
					res = e2.getValue().compareTo(e1.getValue());
				}
				return res != 0 ? res : 1;
			}
		);
		sortedEntries.addAll(map.entrySet());
		return sortedEntries;
	}
	
	// -------------------------------------------- //
	// MATH
	// -------------------------------------------- //

	public static <T extends Number> T limitNumber(T d, T min, T max)
	{
		if (d.doubleValue() < min.doubleValue())
		{
			return min;
		}
		
		if (d.doubleValue() > max.doubleValue())
		{
			return max;
		}
		
		return d;
	}
	
	public static long probabilityRound(double value)
	{
		long ret = (long) Math.floor(value);
		double probability = value % 1;
		if (MassiveCore.random.nextDouble() < probability) ret += 1;
		return ret;
	}
	
	public static int probabilityRound(float value)
	{
		int ret = (int) Math.floor(value);
		float probability = value % 1;
		if (MassiveCore.random.nextFloat() < probability) ret += 1;
		return ret;
	}
	
	public static double round(double value, int places)
	{
		if (places < 0) throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	// -------------------------------------------- //
	// PREDICATE
	// -------------------------------------------- //

	public static <T> Predicate<T> predicatesAnd(Predicate<? super T>... predicates)
	{
		List<Predicate<? super T>> predicateList = Arrays.asList(predicates); // Must be explicit this way
		return predicatesAnd(predicateList);
	}

	public static <T> Predicate<T> predicatesAnd(Collection<Predicate<? super T>> predicates)
	{
		if (predicates.isEmpty()) throw new IllegalArgumentException("isEmpty");

		PredicateAnd<T> predicate = new PredicateAnd<>();
		predicate.predicates = new MassiveList<>(predicates);

		return predicate;
	}

	private static class PredicateAnd<T> implements Predicate<T>
	{
		private List<Predicate<? super T>> predicates;

		@Override
		public boolean test(T t)
		{
			for (Predicate<? super T> predicate : predicates)
			{
				if (predicate.test(t)) continue;
				return false;
			}
			return true;
		}
	}

	// -------------------------------------------- //
	// EXTRACTION
	// -------------------------------------------- //
	
	protected static Map<Class<?>, Map<String, Set<Extractor>>> classesPropertiesExtractors = new HashMap<>();

	protected static Map<String, Set<Extractor>> getPropertiesExtractors(Class<?> propertyClass)
	{
		classesPropertiesExtractors.computeIfAbsent(propertyClass, x -> new HashMap<>());
		return classesPropertiesExtractors.get(propertyClass);
	}
	
	protected static Set<Extractor> getExtractors(Class<?> propertyClass, String propertyName)
	{
		Map<String, Set<Extractor>> propertiesExtractors = getPropertiesExtractors(propertyClass);

		propertiesExtractors.computeIfAbsent(propertyName, x -> new HashSet<>());
		return propertiesExtractors.get(propertyName);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T extract(Class<T> propertyClass, String propertyName, Object o)
	{
		Object ret = null;
		for (Extractor extractor : getExtractors(propertyClass, propertyName))
		{
			ret = extractor.extract(o);
			if (ret != null) break;
		}
		return (T) ret;
	}
	
	public static <T> void registerExtractor(Class<T> clazz, String propertyName, Extractor extractor)
	{
		getExtractors(clazz, propertyName).add(extractor);
	}
	
	// -------------------------------------------- //
	// STATIC CONSTRUCT
	// -------------------------------------------- //
	
	static
	{
		registerExtractor(World.class, "world", ExtractorWorld.get());
		registerExtractor(String.class, "worldName", ExtractorWorldName.get());
		
		// The accountId extractor is used for the money mixin.
		registerExtractor(String.class, "accountId", ExtractorSenderId.get());
	}
	
}
