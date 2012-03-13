package us.fitzpatricksr.cownet.plots;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import us.fitzpatricksr.cownet.BlockUtils;
import us.fitzpatricksr.cownet.Plots;

/**
 * Plots centered around where the player is standing.
 */
public class PlayerCenteredClaim implements Plots.AbstractClaim {
    private final Vector sizeVectorOffset;

    public PlayerCenteredClaim(Plots plugin) {
        int gridSizeX = plugin.getConfigInt("plotSize", 64);
        int gridSizeZ = plugin.getConfigInt("plotSize", 64);
        sizeVectorOffset = new Vector(gridSizeX /2, 0, gridSizeZ /2);
    }

    public ProtectedRegion defineClaim(Player p, String name) {
        Location loc = p.getLocation();
        Vector minVector = loc.toVector().subtract(sizeVectorOffset);
        Vector maxVector = loc.toVector().add(sizeVectorOffset);

        BlockVector min = new BlockVector(minVector.getX(), 1, minVector.getZ());
        BlockVector max = new BlockVector(maxVector.getX(), 255, maxVector.getZ());
        return new ProtectedCuboidRegion(name, min, max);
    }

    public void decorateClaim(Player p, ProtectedRegion region) {
        BlockUtils.manageFences(p, region, true);
    }

    public void dedecorateClaim(Player p, ProtectedRegion region) {
        BlockUtils.manageFences(p, region, false);
    }
}
