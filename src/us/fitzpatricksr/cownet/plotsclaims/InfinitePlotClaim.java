package us.fitzpatricksr.cownet.plotsclaims;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import uk.co.jacekk.bukkit.infiniteplots.InfinitePlotsGenerator;
import us.fitzpatricksr.cownet.Plots;

/**
 * Plots for InfinitePlots generated worlds
 */
public class InfinitePlotClaim implements Plots.AbstractClaim {
    private int roadOffsetX = 4;
    private int roadOffsetZ = 4;
    private int walkwaySize = 7; // width of walkway between plots, this is not configurable in InfinitePlots so I'm not going to make it configurable in this plugin.
    private WorldGuardPlugin wgp;

    public InfinitePlotClaim(WorldGuardPlugin wg) {
        this.wgp = wg;
    }

    public ProtectedRegion defineClaim(Player p, String name) {
        Location loc = p.getLocation();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        World w = p.getWorld();
        ChunkGenerator cg = w.getGenerator();
        double plotSize = ((InfinitePlotsGenerator)cg).getPlotSize();
        double move = plotSize+walkwaySize;

        x = (int)(Math.floor((x-roadOffsetX)/move)*move+roadOffsetX);
        z = (int)(Math.floor((z-roadOffsetZ)/move)*move+roadOffsetZ);

        BlockVector min = new BlockVector(x, 0, z);
        BlockVector max = new BlockVector(x + (plotSize - 1), 255, z + (plotSize - 1));

        return new ProtectedCuboidRegion(name, min, max);
    }

    public void constructClaim(Player p, String name) {
    }

}