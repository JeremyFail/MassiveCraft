package com.massivecraft.factions.util;

import com.massivecraft.factions.TerritoryAccess;
import com.massivecraft.factions.entity.Board;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.ps.PS;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Claim-edge wilderness-band search (tier-2 after pure random fails).
 * Picks a random normal claim, walks outward until the chunk is wilderness and at least
 * {@link MConf#wildTpMinChunksFromClaims} from all normal claims, then samples that band.
 */
public final class WildTpClaimEdgeSearch
{
	/**
	 * Prevents instantiation; all API methods are static.
	 */
	private WildTpClaimEdgeSearch()
	{
	}

	/**
	 * Searches near claim edges for wilderness within the remaining attempt budget.
	 *
	 * @param world target world
	 * @param attemptsRemaining shared attempt counter (decremented per sample)
	 * @return valid PS or null if none found before the budget runs out
	 */
	public static PS find(World world, AtomicInteger attemptsRemaining)
	{
		Board board = BoardColl.get().get(world.getName());
		if (board == null) return null;

		List<PS> claimed = new ArrayList<>();
		for (Entry<PS, TerritoryAccess> entry : board.getMap().entrySet())
		{
			Faction faction = entry.getValue().getHostFaction();
			if (faction != null && faction.isNormal())
			{
				claimed.add(entry.getKey().withWorld(world.getName()));
			}
		}
		if (claimed.isEmpty()) return null;

		ThreadLocalRandom random = ThreadLocalRandom.current();
		int minFromClaims = Math.max(0, MConf.get().wildTpMinChunksFromClaims);

		while (attemptsRemaining.get() > 0)
		{
			PS claim = claimed.get(random.nextInt(claimed.size()));
			int cx = claim.getChunkX();
			int cz = claim.getChunkZ();

			int dx = 0;
			int dz = 0;
			int dir = random.nextInt(4);
			if (dir == 0) dx = 1;
			else if (dir == 1) dx = -1;
			else if (dir == 2) dz = 1;
			else dz = -1;

			// Walk outward until wilderness AND far enough from every normal claim
			PS bandChunk = null;
			for (int step = Math.max(1, minFromClaims); step < 256; step++)
			{
				PS at = PS.valueOf(world.getName(), cx + dx * step, cz + dz * step);
				if (!BoardColl.get().getFactionAt(at).isNone()) continue;
				if (!WildTpDestinationValidator.isFarEnoughFromClaims(at)) continue;

				bandChunk = at;
				int extra = random.nextInt(4);
				for (int e = extra; e >= 1; e--)
				{
					PS farther = PS.valueOf(world.getName(), cx + dx * (step + e), cz + dz * (step + e));
					if (!BoardColl.get().getFactionAt(farther).isNone()) continue;
					if (!WildTpDestinationValidator.isFarEnoughFromClaims(farther)) continue;
					bandChunk = farther;
					break;
				}
				break;
			}
			if (bandChunk == null)
			{
				// Avoid burning the whole attempt budget on impossible directions
				if (attemptsRemaining.get() > 0) attemptsRemaining.decrementAndGet();
				continue;
			}

			for (int i = 0; i < 4 && attemptsRemaining.get() > 0; i++)
			{
				attemptsRemaining.decrementAndGet();
				int blockX = (bandChunk.getChunkX() << 4) + random.nextInt(16);
				int blockZ = (bandChunk.getChunkZ() << 4) + random.nextInt(16);
				WildTpSearchUtil.ensureChunkLoaded(world, blockX, blockZ);
				Location loc = WildTpDestinationValidator.resolveTopLocation(world, blockX, blockZ);
				if (loc == null) continue;
				PS ps = PS.valueOf(loc);
				if (WildTpDestinationValidator.isValid(ps, false))
				{
					return ps;
				}
			}
		}
		return null;
	}

}
