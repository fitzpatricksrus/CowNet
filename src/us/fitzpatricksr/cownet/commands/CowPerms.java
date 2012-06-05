package us.fitzpatricksr.cownet.commands;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.utils.CowNetConfig;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Permissions are a flat linked list of permissions groups.  A group may inherit and override perms from another
 * group.  Permissions are resolved by loading the group with the same name as the active user and overriding
 * its perm values with the perms of the current world.
 * <p/>
 * Permission order of precedence is player, region, world, group
 * <p/>
 * This means that giving a specific player permissions allows them to use
 * them ANYWHERE, regardless of what world they are on.  However,
 * group permissions are overridden by world permissions.
 */
public class CowPerms extends CowNetThingy implements Listener {
	private static char SEPARATOR = '/';
	private CowNetConfig permsFile;
	private HashMap<String, PermissionAttachment> permissions = new HashMap<String, PermissionAttachment>();
	@Setting
	private String worldPrefix = "world-";
	@Setting
	private String regionPrefix = "region-";
	@Setting
	private String inheritTag = "inherit";
	@Setting
	private String permsFileName = "CowPerms.yml";
	@Setting
	private String defaultGroupName = "default";

	@Override
	protected String getHelpString(CommandSender sender) {
		return "usage: /cperms commands";
	}

	@Override
	protected void onEnable() throws IOException, InvalidConfigurationException {
		permsFile = new CowNetConfig(getPlugin(), permsFileName);
		permsFile.setPathSeparator('/');
		permsFile.loadConfig();
		for (Player p : getPlugin().getServer().getOnlinePlayers()) {
			registerPlayer(p);
		}
	}

	@Override
	public void onDisable() {
		for (Player p : getPlugin().getServer().getOnlinePlayers()) {
			unregisterPlayer(p);
		}
		permsFile = null;
	}

	//----------------------------------------
	// Commands
	//

	@CowCommand(opOnly = true)
	protected boolean doList(CommandSender player, String playerName) {
		return doList(player, playerName, null);
	}

	@CowCommand(opOnly = true)
	protected boolean doList(CommandSender player, String playerName, String filter) {
		playerName = playerName.toLowerCase();
		OfflinePlayer dumpPlayer = getPlugin().getServer().getOfflinePlayer(playerName);
		boolean isOp = dumpPlayer != null && dumpPlayer.isOp();

		Map<String, String> rawPerms = getRawPermTree(playerName);
		if (dumpPlayer != null) {
			Map<String, Boolean> resolvedPerms = resolvePermissions(rawPerms, isOp);
			rawPerms.clear();
			for (Map.Entry<String, Boolean> entry : resolvedPerms.entrySet()) {
				rawPerms.put(entry.getKey(), String.valueOf(entry.getValue()));
			}
		}
		LinkedList<String> keys = new LinkedList<String>();
		keys.addAll(rawPerms.keySet());
		Collections.sort(keys);
		for (String key : keys) {
			String line = key + ": " + rawPerms.get(key);
			if ((filter == null) || line.contains(filter)) {
				player.sendMessage(key + ": " + rawPerms.get(key));
			}
		}
		player.sendMessage("Group: " + getGroup(playerName));
		return true;
	}

	@CowCommand(opOnly = true)
	protected boolean doSet(CommandSender player, String playerName, String permName, String value) {
		playerName = playerName.toLowerCase();
		debugInfo("Setting " + playerName + SEPARATOR + permName + " to " + value);

		if ("op".equalsIgnoreCase(value) || isMetaSetting(permName)) {
			permsFile.set(playerName + SEPARATOR + permName, value);
		} else {
			permsFile.set(playerName + SEPARATOR + permName, Boolean.valueOf(value));
		}
		try {
			permsFile.saveConfig();
			Player p = getPlugin().getServer().getPlayer(playerName);
			if (p != null) {
				refreshPermissions(p);
			}
			doTest(player, playerName, permName);
		} catch (IOException e) {
			player.sendMessage("Could not set permissions.");
			e.printStackTrace();
		}
		return true;
	}

