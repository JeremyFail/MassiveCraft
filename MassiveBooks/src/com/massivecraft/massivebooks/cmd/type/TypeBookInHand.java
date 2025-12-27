package com.massivecraft.massivebooks.cmd.type;

import com.massivecraft.massivebooks.Lang;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.TypeAbstract;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.InventoryUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class TypeBookInHand extends TypeAbstract<ItemStack>
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	public static final TypeBookInHand WRITTEN = new TypeBookInHand(true, false);
	public static TypeBookInHand getWritten() { return WRITTEN; }
	
	public static final TypeBookInHand QUILL = new TypeBookInHand(false, true);
	public static TypeBookInHand getQuill() { return QUILL; }
	
	public static final TypeBookInHand EITHER = new TypeBookInHand(true, true);
	public static TypeBookInHand getEither() { return EITHER; }
	
	private TypeBookInHand(boolean acceptingWrittenBook, boolean acceptingBookAndQuill)
	{
		super(ItemStack.class);
		this.acceptingWrittenBook = acceptingWrittenBook;
		this.acceptingBookAndQuill = acceptingBookAndQuill;
	}
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private final boolean acceptingWrittenBook;
	public boolean isAcceptingWrittenBook() { return this.acceptingWrittenBook; }
	
	private final boolean acceptingBookAndQuill;
	public boolean isAcceptingBookAndQuill() { return this.acceptingBookAndQuill; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public String getVisualInner(ItemStack value, CommandSender sender)
	{
		return Txt.getItemName(value);
	}

	@Override
	public ItemStack read(String arg, CommandSender sender) throws MassiveException
	{
		ItemStack ret = this.getItemStack(sender);
		if (ret != null) return ret;
		
		throw new MassiveException().addMessage(this.getError());
	}

	@Override
	public Collection<String> getTabList(CommandSender sender, String arg)
	{
		return null;
	}

	// -------------------------------------------- //
	// EXTRAS
	// -------------------------------------------- //
	
	public String getAcceptedItemsDesc()
	{
		if (this.acceptingWrittenBook && this.acceptingBookAndQuill) return Lang.ACCEPTED_ITEMS_EITHER;
		if (this.acceptingWrittenBook) return Lang.ACCEPTED_ITEMS_WRITTEN;
		return Lang.ACCEPTED_ITEMS_QUILL;
	}
	
	public String getError()
	{
		return String.format(Lang.BOOK_IN_HAND_ERROR_TEMPLATE, this.getAcceptedItemsDesc());
	}
	
	public ItemStack getItemStack(CommandSender sender)
	{
		Player player = IdUtil.getAsPlayer(sender);
		if (player == null) return null;
		
		ItemStack ret = InventoryUtil.getMainHand(player);
		if (ret == null) return null;
		
		Material type = ret.getType();
		if (type == Material.WRITTEN_BOOK && this.isAcceptingWrittenBook()) return ret;
		if (type == Material.WRITABLE_BOOK && this.isAcceptingBookAndQuill()) return ret;
		
		return null;
	}

	

}
