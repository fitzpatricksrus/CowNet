package us.fitzpatricksr.cownet.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
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
 * group1:
 * perms1: value
 * perms2: value
 * group2:
 * inherit: group1
 * perms1: value
 * perms2: value
 * <p/>
 * player1:
 * perms1: value
 * perms2: value
 * player2:
 * inherit: group1,group2
 * perms1: value
 * perms2: value
 * <p/>
 * world1:
 * inherit: group1
 * perms1: value
 * perms2: value
 * world2:
 * perms1: value
 * perms2: value
 */
public class CowPerms extends CowNetThingy implements Listener {
	private static char SEPARATOR = '/';
	private CowNetConfig permsFile;
	private HashMap<String, PermissionAttachment> permissions = new HashMap<String, PermissionAttachment>();
	@Setting
	private String permsFileName = "CowPerms.yml";
	@Setting
	private String defaultGroupName = "default";
	@Setting
	private String inheritTag = "inherit";

	@Override
	protected String getHelpString(CommandSender sender) {
		return "usage: /cperms commands";
	}

	//----------------------------------------
	// Commands
	//

	@CowCommand(opOnly = true)
	protected boolean doList(CommandSender player, String playerName) {
		Player dumpPlayer = getPlugin().getServer().getPlayer(playerName);
		if (dumpPlayer == null) {
			Map<String, Boolean> perms = calcGroupPerms(playerName);
			LinkedList<String> keys = new LinkedList<String>();
			keys.addAll(perms.keySet());
			Collections.sort(keys);
			for (String key : keys) {
				player.sendMessage(key + ": " + perms.get(key));
			}
		} else {
			Map<String, Boolean> perms = calcPlayerPerms(dumpPlayer);
			LinkedList<String> keys = new LinkedList<String>();
			keys.addAll(perms.keySet());
			Collections.sort(keys);
			for (String key : keys) {
				player.sendMessage(key + ": " + perms.get(key));
			}
		}
		return true;
	}

	@CowCommand(opOnly = true)
	protected boolean doCheck(CommandSender player, String permName) {
		player.sendMessage(permName + ": " + permsFile.get(permName));
		return true;
	}

	@CowCommand(opOnly = true)
	protected boolean doCheck(CommandSender player, String playerName, String permName) {
		Player dumpPlayer = getPlugin().getServer().getPlayer(playerName);
		if (dumpPlayer == null) {
			Map<String, Boolean> perms = calcGroupPerms(playerName);
			player.sendMessage(permName + ": " + perms.get(permName));
		} else {
			Map<String, Boolean> perms = calcPlayerPerms(dumpPlayer);
			player.sendMessage(permName + ": " + perms.get(permName));
		}
		return true;
	}

	@CowCommand(opOnly = true)
	protected boolean doSet(CommandSender player, String playerName, String permName, String value) {
		debugInfo("Setting " + playerName + SEPARATOR + permName + " to " + value);
		if ("op".equalsIgnoreCase(value)) {
			permsFile.set(playerName + SEPARATOR + permName, value);
		} else {
			permsFile.set(playerName + SEPARATOR + permName, Boolean.valueOf(value));
		}
		try {
			permsFile.saveConfig();
			doCheck(player, permName);
		} catch (IOException e) {
			player.sendMessage("Could not set permissions.");
			e.printStackTrace();
		}
		return true;
	}
	//----------------------------------------
	// API
	//

	public String getGroup(String playerName) {
		return permsFile.getString(playerName + SEPARATOR + inheritTag);
	}

	public void setGroup(String playerName, String groupName) throws IOException {
		permsFile.set(playerName + SEPARATOR + inheritTag, groupName);
		permsFile.saveConfig();
		Player p = getPlugin().getServer().getPlayer(playerName);
		if (p != null) {
			refreshPermissions(p);
		}
	}

