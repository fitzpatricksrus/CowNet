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

        String permission() default "";

        String help() default "";
    }

    private Logger logger = Logger.getLogger("Minecraft");
    private JavaPlugin plugin;
    private String trigger = "";
    private String permissionNode = "";
    private boolean isEnabled = false;
    @Setting()
    private boolean isDebug = false;

    protected CowNetThingy() {
        //for testing only
        trigger = "test";
        isEnabled = true;
        isDebug = true;
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

    @Deprecated
    protected boolean handleCommand(Player sender, Command cmd, String[] args) {
        return false;
    }

    @Deprecated
    protected boolean handleCommand(CommandSender sender, Command cmd, String[] args) {
        return false;
    }

    //------------------------------------
    // built-in command

    @SubCommand
    public final boolean doHelp(CommandSender sender) {
        for (String help : getHelpText(sender)) {
            sender.sendMessage(help);
        }
        return true;
    }

    @SubCommand(opOnly = true)
    public final boolean doReload(CommandSender sender) {
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
    private final boolean doSettings(CommandSender sender) {
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
    public final boolean doSet(CommandSender sender, String settingName, String settingValue) {
        if (sender.isOp()) {
            // set <setting> <value>
            Class c = getClass();
            while (c != null && !c.equals(Object.class)) {
                for (Field field : c.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Setting.class) && field.getName().equalsIgnoreCase(settingName)) {
                        try {
                            field.setAccessible(true);
                            if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
                                field.set(this, Boolean.valueOf(settingValue));
                                return true;
                            } else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
                                field.set(this, Integer.valueOf(settingValue));
                                return true;
                            } else if (field.getType().equals(String.class)) {
                                field.set(this, settingValue);
                                return true;
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } finally {
                            field.setAccessible(false);
                        }
                    }
                }
                c = c.getSuperclass();
            }
            sender.sendMessage("Setting not found.");
            return true;
        } else {
            sender.sendMessage("You don't have permission.");
        }
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
        Method method = findHandlerMethod(sender, generateMethodName(trigger), args.length);
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

        return (sender instanceof Player) ? handleCommand((Player) sender, null, args) || handleCommand(sender, null, args) : handleCommand(sender, null, args);
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

    public static void main(String[] args) {
        CowNetThingy thingy = new CowNetThingy();
        thingy.dispatchMethod(null, null, new String[]{"commands"});
    }
}