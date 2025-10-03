package com.massivecraft.factions.cmd;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Visibility;
import com.massivecraft.massivecore.command.type.primitive.TypeStringConfirmation;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.util.ConfirmationUtil;

public class CmdFactionsMoneyconvert extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsMoneyconvert()
	{
		// Parameters
		this.addParameter(TypeStringConfirmation.get(), "confirmation", "");

		// Low priority
		this.setPriority(-100);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public Visibility getVisibility()
	{
		//return Visibility.INVISIBLE;
		return MConf.get().useNewMoneySystem ? Visibility.INVISIBLE : Visibility.SECRET;
	}

	@Override
	public void perform() throws MassiveException
	{
		if (MConf.get().useNewMoneySystem)
		{
			throw new MassiveException().addMsg("<b>The economy system is already converted.");
		}

		// Args
		if (!this.argIsSet(0))
		{
			msg("<i>Money in Factions used to be stored within the applicable economy plugin." +
					" This is problematic because not all economy plugins support that." +
					" This command allows to convert to the new system where the money of a Faction" +
					" is stored within the Factions plugin. Then all economy plugins can be used with Factions.");
		}

		ConfirmationUtil.tryConfirm(this);

		MConf.get().useNewMoneySystem = true;

		for (Faction f : FactionColl.get().getAll())
		{
			if (!Money.exists(f))
			{
				msg("<h>%s <i>does not have any money.", f.getName());
				continue;
			}

			double money = Money.get(f);
			f.setMoney(money);

			Money.set(f, null, 0);

			msg("<h>%s <i>has <h>%s <i> and has been converted.", f.getName(), Money.format(money));
		}
		msg("<i>Converted all factions. Hooray!");
	}
	
}
