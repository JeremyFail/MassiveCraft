package com.massivecraft.factions.cmd.req;

import com.massivecraft.factions.Perm;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementAbstract;
import com.massivecraft.massivecore.util.PermissionUtil;
import org.bukkit.command.CommandSender;

/**
 * Satisfied if the sender has factions.perm.inspect or factions.perm.show (backward compatibility).
 */
public class ReqPermInspectOrShow extends RequirementAbstract
{
	private static final long serialVersionUID = 1L;

	private static final ReqPermInspectOrShow i = new ReqPermInspectOrShow();
	public static ReqPermInspectOrShow get() { return i; }

	@Override
	public boolean apply(CommandSender sender, MassiveCommand command)
	{
		return Perm.PERM_INSPECT.has(sender) || Perm.PERM_SHOW.has(sender);
	}

	@Override
	public String createErrorMessage(CommandSender sender, MassiveCommand command)
	{
		return PermissionUtil.getPermissionDeniedMessage(Perm.PERM_INSPECT.getId());
	}
}
