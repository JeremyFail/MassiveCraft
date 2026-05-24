package com.massivecraft.factions.cmd.req;

import com.massivecraft.factions.Perm;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementAbstract;
import com.massivecraft.massivecore.util.PermissionUtil;
import org.bukkit.command.CommandSender;

/**
 * Satisfied if the sender has factions.flag.view or factions.flag.show (backward compatibility).
 */
public class ReqFlagViewOrShow extends RequirementAbstract
{
	private static final long serialVersionUID = 1L;

	private static final ReqFlagViewOrShow i = new ReqFlagViewOrShow();
	public static ReqFlagViewOrShow get() { return i; }

	@Override
	public boolean apply(CommandSender sender, MassiveCommand command)
	{
		return Perm.FLAG_VIEW.has(sender) || Perm.FLAG_SHOW.has(sender);
	}

	@Override
	public String createErrorMessage(CommandSender sender, MassiveCommand command)
	{
		return PermissionUtil.getPermissionDeniedMessage(Perm.FLAG_VIEW.getId());
	}
}
