package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeMPermable;
import com.massivecraft.factions.entity.Board;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPerm.MPermable;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.mixin.MixinWorld;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.ps.PSFormatHumanSpace;
import com.massivecraft.massivecore.util.Txt;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class CmdFactionsAccessInspect extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsAccessInspect()
	{
		// Parameters
		this.addParameter(TypeMPermable.get(), "rank/rel/player/faction");
		this.addParameter(TypeFaction.get(), "faction", "your");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Parameter
		Faction faction = this.readArgAt(1, msenderFaction);
		MPermable mpermable = TypeMPermable.get(faction).read(this.argAt(0), sender);

		String factionId = faction.getId();

		// Check if they have access perms, unless they are checking for their own access
		if (mpermable != msender && mpermable != msenderFaction && mpermable != msender.getRank())
		{
			if ( ! MPerm.getPermAccess().has(msender, faction, true)) return;
		}

		// Turn into id->chunks
		// And filter the ones that are empty
		Map<String, Set<PS>> world2Chunks = new MassiveMap<>();
		for (Board board : BoardColl.get().getAll())
		{
			String worldId = board.getId();
			Set<PS> chunks = board.getMap().entrySet().stream()
				.filter(e -> e.getValue().getHostFactionId().equals(factionId))
				.filter(e -> e.getValue().isGranted(mpermable))
				.map(Entry::getKey)
				.collect(Collectors.toSet());
			if ( ! chunks.isEmpty()) world2Chunks.put(worldId, chunks);
		}

		if (world2Chunks.isEmpty())
		{
			msg("%s <i>has no special access in <reset>%s<i>.", mpermable.getDisplayName(msender), faction.describeTo(msender));
			return;
		}

		msg("%s <i>has special access in <reset>%s <i>in the following chunks:", mpermable.getDisplayName(msender), faction.describeTo(msender));

		for (Entry<String, Set<PS>> entry : world2Chunks.entrySet())
		{
			String worldId = entry.getKey();
			Set<PS> chunks = entry.getValue();

			String worldName = MixinWorld.get().getWorldDisplayName(worldId);

			// Remove world from chunks
			List<String> chunkNames = chunks.stream()
						 .map(PS::getChunkCoords)
						 .map(PSFormatHumanSpace.get()::format)
						 .collect(Collectors.toList());

			String chunkDesc = Txt.implodeCommaAnd(chunkNames, Txt.parse("<i>"));

			msg("%s<i> (%d): <reset>%s", worldName, chunks.size(), chunkDesc);
		}
	}
	
}