	//----------------------------------------
	// attach/detach as players come and go
	//
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

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerJoinEvent event) {
		debugInfo("Player " + event.getPlayer().getName() + " joined, registering...");
		Player player = event.getPlayer();
		String playerInheritTag = player.getName() + inheritTag;
		if (permsFile.getString(playerInheritTag) == null) {
			permsFile.set(playerInheritTag, defaultGroupName);
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

	protected void registerPlayer(Player player) {
		if (permissions.containsKey(player.getName())) {
			debugInfo("Registering " + player.getName() + ": was already registered");
			unregisterPlayer(player);
		}
		PermissionAttachment attachment = player.addAttachment(getPlugin());
		permissions.put(player.getName(), attachment);
		refreshPermissions(player);
	}

	protected void unregisterPlayer(Player player) {
		if (permissions.containsKey(player.getName())) {
			try {
				player.removeAttachment(permissions.get(player.getName()));
			} catch (IllegalArgumentException ex) {
				debugInfo("Unregistering " + player.getName() + ": player did not have attachment");
			}
			permissions.remove(player.getName());
		} else {
			debugInfo("Unregistering " + player.getName() + ": was not registered");
		}
	}

	//----------------------------------------
	// utils
	//

	public void refreshPermissions() {
		for (Player p : getPlugin().getServer().getOnlinePlayers()) {
			refreshPermissions(p);
		}
	}

	public void refreshPermissions(Player player) {
		String playerName = player.getName();

		PermissionAttachment attachment = permissions.get(playerName);
		if (attachment == null) {
			debugInfo("Calculating permissions on " + playerName + ": attachment was null");
			return;
		}

		for (String key : attachment.getPermissions().keySet()) {
			attachment.unsetPermission(key);
		}

		for (Map.Entry<String, Boolean> entry : calcEffectivePerms(player).entrySet()) {
			attachment.setPermission(entry.getKey(), entry.getValue());
			debugInfo(playerName + SEPARATOR + entry.getKey() + ": " + entry.getValue());
		}

		player.recalculatePermissions();
	}

	private Map<String, Boolean> calcGroupPerms(String groupName) {
		Map<String, Boolean> result = new HashMap<String, Boolean>();
		mergePermissions(loadPerms(groupName.toLowerCase()), result, false);
		return result;
	}

	private Map<String, Boolean> calcPlayerPerms(OfflinePlayer player) {
		boolean isOp = player.isOp();
		Map<String, Boolean> result = new HashMap<String, Boolean>();
		mergePermissions(loadPerms(player.getName().toLowerCase()), result, isOp);
		return result;
	}

	private Map<String, Boolean> calcEffectivePerms(Player player) {
		boolean isOp = player.isOp();
		Map<String, Boolean> result = new HashMap<String, Boolean>();
		mergePermissions(loadPerms(player.getName().toLowerCase()), result, isOp);
		mergePermissions(loadPerms(((Player) player).getWorld().getName().toLowerCase()), result, isOp);
		return result;
	}

	private void mergePermissions(Map<String, String> node, Map<String, Boolean> result, boolean isOp) {
		String inherit = node.get(inheritTag);
		if (inherit != null) {
			String[] parents = inherit.split(",");
			for (String parent : parents) {
				mergePermissions(loadPerms(parent), result, isOp);
			}
			node.remove(inheritTag);
		}
		for (String key : node.keySet()) {
			String val = node.get(key);
			result.put(key, (isOp && "op".equalsIgnoreCase(val)) || "true".equalsIgnoreCase(val));
		}
	}

	private Map<String, String> loadPerms(String groupName) {
		Map<String, String> result = new HashMap<String, String>();
		ConfigurationSection cs = permsFile.getConfigurationSection(groupName.toLowerCase());
		if (cs == null) {
			debugInfo("could not find " + groupName);
		} else {
			for (String key : cs.getKeys(true)) {
				if (cs.isBoolean(key)) {
					result.put(key, "" + cs.getBoolean(key));
					debugInfo("[" + key + "," + cs.getString(key) + "]");
				} else {
					boolean isBool = cs.isBoolean(key);
					result.put(key, cs.get(key).toString());
					debugInfo("[" + key + "," + cs.getString(key) + "]");
				}
			}
		}
		return result;
	}
}


