package com.massivecraft.factions.cmd;

import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MFlagColl;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.util.Txt;

import java.util.List;
import java.util.stream.Collectors;

public class CmdFactionsDocumentationFlags extends FactionsCommandDocumentation
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsDocumentationFlags()
	{

	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		msgDoc("Flags are a way to give certain factions certain attributes " +
				   " such as disabling pvp or enabling friendly fire.");
		msgDoc("To see all the flags type:");
		message(CmdFactions.get().cmdFactionsFlag.cmdFactionsFlagList.getTemplate(false, true, sender));

		List<String> flags = MFlagColl.get().getAll(MFlag::isEditable).stream().map(flag -> Txt.parse("<h>%s", flag.getName())).collect(Collectors.toList());
		String str = Txt.implodeCommaAndDot(flags, Txt.parse("<i>"));
		msgDoc("The flags you can edit for your faction are: %s", str);
	}
	
}
