package us.fitzpatricksr.cownet;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class CowNetMod extends JavaPlugin {
    private static final String COWNET = "cownet";
    private final Logger logger = Logger.getLogger("Minecraft");
    private Plots plots;

    @Override
    public void onDisable() {
        logger.info("CowNetMod is now disabled!");
        getServer().getScheduler().cancelAllTasks();
    }

    @Override
    public void onEnable() {
        this.getConfig().options().copyDefaults(true);
        if (getConfig().getBoolean("cownet.enable", true)) {
            logger.info("CowNetMod enabled.");
            new StarveCommand(this, COWNET, "starve");
            new BounceCommand(this, COWNET, "bounce");
            NoSwearing noSwearingMod = new NoSwearing(this, COWNET, "noswearing");
            new ExplodingSheep(this, COWNET, "tntsheep");
            new LoginHistory(this, COWNET, "logins");
            plots = new Plots(this, COWNET, "plot", noSwearingMod);
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
        return plots.getDefaultWorldGenerator(worldName, id);
    }

}

