package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.req.ReqBankCommandsEnabled;

public class CmdFactionsMoney extends FactionsCommand
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public CmdFactionsMoneyBalance cmdMoneyBalance = new CmdFactionsMoneyBalance();
	public CmdFactionsMoneyDeposit cmdMoneyDeposit = new CmdFactionsMoneyDeposit();
	public CmdFactionsMoneyWithdraw cmdMoneyWithdraw = new CmdFactionsMoneyWithdraw();
	public CmdFactionsMoneyTransferF2f cmdMoneyTransferFf = new CmdFactionsMoneyTransferF2f();
	public CmdFactionsMoneyTransferF2p cmdMoneyTransferFp = new CmdFactionsMoneyTransferF2p();
	public CmdFactionsMoneyTransferP2f cmdMoneyTransferPf = new CmdFactionsMoneyTransferP2f();
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsMoney()
	{
		// Requirements
		this.addRequirements(ReqBankCommandsEnabled.get());
	}
	
}
