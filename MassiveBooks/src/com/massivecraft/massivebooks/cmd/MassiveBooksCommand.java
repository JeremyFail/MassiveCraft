package com.massivecraft.massivebooks.cmd;

import com.massivecraft.massivebooks.entity.MPlayer;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.Visibility;

public abstract class MassiveBooksCommand extends MassiveCommand
{
	public MPlayer mme;
	
	@Override
	public void senderFields(boolean set)
	{
		this.mme = set ? MPlayer.get(this.sender) : null;
	}
	
	public MassiveBooksCommand()
	{
		this.setVisibility(Visibility.SECRET);
	}
}