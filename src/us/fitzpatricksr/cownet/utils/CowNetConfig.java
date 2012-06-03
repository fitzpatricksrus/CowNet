package us.fitzpatricksr.cownet.utils;


import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class CowNetConfig extends YamlConfiguration {
	private JavaPlugin plugin;
	private String fileName;

	public CowNetConfig(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	public CowNetConfig(JavaPlugin plugin, String fileName) {
		this.plugin = plugin;
		this.fileName = fileName;
	}

	protected String getFileName() {
		return fileName;
	}

	public void loadConfig() throws IOException, InvalidConfigurationException {
		load(getConfigFile());
	}

	public void saveConfig() throws IOException {
		save(getConfigFile());
	}

	public void setPathSeparator(char separator) {
		options().pathSeparator('/');
	}

	protected ConfigurationSection getNode(String node) {
		for (String entry : getKeys(true)) {
			if (node.equalsIgnoreCase(entry) && isConfigurationSection(entry)) {
				return getConfigurationSection(entry);
			}
		}
		return null;
	}

	protected File getConfigFile() throws IOException {
		File folder = plugin.getDataFolder();
		if (!folder.exists()) {
			folder.mkdir();
		}
		File file = new File(folder, getFileName());
		if (!file.exists()) {
			file.createNewFile();
		}
		return file;
	}

	/*
			Utility routines for saving state.
		 */

	public static void deserialize(Object dest, Map<String, Object> map) {
		Class<?> c = dest.getClass();
		for (Field f : c.getFields()) {
			String key = f.getName();
			Object value = map.get(key);
			if (value != null) {
				try {
					f.set(dest, value);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static Map<String, Object> serialize(Object source) {
		HashMap<String, Object> result = new HashMap<String, Object>();
		Class<?> c = source.getClass();
		for (Field f : c.getFields()) {
			int modifiers = f.getModifiers();
			if (!Modifier.isVolatile(modifiers) && !Modifier.isTransient(modifiers) && !Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
				String key = f.getName();
				Object value = null;
				try {
					value = f.get(source);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				result.put(key, value);
			}
		}
		return result;
	}
}
