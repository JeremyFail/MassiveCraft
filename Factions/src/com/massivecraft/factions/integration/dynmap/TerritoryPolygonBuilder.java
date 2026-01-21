package com.massivecraft.factions.integration.dynmap;

import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.ps.PS;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper for building Dynmap territory polygons (outer boundary with holes).
 *
 * <p>
 * This class encapsulates an "etch-a-sketch" algorithm used by
 * {@link EngineDynmap} so the engine can remain focused on scheduling and
 * Dynmap API interaction.
 * </p>
 */
public final class TerritoryPolygonBuilder
{
	private TerritoryPolygonBuilder() {}

	/**
	 * Builds a final polygon for a contiguous territory region:
	 * outer boundary plus any interior holes with cutouts.
	 * 
	 * <p>
	 * This is the main entry point for converting a set of claimed chunks into
	 * a single polygon suitable for Dynmap rendering.
     * </p>
     * <p>
     * <strong>The algorithm:</strong>
	 * <ol>
	 * <li>Traces the outer boundary clockwise using wall-following</li>
	 * <li>Detects interior holes using diagonal flood-fill</li>
	 * <li>Groups diagonally-connected holes into clusters</li>
	 * <li>Creates perpendicular cutouts from outer boundary to each cluster</li>
	 * <li>Returns a single continuous polygon path</li>
	 * </ol>
	 * </p>
	 * 
	 * @param polygonChunks Set of contiguous claimed chunks
	 * @return Single polygon as list of corner coordinates, or empty list if input is null/empty
	 */
	public static List<PS> buildPolygon(Set<PS> polygonChunks)
	{
		if (polygonChunks == null || polygonChunks.isEmpty()) return new MassiveList<>();
		List<List<PS>> polygonWithHoles = getPolygonWithHoles(polygonChunks);
		if (polygonWithHoles.isEmpty()) return new MassiveList<>();
		if (polygonWithHoles.size() == 1) return new MassiveList<>(polygonWithHoles.get(0));
		return createPolygonWithCutouts(polygonWithHoles);
	}

	// ------------------------------------------------------------------------
	// CORE PIPELINE
	// ------------------------------------------------------------------------

	/**
	 * Gets a polygon with holes, including the outer boundary and any interior holes.
	 * 
	 * <p>
	 * <ol>
	 * <li>Traces the outer boundary clockwise</li>
	 * <li>Identifies all unclaimed chunks within the bounding box</li>
	 * <li>Groups unclaimed chunks using diagonal-aware flood fill</li>
	 * <li>Filters out groups touching the bounding box (exterior connections)</li>
	 * <li>Splits interior groups into orthogonal components</li>
	 * <li>Traces each component boundary counter-clockwise</li>
	 * </ol>
	 * </p>
	 * 
	 * @param polygonChunks Set of contiguous claimed chunks
	 * @return List where index [0] is outer boundary, [1..N] are hole boundaries
	 */
	private static List<List<PS>> getPolygonWithHoles(Set<PS> polygonChunks)
	{
		List<List<PS>> result = new MassiveList<>();
		
		// 1. Trace outer boundary (clockwise)
		List<PS> outerBoundary = getLineList(polygonChunks);
		result.add(outerBoundary);
		
		// 2. Find bounding box to search for holes
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
		for (PS chunk : polygonChunks)
		{
			int x = chunk.getChunkX();
			int z = chunk.getChunkZ();
			if (x < minX) minX = x;
			if (x > maxX) maxX = x;
			if (z < minZ) minZ = z;
			if (z > maxZ) maxZ = z;
		}
		
		// 3. Find all unclaimed chunks within the bounding box
		Set<PS> potentialHoles = new MassiveSet<>();
		for (int x = minX; x <= maxX; x++)
		{
			for (int z = minZ; z <= maxZ; z++)
			{
				PS chunk = PS.valueOf(x, z);
				if (!polygonChunks.contains(chunk))
				{
					potentialHoles.add(chunk);
				}
			}
		}
		
		// 4. Group unclaimed chunks into separate holes using DIAGONAL-AWARE flood fill
		// This treats kiddy-corner chunks as part of the same hole for the
		// purpose of deciding whether the unclaimed area connects to the
		// exterior (touches the bounding box). After we know a diagonal group
		// is a true interior hole, we further split it into orthogonal
		// components so that shapes consisting only of diagonal connections
		// (like two squares touching at a corner) still produce separate
		// hole boundaries and all interior voids are carved out.
		while (!potentialHoles.isEmpty())
		{
			Set<PS> hole = new MassiveSet<>();
			PS start = potentialHoles.iterator().next();
			floodFillDiagonal(potentialHoles, hole, start);
			
			// Check if this diagonal group touches the bounding box edge (not
			// a real hole - it connects to outside via at least a diagonal
			// path). In that case we skip the entire group.
			if (touchesBoundary(hole, minX, maxX, minZ, maxZ))
			{
				continue;
			}
			
			// This is a true interior hole region. It may, however, consist of
			// several orthogonally-disconnected components that are only
			// connected diagonally (kiddy-corner). We want each of those
			// components to produce its own hole boundary so that all voids
			// are removed from the final filled area.
			for (Set<PS> component : splitOrthogonalComponents(hole))
			{
				if (component.isEmpty()) continue;
				List<PS> holeBoundary = getLineListForHole(component);
				if (!holeBoundary.isEmpty()) result.add(holeBoundary);
			}
		}
		
		return result;
	}

