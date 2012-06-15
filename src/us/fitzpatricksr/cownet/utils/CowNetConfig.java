package us.fitzpatricksr.cownet.utils;


import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class CowNetConfig extends BasePersistentState {
	private JavaPlugin plugin;
	private String fileName;
	private YamlConfiguration configuration;

	public CowNetConfig(JavaPlugin plugin) {
		this.plugin = plugin;
		configuration = new YamlConfiguration();
	}

	public CowNetConfig(JavaPlugin plugin, String fileName) {
		this.plugin = plugin;
		this.fileName = fileName;
		configuration = new YamlConfiguration();
	}

	protected JavaPlugin getPlugin() {
		return plugin;
	}

	@Override
	protected MemoryConfiguration getConfig() {
		return configuration;
	}

	protected String getFileName() {
		return fileName;
	}

	public void loadConfig() throws IOException, InvalidConfigurationException {
		configuration.load(getConfigFile());
	}

	public void saveConfig() throws IOException {
		configuration.save(getConfigFile());
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
}
