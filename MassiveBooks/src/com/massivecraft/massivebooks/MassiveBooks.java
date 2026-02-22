package com.massivecraft.massivebooks;

import com.massivecraft.massivebooks.cmd.CmdBook;
import com.massivecraft.massivebooks.entity.MBookColl;
import com.massivecraft.massivebooks.entity.MConfColl;
import com.massivecraft.massivebooks.entity.migrator.MigratorMBook001IntIdToString;
import com.massivecraft.massivebooks.entity.migrator.MigratorMBook002BookIdAndType;
import com.massivecraft.massivebooks.integration.placeholderapi.IntegrationPlaceholderAPI;
import com.massivecraft.massivecore.MassivePlugin;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MassiveBooks extends MassivePlugin 
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static MassiveBooks i;
	public static MassiveBooks get() { return i; }
	public MassiveBooks() { MassiveBooks.i = this; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<Class<?>> getClassesActiveMigrators()
	{
		return MUtil.list(
			MigratorMBook001IntIdToString.class,
			MigratorMBook002BookIdAndType.class
		);
	}
	
	@Override
	public void onEnableInner()
	{
		// Activate
		this.activateAuto();
	}
	
	@Override
	public List<Class<?>> getClassesActiveColls()
	{
		return MUtil.list(
			MConfColl.class,
			MBookColl.class
		);
	}
	
	@Override
	public List<Class<?>> getClassesActiveCommands()
	{
		return MUtil.list(
			CmdBook.class
		);
	}
	
	@Override
	public List<Class<?>> getClassesActiveEngines()
	{
		return MUtil.list(
			EngineMain.class,
			EnginePowertool.class
		);
	}
	
	@Override
	public List<Class<?>> getClassesActiveIntegrations()
	{
		return MUtil.list(
			IntegrationPlaceholderAPI.class
		);
	}
	
	/**
	 * Whether PlaceholderAPI integration is active (plugin present and enabled).
	 */
	public boolean isPapiEnabled()
	{
		return IntegrationPlaceholderAPI.get().isIntegrationActive();
	}

	/**
	 * Return a copy of the raw book with placeholders parsed for the given viewer.
	 * When PAPI is active, delegates to PlaceholderAPIProcessor; otherwise returns a clone.
	 *
	 * @param raw    Raw book (e.g. from MBook), may contain unparsed placeholders.
	 * @param viewer The player who will see the book (reader/holder).
	 * @return Parsed copy for this viewer, or clone if PAPI inactive.
	 */
	public ItemStack processBookPlaceholdersForViewer(ItemStack raw, Player viewer)
	{
		if (raw == null || viewer == null) return raw == null ? null : raw.clone();
		ItemStack out;
		if (isPapiEnabled())
			out = com.massivecraft.massivebooks.integration.placeholderapi.PlaceholderAPIProcessor.processBookForViewer(raw, viewer);
		else
			out = raw.clone();
		BookUtil.applyColorCodesToBook(out);
		return out;
	}
}
