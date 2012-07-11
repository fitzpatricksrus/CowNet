package us.fitzpatricksr.cownet.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.utils.CowNetConfig;
import us.fitzpatricksr.cownet.utils.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CowWarp extends CowNetThingy {
	@Setting
	private String publicWarpKey = "public";
	@Setting
	private String defaultWorld = "guestWorld";
	@Setting
	private double defaultPitch = 0.0;
	@Setting
	private double defaultYaw = 0.0;
	@Setting
	private double defaultX = 0.0;
	@Setting
	private double defaultY = 0.0;
	@Setting
	private double defaultZ = 0.0;

	private Map<String, WarpData> warps; // playerName -> warpData

	@Override
	protected void onEnable() throws Exception {
	}

	@Override
	protected void onDisable() {
	}

	@Override
	protected void reloadManualSettings() throws Exception {
		// clear the cache and force it to be reloaded from disk
		warps = new HashMap<String, WarpData>();
	}

	@CowCommand
	protected boolean doCowwarp(Player player, String warpName) {
		Location loc = getWarp(player.getName(), warpName);
		if (loc != null) {
			player.teleport(loc);
		} else {
			player.sendMessage("Warp not found.");
		}
		return true;
	}

	@CowCommand
	protected boolean doSet(Player player, String warpName) {
		WarpData warps = getWarpsFor(player.getName());
		try {
			warps.setWarp(warpName, player.getLocation());
		} catch (IOException e) {
			player.sendMessage("There was a problem and the warp could not be set.");
			e.printStackTrace();
		}
		return true;
	}

	@CowCommand
	protected boolean doRemove(Player player, String warpName) {
		WarpData warps = getWarpsFor(player.getName());
		try {
			warps.removeWarp(warpName);
		} catch (IOException e) {
			player.sendMessage("There was a problem and the warp could not be removed.");
			e.printStackTrace();
		}
		return true;
	}

	@CowCommand
	protected boolean doList(Player player) {
		player.sendMessage("Public Warps:");
		doList(player, publicWarpKey);
		player.sendMessage("");
		player.sendMessage("Private Warps:");
		doList(player, player.getName());
		return true;
	}

	@CowCommand(opOnly = true)
	protected boolean doList(CommandSender sender, String playerName) {
		WarpData data = getWarpsFor(playerName);
		sender.sendMessage(StringUtils.flatten(data.getWarpNames(), ","));
		return true;
	}

	@CowCommand
	protected boolean doInfo(Player player, String warpName) {
		return doInfo(player, player.getName(), warpName);
	}

	@CowCommand(opOnly = true)
	protected boolean doInfo(CommandSender sender, String playerName, String warpName) {
		Location loc = getWarp(playerName, warpName);
		if (loc != null) {
			sender.sendMessage(loc.getWorld().getName() + " - " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
		} else {
			sender.sendMessage("Warp not found.");
		}
		return true;
	}

	private Location getWarp(String playerName, String warpName) {
		WarpData warps = getWarpsFor(playerName);
		if (warps == null) {
			warps = getWarpsFor(publicWarpKey);
		}
		return warps.getWarp(warpName);
	}

	// return a map of warp points for a particular player
	private WarpData getWarpsFor(String playerName) {
		playerName = playerName.toLowerCase();
		WarpData result = warps.get(playerName);
		if (result == null) {
			try {
				result = new WarpData(getPlugin(), playerName);
				warps.put(playerName, result);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	private class WarpData extends CowNetConfig {
		// The structure of a warp point is as follows:
		// warps.warpname.invitees: List<String> (optional)
		// warps.warpname.world: string
		// warps.warpname.pitch: double
		// warps.warpname.yaw: double
		// warps.warpname.x: double
		// warps.warpname.y: double
		// warps.warpname.z: double

		public WarpData(JavaPlugin plugin, String playerName) throws IOException, InvalidConfigurationException {
			super(plugin, "Warps-" + playerName + ".yml");
			loadConfig();
		}

		public Location getWarp(String warpName) {
			warpName = warpName.toLowerCase();
			if (hasConfigValue("warps." + warpName + ".world")) {
				String world = getConfigValue("warps." + warpName + ".world", defaultWorld);
				double pitch = getConfigValue("warps." + warpName + ".pitch", defaultPitch);
				double yaw = getConfigValue("warps." + warpName + ".yaw", defaultYaw);
				double x = getConfigValue("warps." + warpName + ".x", defaultX);
				double y = getConfigValue("warps." + warpName + ".y", defaultY);
				double z = getConfigValue("warps." + warpName + ".z", defaultZ);
				return new Location(getPlugin().getServer().getWorld(world), x, y, z, (float) yaw, (float) pitch);
			} else {
				return null;
			}
		}

		public void setWarp(String warpName, Location loc) throws IOException {
			warpName = warpName.toLowerCase();
			updateConfigValue("warps." + warpName + ".world", loc.getWorld().getName());
			updateConfigValue("warps." + warpName + ".pitch", loc.getPitch());
			updateConfigValue("warps." + warpName + ".yaw", loc.getYaw());
			updateConfigValue("warps." + warpName + ".x", loc.getX());
			updateConfigValue("warps." + warpName + ".y", loc.getY());
			updateConfigValue("warps." + warpName + ".z", loc.getZ());
			saveConfig();
		}

		public void removeWarp(String warpName) throws IOException {
			warpName = warpName.toLowerCase();
			removeConfigValue("warps." + warpName + ".world");
			removeConfigValue("warps." + warpName + ".pitch");
			removeConfigValue("warps." + warpName + ".yaw");
			removeConfigValue("warps." + warpName + ".x");
			removeConfigValue("warps." + warpName + ".y");
			removeConfigValue("warps." + warpName + ".z");
			saveConfig();
		}

		public Set<String> getWarpNames() {
			//hey jf - should you try to filter out the ones that aren't going to work because the world
			//is no longer there?
			if (getNode("warps") != null) {
				return getNode("warps").getKeys(false);
			} else {
				return Collections.emptySet();
			}
		}
	}
}