	// ------------------------------------------------------------------------
	// HOLE DETECTION HELPERS
	// ------------------------------------------------------------------------

	/**
	 * Performs an 8-directional flood fill to group diagonally-connected chunks.
	 * 
	 * <p>
	 * This treats "kiddy corner" chunks (touching only at a corner) as connected,
	 * which is essential for identifying hole regions that should be considered
	 * as a single logical group even when only diagonally adjacent.
	 * </p>
	 * 
	 * @param source Set of chunks to search through (modified: chunks are removed as discovered)
	 * @param destination Set to populate with connected chunks (modified: chunks are added)
	 * @param start Starting chunk for the flood fill
	 */
	private static void floodFillDiagonal(Set<PS> source, Set<PS> destination, PS start)
	{
		ArrayDeque<PS> stack = new ArrayDeque<>();
		stack.push(start);
		
		while (!stack.isEmpty())
		{
			PS next = stack.pop();
			if (!source.remove(next)) continue;
			
			destination.add(next);
			
			int x = next.getChunkX();
			int z = next.getChunkZ();
			
			// Check all 8 neighbors (orthogonal + diagonal)
			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dz = -1; dz <= 1; dz++)
				{
					if (dx == 0 && dz == 0) continue; // Skip self
					
					PS adjacent = PS.valueOf(x + dx, z + dz);
					if (source.contains(adjacent))
					{
						stack.push(adjacent);
					}
				}
			}
		}
	}

	/**
	 * Splits a set of chunks into 4-directionally connected components.
	 * 
	 * <p>
	 * After determining that a diagonal group is a true interior hole (using
	 * {@link #floodFillDiagonal}), we split it into orthogonal components.
	 * This ensures that shapes made of diagonally-connected squares (e.g.,
	 * two holes touching only at a corner) produce separate hole boundaries,
	 * so all interior voids are properly carved out.
	 * </p>
	 * 
	 * @param chunks Diagonally-connected set of chunks to split
	 * @return List of orthogonally-connected components
	 */
	private static List<Set<PS>> splitOrthogonalComponents(Set<PS> chunks)
	{
		List<Set<PS>> components = new MassiveList<>();
		Set<PS> remaining = new MassiveSet<>(chunks);
		
		while (!remaining.isEmpty())
		{
			Set<PS> component = new MassiveSet<>();
			PS start = remaining.iterator().next();
			
			ArrayDeque<PS> stack = new ArrayDeque<>();
			stack.push(start);
			
			while (!stack.isEmpty())
			{
				PS next = stack.pop();
				if (!remaining.remove(next)) continue;
				
				component.add(next);
				
				int x = next.getChunkX();
				int z = next.getChunkZ();
				
				PS[] orthogonal = new PS[] {
					PS.valueOf(x + 1, z),
					PS.valueOf(x - 1, z),
					PS.valueOf(x, z + 1),
					PS.valueOf(x, z - 1)
				};
				for (PS adj : orthogonal)
				{
					if (remaining.contains(adj)) stack.push(adj);
				}
			}
			
			components.add(component);
		}
		
		return components;
	}

	/**
	 * Checks if any chunk in the set touches the bounding box edge.
	 * 
	 * <p>
	 * Used to filter out unclaimed regions that connect to the exterior.
	 * If an unclaimed group touches the bounding box, it's not a true
	 * interior hole and should be ignored.
	 * </p>
	 * 
	 * @param chunks Set of chunks to check
	 * @param minX Minimum X coordinate of bounding box
	 * @param maxX Maximum X coordinate of bounding box
	 * @param minZ Minimum Z coordinate of bounding box
	 * @param maxZ Maximum Z coordinate of bounding box
	 * @return true if any chunk is on the bounding box edge
	 */
	private static boolean touchesBoundary(Set<PS> chunks, int minX, int maxX, int minZ, int maxZ)
	{
		for (PS chunk : chunks)
		{
			int x = chunk.getChunkX();
			int z = chunk.getChunkZ();
			if (x == minX || x == maxX || z == minZ || z == maxZ)
			{
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------------
	// CUTOUT + CLUSTER PIPELINE
	// ------------------------------------------------------------------------

	/**
	 * Creates a single polygon with cutouts connecting to holes using the "Etch-a-Sketch" algorithm.
	 * 
	 * <p>
	 * <ol>
	 * <li>Build edge list from outer boundary</li>
	 * <li>Map all hole corners for shared-corner detection</li>
	 * <li>Group holes into clusters (holes sharing corners = one cluster)</li>
	 * <li>Find shortest perpendicular cutout for each cluster</li>
	 * <li>Sort cutouts by position along outer boundary</li>
	 * <li>Walk outer boundary, inserting cutouts and hole traversals</li>
	 * </ol>
	 * </p>
	 * 
	 * @param polygonWithHoles List where [0] is outer boundary, [1..N] are holes
	 * @return Single continuous polygon with holes connected via cutouts
	 */
	private static List<PS> createPolygonWithCutouts(List<List<PS>> polygonWithHoles)
	{
		List<PS> outer = polygonWithHoles.get(0);
		
		if (polygonWithHoles.size() == 1)
		{
			return new MassiveList<>(outer);
		}
		
		List<Edge> outerEdges = buildEdgeList(outer);
		
		Map<PS, List<HoleCornerRef>> holeCornerMap = buildHoleCornerMap(polygonWithHoles);
		
		// Group holes that share corners into clusters.
		List<Set<Integer>> holeClusters = new MassiveList<>();
		Set<Integer> unassigned = new MassiveSet<>();
		for (int holeIdx = 1; holeIdx < polygonWithHoles.size(); holeIdx++)
		{
			if (!polygonWithHoles.get(holeIdx).isEmpty()) unassigned.add(holeIdx);
		}
		
		while (!unassigned.isEmpty())
		{
			Set<Integer> cluster = new MassiveSet<>();
			ArrayDeque<Integer> queue = new ArrayDeque<>();
			
			Integer startHole = unassigned.iterator().next();
			queue.add(startHole);
			unassigned.remove(startHole);
			cluster.add(startHole);
			
			while (!queue.isEmpty())
			{
				int holeIdx = queue.pop();
				List<PS> hole = polygonWithHoles.get(holeIdx);
				for (PS corner : hole)
				{
					List<HoleCornerRef> refs = holeCornerMap.get(corner);
					if (refs == null) continue;
					for (HoleCornerRef ref : refs)
					{
						int otherIdx = ref.holeIndex;
						if (otherIdx <= 0) continue; // skip outer boundary
						if (!unassigned.remove(otherIdx)) continue;
						cluster.add(otherIdx);
						queue.add(otherIdx);
					}
				}
			}
			
			holeClusters.add(cluster);
		}
		
		// For each cluster, find a single best cutout to the outer boundary.
		List<CutoutConnection> connections = new MassiveList<>();
		for (Set<Integer> cluster : holeClusters)
		{
			CutoutConnection connection = findShortestPerpendicularConnectionForCluster(outer, outerEdges, polygonWithHoles, cluster);
			if (connection == null)
			{
				EngineDynmap.logSevere("Failed to find connection for hole cluster - skipping");
				continue;
			}
			connections.add(connection);
		}
		
		// Sort connections in traversal order along the outer boundary.
		connections.sort((a, b) -> {
			int cmp = Integer.compare(a.outerEdgeIndex, b.outerEdgeIndex);
			if (cmp != 0) return cmp;
			return Double.compare(a.distanceAlongEdge, b.distanceAlongEdge);
		});
		
		return buildPolygonWithCutouts(outer, polygonWithHoles, connections, holeCornerMap);
	}

	// ------------------------------------------------------------------------
	// BOUNDARY TRACING (OUTER + HOLE)
	// ------------------------------------------------------------------------

	private enum Direction { XPLUS, ZPLUS, XMINUS, ZMINUS }

	/**
	 * Finds the chunk with the lexicographically smallest (X, Z) coordinates.
	 * 
	 * <p>
	 * This is used as the guaranteed starting point for wall-following algorithms,
	 * ensuring consistent and deterministic polygon tracing.
	 * </p>
	 * 
	 * @param pss Set of chunks to search
	 * @return Chunk at the minimum (X, Z) position
	 */
	private static PS getMinimum(Set<PS> pss)
	{
		int minimumX = Integer.MAX_VALUE;
		int minimumZ = Integer.MAX_VALUE;

		for (PS chunk : pss)
		{
			int chunkX = chunk.getChunkX();
			int chunkZ = chunk.getChunkZ();

			if (chunkX < minimumX)
			{
				minimumX = chunkX;
				minimumZ = chunkZ;
			}
			else if (chunkX == minimumX && chunkZ < minimumZ)
			{
				minimumZ = chunkZ;
			}
		}
		return PS.valueOf(minimumX, minimumZ);
	}

	/**
	 * Traces the boundary of a hole counter-clockwise using right-hand wall-following.
	 * 
	 * <p>
	 * For holes, we need a counter-clockwise path (opposite of outer boundary).
	 * This is achieved by treating the hole chunks as "solid" and tracing around them.
	 * Includes iteration limiting to detect and log potential infinite loops.
	 * </p>
	 * 
	 * @param holeChunks Set of unclaimed chunks forming the hole
	 * @return List of corner positions forming the hole outline (counter-clockwise)
	 */
	private static List<PS> getLineListForHole(Set<PS> holeChunks)
	{
		PS minimumChunk = getMinimum(holeChunks);

		int initialX = minimumChunk.getChunkX();
		int initialZ = minimumChunk.getChunkZ();
		int currentX = initialX;
		int currentZ = initialZ;

		Direction direction = Direction.XPLUS;
		List<PS> linelist = new MassiveList<>();

		linelist.add(minimumChunk); // Add start point
		
		int iterations = 0;
		int maxIterations = holeChunks.size() * 4 + 100;
		
		// Wall-follow until we return to start position facing south (ZMINUS)
		while (((currentX != initialX) || (currentZ != initialZ) || (direction != Direction.ZMINUS)) && iterations < maxIterations)
		{
			iterations++;
			
			switch (direction)
			{
				case XPLUS: // Segment in X+ Direction
					if (!holeChunks.contains(PS.valueOf(currentX + 1, currentZ)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX + 1, currentZ)); // Finish line
						direction = Direction.ZPLUS; // Change direction
					}
					else if (!holeChunks.contains(PS.valueOf(currentX + 1, currentZ - 1)))
					{ // Straight?
						currentX++;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX + 1, currentZ)); // Finish line
						direction = Direction.ZMINUS;
						currentX++;
						currentZ--;
					}
					break;
				case ZPLUS: // Segment in Z+ Direction
					if (!holeChunks.contains(PS.valueOf(currentX, currentZ + 1)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX + 1, currentZ + 1)); // Finish line
						direction = Direction.XMINUS; // Change direction
					}
					else if (!holeChunks.contains(PS.valueOf(currentX + 1, currentZ + 1)))
					{ // Straight?
						currentZ++;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX + 1, currentZ + 1)); // Finish line
						direction = Direction.XPLUS;
						currentX++;
						currentZ++;
					}
					break;
				case XMINUS: // Segment in X- Direction
					if (!holeChunks.contains(PS.valueOf(currentX - 1, currentZ)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX, currentZ + 1)); // Finish line
						direction = Direction.ZMINUS; // Change direction
					}
					else if (!holeChunks.contains(PS.valueOf(currentX - 1, currentZ + 1)))
					{ // Straight?
						currentX--;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX, currentZ + 1)); // Finish line
						direction = Direction.ZPLUS;
						currentX--;
						currentZ++;
					}
					break;
				case ZMINUS: // Segment in Z- Direction
					if (!holeChunks.contains(PS.valueOf(currentX, currentZ - 1)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX, currentZ)); // Finish line
						direction = Direction.XPLUS; // Change direction
					}
					else if (!holeChunks.contains(PS.valueOf(currentX - 1, currentZ - 1)))
					{ // Straight?
						currentZ--;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX, currentZ)); // Finish line
						direction = Direction.XMINUS;
						currentX--;
						currentZ--;
					}
					break;
			}
		}
		
		if (iterations >= maxIterations)
		{
			EngineDynmap.logSevere("getLineListForHole exceeded maximum iterations - possible infinite loop detected for hole at " + minimumChunk);
		}

		return linelist;
	}

	/**
	 * Traces the outer boundary of claimed chunks clockwise using right-hand wall-following.
	 * 
	 * <p>
	 * This algorithm walks around the perimeter starting from the minimum (X, Z) position.
	 * At each step, it checks three directions:
	 * <ul>
	 * <li>Right turn: Adjacent chunk in current direction is unclaimed</li>
	 * <li>Straight: Adjacent chunk is claimed, but diagonal-left is unclaimed</li>
	 * <li>Left turn: Both adjacent and diagonal-left chunks are claimed</li>
	 * </ul>
	 * The result is a clockwise list of corner coordinates.
	 * </p>
	 * 
	 * @param polygonChunks Set of contiguous claimed chunks
	 * @return List of corner positions forming the outer boundary (clockwise)
	 */
	private static List<PS> getLineList(Set<PS> polygonChunks)
	{
		PS minimumChunk = getMinimum(polygonChunks);

		int initialX = minimumChunk.getChunkX();
		int initialZ = minimumChunk.getChunkZ();
		int currentX = initialX;
		int currentZ = initialZ;

		Direction direction = Direction.XPLUS;
		List<PS> linelist = new MassiveList<>();

		linelist.add(minimumChunk); // Add start point
		
		while ((currentX != initialX) || (currentZ != initialZ) || (direction != Direction.ZMINUS))
		{
			switch (direction)
			{
				case XPLUS: // Segment in X+ Direction
					if (!polygonChunks.contains(PS.valueOf(currentX + 1, currentZ)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX + 1, currentZ)); // Finish line
						direction = Direction.ZPLUS; // Change direction
					}
					else if (!polygonChunks.contains(PS.valueOf(currentX + 1, currentZ - 1)))
					{ // Straight?
						currentX++;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX + 1, currentZ)); // Finish line
						direction = Direction.ZMINUS;
						currentX++;
						currentZ--;
					}
					break;
				case ZPLUS: // Segment in Z+ Direction
					if (!polygonChunks.contains(PS.valueOf(currentX, currentZ + 1)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX + 1, currentZ + 1)); // Finish line
						direction = Direction.XMINUS; // Change direction
					}
					else if (!polygonChunks.contains(PS.valueOf(currentX + 1, currentZ + 1)))
					{ // Straight?
						currentZ++;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX + 1, currentZ + 1)); // Finish line
						direction = Direction.XPLUS;
						currentX++;
						currentZ++;
					}
					break;
				case XMINUS: // Segment in X- Direction
					if (!polygonChunks.contains(PS.valueOf(currentX - 1, currentZ)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX, currentZ + 1)); // Finish line
						direction = Direction.ZMINUS; // Change direction
					}
					else if (!polygonChunks.contains(PS.valueOf(currentX - 1, currentZ + 1)))
					{ // Straight?
						currentX--;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX, currentZ + 1)); // Finish line
						direction = Direction.ZPLUS;
						currentX--;
						currentZ++;
					}
					break;
				case ZMINUS: // Segment in Z- Direction
					if (!polygonChunks.contains(PS.valueOf(currentX, currentZ - 1)))
					{ // Right turn?
						linelist.add(PS.valueOf(currentX, currentZ)); // Finish line
						direction = Direction.XPLUS; // Change direction
					}
					else if (!polygonChunks.contains(PS.valueOf(currentX - 1, currentZ - 1)))
					{ // Straight?
						currentZ--;
					}
					else
					{ // Left turn
						linelist.add(PS.valueOf(currentX, currentZ)); // Finish line
						direction = Direction.XMINUS;
						currentX--;
						currentZ--;
					}
					break;
			}
		}

		return linelist;
	}

	// ------------------------------------------------------------------------
	// EDGE + CUTOUT HELPERS
	// ------------------------------------------------------------------------

	/**
	 * Represents an edge (line segment) of a polygon boundary.
	 * Used for perpendicular intersection calculations when finding cutout paths.
	 */
	private static class Edge
	{
		final PS start;
		final PS end;
		final int edgeIndex;  // Index of start corner in the boundary list
		final boolean isHorizontal;  // true if horizontal (same Z), false if vertical (same X)
		
		Edge(PS start, PS end, int edgeIndex)
		{
			this.start = start;
			this.end = end;
			this.edgeIndex = edgeIndex;
			this.isHorizontal = (start.getChunkZ() == end.getChunkZ());
		}
		
		/**
		 * Checks if a perpendicular line from a point intersects this edge.
		 * 
		 * @param point Starting point for the perpendicular line
		 * @param horizontal If true, check horizontal line from point; if false, check vertical
		 * @return Intersection point on this edge, or null if no intersection
		 */
		PS getPerpendicularIntersection(PS point, boolean horizontal)
		{
			int px = point.getChunkX();
			int pz = point.getChunkZ();
			
			if (horizontal)
			{
				// Horizontal ray can only intersect vertical.edge
				if (this.isHorizontal) return null;
				
				int edgeX = this.start.getChunkX();
				int minZ = Math.min(this.start.getChunkZ(), this.end.getChunkZ());
				int maxZ = Math.max(this.start.getChunkZ(), this.end.getChunkZ());
				
				if (pz >= minZ && pz <= maxZ)
				{
					return PS.valueOf(edgeX, pz);
				}
			}
			else
			{
				// Vertical ray can only intersect horizontal edge
				if (!this.isHorizontal) return null;
				
				int edgeZ = this.start.getChunkZ();
				int minX = Math.min(this.start.getChunkX(), this.end.getChunkX());
				int maxX = Math.max(this.start.getChunkX(), this.end.getChunkX());
				
				if (px >= minX && px <= maxX)
				{
					return PS.valueOf(px, edgeZ);
				}
			}
			
			return null;
		}
	}
	
	/**
	 * Builds a list of edges from a polygon boundary.
	 * 
	 * <p>
	 * Each edge connects consecutive corners in the boundary, with the last
	 * edge wrapping around to connect the final corner back to the first.
	 * </p>
	 * 
	 * @param boundary List of corner positions forming a closed polygon
	 * @return List of edges with geometry information
	 */
	private static List<Edge> buildEdgeList(List<PS> boundary)
	{
		List<Edge> edges = new MassiveList<>();
		for (int i = 0; i < boundary.size(); i++)
		{
			PS start = boundary.get(i);
			PS end = boundary.get((i + 1) % boundary.size());
			edges.add(new Edge(start, end, i));
		}
		return edges;
	}
	
	/**
	 * Reference to a specific corner within a hole boundary.
	 * Used to track which holes share common corners.
	 */
	private static class HoleCornerRef
	{
		final int holeIndex;  // Index of the hole in polygonWithHoles [1..N]
		
		HoleCornerRef(int holeIndex, int cornerIndex)
		{
			this.holeIndex = holeIndex;
		}
	}
	
	/**
	 * Builds a map from corner position to all hole boundaries that include that corner.
	 * 
	 * <p>
	 * This map is used to detect when multiple holes share a corner (kiddy-corner
	 * connection), which allows the algorithm to traverse all connected holes via
	 * a single cutout path.
	 * </p>
	 * 
	 * @param polygonWithHoles List where [0] is outer, [1..N] are holes
	 * @return Map from corner position to list of (holeIndex, cornerIndex) references
	 */
	private static Map<PS, List<HoleCornerRef>> buildHoleCornerMap(List<List<PS>> polygonWithHoles)
	{
		Map<PS, List<HoleCornerRef>> ret = new MassiveMap<>();
		for (int holeIdx = 1; holeIdx < polygonWithHoles.size(); holeIdx++)
		{
			List<PS> hole = polygonWithHoles.get(holeIdx);
			for (int ci = 0; ci < hole.size(); ci++)
			{
				PS corner = hole.get(ci);
				ret.computeIfAbsent(corner, k -> new MassiveList<>()).add(new HoleCornerRef(holeIdx, ci));
			}
		}
		return ret;
	}
	
	/**
	 * Finds the shortest perpendicular cutout from a hole cluster to the outer boundary.
	 * 
	 * <p>
	 * For each corner of every hole in the cluster, checks four perpendicular
	 * directions (horizontal left/right, vertical up/down) and finds where each
	 * intersects an outer edge. Returns the connection with minimal distance.
	 * </p>
	 * 
	 * @param outer Outer boundary corners
	 * @param outerEdges Edge list for the outer boundary
	 * @param polygonWithHoles List where [0] is outer, [1..N] are holes
	 * @param holeIndices Set of hole indices forming the cluster
	 * @return Best cutout connection, or null if none found
	 */
	private static CutoutConnection findShortestPerpendicularConnectionForCluster(List<PS> outer, List<Edge> outerEdges, List<List<PS>> polygonWithHoles, Set<Integer> holeIndices)
	{
		CutoutConnection best = null;
		int bestDistance = Integer.MAX_VALUE;
		
		for (int holeIndex : holeIndices)
		{
			List<PS> hole = polygonWithHoles.get(holeIndex);
			for (int holeCornerIdx = 0; holeCornerIdx < hole.size(); holeCornerIdx++)
			{
				PS holeCorner = hole.get(holeCornerIdx);
				int hx = holeCorner.getChunkX();
				int hz = holeCorner.getChunkZ();
				
				// Check all 4 perpendicular directions from this hole corner:
				// 0 = horizontal left, 1 = horizontal right, 2 = vertical up, 3 = vertical down
				for (int dir = 0; dir < 4; dir++)
				{
					boolean horizontal = (dir < 2);
					boolean positive = (dir % 2 == 1);
					
					PS closestIntersection = null;
					int closestDistance = Integer.MAX_VALUE;
					Edge closestEdge = null;
					
					for (Edge edge : outerEdges)
					{
						PS intersection = edge.getPerpendicularIntersection(holeCorner, horizontal);
						if (intersection == null) continue;
						
						int ix = intersection.getChunkX();
						int iz = intersection.getChunkZ();
						
						int distance;
						boolean correctDirection;
						
						if (horizontal)
						{
							distance = Math.abs(ix - hx);
							correctDirection = positive ? (ix > hx) : (ix < hx);
						}
						else
						{
							distance = Math.abs(iz - hz);
							correctDirection = positive ? (iz > hz) : (iz < hz);
						}
						
						if (correctDirection && distance > 0 && distance < closestDistance)
						{
							closestDistance = distance;
							closestIntersection = intersection;
							closestEdge = edge;
						}
					}
					
					if (closestIntersection != null && closestDistance < bestDistance)
					{
						bestDistance = closestDistance;
						best = new CutoutConnection();
						best.holePoint = holeCorner;
						best.holeIndex = holeIndex;
						best.outerPoint = closestIntersection;
						best.outerEdgeIndex = closestEdge.edgeIndex;
						best.clusterHoleIndices = new MassiveSet<>(holeIndices);
						
						if (closestEdge.isHorizontal)
						{
							best.distanceAlongEdge = Math.abs(closestIntersection.getChunkX() - closestEdge.start.getChunkX());
						}
						else
						{
							best.distanceAlongEdge = Math.abs(closestIntersection.getChunkZ() - closestEdge.start.getChunkZ());
						}
						
						best.isOnEdge = !outer.contains(closestIntersection);
						if (!best.isOnEdge)
						{
							best.outerIndex = outer.indexOf(closestIntersection);
						}
						else
						{
							best.outerIndex = closestEdge.edgeIndex;
						}
					}
				}
			}
		}
		
		return best;
	}
	
	/**
	 * Builds the final polygon by walking the outer boundary and inserting cutouts to holes.
	 * 
	 * <p>
	 * This implements the "Etch-a-Sketch" approach: walk the outer boundary once,
	 * and whenever we encounter a cutout connection point, traverse into the hole
	 * cluster (recursively visiting all connected holes), then return to continue
	 * the outer boundary.
	 * </p>
	 * 
	 * @param outer Outer boundary corners
	 * @param polygonWithHoles List where [0] is outer, [1..N] are holes
	 * @param connections Sorted list of cutout connections
	 * @param holeCornerMap Map for detecting shared corners between holes
	 * @return Single continuous polygon path
	 */
	private static List<PS> buildPolygonWithCutouts(List<PS> outer, List<List<PS>> polygonWithHoles, List<CutoutConnection> connections, Map<PS, List<HoleCornerRef>> holeCornerMap)
	{
		List<PS> result = new MassiveList<>();
		int connectionIdx = 0;
		
		for (int i = 0; i < outer.size(); i++)
		{
			PS currentCorner = outer.get(i);
			result.add(currentCorner);
			
			while (connectionIdx < connections.size())
			{
				CutoutConnection conn = connections.get(connectionIdx);
				
				if (conn.outerEdgeIndex != i && conn.outerIndex != i)
				{
					break;
				}
				
				if (!conn.isOnEdge && conn.outerIndex == i)
				{
					result.addAll(drawCutoutToCluster(conn, polygonWithHoles, holeCornerMap));
					connectionIdx++;
					continue;
				}
				
				if (conn.isOnEdge && conn.outerEdgeIndex == i)
				{
					result.add(conn.outerPoint);
					result.addAll(drawCutoutToCluster(conn, polygonWithHoles, holeCornerMap));
					connectionIdx++;
					continue;
				}
				
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Draws a cutout path from the outer boundary into a hole cluster and back.
	 * 
	 * <p>
	 * Generates the sequence of points to:
	 * <ol>
	 * <li>Enter the cluster at the cutout connection point</li>
	 * <li>Recursively traverse all holes in the cluster via shared corners</li>
	 * <li>Return to the entry point</li>
	 * <li>Return to the outer boundary edge point (if cutout originated on an edge)</li>
	 * </ol>
	 * </p>
	 * 
	 * @param conn Cutout connection defining entry point and cluster
	 * @param polygonWithHoles List where [0] is outer, [1..N] are holes
	 * @param holeCornerMap Map for detecting shared corners
	 * @return List of points to add to the polygon (not including current outer position)
	 */
	private static List<PS> drawCutoutToCluster(CutoutConnection conn, List<List<PS>> polygonWithHoles, Map<PS, List<HoleCornerRef>> holeCornerMap)
	{
		List<PS> result = new MassiveList<>();
		Set<Integer> visitedHoles = new MassiveSet<>();
		
		// Draw to the entry point for the cluster
		result.add(conn.holePoint);
		
		// Walk the root hole and recursively any connected holes that
		// share corners with it, all using a single cutout.
		traverseHoleCluster(conn.holeIndex, conn.holePoint, conn.clusterHoleIndices, visitedHoles, result, polygonWithHoles, holeCornerMap);
		
		// Return to the entry point to complete the cluster walk
		result.add(conn.holePoint);
		
		// If we came from an edge point, add that too
		if (conn.isOnEdge)
		{
			result.add(conn.outerPoint);
		}
		
		return result;
	}
	
	/**
	 * Recursively traverses a hole cluster starting from a given hole and corner.
	 * 
	 * <p>
	 * This method walks around a hole boundary counter-clockwise, and whenever it
	 * encounters a corner shared with another unvisited hole in the same cluster,
	 * it recursively traverses that hole first. This creates a depth-first traversal
	 * through all connected holes, using a single cutout path.
	 * </p>
	 * 
	 * @param holeIndex Index of the current hole to traverse
	 * @param startCorner Corner to begin traversal from
	 * @param clusterHoleIndices All hole indices in this cluster
	 * @param visitedHoles Set tracking which holes have been traversed (modified)
	 * @param out Output list to append corner positions to (modified)
	 * @param polygonWithHoles List where [0] is outer, [1..N] are holes
	 * @param holeCornerMap Map for detecting shared corners
	 */
	private static void traverseHoleCluster(int holeIndex, PS startCorner, Set<Integer> clusterHoleIndices, Set<Integer> visitedHoles,
									 List<PS> out, List<List<PS>> polygonWithHoles, Map<PS, List<HoleCornerRef>> holeCornerMap)
	{
		if (!clusterHoleIndices.contains(holeIndex)) return;
		if (!visitedHoles.add(holeIndex)) return; // already walked
		
		List<PS> hole = polygonWithHoles.get(holeIndex);
		int startIdx = hole.indexOf(startCorner);
		if (startIdx < 0) return; // safety
		
		// Rotate hole boundary so startCorner is first, then walk remaining corners
		List<PS> rotated = rotateToStart(hole, startIdx);
		int n = rotated.size();
		
		// Walk counter-clockwise from index 1 (skip startCorner, already added by caller)
		for (int i = 1; i < n; i++)
		{
			PS corner = rotated.get(i);
			out.add(corner);
			
			List<HoleCornerRef> refs = holeCornerMap.get(corner);
			if (refs == null) continue;
			
			for (HoleCornerRef ref : refs)
			{
				int otherHoleIndex = ref.holeIndex;
				if (otherHoleIndex == holeIndex) continue;
				if (!clusterHoleIndices.contains(otherHoleIndex)) continue;
				if (visitedHoles.contains(otherHoleIndex)) continue;
				
				// Recursively traverse the connected hole
				traverseHoleCluster(otherHoleIndex, corner, clusterHoleIndices, visitedHoles, out, polygonWithHoles, holeCornerMap);
				
				// After returning from the recursive traversal, we are conceptually
				// back at this shared corner, so we re-add it to keep the path
				// continuous before continuing around the current hole.
				out.add(corner);
			}
		}
	}
	
	/**
	 * Rotates a list so the element at the specified index becomes the first element.
	 * 
	 * <p>
	 * Treats the list as circular, maintaining order. Used to reorient hole
	 * boundaries to start from a specific corner without changing the sequence.
	 * </p>
	 * 
	 * @param list List to rotate
	 * @param startIndex Index that should become position 0
	 * @return New list with elements rotated
	 */
	private static List<PS> rotateToStart(List<PS> list, int startIndex)
	{
		if (list.isEmpty() || startIndex == 0) return new MassiveList<>(list);
		
		List<PS> result = new MassiveList<>();
		for (int i = 0; i < list.size(); i++)
		{
			result.add(list.get((startIndex + i) % list.size()));
		}
		return result;
	}
	
	/**
	 * Stores information about a cutout connection between the outer boundary and a hole cluster.
	 * Contains geometry for the cutout path and identifies which holes are traversed via this cutout.
	 */
	private static class CutoutConnection
	{
		PS outerPoint;  // Point on outer boundary where cutout connects
		int outerIndex;  // Index of the corner (if outerPoint is a corner)
		int outerEdgeIndex;  // Index of the edge (for sorting when outerPoint is on an edge)
		double distanceAlongEdge;  // Distance from edge start to outerPoint
		boolean isOnEdge;  // True if outerPoint is on an edge, false if at a corner
		PS holePoint;  // Entry point into the hole cluster
		int holeIndex;  // Index of the first hole to traverse
		Set<Integer> clusterHoleIndices;  // All holes reachable via shared corners (including holeIndex)
	}
}
