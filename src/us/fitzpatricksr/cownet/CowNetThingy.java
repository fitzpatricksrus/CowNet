package us.fitzpatricksr.cownet;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class CowNetThingy implements CommandExecutor {
    private Logger logger = Logger.getLogger("Minecraft");
    private Plugin plugin;
    private String trigger;
    private String permissionNode;
    private boolean isEnabled;

    public CowNetThingy(JavaPlugin plugin, String permissionRoot, String trigger) {
        this.plugin = plugin;
        this.trigger = trigger;
        this.permissionNode = permissionRoot+"."+trigger;
        this.isEnabled = plugin.getConfig().getBoolean(trigger + ".enable");
        if (!this.isEnabled()){
            //allow this common alias
            this.isEnabled = plugin.getConfig().getBoolean(trigger + ".enabled");
        }
        if (isEnabled) {
            plugin.getCommand(trigger).setExecutor(this);
            logger.info(trigger+" enabled");
        } else {
            logger.info(trigger+" disabled");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player)sender;
            if (args.length == 1) {
                if ("help".equalsIgnoreCase(args[0])) {
                    player.sendMessage(getHelpString(player));
                    return true;
                } else if ("reload".equalsIgnoreCase(args[0])) {
                    reload();
                    player.sendMessage("Reloaded...");
                    return true;
                }
            }
            if (!hasPermissions(player)) {
                player.sendMessage("Sorry, you don't have permission");
                return false;
            }
            return onCommand(player, cmd, args);
        } else if (sender.getClass().getName().contains("Console")) {
            // commands from the console
            return onCommand(cmd, args);
        } else {
            return false;
        }
    }

    public final int getConfigInt(String key, int def) {
        return plugin.getConfig().getInt(trigger+key, def);
    }
    public final boolean getConfigBoolean(String key, boolean def) {
        return plugin.getConfig().getBoolean(trigger+key, def);
    }
    public final String getConfigString(String key, String def) {
        return plugin.getConfig().getString(trigger + key, def);
    }
    public final boolean hasPermissions(Player player) {
        return player.isOp() || player.hasPermission(permissionNode);
    }
    public final boolean hasPermissions(Player player, String perm) {
        return player.isOp() || player.hasPermission(permissionNode+"."+perm);
    }
    public final Plugin getPlugin() {
        return plugin;
    }
    public final void logInfo(String msg) {
        logger.info(permissionNode+": "+msg);
    }
    public final boolean isEnabled() {
        return isEnabled;
    }
    protected void reload() {
    }
    protected String getHelpString(Player player) {
        return "There isn't any help for you...";
    }
    protected boolean onCommand(Player sender, Command cmd, String[] args) {
        return onCommand(cmd, args);
    }
    protected boolean onCommand(Command cmd, String[] args) {
        return false;
    }
}