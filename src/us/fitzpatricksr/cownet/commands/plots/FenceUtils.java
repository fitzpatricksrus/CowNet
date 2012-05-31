package us.fitzpatricksr.cownet.commands.plots;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import us.fitzpatricksr.cownet.utils.BlockUtils;

public class FenceUtils {
	public static void manageFences(Player p, ProtectedRegion region, boolean addFences) {
		World world = p.getWorld();
		BlockVector minVector = region.getMinimumPoint();
		BlockVector maxVector = region.getMaximumPoint();

		int minX = minVector.getBlockX();
		int minZ = minVector.getBlockZ();
		int maxX = maxVector.getBlockX();
		int maxZ = maxVector.getBlockZ();

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