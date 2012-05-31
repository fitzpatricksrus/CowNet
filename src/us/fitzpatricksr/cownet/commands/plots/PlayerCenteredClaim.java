package us.fitzpatricksr.cownet.commands.plots;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import us.fitzpatricksr.cownet.commands.Plot;

/**
 * Plot centered around where the player is standing.
 */
public class PlayerCenteredClaim implements Plot.AbstractClaim {
	private final Vector sizeVectorOffset;

	public PlayerCenteredClaim(Plot plugin) {
		int gridSizeX = plugin.getConfigValue("plotSize", 64);
		int gridSizeZ = plugin.getConfigValue("plotSize", 64);
		sizeVectorOffset = new Vector(gridSizeX / 2, 0, gridSizeZ / 2);
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
		FenceUtils.manageFences(p, region, true);
	}

	public void dedecorateClaim(Player p, ProtectedRegion region) {
		FenceUtils.manageFences(p, region, false);
	}
}
