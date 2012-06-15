package us.fitzpatricksr.cownet.utils;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public interface PersistentState {
	boolean hasConfigValue(String key);

	int getConfigValue(String key, int def);

	long getConfigValue(String key, long def);

	double getConfigValue(String key, double def);

	boolean getConfigValue(String key, boolean def);

	String getConfigValue(String key, String def);

	Map getConfigValue(String key, Map def);

	List<?> getConfigValue(String key, List<?> def);

	Object getConfigValue(String key, Object def);

	List<String> getStringList(String key, List<String> def);

	ConfigurationSection getConfigurationSection(String key);

	void updateConfigValue(String key, int value);

	void updateConfigValue(String key, long value);

	void updateConfigValue(String key, double value);

	void updateConfigValue(String key, boolean value);

	void updateConfigValue(String key, String value);

	void updateConfigValue(String key, Map<String, ?> value);

	void updateConfigValue(String key, List<?> value);

	void updateConfigValue(String key, Object o);
}
