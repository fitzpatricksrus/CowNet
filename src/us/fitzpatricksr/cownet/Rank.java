package us.fitzpatricksr.cownet;

import com.platymuus.bukkit.permissions.Group;
import com.platymuus.bukkit.permissions.PermissionsPlugin;
import com.platymuus.bukkit.permissions.PermissionsUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetThingy;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Rank extends CowNetThingy {
    private static final String UP_CMD = "up";
    private static final String DOWN_CMD = "down";
    private PermissionsPlugin permsPlugin;
    private PermissionsUtils permsUtils;
    private LinkedList<String> permsGroups = new LinkedList<String>();

    public Rank(JavaPlugin plugin, String permissionRoot, String trigger) {
        super(plugin, permissionRoot, trigger);
        if (isEnabled()) {
            reload();
        }
    }

    protected void reload() {
        permsPlugin = (PermissionsPlugin) getPlugin().getServer().getPluginManager().getPlugin("PermissionsBukkit");
        permsUtils = new PermissionsUtils(permsPlugin);
        if (permsPlugin == null) {
            logInfo("Could not find permissions plugin.");
            disable();
        } else {
            String groupNamesString = getConfigString("groupNames", null);
            if (groupNamesString == null) {
                logInfo("Could not find groupNames in config file.  ");
                disable();
            } else {
                String[] groupNames = groupNamesString.split(",");
                permsGroups.addAll(Arrays.asList(groupNames));
                logInfo("found " + permsGroups.size() + " groups: " + groupNamesString);
            }
        }
    }

    protected boolean handleCommand(CommandSender sender, Command cmd, String[] args) {
        if (args.length > 2) return false;
        try {
            String subCmd = (args.length > 1) ? args[1] : null;
            String playerName = args[0];
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
            } else if (args.length == 1) {
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

