package us.fitzpatricksr.cownet.commands;

import org.bukkit.command.CommandSender;
import us.fitzpatricksr.cownet.CowNetThingy;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

public class CowRank extends CowNetThingy {
	private static final String UP_CMD = "up";
	private static final String DOWN_CMD = "down";
	private static final String NONE_CMD = "none";
	private CowPerms permsPlugin;
	private LinkedList<String> permsGroups = new LinkedList<String>();

	public CowRank(CowPerms perms) {
		this.permsPlugin = perms;
	}

	protected void reloadManualSettings() throws Exception {
		String groupNamesString = getConfigValue("groupNames", "");
		if (groupNamesString.isEmpty()) {
			logInfo("Could not find groupNames in config file.");
			throw new IllegalArgumentException("Could not find groupNames in config file.");
		} else {
			String[] groupNames = groupNamesString.split(",");
			permsGroups.addAll(Arrays.asList(groupNames));
			logInfo("found " + permsGroups.size() + " groups: " + groupNamesString);
		}
	}

	@Override
	protected String getHelpString(CommandSender sender) {
		return "usage: rank <userName> [UP | DOWN]";
	}

	@CowCommand(opOnly = true)
	protected boolean doCowrank(CommandSender sender, String playerName) {
		return doCowrank(sender, playerName, NONE_CMD);
	}

	@CowCommand(opOnly = true)
	protected boolean doCowrank(CommandSender sender, String playerName, String subCmd) {
		try {
			if (UP_CMD.equalsIgnoreCase(subCmd)) {
				int ndx = getPlayerGroup(playerName);
				if (ndx == -1) {
					sender.sendMessage("Could not find " + playerName);
				} else if (ndx < permsGroups.size() - 1) {
					permsPlugin.setGroup(playerName, permsGroups.get(ndx + 1));
					sender.sendMessage("Change player " + playerName + " from " + permsGroups.get(ndx) + " to " + permsGroups.get(ndx + 1));
				}
			} else if (DOWN_CMD.equalsIgnoreCase(subCmd)) {
				int ndx = getPlayerGroup(playerName);
				if (ndx == -1) {
					sender.sendMessage("Could not find " + playerName);
				} else if (ndx > 0) {
					permsPlugin.setGroup(playerName, permsGroups.get(ndx - 1));
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
		} catch (IOException e) {
			return false;
		} catch (IndexOutOfBoundsException e) {
			return false;
		}
		return true;
	}

	private int getPlayerGroup(String player) {
		String playersGroup = permsPlugin.getGroup(player);
		int i = 0;
		for (String permGroup : permsGroups) {
			if (permGroup.equalsIgnoreCase(playersGroup)) {
				return i;
			}
			i++;
		}
		return -1;
	}
}
