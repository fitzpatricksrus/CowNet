package us.fitzpatricksr.cownet;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple utilities for checking block properties and putting down fences.
 */
public class BlockUtils {
    public static final Set<Material> PASS_THROUGH_BLOCKS = new HashSet<Material>(
            Arrays.asList(
                    new Material[]{
                            Material.AIR,
                            Material.WATER,
                            Material.STATIONARY_WATER,
                            Material.LEAVES,
                            Material.WEB,
                            Material.FEATHER,
                            Material.FENCE,
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

    public static Location getHighestLandLocation(Location start) {
        Location loc = start.clone();
        for (int i = 254; i > 0; i--) {
            loc.setY(i);
            if (!BlockUtils.PASS_THROUGH_BLOCKS.contains(loc.getBlock().getType())) {
                return loc;
            }
        }
        return start;
    }

    public static void manageFences(Player p, ProtectedRegion region, boolean addFences) {
        World world = p.getWorld();
        BlockVector minVector = region.getMinimumPoint();
        BlockVector maxVector = region.getMaximumPoint();

        int minX = minVector.getBlockX();
        int minZ = minVector.getBlockZ() + 1;
        int maxX = maxVector.getBlockX();
        int maxZ = maxVector.getBlockZ() + 1;

        // (minX, minZ) -> (maxX, minZ)
        // (minX, maxZ) -> (maxX, maxZ)
        for (int x = minX; x <= maxX; x++) {
            placeFenceAt(world, x, minZ, addFences);
            placeFenceAt(world, x, maxZ, addFences);
        }

        // (minX, minZ) -> (minX, maxZ)
        // (maxX, minZ) -> (maxX, maxZ)
        for (int z = minZ; z <= maxZ; z++) {
            placeFenceAt(world, minX, z, addFences);
            placeFenceAt(world, maxX, z, addFences);
        }
    }

    private static void placeFenceAt(World world, int x, int z, boolean addFences) {
        Location loc = BlockUtils.getHighestLandLocation(new Location(world, x, 0, z));
        loc.setY(loc.getY() + 1);
        Block fenceBlock = world.getBlockAt(loc);
        if (addFences) {
            fenceBlock.setType(Material.FENCE);
            fenceBlock.getState().update(true);
        } else {
            //remove fence if it is there
            Material type = fenceBlock.getType();
            if (type.equals(Material.FENCE) || type.equals(Material.FENCE_GATE)) {
                fenceBlock.setType(Material.AIR);
                fenceBlock.getState().update(true);
            }
        }
    }
}