	@CowCommand(opOnly = true)
	protected boolean doTest(CommandSender sender, String playerName, String perm) {
		playerName = playerName.toLowerCase();
		Player p = getPlugin().getServer().getPlayer(playerName);
		if (p == null) {
			// it's not an online player, so we fall back to just the declared stuff
			OfflinePlayer dumpPlayer = getPlugin().getServer().getOfflinePlayer(playerName);
			if (dumpPlayer != null) {
				Map<String, String> perms = getPlayerPerms(dumpPlayer, true);
				sender.sendMessage(perm + ": " + perms.get(perm));
			} else {
				Map<String, String> perms = getGroupPerms(playerName);
				sender.sendMessage(perm + ": " + perms.get(perm));
			}
		} else {
			PermissionAttachment attachment = permissions.get(playerName);
			boolean hasPerm = p.hasPermission(perm);
			Boolean attachPerm = attachment.getPermissions().get(perm);
			String playerPerm = getPlayerPerms(p, false).get(perm);
			String groupPerm = getGroupPerms(p).get(perm);
			String worldPerm = getWorldPerms(p).get(perm);
			String regionPerm = getRegionPerms(p).get(perm);


			sender.sendMessage(playerName + SEPARATOR + perm +
					" [final=" + hasPerm +
					", g=" + groupPerm +
					", w=" + worldPerm +
					", r=" + regionPerm +
					", p=" + playerPerm +
					", a=" + attachPerm + "]");
		}
		return true;
	}

	//----------------------------------------
	// API
	//

	public String getGroup(String playerName) {
		playerName = playerName.toLowerCase();
		return permsFile.getString(playerName + SEPARATOR + inheritTag);
	}

	public void setGroup(String playerName, String groupName) throws IOException {
		playerName = playerName.toLowerCase();
		groupName = groupName.toLowerCase();
		permsFile.set(playerName + SEPARATOR + inheritTag, groupName);
		permsFile.saveConfig();
		Player p = getPlugin().getServer().getPlayer(playerName);
		if (p != null) {
			refreshPermissions(p);
		}
	}

