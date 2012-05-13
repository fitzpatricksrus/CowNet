package us.fitzpatricksr.cownet;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class CowNetMod extends JavaPlugin {
    private static final String COWNET = "cownet";
    private final Logger logger = Logger.getLogger("Minecraft");
    private Plot plot;

    @Override
    public void onDisable() {
        logger.info("CowNetMod is now disabled!");
        // cancel all tasks for this plugin
        getServer().getScheduler().cancelTasks(this);
    }

    @Override
    public void onEnable() {
        if (getResource("config.yml") == null) {
            this.saveConfig();
        }
        this.getConfig().options().copyDefaults(true);
        if (getConfig().getBoolean("cownet.enable", true)) {
            logger.info("CowNetMod enabled.");
            new Starve(this, COWNET);
            new Bounce(this, COWNET);
            NoSwearing noSwearingMod = new NoSwearing(this, COWNET, "noswearing");
            new TntSheep(this, COWNET);
            new Logins(this, COWNET);
            plot = new Plot(this, COWNET, noSwearingMod);
            new Rank(this, COWNET);
            new Timber(this, COWNET);
            new HardCore(this, COWNET);
            new HungerGames(this, COWNET);
            new Nickname(this, COWNET);
//            new FlingPortal(this, COWNET, "flingportal");
//            new Hide(this, COWNET, "Hide");
        } else {
            logger.info("CowNetMod disabled");
            getPluginLoader().disablePlugin(this);
        }
        this.saveConfig();
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        logger.info("CowNetMod is the terrain generator for " + worldName + "  id:" + id);
        return plot.getDefaultWorldGenerator(worldName, id);
    }

}

