package us.fitzpatricksr.cownet;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import cosine.boseconomy.BOSEconomy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetThingy;
import us.fitzpatricksr.cownet.utils.SchematicUtils;
import us.fitzpatricksr.cownet.utils.StringUtils;
import us.fitzpatricksr.cownet.utils.TimeUtils;

import java.io.File;

public class Snapshot extends CowNetThingy {
	@Setting
	private long resetInterval = 60;  // in seconds

	public Snapshot(JavaPlugin plugin, String permissionRoot) {
		super(plugin, permissionRoot);
	}

	@Override
	public void onEnable() {
		// start the countdown
		debugInfo("String countdown timer");
		performCountdown(0);
	}

	@Override
	protected String getHelpString(CommandSender sender) {
		return "usage: snapshot [ 'delete' | 'reset' | 'create > ]";
	}

	@CowCommand(permission = "release")
	private boolean doDelete(Player player) {
		return false;
	}

	@CowCommand
	private boolean doUnsnapshot(Player player) {
		World world = player.getWorld();
		RegionManager regionManager = getWorldGuard().getRegionManager(world);
		ApplicableRegionSet regions = regionManager.getApplicableRegions(player.getLocation());
		if (regions.size() == 0) {
			player.sendMessage("You are not in a WorldGuard region.  Nothing to snapshot.");
		} else if (regions.size() > 1) {
			player.sendMessage("You are multiple regions:");
			for (ProtectedRegion region : regions) {
				player.sendMessage("  " + region.getId());
			}
		} else {
			ProtectedRegion region = regions.iterator().next();
			String regionName = region.getId();
			if (!region.isOwner(player.getName())) {
				player.sendMessage("You do not own this region: " + regionName);
			} else {
				logInfo("Taking snapshot of " + regionName);
				File schematicFile = new File(getSchematicsFolder(), regionName + ".schematic");
				if (schematicFile.exists()) {
					schematicFile.delete();
				}
				debugInfo("  file: " + schematicFile);
				logInfo("Snapshot removed.");
			}
		}
		return true;
	}

	@CowCommand
	private boolean doSnapshot(Player player) {
		World world = player.getWorld();
		RegionManager regionManager = getWorldGuard().getRegionManager(world);
		ApplicableRegionSet regions = regionManager.getApplicableRegions(player.getLocation());
		if (regions.size() == 0) {
			player.sendMessage("You are not in a WorldGuard region.  Nothing to snapshot.");
		} else if (regions.size() > 1) {
			player.sendMessage("You are multiple regions:");
			for (ProtectedRegion region : regions) {
				player.sendMessage("  " + region.getId());
			}
		} else {
			ProtectedRegion region = regions.iterator().next();
			String regionName = region.getId();
			logInfo("Taking snapshot of " + regionName);
			File schematicFile = new File(getSchematicsFolder(), regionName + ".schematic");
			if (schematicFile.exists()) {
				schematicFile.delete();
			}
			Vector min = region.getMinimumPoint();
			Vector max = region.getMaximumPoint();
			Location loc1 = new Location(world, min.getX(), min.getY(), min.getZ());
			Location loc2 = new Location(world, max.getX(), max.getY(), max.getZ());
			debugInfo("  file: " + schematicFile);
			debugInfo("  loc1: " + loc1);
			debugInfo("  loc2: " + loc2);
			SchematicUtils.saveSchematic(schematicFile, loc1, loc2);
			logInfo("Saved.");
		}
		return true;
	}

	@CowCommand
	private boolean doReset(Player player) {
		return resetWorldRegions(player.getWorld());
	}

	private boolean resetWorldRegions(World w) {
		RegionManager regionManager = getWorldGuard().getRegionManager(w);
		for (ProtectedRegion region : regionManager.getRegions().values()) {
			String regionName = region.getId();
			File schematicFile = getRegionSaveFile(region);
			if (schematicFile != null) {
				logInfo("Resetting " + regionName);
				Vector min = region.getMinimumPoint();
				Location where = new Location(w, min.getX(), min.getY(), min.getZ());
				debugInfo("  file: " + schematicFile);
				debugInfo("  location: " + where);
				SchematicUtils.placeSchematic(schematicFile, where, true);
			} else {
				debugInfo("Skipping region: " + regionName);
			}
		}
		return true;
	}