	//----------------------------------------
	// event handling
	//

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerJoinEvent event) {
		debugInfo("Player " + event.getPlayer().getName() + " joined, registering...");
		Player player = event.getPlayer();
		String playerName = player.getName().toLowerCase();
		String group = getGroup(playerName);
		if (group == null) {
			try {
				setGroup(playerName, defaultGroupName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		registerPlayer(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerKick(PlayerKickEvent event) {
		debugInfo("Player " + event.getPlayer().getName() + " kicked, unregistering...");
		unregisterPlayer(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		debugInfo("Player " + event.getPlayer().getName() + " quit, unregistering...");
		unregisterPlayer(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onWorldChange(PlayerChangedWorldEvent event) {
		refreshPermissions(event.getPlayer());
	}

	// Prevent doing things in the event of permissions.build: false

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR) {
			return;
		}
		if (!event.getPlayer().hasPermission("permissions.build")) {
			event.getPlayer().sendMessage("You don't have permission to build.");
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!event.getPlayer().hasPermission("permissions.build")) {
			event.getPlayer().sendMessage("You don't have permission to build.");
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!event.getPlayer().hasPermission("permissions.build")) {
			event.getPlayer().sendMessage("You don't have permission to build.");
			event.setCancelled(true);
		}
	}

	//----------------------------------------
	// utils
	//

	protected void registerPlayer(Player player) {
		String name = player.getName().toLowerCase();
		if (permissions.containsKey(name)) {
			debugInfo("Registering " + name + ": was already registered");
			unregisterPlayer(player);
		}
		PermissionAttachment attachment = player.addAttachment(getPlugin());
		permissions.put(name, attachment);
		refreshPermissions(player);
	}

	protected void unregisterPlayer(Player player) {
		String name = player.getName().toLowerCase();
		if (permissions.containsKey(name)) {
			try {
				player.removeAttachment(permissions.get(name));
			} catch (IllegalArgumentException ex) {
				debugInfo("Unregistering " + name + ": player did not have attachment");
			}
			permissions.remove(name);
		} else {
			debugInfo("Unregistering " + name + ": was not registered");
		}
	}

	public void refreshPermissionsForAllPlayers() {
		for (Player p : getPlugin().getServer().getOnlinePlayers()) {
			refreshPermissions(p);
		}
	}

	public void refreshPermissions(Player player) {
		String playerName = player.getName().toLowerCase();
		String groupName = getGroup(playerName).toLowerCase();
		boolean isOp = player.isOp();

		PermissionAttachment attachment = permissions.get(playerName);
		if (attachment == null) {
			debugInfo("Calculating permissions on " + playerName + ": attachment was null");
			return;
		}

		for (String key : attachment.getPermissions().keySet()) {
			attachment.unsetPermission(key);
		}

		// group perms
		// world perms
		// region perms
		// player perms

		// load the perms for the group the player is in
		Map<String, String> temp = getGroupPerms(player);
		// override with world constraints
		temp.putAll(getWorldPerms(player));
		// override with region constraints if worldGuard is present
		temp.putAll(getRegionPerms(player));
		// override with player permissions, but skip the inherit tag
		temp.putAll(getPlayerPerms(player, false));

		for (Map.Entry<String, String> entry : temp.entrySet()) {
			String value = entry.getValue();
			boolean hasPerm = "true".equalsIgnoreCase(value) || ("op".equalsIgnoreCase(value) && isOp);
			attachment.setPermission(entry.getKey(), hasPerm);
			debugInfo(playerName + SEPARATOR + entry.getKey() + ": " + hasPerm);
		}

		player.recalculatePermissions();
	}

	private Map<String, Boolean> resolvePermissions(Map<String, String> perms, boolean isOp) {
		Map<String, Boolean> result = new HashMap<String, Boolean>();
		for (Map.Entry<String, String> entry : perms.entrySet()) {
			String value = entry.getValue();
			result.put(entry.getKey(), "true".equalsIgnoreCase(value) || ("op".equalsIgnoreCase(value) && isOp));
		}
		return result;
	}

	private Map<String, String> getGroupPerms(OfflinePlayer player) {
		return getGroupPerms(getGroup(player.getName().toLowerCase()));
	}

	private Map<String, String> getGroupPerms(String groupName) {
		groupName = groupName.toLowerCase();
		return getRawPermTree(groupName.toLowerCase());
	}

	private Map<String, String> getWorldPerms(Player player) {
		String worldName = player.getWorld().getName().toLowerCase();
		return getRawPermTree(worldPrefix + worldName);
	}

	private Map<String, String> getRegionPerms(Player player) {
		// override with region constraints if worldGuard is present
		if (getWorldGuard() != null) {
			Map<String, String> result = new HashMap<String, String>();
			RegionManager regionManager = getWorldGuard().getRegionManager(player.getWorld());
			ApplicableRegionSet regions = regionManager.getApplicableRegions(player.getLocation());
			for (ProtectedRegion region : regions) {
				result.putAll(getRawPermTree(regionPrefix + region.getId().toLowerCase()));
			}
			return result;
		} else {
			return Collections.emptyMap();
		}
	}

	private Map<String, String> getPlayerPerms(OfflinePlayer player, boolean deep) {
		Map<String, String> result = (deep) ? getGroupPerms(player) : new HashMap<String, String>();
		result.putAll(loadRawPermNode(player.getName().toLowerCase()));
		result.remove(inheritTag);
		return result;
	}

	// Take the given node, and merge it with it's inherited nodes
	private Map<String, String> getRawPermTree(String groupName) {
		Map<String, String> result = new HashMap<String, String>();
		loadPermsTree(loadRawPermNode(groupName), result);
		return result;
	}

	private void loadPermsTree(Map<String, String> node, Map<String, String> result) {
		String inherit = node.get(inheritTag);
		if (inherit != null) {
			String[] parents = inherit.split(",");
			for (String parent : parents) {
				loadPermsTree(loadRawPermNode(parent), result);
			}
			node.remove(inheritTag);
		}
		for (String key : node.keySet()) {
			String val = node.get(key);
			result.put(key, val);
		}
	}

	// Load perms for a single node, but don't inherit any values
	private Map<String, String> loadRawPermNode(String groupName) {
		Map<String, String> result = new HashMap<String, String>();
		ConfigurationSection cs = permsFile.getConfigurationSection(groupName.toLowerCase());
		if (cs == null) {
			debugInfo("could not find " + groupName);
		} else {
			for (String key : cs.getKeys(true)) {
				result.put(key.toLowerCase(), cs.get(key).toString().toLowerCase());
				//				debugInfo("[" + key + "," + result.get(key) + "]");
			}
		}
		return result;
	}

	private boolean isMetaSetting(String key) {
		return inheritTag.equalsIgnoreCase(key);
	}

	private WorldGuardPlugin getWorldGuard() {
		return (WorldGuardPlugin) getPlugin().getServer().getPluginManager().getPlugin("WorldGuard");
	}
}


