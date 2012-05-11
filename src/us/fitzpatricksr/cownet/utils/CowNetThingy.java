package us.fitzpatricksr.cownet.utils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

public class CowNetThingy implements CommandExecutor {
    private Logger logger = Logger.getLogger("Minecraft");
    private JavaPlugin plugin;
    private String trigger = "";
    private String permissionNode = "";
    private boolean isEnabled = false;
    @SettingsTwiddler.Setting()
    private boolean isDebug = false;

    protected CowNetThingy() {
        //for testing only
    }

    public CowNetThingy(JavaPlugin plugin, String permissionRoot, String trigger) {
        this.plugin = plugin;
        this.trigger = trigger;
        this.permissionNode = permissionRoot + "." + trigger;
        this.isEnabled = getConfigBoolean("enable", false);
        if (!this.isEnabled()) {
            //allow this common alias
            this.isEnabled = getConfigBoolean("enabled", true);
        }
        this.isDebug = getConfigBoolean("debug", false);
        if (isEnabled) {
            plugin.getCommand(trigger).setExecutor(this);
            logInfo(trigger + " enabled");
        } else {
            logInfo(trigger + " disabled");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            if ("help".equalsIgnoreCase(args[0])) {
                for (String help : getHelpText(sender)) {
                    sender.sendMessage(help);
                }
                return true;
            } else if ("reload".equalsIgnoreCase(args[0])) {
                getPlugin().reloadConfig();
                reload();
                sender.sendMessage("Reloaded...");
                return true;
            } else if ("debug".equalsIgnoreCase(args[0])) {
                isDebug = !isDebug;
                sender.sendMessage("Debug = " + isDebug);
                return true;
            } else if ("settings".equalsIgnoreCase(args[0]) && sender.isOp()) {
                Map<String, String> settings = SettingsTwiddler.getSettings(this);
                for (String key : settings.keySet()) {
                    sender.sendMessage(key + ": " + settings.get(key));
                }
                return true;
            }
        } else if (args.length == 3) {
            if ("set".equalsIgnoreCase(args[0]) && sender.isOp()) {
                // set <setting> <value>
                SettingsTwiddler.setSetting(this, args[1], args[2]);
                return true;
            }
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!hasPermissions(player)) {
                player.sendMessage("Sorry, you don't have permission");
                return true;
            }
            return handleCommand(sender, cmd, args) || handleCommand(player, cmd, args);
        } else if (sender.getClass().getName().contains("Console")) {
            // commands from the console
            return handleCommand(sender, cmd, args);
        } else {
            logInfo("Could not handle command from " + sender);
            return true;
        }
    }

    public final String getTrigger() {
        return trigger;
    }

    public final ConfigurationSection getConfigSection() {
        return (ConfigurationSection) plugin.getConfig().get(trigger);
    }

    public final int getConfigInt(String key, int def) {
        return plugin.getConfig().getInt(trigger + "." + key, def);
    }

    public final long getConfigLong(String key, long def) {
        return plugin.getConfig().getLong(trigger + "." + key, def);
    }

    public final double getConfigDouble(String key, double def) {
        return plugin.getConfig().getDouble(trigger + "." + key, def);
    }

    public final boolean getConfigBoolean(String key, boolean def) {
        return plugin.getConfig().getBoolean(trigger + "." + key, def);
    }

    public final String getConfigString(String key, String def) {
        return plugin.getConfig().getString(trigger + "." + key, def);
    }

    public final void saveConfiguration() {
        plugin.saveConfig();
    }

    public final boolean hasPermissions(Player player) {
        if (player.isOp() || player.hasPermission(permissionNode)) {
            return true;
        } else {
            logInfo(player.getName() + " does not have " + permissionNode);
            return false;
        }
    }

    public final boolean hasPermissions(CommandSender player, String perm) {
        return hasPermissions(player, perm, false) || player.hasPermission("*");
    }

    public final boolean hasPermissionsOrOp(CommandSender player, String perm) {
        return hasPermissions(player, perm, true) || player.hasPermission("*");
    }

    private boolean hasPermissions(CommandSender player, String perm, boolean allowOps) {
        if ((allowOps && player.isOp()) || player.hasPermission(permissionNode + "." + perm)) {
            return true;
        } else {
            logInfo(player.getName() + " does not have explicit " + permissionNode + "." + perm);
            return false;
        }
    }

    public final JavaPlugin getPlugin() {
        return plugin;
    }

    public final void logInfo(String msg) {
        logger.info(permissionNode + ": " + msg);
    }

    public final void debugInfo(String msg) {
        if (isDebug) {
            logger.info("[" + permissionNode + "]: " + msg);
        }
    }

    public final boolean isEnabled() {
        return isEnabled;
    }

    public final boolean isDebug() {
        return isDebug;
    }

    // disable the plugin if there was some critical startup error.
    protected void disable() {
        isEnabled = false;
        logInfo("Plugin disabled.");
    }

    protected void reload() {
    }

    protected String[] getHelpText(CommandSender sender) {
        return new String[]{getHelpString(sender)};
    }

    protected String getHelpString(CommandSender sender) {
        return "There isn't any help for you...";
    }

    protected boolean handleCommand(Player sender, Command cmd, String[] args) {
        return false;
    }

    protected boolean handleCommand(CommandSender sender, Command cmd, String[] args) {
        // hey jf - this used to just return false
        return dispatchMethod(sender, cmd, args);
    }

    static Class[] args0 = new Class[]{CommandSender.class, Command.class};
    static Class[] args1 = new Class[]{CommandSender.class, Command.class, String.class};
    static Class[] args2 = new Class[]{CommandSender.class, Command.class, String.class, String.class};
    static Class[] args3 = new Class[]{CommandSender.class, Command.class, String.class, String.class, String.class};
    static Class[] args4 = new Class[]{CommandSender.class, Command.class, String.class, String.class, String.class, String.class};
    static Class[][] argsX = {args0, args1, args2, args3, args4};

    private Method getMethod(String[] args) {
        String methodName = args[0];
        methodName = "do" + methodName.toUpperCase().substring(0, 1) + methodName.toLowerCase().substring(1);
        debugInfo(methodName);

        try {
            int paramCount = args.length - 1;
            Method method = getClass().getMethod(methodName, argsX[paramCount]);
            if (method.getReturnType().equals(boolean.class)) {
                return method;
            } else {
                return null;
            }
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private boolean dispatchMethod(CommandSender sender, Command cmd, String[] args) {
        try {
            LinkedList<Object> paramList = new LinkedList<Object>();
            paramList.addFirst(sender);
            paramList.addLast(cmd);
            for (int i = 1; i < args.length; i++) {
                paramList.addLast(args[i]);
            }
            Object[] params = paramList.toArray();
            Method method = getMethod(args);
            if (method != null) {
                Object result = method.invoke(this, params);
                return (Boolean) result;
            }
        } catch (InvocationTargetException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return false;
    }


}