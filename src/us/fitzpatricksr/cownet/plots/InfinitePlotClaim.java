package us.fitzpatricksr.cownet.plots;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.generator.ChunkGenerator;
import us.fitzpatricksr.cownet.BlockUtils;
import us.fitzpatricksr.cownet.Plots;

/**
 * Plots for InfinitePlots generated worlds
 */
public class InfinitePlotClaim implements Plots.AbstractClaim {
    private int roadOffsetX = 4;
    private int roadOffsetZ = 4;
    private int walkwaySize = 7; // width of walkway between plots, this is not configurable in InfinitePlots so I'm not going to make it configurable in this plugin.
    private int plotSize;
    
    public InfinitePlotClaim(int plotSize) {
        this.plotSize = plotSize;
    }

    public ProtectedRegion defineClaim(Player p, String name) {
        Location loc = p.getLocation();
        double x = loc.getX();
        double z = loc.getZ();
        double move = plotSize+walkwaySize;

        x = (int)(Math.floor((x-roadOffsetX)/move)*move+roadOffsetX);
        z = (int)(Math.floor((z-roadOffsetZ)/move)*move+roadOffsetZ);

        BlockVector min = new BlockVector(x, 0, z);
        BlockVector max = new BlockVector(x + (plotSize - 1), 255, z + (plotSize - 1));

        return new ProtectedCuboidRegion(name, min, max);
    }

    public void decorateClaim(Player p, ProtectedRegion region) {
        BlockUtils.manageFences(p, region, true);
    }

    public void dedecorateClaim(Player p, ProtectedRegion region) {
        BlockUtils.manageFences(p, region, false);
    }
}