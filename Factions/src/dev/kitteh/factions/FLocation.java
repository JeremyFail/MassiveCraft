package dev.kitteh.factions;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.massivecraft.factions.LegacyApiWarningManager;
import com.massivecraft.factions.util.MiscUtil;

import dev.kitteh.factions.util.LazyLocation;

/**
 * This class attempts to provide minimal compatibility with FactionsUUID by
 * providing wrapper classes/methods that convert to the V2/V3 class structure.
 * 
 * <p><strong>DO NOT USE THIS FOR NEW IMPLEMENTATIONS.</strong></p>
 * 
 * @deprecated
 */
@Deprecated
public class FLocation
{
    
    // TODO: Remove methods that were removed from the legacy API

    private Location location;

    //----------------------------------------------//
    // Constructors
    //----------------------------------------------//
    
    // Preferred constructor
    public FLocation(Location location)
    {
        // Check and warn about legacy API usage
        LegacyApiWarningManager.checkAndWarnLegacyUsage(true);
        
        this.location = location;
    }

    public FLocation(LazyLocation loc)
    {
        this(loc.getLocation());
    }

    public FLocation()
    {
        this("world", 0, 0);
    }

    public FLocation(String worldName, int x, int z) 
    {
        this(new Location(Bukkit.getWorld(worldName), x, 0, z));
    }

    public FLocation(World world, int x, int z) 
    {
        this(new Location(world, x, 0, z));
    }

    public FLocation(Chunk chunk) 
    {
        this(new Location(
            chunk.getWorld(), chunk.getX() * 16, 0, chunk.getZ() * 16));
    }

    public FLocation(Player player)
    {
        this(player.getLocation());
    }

    public FLocation(FPlayer fplayer)
    {
        this(fplayer.getPlayer());
    }

    public FLocation(Block block)
    {
        this(block.getLocation());
    }

    //----------------------------------------------//
    // Getters and Setters
    //----------------------------------------------//
    
    public Location getLocation()
    {
        return this.location;
    }

    // TODO: Add more methods from legacy factions?
    
    public String getWorldName()
    {
        return location.getWorld().getName();
    }

    public World getWorld()
    {
        return location.getWorld();
    }

    public World world()
    {
        return getWorld();
    }

    public Faction faction()
    {
        return Board.getInstance().factionAt(this);
    }

    public long getX()
    {
        return location.getBlockX();
    }

    public long getZ()
    {
        return location.getBlockZ();
    }

    public String getCoordString()
    {
        return "" + getX() + "," + getZ();
    }

    public String asCoordString()
    {
        return getCoordString();
    }

    public Chunk getChunk()
    {
        return location.getChunk();
    }

    public Chunk asChunk()
    {
        return getChunk();
    }

    @Override
    public String toString()
    {
        return "[" + this.getWorldName() + "," + this.getCoordString() + "]";
    }

    public static FLocation fromString(String string)
    {
        int index = string.indexOf(',');
        int start = 1;
        String worldName = string.substring(start, index);
        start = index + 1;
        index = string.indexOf(',', start);
        int x = Integer.parseInt(string.substring(start, index));
        int y = Integer.parseInt(string.substring(index + 1, string.length() - 1));
        return new FLocation(worldName, x, y);
    }

    //----------------------------------------------//
    // Block/Chunk/Region Value Transformation
    //----------------------------------------------//

    // bit-shifting is used because it's much faster than standard division and multiplication
    public static int blockToChunk(int blockVal)
    {
        // 1 chunk is 16x16 blocks
        return blockVal >> 4;   // ">> 4" == "/ 16"
    }

    public static int blockToRegion(int blockVal)
    {
        // 1 region is 512x512 blocks
        return blockVal >> 9;   // ">> 9" == "/ 512"
    }

    public static int chunkToRegion(int chunkVal)
    {
        // 1 region is 32x32 chunks
        return chunkVal >> 5;   // ">> 5" == "/ 32"
    }

    public static int chunkToBlock(int chunkVal)
    {
        return chunkVal << 4;   // "<< 4" == "* 16"
    }

    public static int regionToBlock(int regionVal)
    {
        return regionVal << 9;   // "<< 9" == "* 512"
    }

    public static int regionToChunk(int regionVal)
    {
        return regionVal << 5;   // "<< 5" == "* 32"
    }

    //----------------------------------------------//
    // Misc Geometry
    //----------------------------------------------//

    public FLocation getRelative(int dx, int dz)
    {
        return new FLocation(this.location.getWorld(), this.location.getBlockX() + dx, this.location.getBlockZ() + dz);
    }

    public FLocation relative(int dx, int dz)
    {
        return getRelative(dx, dz);
    }

    public double getDistanceTo(FLocation that)
    {
        double dx = that.location.getBlockX() - this.location.getBlockX();
        double dz = that.location.getBlockZ() - this.location.getBlockZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public double getDistanceSquaredTo(FLocation that)
    {
        double dx = that.location.getBlockX() - this.location.getBlockX();
        double dz = that.location.getBlockZ() - this.location.getBlockZ();
        return dx * dx + dz * dz;
    }

    public boolean isInChunk(Location loc)
    {
        if (loc == null)
        {
            return false;
        }
        Chunk chunk = loc.getChunk();
        return loc.getWorld().getName().equalsIgnoreCase(getWorldName()) 
                && chunk.getX() == this.location.getChunk().getX() 
                && chunk.getZ() == this.location.getChunk().getZ();
    }

    public boolean contains(Location loc)
    {
        return isInChunk(loc);
    }

    public boolean contains(LazyLocation loc)
    {
        return isInChunk(loc.getLocation());
    }

    //----------------------------------------------//
    // Some Geometry
    //----------------------------------------------//
    public Set<FLocation> getCircle(double radius)
    {
        double radiusSquared = radius * radius;

        Set<FLocation> ret = new LinkedHashSet<>();
        if (radius <= 0)
        {
            return ret;
        }

        int xfrom = (int) Math.floor(this.location.getX() - radius);
        int xto = (int) Math.ceil(this.location.getX() + radius);
        int zfrom = (int) Math.floor(this.location.getZ() - radius);
        int zto = (int) Math.ceil(this.location.getZ() + radius);

        for (int x = xfrom; x <= xto; x++)
        {
            for (int z = zfrom; z <= zto; z++)
            {
                FLocation potential = new FLocation(this.location.getWorld(), x, z);
                if (this.getDistanceSquaredTo(potential) <= radiusSquared)
                {
                    ret.add(potential);
                }
            }
        }

        return ret;
    }

    public static HashSet<FLocation> getArea(FLocation from, FLocation to)
    {
        HashSet<FLocation> ret = new HashSet<>();

        for (long x : MiscUtil.range(from.getX(), to.getX()))
        {
            for (long z : MiscUtil.range(from.getZ(), to.getZ()))
            {
                ret.add(new FLocation(from.getWorldName(), (int) x, (int) z));
            }
        }

        return ret;
    }

    //----------------------------------------------//
    // Comparison
    //----------------------------------------------//

    @Override
    public int hashCode() 
    {
        return location.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FLocation)) {
            return false;
        }

        FLocation that = (FLocation) obj;
        return that.getLocation().equals(this.getLocation());
    }

}
