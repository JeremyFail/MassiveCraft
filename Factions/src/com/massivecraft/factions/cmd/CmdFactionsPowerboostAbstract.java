package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.FactionsParticipator;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.Type;
import com.massivecraft.massivecore.command.type.primitive.TypeDouble;
import com.massivecraft.massivecore.util.Txt;

public abstract class CmdFactionsPowerboostAbstract extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	protected CmdFactionsPowerboostAbstract(Type<? extends FactionsParticipator> parameterType, String parameterName)
	{
		// Parameters
		this.addParameter(parameterType, parameterName);
		if (!this.getClass().getSimpleName().contains("Show"))
		{
			this.addParameter(TypeDouble.get(), "amount");
		}
	}

	// -------------------------------------------- //
	// ABSTRACT
	// -------------------------------------------- //

	public abstract double calcNewPowerboost(double current, double d);

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Parameters
		FactionsParticipator factionsParticipator = this.readArg();

		boolean updated = false;
		// Try set the powerBoost
		if (this.argIsSet(1))
		{
			// Yes updated
			updated = true;

			// Calc powerboost
			double current = factionsParticipator.getPowerBoost();
			double number = this.readArg();
			double powerBoost = this.calcNewPowerboost(current, number);

			// Set
			factionsParticipator.setPowerBoost(powerBoost);
		}
		
		// Inform
		this.informPowerBoost(factionsParticipator, updated);
	}

	private void informPowerBoost(FactionsParticipator factionsParticipator, boolean updated)
	{
		// Prepare
		Double powerBoost = factionsParticipator.getPowerBoost();
		String participatorDescribe = factionsParticipator.describeTo(msender, true);
		String powerDescription = Txt.parse(Double.compare(powerBoost, 0D) >= 0 ? "<g>bonus" : "<b>penalty");
		String when = updated ? "now " : "";
		String verb = factionsParticipator.equals(msender) ? "have" : "has";
		
		// Create message
		String messagePlayer = Txt.parse("<i>%s<i> %s%s a power %s<i> of <h>%.2f<i> to min and max power levels.", participatorDescribe, when, verb, powerDescription, powerBoost);
		String messageLog = Txt.parse("%s %s set the power %s<i> for %s<i> to <h>%.2f<i>.", msender.getName(), verb, powerDescription, factionsParticipator.getName(), powerBoost);
		
		// Inform
		msender.message(messagePlayer);
		if (updated) Factions.get().log(messageLog);
	}
	
}
