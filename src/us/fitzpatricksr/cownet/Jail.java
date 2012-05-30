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

import java.io.File;

public class Jail extends CowNetThingy {
	@Setting
	private long resetNotifyInterval = 20 * 60 * 2;  // 2 minutes
	@Setting
	private long resetInterval = 20 * 60 * 60;  // 1 hour
	@Setting
	private String jailName = "jail";  // name of the jail region

	private WorldGuardPlugin worldGuard;
	private WorldEditPlugin worldEdit;
	private BOSEconomy economy;
	private long lastResetClock = 0;

	public Jail(JavaPlugin plugin, String permissionRoot) {
		super(plugin, permissionRoot);
		if (isEnabled()) {
			reloadSettings();
			//get WorldGuard and WorldEdit plugins
			Plugin worldPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
			if (worldPlugin == null || !(worldPlugin instanceof WorldGuardPlugin)) {
				throw new RuntimeException("WorldGuard must be loaded first");
			} else {
				logInfo("WorldGuard found.");
			}
			worldGuard = (WorldGuardPlugin) worldPlugin;
			Plugin wePlugin = plugin.getServer().getPluginManager().getPlugin("WorldEdit");
			if (wePlugin == null || !(wePlugin instanceof WorldEditPlugin)) {
				throw new RuntimeException("WorldEdit must be loaded first");
			} else {
				logInfo("WorldEdit found.");
			}
			worldEdit = (WorldEditPlugin) wePlugin;
			Plugin econ = plugin.getServer().getPluginManager().getPlugin("BOSEconomy");
			if (econ instanceof BOSEconomy) {
				this.economy = (BOSEconomy) econ;
				logInfo("Found BOSEconomy.  Economy enable.");
			} else {
				logInfo("Could not find BOSEconomy.  Economy disabled.");
			}
			getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
				public void run() {
					//doReset();
				}
			}, resetNotifyInterval, resetNotifyInterval);
		}
	}

	@Override
	protected String getHelpString(CommandSender sender) {
		return "usage: jail [ 'release <player>' | 'reset' | 'snapshot' | <player> ]";
	}

	@CowCommand(permission = "jail")
	protected boolean doJail(Player player, String whoToJail) {
		return false;
	}

	@CowCommand(permission = "release")
	protected boolean doRelease(Player player, String whoToRelease) {
		return false;
	}

	@CowCommand
	protected boolean doUnsnapshot(Player player) {
		World world = player.getWorld();
		RegionManager regionManager = worldGuard.getRegionManager(world);
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
	protected boolean doSnapshot(Player player) {
		World world = player.getWorld();
		RegionManager regionManager = worldGuard.getRegionManager(world);
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
	protected boolean doReset(Player player) {
		return resetWorld(player.getWorld());
	}

	protected boolean resetWorld(World w) {
		RegionManager regionManager = worldGuard.getRegionManager(w);
		for (ProtectedRegion region : regionManager.getRegions().values()) {
			String regionName = region.getId();
			File schematicFile = new File(getSchematicsFolder(), regionName + ".schematic");
			if (schematicFile.exists()) {
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
}

