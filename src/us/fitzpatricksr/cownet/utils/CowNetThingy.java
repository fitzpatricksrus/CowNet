package us.fitzpatricksr.cownet.utils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

public class CowNetThingy implements CommandExecutor {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Setting {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface SubCommand {
        boolean opOnly() default false;
    }

    private Logger logger = Logger.getLogger("Minecraft");
    private JavaPlugin plugin;
    private String permissionNode = "";
    private boolean isEnabled = false;

    @Setting
    private boolean isDebug = false;

    public CowNetThingy(JavaPlugin plugin, String permissionRoot) {
        this.plugin = plugin;
        this.permissionNode = permissionRoot + "." + getTrigger();
        this.isEnabled = getConfigValue("enable", false);
        if (!this.isEnabled()) {
            //allow this common alias
            this.isEnabled = getConfigValue("enabled", true);
        }
        isDebug = getConfigValue("debug", isDebug);
        if (isEnabled) {
            logInfo(getTrigger() + " enabled");
            plugin.getCommand(getTrigger()).setExecutor(this);
        } else {
            logInfo(getTrigger() + " disabled");
        }
        logInfo("commands: " + StringUtils.flatten(getHandlerMethods()));
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!hasPermissions(player)) {
                player.sendMessage("Sorry, you don't have permission");
                return true;
            }
        }
        return dispatchMethod(sender, cmd, args);
    }

    public final String getTrigger() {
        return getClass().getSimpleName().toLowerCase();
    }

    public final ConfigurationSection getConfigSection() {
        return (ConfigurationSection) plugin.getConfig().get(getTrigger());
    }

    public final int getConfigValue(String key, int def) {
        return plugin.getConfig().getInt(getTrigger() + "." + key, def);
    }

    public final long getConfigValue(String key, long def) {
        return plugin.getConfig().getLong(getTrigger() + "." + key, def);
    }

    public final double getConfigValue(String key, double def) {
        return plugin.getConfig().getDouble(getTrigger() + "." + key, def);
    }

    public final boolean getConfigValue(String key, boolean def) {
        return plugin.getConfig().getBoolean(getTrigger() + "." + key, def);
    }

    public final String getConfigValue(String key, String def) {
        return plugin.getConfig().getString(getTrigger() + "." + key, def);
    }

    public final void updateConfigValue(String key, int value) {
        plugin.getConfig().set(getTrigger() + "." + key, value);
    }

    public final void updateConfigValue(String key, long value) {
        plugin.getConfig().set(getTrigger() + "." + key, value);
    }

    public final void updateConfigValue(String key, double value) {
        plugin.getConfig().set(getTrigger() + "." + key, value);
    }

    public final void updateConfigValue(String key, boolean value) {
        plugin.getConfig().set(getTrigger() + "." + key, value);
    }

    public final void updateConfigValue(String key, String value) {
        plugin.getConfig().set(getTrigger() + "." + key, value);
    }

    public final void saveConfiguration() {
        plugin.saveConfig();
    }

    // check if player has permission to use this plugin
    public final boolean hasPermissions(Player player) {
        if (player.hasPermission(permissionNode)) {
            return true;
        } else {
            logInfo(player.getName() + " does not have " + permissionNode);
            return false;
        }
    }

    // check if player has the specified permission for this plugin
    public final boolean hasPermissions(CommandSender player, String perm) {
        return checkPermissions(player, perm);
    }

    // just a utility to help with the multiple checks above.
    private boolean checkPermissions(CommandSender player, String perm) {
        if (player.hasPermission(permissionNode + "." + perm)) {
            debugInfo(player.getName() + " has " + permissionNode + "." + perm);
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
        isDebug = getConfigValue("debug", isDebug);
    }

    //------------------------------------
    // built-in commands

    @SubCommand
    private boolean doHelp(CommandSender sender) {
        for (String help : getHelpText(sender)) {
            sender.sendMessage(help);
        }
        return true;
    }

    protected String[] getHelpText(CommandSender sender) {
        return new String[]{getHelpString(sender)};
    }

    protected String getHelpString(CommandSender sender) {
        return "There isn't any help for you...";
    }

    @SubCommand(opOnly = true)
    private boolean doReload(CommandSender sender) {
        if (sender.isOp()) {
            getPlugin().reloadConfig();
            reload();
            sender.sendMessage("Reloaded...");
        } else {
            sender.sendMessage("Permission denied.");
        }
        return true;
    }

    @SubCommand
    private boolean doSettings(CommandSender sender) {
        HashMap<String, String> settings = new HashMap<String, String>();
        Class c = getClass();
        while (c != null && !c.equals(Object.class)) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(Setting.class)) {
                    f.setAccessible(true);
                    try {
                        settings.put(f.getName(), f.get(this).toString());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } finally {
                        f.setAccessible(false);
                    }
                }
            }
            c = c.getSuperclass();
        }
        for (String key : settings.keySet()) {
            sender.sendMessage(key + ": " + settings.get(key));
        }
        return true;
    }

    @SubCommand(opOnly = true)
    private boolean doSet(CommandSender sender, String settingName, String settingValue) {
        // set <setting> <value>
        Class c = getClass();
        while (c != null && !c.equals(Object.class)) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(Setting.class) && field.getName().equalsIgnoreCase(settingName)) {
                    try {
                        field.setAccessible(true);
                        if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
                            Boolean value = Boolean.valueOf(settingValue);
                            field.set(this, value);
                            updateConfigValue(settingName, value);
                        } else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
                            Integer value = Integer.valueOf(settingValue);
                            field.set(this, value);
                            updateConfigValue(settingName, value);
                        } else if (field.getType().equals(Long.class) || field.getType().equals(long.class)) {
                            Long value = Long.valueOf(settingValue);
                            field.set(this, value);
                            updateConfigValue(settingName, value);
                        } else if (field.getType().equals(Double.class) || field.getType().equals(double.class)) {
                            Double value = Double.valueOf(settingValue);
                            field.set(this, value);
                            updateConfigValue(settingName, value);
                        } else if (field.getType().equals(String.class)) {
                            field.set(this, settingValue);
                            updateConfigValue(settingName, settingValue);
                        } else {
                            sender.sendMessage("Setting not found.");
                            return true;
                        }
                        doSettings(sender);
                        saveConfiguration();
                        return true;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } finally {
                        field.setAccessible(false);
                    }
                }
            }
            c = c.getSuperclass();
        }
        sender.sendMessage("Setting not found.");
        return true;
    }

    @SubCommand
    private boolean doCommands(CommandSender sender) {
        for (String i : getHandlerMethods()) {
            sender.sendMessage(i);
        }
        return true;
    }

    //------------------------------------
    // command dispatch methods

    static Class[] args0 = new Class[]{CommandSender.class};
    static Class[] args0p = new Class[]{Player.class};
    static Class[] args1 = new Class[]{CommandSender.class, String.class};
    static Class[] args1p = new Class[]{Player.class, String.class};
    static Class[] args2 = new Class[]{CommandSender.class, String.class, String.class};
    static Class[] args2p = new Class[]{Player.class, String.class, String.class};
    static Class[] args3 = new Class[]{CommandSender.class, String.class, String.class, String.class};
    static Class[] args3p = new Class[]{Player.class, String.class, String.class, String.class};
    static Class[] args4 = new Class[]{CommandSender.class, String.class, String.class, String.class, String.class};
    static Class[] args4p = new Class[]{Player.class, String.class, String.class, String.class, String.class};
    static Class[][] argsX = {args0, args1, args2, args3, args4};
    static Class[][] argsXp = {args0p, args1p, args2p, args3p, args4p};

    private String generateMethodName(String cmdName) {
        return "do" + cmdName.toUpperCase().substring(0, 1) + cmdName.toLowerCase().substring(1);
    }

    private boolean dispatchMethod(CommandSender sender, Command cmd, String[] args) {
        if (args.length > 0) {
            // check to see if it's a subcommand
            Method method = findHandlerMethod(sender, generateMethodName(args[0]), args.length - 1);
            if (method != null) {
                if (method.getAnnotation(SubCommand.class) != null) {
                    //looks like we have a subcommand method.  If it's marked a subcommand execute it.
                    SubCommand annotation = method.getAnnotation(SubCommand.class);
                    if (annotation.opOnly() && !sender.isOp()) {
                        sender.sendMessage("You don't have permissions.");
                    } else {
                        try {
                            method.setAccessible(true);
                            LinkedList<Object> paramList = new LinkedList<Object>();
                            paramList.addFirst(sender);
                            for (int i = 1; i < args.length; i++) {
                                paramList.addLast(args[i]);
                            }
                            Object[] params = paramList.toArray();
                            Object result = method.invoke(this, params);
                            return (Boolean) result;
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                            return false;
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                            return false;
                        } finally {
                            method.setAccessible(false);
                        }
                    }
                }
            }
        }
        Method method = findHandlerMethod(sender, generateMethodName(getTrigger()), args.length);
        if (method != null) {
            try {
                method.setAccessible(true);
                LinkedList<Object> paramList = new LinkedList<Object>();
                paramList.addFirst(sender);
                for (String arg : args) {
                    paramList.addLast(arg);
                }
                Object[] params = paramList.toArray();
                Object result = method.invoke(this, params);
                return (Boolean) result;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return false;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            } finally {
                method.setAccessible(false);
            }
        }

        return false;
    }

    protected Method findHandlerMethod(CommandSender sender, String methodName, int stringParamCount) {
        logInfo("findMethod(" + ((sender instanceof Player) ? "player" : "sender") + "," + methodName + "," + stringParamCount + ")");
        Method method = null;
        if (sender instanceof Player) {
            method = getMethod(methodName, stringParamCount, argsXp);
        }
        if (method == null) {
            method = getMethod(methodName, stringParamCount, argsX);
        }
        return method;
    }

    private Method getMethod(String methodName, int stringParamCount, Class[][] signature) {
        Class clazz = getClass();
        while (clazz != Object.class) {
            try {
                Method method = clazz.getDeclaredMethod(methodName, signature[stringParamCount]);
                if (method.getReturnType().equals(boolean.class)) {
                    return method;
                }
            } catch (NoSuchMethodException e) {
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private List<String> getHandlerMethods() {
        // hey jf - this is a hacky, not so correct implementation
        HashSet<String> methods = new HashSet<String>();
        Class clazz = getClass();
        while (clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                if ((method.getAnnotation(SubCommand.class) != null) || (method.getName().equals(generateMethodName(getTrigger())))) {
                    String name = method.getName().substring(2).toLowerCase();
                    methods.add(name);
                }
            }
            clazz = clazz.getSuperclass();
        }

        LinkedList<String> result = new LinkedList<String>();
        result.addAll(methods);
        Collections.sort(result);
        return result;
    }
}