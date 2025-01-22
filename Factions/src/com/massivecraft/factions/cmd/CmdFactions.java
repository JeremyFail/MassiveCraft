package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.command.MassiveCommandDeprecated;
import com.massivecraft.massivecore.command.MassiveCommandVersion;

import java.util.List;

public class CmdFactions extends FactionsCommand
{
	// -------------------------------------------- //
	// INSTANCE
	// -------------------------------------------- //
	
	private static CmdFactions i = new CmdFactions();
	public static CmdFactions get() { return i; }
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //

	public CmdFactionsDocumentation cmdFactionsDocumentation = new CmdFactionsDocumentation();
	public CmdFactionsList cmdFactionsList = new CmdFactionsList();
	public CmdFactionsFaction cmdFactionsFaction = new CmdFactionsFaction();
	public CmdFactionsPlayer cmdFactionsPlayer = new CmdFactionsPlayer();
	public CmdFactionsStatus cmdFactionsStatus = new CmdFactionsStatus();
	public CmdFactionsJoin cmdFactionsJoin = new CmdFactionsJoin();
	public CmdFactionsLeave cmdFactionsLeave = new CmdFactionsLeave();
	public CmdFactionsWarp cmdFactionsWarp = new CmdFactionsWarp();
	public CmdFactionsHome cmdFactionsHome = new CmdFactionsHome();
	public CmdFactionsSethome cmdFactionsSethome = new CmdFactionsSethome();
	public CmdFactionsUnsethome cmdFactionsUnsethome = new CmdFactionsUnsethome();
	public CmdFactionsVote cmdFactionsVote = new CmdFactionsVote();
	public CmdFactionsMap cmdFactionsMap = new CmdFactionsMap();
	public CmdFactionsCreate cmdFactionsCreate = new CmdFactionsCreate();
	public CmdFactionsName cmdFactionsName = new CmdFactionsName();
	public CmdFactionsDescription cmdFactionsDescription = new CmdFactionsDescription();
	public CmdFactionsMotd cmdFactionsMotd = new CmdFactionsMotd();
	public CmdFactionsInvite cmdFactionsInvite = new CmdFactionsInvite();
	public CmdFactionsKick cmdFactionsKick = new CmdFactionsKick();
	public CmdFactionsTitle cmdFactionsTitle = new CmdFactionsTitle();
	public CmdFactionsRank cmdFactionsRank = new CmdFactionsRank();
	public CmdFactionsMoney cmdFactionsMoney = new CmdFactionsMoney();
	public CmdFactionsTop cmdFactionsTop = new CmdFactionsTop();
	public CmdFactionsSeeChunk cmdFactionsSeeChunk = new CmdFactionsSeeChunk();
	public CmdFactionsSeeChunkOld cmdFactionsSeeChunkOld = new CmdFactionsSeeChunkOld();
	public CmdFactionsTerritorytitles cmdFactionsTerritorytitles = new CmdFactionsTerritorytitles();
	public CmdFactionsClaim cmdFactionsClaim = new CmdFactionsClaim();
	public CmdFactionsUnclaim cmdFactionsUnclaim = new CmdFactionsUnclaim();
	public CmdFactionsAccess cmdFactionsAccess = new CmdFactionsAccess();
	public CmdFactionsChunkname cmdFactionsChunkname = new CmdFactionsChunkname();
	public CmdFactionsRelation cmdFactionsRelation = new CmdFactionsRelation();
	public CmdFactionsRelationOld cmdFactionsRelationOldAlly = new CmdFactionsRelationOld("ally");
	public CmdFactionsRelationOld cmdFactionsRelationOldTruce = new CmdFactionsRelationOld("truce");
	public CmdFactionsRelationOld cmdFactionsRelationOldNeutral = new CmdFactionsRelationOld("neutral");
	public CmdFactionsRelationOld cmdFactionsRelationOldEnemy = new CmdFactionsRelationOld("enemy");
	public CmdFactionsTax cmdFactionsTax = new CmdFactionsTax();
	public CmdFactionsPerm cmdFactionsPerm = new CmdFactionsPerm();
	public CmdFactionsFlag cmdFactionsFlag = new CmdFactionsFlag();
	public CmdFactionsFly cmdFactionsFly = new CmdFactionsFly();
	public CmdFactionsUnstuck cmdFactionsUnstuck = new CmdFactionsUnstuck();
	public CmdFactionsOverride cmdFactionsOverride = new CmdFactionsOverride();
	public CmdFactionsDisband cmdFactionsDisband = new CmdFactionsDisband();
	public CmdFactionsPowerboost cmdFactionsPowerBoost = new CmdFactionsPowerboost();
	public CmdFactionsSetpower cmdFactionsSetpower = new CmdFactionsSetpower();
	public CmdFactionsMoneyconvert cmdFactionsMoneyconvert = new CmdFactionsMoneyconvert();
	public CmdFactionsConfig cmdFactionsConfig = new CmdFactionsConfig();
	public CmdFactionsClean cmdFactionsClean = new CmdFactionsClean();
	public MassiveCommandVersion cmdFactionsVersion = new MassiveCommandVersion(Factions.get());
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactions()
	{
		// Old rank stuff
		this.addChild(new CmdFactionsRankOld("demote"));
		this.addChild(new CmdFactionsRankOld("promote"));

		// Deprecated Commands
		this.addChild(new MassiveCommandDeprecated(this.cmdFactionsRank, "leader", "owner", "officer", "moderator", "coleader"));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return MConf.get().aliasesF;
	}

}
