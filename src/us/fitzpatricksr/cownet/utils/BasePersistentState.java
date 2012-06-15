package us.fitzpatricksr.cownet.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BasePersistentState implements PersistentState {
	protected abstract MemoryConfiguration getConfig();

	public void setPathSeparator(char separator) {
		getConfig().options().pathSeparator(separator);
	}

	protected ConfigurationSection getNode(String node) {
		for (String entry : getConfig().getKeys(true)) {
			if (node.equalsIgnoreCase(entry) && getConfig().isConfigurationSection(entry)) {
				return getConfig().getConfigurationSection(entry);
			}
		}
		return null;
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
					f.setAccessible(true);
					f.set(dest, value);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} finally {
					f.setAccessible(false);
				}
			}
		}
	}

	public static Map<String, Object> serialize(Object source) {
		HashMap<String, Object> result = new HashMap<String, Object>();
		Class<?> c = source.getClass();
		for (Field f : c.getFields()) {
			int modifiers = f.getModifiers();
			if (!Modifier.isVolatile(modifiers) && !Modifier.isTransient(modifiers) && !Modifier.isStatic(modifiers)) {
				String key = f.getName();
				Object value = null;
				try {
					f.setAccessible(true);
					value = f.get(source);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} finally {
					f.setAccessible(false);
				}
				result.put(key, value);
			}
		}
		return result;
	}

	@Override
	public final boolean hasConfigValue(String key) {
		return getConfig().contains(key);
	}

	@Override
	public final int getConfigValue(String key, int def) {
		return getConfig().getInt(key, def);
	}

	@Override
	public final long getConfigValue(String key, long def) {
		return getConfig().getLong(key, def);
	}

	@Override
	public final double getConfigValue(String key, double def) {
		return getConfig().getDouble(key, def);
	}

	@Override
	public final boolean getConfigValue(String key, boolean def) {
		return getConfig().getBoolean(key, def);
	}

	@Override
	public final String getConfigValue(String key, String def) {
		return getConfig().getString(key, def);
	}

	@Override
	public final Map getConfigValue(String key, Map def) {
		if (getConfig().isConfigurationSection(key)) {
			ConfigurationSection section = getConfig().getConfigurationSection(key);
			for (String k : section.getKeys(false)) {
				def.put(k, section.get(k));
			}
			return def;
		} else {
			return def;
		}
	}

	@Override
	public List<?> getConfigValue(String key, List<?> def) {
		return getConfig().getList(key, def);
	}

	@Override
	public Object getConfigValue(String key, Object def) {
		return getConfig().get(key, def);
	}

	@Override
	public List<String> getStringList(String key, List<String> def) {
		List<String> result = getConfig().getStringList(key);
		return (result != null) ? result : def;
	}

	@Override
	public ConfigurationSection getConfigurationSection(String key) {
		return getConfig().getConfigurationSection(key);
	}

	@Override
	public final void updateConfigValue(String key, int value) {
		getConfig().set(key, value);
	}

	@Override
	public final void updateConfigValue(String key, long value) {
		getConfig().set(key, value);
	}

	@Override
	public final void updateConfigValue(String key, double value) {
		getConfig().set(key, value);
	}

	@Override
	public final void updateConfigValue(String key, boolean value) {
		getConfig().set(key, value);
	}

	@Override
	public final void updateConfigValue(String key, String value) {
		getConfig().set(key, value);
	}

	@Override
	public final void updateConfigValue(String key, Map<String, ?> value) {
		getConfig().set(key, value);
	}

	@Override
	public void updateConfigValue(String key, List<?> value) {
		getConfig().set(key, value);
	}

	@Override
	public void updateConfigValue(String key, Object o) {
		getConfig().set(key, o);
	}


}
