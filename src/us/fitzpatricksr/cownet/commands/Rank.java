package us.fitzpatricksr.cownet.commands;

import com.platymuus.bukkit.permissions.Group;
import com.platymuus.bukkit.permissions.PermissionsPlugin;
import com.platymuus.bukkit.permissions.PermissionsUtils;
import org.bukkit.command.CommandSender;
import us.fitzpatricksr.cownet.CowNetThingy;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Rank extends CowNetThingy {
	private static final String UP_CMD = "up";
	private static final String DOWN_CMD = "down";
	private static final String NONE_CMD = "none";
	private PermissionsPlugin permsPlugin;
	private PermissionsUtils permsUtils;
	private LinkedList<String> permsGroups = new LinkedList<String>();

	protected void reloadManualSettings() throws Exception {
		permsPlugin = (PermissionsPlugin) getPlugin().getServer().getPluginManager().getPlugin("PermissionsBukkit");
		permsUtils = new PermissionsUtils(permsPlugin);
		if (permsPlugin == null) {
			logInfo("Could not find permissions plugin.");
			throw new IllegalStateException("Could not find permissions plugin");
		} else {
			String groupNamesString = getConfigValue("groupNames", null);
			if (groupNamesString == null) {
				logInfo("Could not find groupNames in config file.");
				throw new IllegalArgumentException("Could not find groupNames in config file.");
			} else {
				String[] groupNames = groupNamesString.split(",");
				permsGroups.addAll(Arrays.asList(groupNames));
				logInfo("found " + permsGroups.size() + " groups: " + groupNamesString);
			}
		}
	}

	@Override
	protected String getHelpString(CommandSender sender) {
		return "usage: rank <userName> [UP | DOWN]";
	}

	@CowCommand(opOnly = true)
	protected boolean doRank(CommandSender sender, String playerName) {
		return doRank(sender, playerName, NONE_CMD);
	}

	@CowCommand(opOnly = true)
	protected boolean doRank(CommandSender sender, String playerName, String subCmd) {
		try {
			if (UP_CMD.equalsIgnoreCase(subCmd)) {
				int ndx = getPlayerGroup(playerName);
				if (ndx == -1) {
					sender.sendMessage("Could not find " + playerName);
				} else if (ndx < permsGroups.size() - 1) {
					permsUtils.playerAddGroup(playerName, permsGroups.get(ndx + 1));
					permsUtils.playerRemoveGroup(playerName, permsGroups.get(ndx));
					sender.sendMessage("Change player " + playerName + " from " + permsGroups.get(ndx) + " to " + permsGroups.get(ndx + 1));
				}
			} else if (DOWN_CMD.equalsIgnoreCase(subCmd)) {
				int ndx = getPlayerGroup(playerName);
				if (ndx == -1) {
					sender.sendMessage("Could not find " + playerName);
				} else if (ndx > 0) {
					permsUtils.playerAddGroup(playerName, permsGroups.get(ndx - 1));
					permsUtils.playerRemoveGroup(playerName, permsGroups.get(ndx));
					sender.sendMessage("Change player " + playerName + " from " + permsGroups.get(ndx) + " to " + permsGroups.get(ndx - 1));
				}
			} else if (NONE_CMD.equalsIgnoreCase(subCmd)) {
				int ndx = getPlayerGroup(playerName);
				if (ndx == -1) {
					sender.sendMessage("Could not find " + playerName);
				} else if (ndx > 0) {
					sender.sendMessage(playerName + " is in group " + permsGroups.get(ndx));
				}
			} else {
				return false;
			}
		} catch (IndexOutOfBoundsException e) {
			return false;
		}
		return true;
	}

	private int getPlayerGroup(String player) {
		List<Group> playerGroups = permsPlugin.getGroups(player);
		for (Group g : playerGroups) {
			int i = 0;
			for (String permGroup : permsGroups) {
				if (permGroup.equalsIgnoreCase(g.getName())) {
					return i;
				}
				i++;
			}
		}
		return -1;
	}
}