	private void performCountdown(final long timeWhenDone) {
		long timeLeft = timeWhenDone - System.currentTimeMillis();
		long timeUntilNext = timeUntilNextWake(timeLeft);
		if (TimeUtils.millisToTicks(timeUntilNext) > 0) {
			broadcast("Regenerating jail in " + StringUtils.durationString(timeLeft / 1000));
			getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
				public void run() {
					performCountdown(timeWhenDone);
				}
			}, TimeUtils.millisToTicks(timeUntilNext));
		} else {
			// OK, it's regen time
			for (World w : getPlugin().getServer().getWorlds()) {
				resetWorldRegions(w);
			}
			performCountdown(System.currentTimeMillis() + resetInterval * 1000);
		}

		/*		for (Player player : getPlugin().getServer().getOnlinePlayers()) {
					RegionManager regionManager = getWorldGuard().getRegionManager(player.getWorld());
					ApplicableRegionSet regions = regionManager.getApplicableRegions(player.getLocation());
					for (ProtectedRegion region : regions) {
						if (getRegionSaveFile(region) != null) {
							// OK, this player is in at least one region that needs a message
						}
					}
				} */
	}

	private long timeUntilNextWake(long timeLeft) {
		if (timeLeft <= 0) {
			// OK, time to regenerate
			return 0;
		} else if (timeLeft < 10 * 1000) {
			// final 10 seconds
			return 1 * 1000;
		} else if (timeLeft < 60 * 1000) {
			// final minute
			return 10 * 1000;
		} else if (timeLeft < 5 * 60 * 1000) {
			// final 5 minutes
			return 60 * 1000;
		} else {
			return 5 * 60 * 1000;
		}
	}

	private File getRegionSaveFile(ProtectedRegion region) {
		String regionName = region.getId();
		File schematicFile = new File(getSchematicsFolder(), regionName + ".schematic");
		if (schematicFile.exists()) {
			return schematicFile;
		} else {
			return null;
		}
	}

	protected File getSchematicsFolder() {
		try {
			File folder = getPlugin().getDataFolder();
			if (!folder.exists()) {
				return null;
			}
			return folder;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void broadcast(String msg) {
		for (Player player : getPlugin().getServer().getOnlinePlayers()) {
			player.sendMessage(msg);
		}
	}

	private WorldGuardPlugin getWorldGuard() {
		Plugin worldPlugin = getPlugin().getServer().getPluginManager().getPlugin("WorldGuard");
		if (worldPlugin == null || !(worldPlugin instanceof WorldGuardPlugin)) {
			logInfo("WorldGuard must be loaded first");
			worldPlugin = null;
		} else {
			logInfo("WorldGuard found.");
		}
		return (WorldGuardPlugin) worldPlugin;
	}

	private WorldEditPlugin getWorldEdit() {
		Plugin wePlugin = getPlugin().getServer().getPluginManager().getPlugin("WorldEdit");
		if (wePlugin == null || !(wePlugin instanceof WorldEditPlugin)) {
			logInfo("WorldEdit must be loaded first");
			wePlugin = null;
		} else {
			logInfo("WorldEdit found.");
		}
		return (WorldEditPlugin) wePlugin;
	}

	private BOSEconomy getEconomy() {
		Plugin econ = getPlugin().getServer().getPluginManager().getPlugin("BOSEconomy");
		if ((econ != null) && econ instanceof BOSEconomy) {
			logInfo("Found BOSEconomy.  Economy enable.");
		} else {
			econ = null;
			logInfo("Could not find BOSEconomy.  Economy disabled.");
		}
		return (BOSEconomy) econ;
	}
}
