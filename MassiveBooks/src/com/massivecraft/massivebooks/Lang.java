package com.massivecraft.massivebooks;

import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.mixin.MixinDisplayName;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Lang
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //
	
	public static final String NEW_PLAYER_COMMANDS_FOR_X = Txt.parse("<i>Running newplayer commands for <h>%s<i>.");
	
	public static final String STATE_POWERTOOL = Txt.parse("POWERTOOL");
	public static final String STATE_UNLOCKED = Txt.parse("UNLOCKED");
	public static final String COLOR_STATE = Txt.parse("<h>");
	public static final String COLOR_SIGNATURE = Txt.parse("<white>");
	public static final String BY_EMPHASIZED = Txt.parse("<em>by");
	public static final String NO_TITLE_COLORED = Txt.parse("<silver><em>No Title");
	public static final String NO_AUTHOR_COLORED = Txt.parse("<silver><em>No Author");
	
	public static final String PERMISSION_DESCRIPTION_COPYCOST_TEMPLATE = Txt.parse("copy a book for the cost %f");
	
	public static final String ACCEPTED_ITEMS_WRITTEN = Txt.parse("a written book");
	public static final String ACCEPTED_ITEMS_QUILL = Txt.parse("a book and quill");
	public static final String ACCEPTED_ITEMS_EITHER = Txt.parse("a written book or a book and quill");
	public static final String BOOK_IN_HAND_ERROR_TEMPLATE = Txt.parse("<b>You must be holding %s.");
	
	public static final String SAME_UNLOCK_X = Txt.parse("%s <g>is already unlocked.");
	public static final String ALTER_UNLOCK_X = Txt.parse("%s <g>is now unlocked.");
	
	public static final String SAME_LOCK_X = Txt.parse("%s <g>is already locked.");
	public static final String ALTER_LOCK_X = Txt.parse("%s <g>is now locked.");
	
	public static final String SAME_CLEAR_X = Txt.parse("%s <g>is already cleared.");
	public static final String ALTER_CLEAR_X = Txt.parse("%s <g>was cleared.");
	
	public static final String SAME_TITLE_X = Txt.parse("<g>The title is already %s<g>.");
	public static final String ALTER_TITLE_X_Y = Txt.parse("<g>Changed title from %s <g>to %s<g>.");
	
	public static final String SAME_AUTHOR_X = Txt.parse("<g>The author is already %s<g>.");
	public static final String ALTER_AUTHOR_X_Y = Txt.parse("<g>Changed author from %s <g>to %s<g>.");
	
	public static final String SUCCESS_LOAD = Txt.parse("<g>Loaded %s<g>.");
	public static final String SUCCESS_SAVE = Txt.parse("<g>Saved %s<g>.");
	public static final String SUCCESS_DELETED = Txt.parse("<g>Deleted %s<g>.");
	public static final String BOOK_MUST_HAVE_TITLE = Txt.parse("<b>The book must have a title.");
	
	public static final String AMOUNT_MUST_BE_POSITIVE = Txt.parse("<b>amount must be positive");
	public static final String X_ALREADY_HAVE_Y = Txt.parse("<i>%s <i>already have %s<i>.");
	public static final String NOT_ENOUGH_ROOM_FOR_X_Y = Txt.parse("<b>Not enough room for <h>%d<b> %s<b>.");
	public static final String X_GAVE_Y_Z_L = Txt.parse("<g>%s <g>gave %s <h>%s <g>%s<g>.");
	
	public static final String AUTOUPDATE_CHANGED_TO_TRUE = Txt.parse("<lime>TRUE<i> Autoupdate is now true.");
	public static final String AUTOUPDATE_CHANGED_TO_FALSE = Txt.parse("<rose>FALSE<i> Autoupdate is now false.");
	public static final String AUTOUPDATE_ALREADY_TRUE = Txt.parse("<lime>TRUE<i> Autoupdate was already true.");
	public static final String AUTOUPDATE_ALREADY_FALSE = Txt.parse("<rose>FALSE<i> Autoupdate was already false.");
	public static final String AUTOUPDATE_JOINWARN = Txt.parse("<rose>WARNING: <i>You have disabled autoupdate for books. You may want to toggle it on.");
	public static final String AUTOUPDATE_DERPWARN = Txt.parse("<i>Since autoupdate of serverbooks is disabled serverwide your personal toggle won't have much effect.");
	
	public static final String TIMES_MUST_BE_POSITIVE = Txt.parse("<b>times must be positive");
	public static final String NOT_ENOUGH_X = Txt.parse("<b>Not enough <h>%s<b>.");
	public static final String FAILED_TO_REMOVE_X = Txt.parse("<b>Failed to remomve %s.");
	public static final String RESOURCE_MONEY = Txt.parse("money");
	public static final String RESOURCE_BOOKS = Txt.parse("books");
	public static final String RESOURCE_INKSACS = Txt.parse("ink sacks");
	public static final String RESOURCE_FEATHERS = Txt.parse("feathers");
	public static final String RESOURCE_ROOM = Txt.parse("inventory space");
	public static final String REQUIRED_X_POSSESSED_Y_MISSING_Z = Txt.parse("<k>Required: <v>%s <k>Possessed: <v>%s <k>Missing: <v>%s");
	public static final String SUCCESS_COPY_COPIES_X = Txt.parse("<g>Created <h>%d <g>copies of the book.");
	public static final String SUCCESS_COPY_RESOURCES_X = Txt.parse("<i>Resources used: %s");
	public static final String SUCCESS_COPY_RESOURCES_ENTRY_X_Y = Txt.parse("<k>%s: <v>%s");
	
	public static final String FRAME_CONTAINS_X = Txt.parse("%s");
	public static final String FRAME_LOAD_X = Txt.parse("%s <i>was loaded");
	public static final String FRAME_UNLOAD_X = Txt.parse("%s <i>was unloaded");
	public static final String FRAME_HELP = Txt.parse("<i>You must hold an <h>empty bookandquil <i>to load the book.");
	
	public static final String POWERTOOL_CHANGED_TO_TRUE = Txt.parse("<lime>TRUE<i> The book is now a powertool.");
	public static final String POWERTOOL_CHANGED_TO_FALSE = Txt.parse("<rose>FALSE<i> The book is no longer a powertool.");
	public static final String POWERTOOL_ALREADY_TRUE = Txt.parse("<lime>TRUE<i> The book is already a powertool.");
	public static final String POWERTOOL_ALREADY_FALSE = Txt.parse("<rose>FALSE<i> The book is already not a powertool.");
	
	public static final String COPYRIGHTED_CHANGED_TO_TRUE = Txt.parse("<lime>TRUE<i> The book is now copyrighted.");
	public static final String COPYRIGHTED_CHANGED_TO_FALSE = Txt.parse("<rose>FALSE<i> The book is no longer copyrighted.");
	public static final String COPYRIGHTED_ALREADY_TRUE = Txt.parse("<lime>TRUE<i> The book is already copyrighted.");
	public static final String COPYRIGHTED_ALREADY_FALSE = Txt.parse("<rose>FALSE<i> The book is already not copyrighted.");
	
	public static final String POWERTOOL_NO_RUNNABLE_LINES = Txt.parse("<b>This powertool does not contain any runnable lines.");
	public static final String POWERTOOL_ISSUE_AT_LINE_X_Y = Txt.parse("<b>Issue at line #%d: %s");
	public static final String POWERTOOL_UNKNOWN_TAG_X = Txt.parse("<b>unknown tag <h>%s");
	public static final String POWERTOOL_REQUIRES_YOU = Txt.parse("<b>This powertool should be used on another player.");
	public static final String POWERTOOL_REQUIRES_BLOCK = Txt.parse("<b>This powertool should be used on a block.");
	public static final String POWERTOOL_RAN_X_Y = Txt.parse("<g>Ran Line #%d: <white>%s");
	public static final String POWERTOOL_FAILED_X_Y_Z = Txt.parse("<b>Failed Line #%d: <white>%s <b>%s");
	
	// -------------------------------------------- //
	// METHODS
	// -------------------------------------------- //
	
	public static String getNewPlayerCommandsForX(Player player, Object watcherObject)
	{
		return String.format(NEW_PLAYER_COMMANDS_FOR_X, MixinDisplayName.get().getDisplayName(player, watcherObject));
	}
	
	public static String descTitle(String title)
	{
		if (title == null) return NO_TITLE_COLORED;
		return COLOR_SIGNATURE+title;
	}
	
	public static String descAuthor(String author)
	{
		if (author == null) return NO_AUTHOR_COLORED;
		if (MConf.get().usingAuthorDisplayName)
		{
			return COLOR_SIGNATURE + MixinDisplayName.get().getDisplayName(author, null); // TODO: We must always view from the perspective of the universe unless we use protocollib or something.
		}
		else
		{
			return COLOR_SIGNATURE + IdUtil.getName(author);
		}
	}
	
	public static String descSignature(ItemStack item)
	{
		String title = BookUtil.getTitle(item);
		String author = BookUtil.getAuthor(item);
		
		String titleDescription = descTitle(title);
		String authorDescription = descAuthor(author);
		
		String glue = " " + COLOR_SIGNATURE + Lang.BY_EMPHASIZED + " ";
		
		return titleDescription + glue + authorDescription;
	}
	
	public static String descDisplayName(ItemStack item)
	{
		boolean powertool = BookUtil.containsFlag(item, Const.POWERTOOL);
		boolean unlocked = BookUtil.isUnlocked(item);
		
		if (BookUtil.isBookMetaEmpty(item) && ! powertool) return null;
		
		String ret = descSignature(item);
		
		if (powertool)
		{
			ret = COLOR_STATE + STATE_POWERTOOL + " " + ret;
		}

		if (unlocked)
		{
			ret = COLOR_STATE + STATE_UNLOCKED + " " + ret;
		}
		
		return ret;
	}
	
	public static String descDisplayNameNeverNull(ItemStack item)
	{
		String ret = descDisplayName(item);
		if (ret == null) ret = Txt.getMaterialName(item.getType());
		return ret;
	}
	
	public static String getSameUnlock(ItemStack current)
	{
		return String.format(Lang.SAME_UNLOCK_X, descDisplayNameNeverNull(current));
	}
	public static String getAlterUnlock(ItemStack before)
	{
		return String.format(Lang.ALTER_UNLOCK_X, descDisplayNameNeverNull(before));
	}
	
	public static String getSameLock(ItemStack current)
	{
		return String.format(Lang.SAME_LOCK_X, descDisplayNameNeverNull(current));
	}
	public static String getAlterLock(ItemStack before)
	{
		return String.format(Lang.ALTER_LOCK_X, descDisplayNameNeverNull(before));
	}
	
	public static String getSameClear(ItemStack current)
	{
		return String.format(Lang.SAME_CLEAR_X, descDisplayNameNeverNull(current));
	}
	public static String getAlterClear(ItemStack before)
	{
		return String.format(Lang.ALTER_CLEAR_X, descDisplayNameNeverNull(before));
	}
	
	public static String getSameTitle(String title)
	{
		return String.format(Lang.SAME_TITLE_X, descTitle(title));
	}
	public static String getAlterTitle(String before, String after)
	{
		return String.format(ALTER_TITLE_X_Y, descTitle(before), descTitle(after));
	}
	
	public static String getSameAuthor(String author)
	{
		return String.format(Lang.SAME_AUTHOR_X, descAuthor(author));
	}
	public static String getAlterAuthor(String before, String after)
	{
		return String.format(ALTER_AUTHOR_X_Y, descAuthor(before), descAuthor(after));
	}
	
	public static String getSuccessLoad(ItemStack item)
	{
		return String.format(Lang.SUCCESS_LOAD, descDisplayNameNeverNull(item));
	}
	public static String getSuccessSave(ItemStack item)
	{
		return String.format(Lang.SUCCESS_SAVE, descDisplayNameNeverNull(item));
	}
	public static String getSuccessDelete(ItemStack item)
	{
		return String.format(Lang.SUCCESS_DELETED, descDisplayNameNeverNull(item));
	}
	
	//
	
	public static String getAlreadyHave(String givee, ItemStack item)
	{
		return String.format(Lang.X_ALREADY_HAVE_Y, givee, descDisplayNameNeverNull(item));
	}
	
	public static String getNotEnoughRoomFor(int amount, ItemStack item)
	{
		return String.format(Lang.NOT_ENOUGH_ROOM_FOR_X_Y, amount, descDisplayNameNeverNull(item));
	}
	
	public static String getGave(String from, String to, int amount, ItemStack item)
	{
		return String.format(Lang.X_GAVE_Y_Z_L, from, to, amount, descDisplayNameNeverNull(item));
	}
	
	//
	
	public static String getSuccessCopyCopies(int i)
	{
		return String.format(Lang.SUCCESS_COPY_COPIES_X, i);
	}
	
	public static String getSuccessCopyResources(double moneyRequired, int booksRequired, int inksacsRequired, int feathersRequired)
	{
		List<String> entries = new ArrayList<>();
		
		if (moneyRequired > 0)
		{
			entries.add(String.format(Lang.SUCCESS_COPY_RESOURCES_ENTRY_X_Y, Lang.RESOURCE_MONEY, moneyRequired));
		}
		
		if (booksRequired > 0)
		{
			entries.add(String.format(Lang.SUCCESS_COPY_RESOURCES_ENTRY_X_Y, Lang.RESOURCE_BOOKS, booksRequired));
		}
		
		if (inksacsRequired > 0)
		{
			entries.add(String.format(Lang.SUCCESS_COPY_RESOURCES_ENTRY_X_Y, Lang.RESOURCE_INKSACS, inksacsRequired));
		}
		
		if (feathersRequired > 0)
		{
			entries.add(String.format(Lang.SUCCESS_COPY_RESOURCES_ENTRY_X_Y, Lang.RESOURCE_FEATHERS, feathersRequired));
		}
		
		return String.format(Lang.SUCCESS_COPY_RESOURCES_X, Txt.implode(entries, " "));
	}

	//
	
	public static String getFrameContains(String displayname)
	{
		return String.format(Lang.FRAME_CONTAINS_X, displayname);
	}
	public static String getFrameLoad(ItemStack item)
	{
		return String.format(Lang.FRAME_LOAD_X, descDisplayNameNeverNull(item));
	}
	public static String getFrameUnload(ItemStack item)
	{
		return String.format(Lang.FRAME_UNLOAD_X, descDisplayNameNeverNull(item));
	}
	public static String getFrameHelp()
	{
		return Lang.FRAME_HELP;
	}
	
	//
	
	public static String getPowertoolIssueAtLine(int lineIndex, String issue)
	{
		return String.format(POWERTOOL_ISSUE_AT_LINE_X_Y, lineIndex, issue);
	}
	public static String getPowertoolUnknownTag(String tag)
	{
		return String.format(POWERTOOL_UNKNOWN_TAG_X, tag);
	}
	public static String getPowertoolRan(int lineIndex, String line)
	{
		return String.format(Lang.POWERTOOL_RAN_X_Y, lineIndex, line);
	}
	public static String getPowertoolRan(int lineIndex, String line, String error)
	{
		return String.format(Lang.POWERTOOL_FAILED_X_Y_Z, lineIndex, line, error);
	}	
	
}
