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
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class CowNetThingy implements CommandExecutor {

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Setting {
		String name() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface CowCommand {
		boolean opOnly() default false;

		String permission() default "";
	}

	private Logger logger = Logger.getLogger("Minecraft");
	private JavaPlugin plugin;
	private String permissionNode = "";
	private boolean isEnabled = false;

	@Setting
	private boolean debug = false;

	public CowNetThingy() {
		// for testing only
	}

	public CowNetThingy(JavaPlugin plugin, String permissionRoot) {
		this.plugin = plugin;
		this.permissionNode = permissionRoot + "." + getTrigger();
		this.isEnabled = getConfigValue("enable", false);
		if (!this.isEnabled()) {
			//allow this common alias
			this.isEnabled = getConfigValue("enabled", true);
		}
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
		return dispatchMethod(sender, args);
	}

	public final String getTrigger() {
		return getClass().getSimpleName().toLowerCase();
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
		if (debug) {
			logger.info("[" + permissionNode + "]: " + msg);
		}
	}

	public final boolean isEnabled() {
		return isEnabled;
	}

	public final boolean isDebug() {
		return debug;
	}

	// disable the plugin if there was some critical startup error.
	protected final void disable() {
		isEnabled = false;
		logInfo("Plugin disabled.");
	}

	//------------------------------------
	// built-in commands

	@CowCommand
	private boolean doHelp(CommandSender sender) {
		for (String help : getHelpText(sender)) {
			sender.sendMessage(help);
		}
		return true;
	}

	protected String[] getHelpText(CommandSender sender) {
		return new String[] {getHelpString(sender)};
	}

	protected String getHelpString(CommandSender sender) {
		return "There is no help for you...";
	}

	//------------------------------------
	// methods related to settings and persistent configuration

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

	/**
	 * This hook allows subclasses to load non-automatic setting data.
	 * You shouldn't call this directly, but through reloadSettings() instead.
	 */
	protected void reloadManualSettings() {
	}

	/**
	 * Subclasses should implement this to return information about
	 * the state of manual settings.
	 *
	 * @return a HashMap of (settingName -> settingValue)
	 */
	protected HashMap<String, String> getManualSettings() {
		//return name, value
		return new HashMap<String, String>();
	}

	/**
	 * This is a hook for subclasses to load configuration information
	 * that isn't in one of the support formats.  Enums for example.
	 * Subclasses should update the value of the runtime setting
	 * and the persistent storage.  Storage will be save automatically.
	 *
	 * @param settingName  the name of the setting to update
	 * @param settingValue the new setting value
	 * @return true if the setting was updated.  false if not.
	 */
	protected boolean updateManualSetting(String settingName, String settingValue) {
		return false;
	}

	/**
	 * Command to reload configuration data from disk.
	 */
	@CowCommand(opOnly = true)
	private boolean doReload(CommandSender sender) {
		getPlugin().reloadConfig();
		reloadSettings(sender);
		sender.sendMessage("Reloaded.");
		return true;
	}

	/**
	 * Dump a list of setting that the specified sender has access to.
	 */
	@CowCommand(opOnly = true)
	private boolean doSettings(CommandSender sender) {
		HashMap<String, Field> autoSettings = getAutomaticSettings();
		HashMap<String, String> manualSettings = getManualSettings();
		List<String> settingNames = new LinkedList<String>(autoSettings.keySet());
		settingNames.addAll(manualSettings.keySet());
		Collections.sort(settingNames);
		for (String settingName : settingNames) {
			Object value = manualSettings.get(settingName);
			if (value == null || value instanceof Field) {
				// it's not a manual setting
				Field f = (value != null) ? (Field) value : autoSettings.get(settingName);
				try {
					f.setAccessible(true);
					value = f.get(this).toString();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} finally {
					f.setAccessible(false);
				}
			}
			if (sender != null) {
				sender.sendMessage(settingName + ": " + value);
			} else {
				// this is a hack to show settings at startup time since there's no sender then
				logInfo(settingName + ": " + value);
			}
		}
		return true;
	}

	/**
	 * command to set the value of a setting, both manual and automatic. Changes will be persistent.
	 */
	@CowCommand(opOnly = true)
	private boolean doSet(CommandSender sender, String settingName, String settingValue) {
		// set <setting> <value>
		if (!setAutoSettingValue(settingName, settingValue) && !updateManualSetting(settingName, settingValue)) {
			sender.sendMessage("Setting not found or could not be updated.");
		} else {
			saveConfiguration();
			doSettings(sender);
		}
		return true;
	}

	/**
	 * Reload all manual and magic settings and dump them to the sender/console.
	 * This is the method that should be called to load settings from a configuration
	 * at startup or after the configuration has been changed by some external source.
	 * When complete, net settings will be dumped to the console.
	 */
	protected final void reloadSettings() {
		reloadSettings(null);
	}

	/**
	 * Reload all manual and magic settings and dump them to the sender/console.
	 * This is the method that should be called to load settings from a configuration
	 * at startup or after the configuration has been changed by some external source.
	 *
	 * @param sender where to dump the new setting values when finished.  If NULL
	 *               settings will be dumped to the console.
	 */
	protected final void reloadSettings(CommandSender sender) {
		reloadAutoSettings();
		reloadManualSettings();
		doSettings(sender);
	}

	private Object getAutoSettingValue(String settingName) {
		return getAutoSettingValue(this, settingName);
	}

	/**
	 * Get the value of the automaticSetting with the specified name for the specified instance
	 *
	 * @param source      the object containing the setting
	 * @param settingName the name of the setting
	 * @return the value of the setting for the specified object
	 */
	private Object getAutoSettingValue(Object source, String settingName) {
		Field field = getAutomaticSettings(source).get(settingName);
		if (field != null) {
			try {
				field.setAccessible(true);
				return field.get(source);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} finally {
				field.setAccessible(false);
			}
		}
		return null;
	}

	private boolean setAutoSettingValue(String settingName, String settingValue) {
		return setAutoSettingValue(this, settingName, settingValue);
	}

	/**
	 * Update the value of the specified setting for the specified object.  The new setting value will
	 * be persisted in the configuration for this CowNetThingy.
	 *
	 * @param source       The object whos setting will change
	 * @param settingName  The name of the setting
	 * @param settingValue The new value that will be set and persisted.
	 * @return true if the new value was set.  false if the setting value was unchanged.
	 */
	protected final boolean setAutoSettingValue(Object source, String settingName, String settingValue) {
		Field field = getAutomaticSettings(source).get(settingName);
		if (field != null) {
			boolean isStatic = Modifier.isStatic(field.getModifiers());
			boolean isClass = source instanceof Class;
			if (isStatic == isClass) {
				try {
					field.setAccessible(true);
					if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
						Boolean value = Boolean.valueOf(settingValue);
						field.set(source, value);
						updateConfigValue(settingName, value);
						return true;
					} else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
						Integer value = Integer.valueOf(settingValue);
						field.set(source, value);
						updateConfigValue(settingName, value);
						return true;
					} else if (field.getType().equals(Long.class) || field.getType().equals(long.class)) {
						Long value = Long.valueOf(settingValue);
						field.set(source, value);
						updateConfigValue(settingName, value);
						return true;
					} else if (field.getType().equals(Double.class) || field.getType().equals(double.class)) {
						Double value = Double.valueOf(settingValue);
						field.set(source, value);
						updateConfigValue(settingName, value);
						return true;
					} else if (field.getType().equals(String.class)) {
						field.set(source, settingValue);
						updateConfigValue(settingName, settingValue);
						return true;
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} finally {
					field.setAccessible(false);
				}
			}
		}
		return false;
	}

	// reload settings that are handled by the settings magic
	private void reloadAutoSettings() {
		reloadAutoSettings(this);
	}

	/**
	 * Reload all the automatic settings for the specified object.  The source of the new setting
	 * values is this CowNetThingy.
	 *
	 * @param source Object instance whos settings should be reloaded.
	 */
	protected final void reloadAutoSettings(Object source) {
		HashMap<String, Field> settings = getAutomaticSettings(source);
		for (String settingName : settings.keySet()) {
			Field field = settings.get(settingName);
			try {
				field.setAccessible(true);
				if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
					field.setBoolean(source, getConfigValue(settingName, field.getBoolean(source)));
				} else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
					field.setInt(source, getConfigValue(settingName, field.getInt(source)));
				} else if (field.getType().equals(Long.class) || field.getType().equals(long.class)) {
					field.setLong(source, getConfigValue(settingName, field.getLong(source)));
				} else if (field.getType().equals(Double.class) || field.getType().equals(double.class)) {
					field.setDouble(source, getConfigValue(settingName, field.getDouble(source)));
				} else if (field.getType().equals(String.class)) {
					field.set(source, getConfigValue(settingName, field.get(source).toString()));
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} finally {
				field.setAccessible(false);
			}
		}
	}

	private HashMap<String, Field> getAutomaticSettings() {
		return getAutomaticSettings(this);
	}

	// return a Map of (settingName -> field) for the specified object
	private HashMap<String, Field> getAutomaticSettings(Object source) {
		Class clazz = (source instanceof Class) ? (Class) source : source.getClass();
		HashMap<String, Field> settings = new HashMap<String, Field>();
		while (clazz != null && !clazz.equals(Object.class)) {
			for (Field f : clazz.getDeclaredFields()) {
				if (f.isAnnotationPresent(Setting.class)) {
					Setting settingAnnotation = f.getAnnotation(Setting.class);
					String name = (settingAnnotation.name().isEmpty()) ? f.getName() : settingAnnotation.name();
					settings.put(name, f);
				}
			}
			clazz = clazz.getSuperclass();
		}
		return settings;
	}

	/**
	 * Return a map of all the automaticSettings for the specified object.  The key is the
	 * name of the setting and the value is the settings current value.
	 *
	 * @param source object to pull settings from
	 * @return a map of (settingName -> settingValue)
	 */
	protected final HashMap<String, String> getSettingValueMapFor(Object source) {
		//this is a very expensive type cast.  I hate java generics.
		HashMap<String, Field> auto = getAutomaticSettings(source);
		HashMap<String, String> result = new HashMap<String, String>(auto.size());
		for (String key : auto.keySet()) {
			try {
				Field field = auto.get(key);
				boolean isClass = source instanceof Class;
				boolean isStatic = Modifier.isStatic(field.getModifiers());
				if (isClass == isStatic) {
					// only expose static for classes and non-statics for instances
					result.put(key, field.get(source).toString());
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return result;
	}


	//------------------------------------
	// command dispatch methods

	/**
	 * dump a list of available commands to sender
	 */
	@CowCommand
	private boolean doCommands(CommandSender sender) {
		for (String i : getHandlerMethods(sender)) {
			sender.sendMessage(i);
		}
		return true;
	}

	private static final Class[] args0 = new Class[] {CommandSender.class};
	private static final Class[] args0p = new Class[] {Player.class};
	private static final Class[] args1 = new Class[] {CommandSender.class, String.class};
	private static final Class[] args1p = new Class[] {Player.class, String.class};
	private static final Class[] args2 = new Class[] {CommandSender.class, String.class, String.class};
	private static final Class[] args2p = new Class[] {Player.class, String.class, String.class};
	private static final Class[] args3 = new Class[] {CommandSender.class, String.class, String.class, String.class};
	private static final Class[] args3p = new Class[] {Player.class, String.class, String.class, String.class};
	private static final Class[] args4 = new Class[] {CommandSender.class, String.class, String.class, String.class, String.class};
	private static final Class[] args4p = new Class[] {Player.class, String.class, String.class, String.class, String.class};
	private static final Class[][] argsX = {args0, args1, args2, args3, args4};
	private static final Class[][] argsXp = {args0p, args1p, args2p, args3p, args4p};
	private static final String COMMAND_METHOD_PREFIX = "do";

	private boolean dispatchMethod(CommandSender sender, String[] args) {
		if (args.length > 0) {
			// check to see if it's a subcommand
			Method method = findHandlerMethod(sender, generateMethodName(args[0]), args.length - 1);
			if (method != null) {
				if (method.getAnnotation(CowCommand.class) != null) {
					//looks like we have a subcommand method.  If it's marked a subcommand execute it.
					CowCommand annotation = method.getAnnotation(CowCommand.class);
					if (!hasMethodPermissions(sender, annotation)) {
						sender.sendMessage("You don't have permissions.");
						return true;
					}

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
		Method method = findHandlerMethod(sender, generateMethodName(getTrigger()), args.length);
		if (method != null) {
			try {
				if (method.getAnnotation(CowCommand.class) != null) {
					//looks like we have a CowCommand method.  If it's marked a CowCommand execute it.
					CowCommand annotation = method.getAnnotation(CowCommand.class);
					if (!hasMethodPermissions(sender, annotation)) {
						sender.sendMessage("You don't have permissions.");
						return true;
					}

					method.setAccessible(true);
					LinkedList<Object> paramList = new LinkedList<Object>();
					paramList.addFirst(sender);
					for (String arg : args) {
						paramList.addLast(arg);
					}
					Object[] params = paramList.toArray();
					Object result = method.invoke(this, params);
					return (Boolean) result;
				}
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
		// hey jf - if we iterate the methods we could have it be case insensitive, but we'd have to
		// deal with walking the class hierarchy and matching parameters
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
				// didn't find one here...
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}

	private List<String> getHandlerMethods() {
		return getHandlerMethods(null);
	}

	private List<String> getHandlerMethods(CommandSender sender) {
		HashSet<String> methods = new HashSet<String>();
		Class clazz = getClass();
		while (clazz != Object.class) {
			for (Method method : clazz.getDeclaredMethods()) {
				if ((method.getAnnotation(CowCommand.class) != null) || (method.getName().equals(generateMethodName(getTrigger())))) {
					//OK, we found a command, only add it if the sender has access to it.
					if (hasMethodPermissions(sender, method.getAnnotation(CowCommand.class))) {
						// strip off COMMAND_METHOD_PREFIX at the beginning.
						String name = method.getName().substring(COMMAND_METHOD_PREFIX.length()).toLowerCase();
						methods.add(name);
					}
				}
			}
			clazz = clazz.getSuperclass();
		}

		LinkedList<String> result = new LinkedList<String>();
		result.addAll(methods);
		Collections.sort(result);
		return result;
	}

	private String generateMethodName(String cmdName) {
		return COMMAND_METHOD_PREFIX + cmdName.toUpperCase().substring(0, 1) + cmdName.toLowerCase().substring(1);
	}

	private boolean hasMethodPermissions(CommandSender sender, CowCommand annotation) {
		if (sender == null) return true;
		if (annotation.opOnly() && !sender.isOp()) {
			return false;
		} else {
			//if it's a player and the method has an annotated permission, check it.
			if (sender instanceof Player) {
				Player player = (Player) sender;
				String permNeeded = annotation.permission();
				if (!permNeeded.isEmpty() && !hasPermissions(player, permNeeded)) {
					sender.sendMessage("You don't have permissions.");
					return false;
				}
			}
			return true;
		}
	}
}