package us.fitzpatricksr.cownet.plotsclaims;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import us.fitzpatricksr.cownet.Plots;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Plots centered around where the player is standing.
 */
public class PlayerCenteredClaim implements Plots.AbstractClaim {
    private static final Set<Material> PASS_THROUGH = new HashSet<Material>(
            Arrays.asList(
                    new Material[]{
                            Material.AIR,
                            Material.WATER,
                            Material.STATIONARY_WATER,
                            Material.LEAVES,
                            Material.WEB,
                            Material.FENCE,
                            Material.FEATHER,
                            Material.FENCE_GATE,
                            Material.LONG_GRASS,
                            Material.DEAD_BUSH,
                            Material.WOOL,
                            Material.YELLOW_FLOWER,
                            Material.RED_ROSE,
                            Material.BROWN_MUSHROOM,
                            Material.RED_MUSHROOM,
                            Material.TORCH,
                            Material.FIRE,
                            Material.CROPS,
                            Material.SNOW,
                            Material.ICE,
                            Material.SNOW_BLOCK,
                            Material.CACTUS,
                            Material.SUGAR_CANE_BLOCK,
                            Material.PUMPKIN,
                            Material.JACK_O_LANTERN,
                            Material.CAKE_BLOCK,
                            Material.MONSTER_EGGS,
                            Material.HUGE_MUSHROOM_1,
                            Material.HUGE_MUSHROOM_2,
                            Material.MELON_BLOCK,
                            Material.PUMPKIN_STEM,
                            Material.MELON_STEM,
                            Material.VINE,
                            Material.WATER_LILY
                    }));
    private final Vector sizeVectorOffset;

    public PlayerCenteredClaim(Plots plugin) {
        int gridSizeX = plugin.getConfigInt("gridSizeX", 64);
        int gridSizeZ = plugin.getConfigInt("gridSizeZ", 64);
        sizeVectorOffset = new Vector(gridSizeX /2, 0, gridSizeZ /2);
    }

    public ProtectedRegion defineClaim(Player p, String name) {
        Location loc = p.getLocation();
        Vector minVector = getMinLocation(loc);
        Vector maxVector = getMaxLocation(loc);

        BlockVector min = new BlockVector(minVector.getX(), 0, minVector.getZ());
        BlockVector max = new BlockVector(maxVector.getX(), 255, maxVector.getZ());
        return new ProtectedCuboidRegion(name, min, max);
    }

    public void constructClaim(Player p, String name) {
        Location loc = p.getLocation();
        World world = p.getWorld();
        Vector minVector = getMinLocation(loc);
        Vector maxVector = getMaxLocation(loc);

        int minX = minVector.getBlockX();
        int minZ = minVector.getBlockZ();
        int maxX = maxVector.getBlockX();
        int maxZ = maxVector.getBlockZ();

        // (minX, minZ) -> (maxX, minZ)
        // (minX, maxZ) -> (maxX, maxZ)
        for (int x = minX; x <= maxX; x++) {
            placeFenceAt(world, x, minZ);
            placeFenceAt(world, x, maxZ);
        }

        // (minX, minZ) -> (minX, maxZ)
        // (maxX, minZ) -> (maxX, maxZ)
        for (int z = minZ; z <= maxZ; z++) {
            placeFenceAt(world, minX, z);
            placeFenceAt(world, maxX, z);
        }
    }

    private void placeFenceAt(World world, int x, int z) {
        Location loc = new Location(world, x, 0, z);
        for (int i = 254; i > 0; i--) {
            loc.setY(i);
            if (!PASS_THROUGH.contains(loc.getBlock().getType())) {
                loc.setY(loc.getY()+1);
                Block fenceBlock = world.getBlockAt(loc);
                fenceBlock.setType(Material.FENCE);
                fenceBlock.getState().update(true);
                return;
            }
        }
    }

    private Vector getMinLocation(Location l) {
        return l.toVector().subtract(sizeVectorOffset);
    }

    private Vector getMaxLocation(Location l) {
        return l.toVector().add(sizeVectorOffset);
    }
}
