package us.fitzpatricksr.cownet.utils;


import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class CowNetConfig extends YamlConfiguration {
    private JavaPlugin plugin;
    private String name;

    public CowNetConfig(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public void loadConfig() throws IOException, InvalidConfigurationException {
        load(getConfigFile());
    }

    public void saveConfig() throws IOException {
        save(getConfigFile());
    }

    protected ConfigurationSection getNode(String node) {
        for (String entry : getKeys(true)) {
            if (node.equalsIgnoreCase(entry) && isConfigurationSection(entry)) {
                return getConfigurationSection(entry);
            }
        }
        return null;
    }

    private File getConfigFile() throws IOException {
        File folder = plugin.getDataFolder();
        if (!folder.exists()) {
            folder.mkdir();
        }
        File file = new File(folder, name);
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }
}
