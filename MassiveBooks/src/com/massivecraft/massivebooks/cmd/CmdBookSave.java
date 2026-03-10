package com.massivecraft.massivebooks.cmd;

import com.massivecraft.massivebooks.BookUtil;
import com.massivecraft.massivebooks.MassiveBooks;
import com.massivecraft.massivebooks.Lang;
import com.massivecraft.massivebooks.Perm;
import com.massivecraft.massivebooks.cmd.type.TypeBookInHand;
import com.massivecraft.massivebooks.entity.MBook;
import com.massivecraft.massivebooks.entity.MBookColl;
import com.massivecraft.massivebooks.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.util.InventoryUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class CmdBookSave extends MassiveBooksCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdBookSave()
	{
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.SAVE));
		this.addRequirements(RequirementIsPlayer.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return new MassiveList<>(MConf.get().getAliasesBookSave());
	}
	
	@Override
	public void perform() throws MassiveException
	{
		Player player = (Player) sender;

		// Prefer main hand; if both hands hold a book, use main hand.
		ItemStack item = null;
		boolean inMainHand = false;
		ItemStack main = InventoryUtil.getMainHand(player);
		if (main != null && BookUtil.hasBookMeta(main))
		{
			item = main;
			inMainHand = true;
		}
		if (item == null)
		{
			ItemStack offhandItem = InventoryUtil.getOffHand(player);
			if (offhandItem != null && BookUtil.hasBookMeta(offhandItem))
			{
				item = offhandItem;
				inMainHand = false;
			}
		}
		if (item == null)
		{
			// In theory this should never happen... but just in case
			throw new MassiveException().addMessage(TypeBookInHand.getEither().getError());
		}

		String title = BookUtil.getTitle(item);
		if (title == null)
		{
			message(Lang.BOOK_MUST_HAVE_TITLE);
			return;
		}

		MBook mbook = MBookColl.get().get(title, true);
		mbook.setItem(item);
		if (mbook.getBookId() == null) mbook.setBookId(UUID.randomUUID());

		// Put the parsed version (for this viewer) in hand so they see placeholders resolved.
		ItemStack parsed = MassiveBooks.get().processBookPlaceholdersForViewer(mbook.getItem(), player);
		parsed.setAmount(item.getAmount());
		BookUtil.applyMBookMetadata(parsed, mbook);
		if (inMainHand)
			InventoryUtil.setMainHand(player, parsed);
		else
			InventoryUtil.setOffHand(player, parsed);

		message(Lang.getSuccessSave(parsed));
	}

}
