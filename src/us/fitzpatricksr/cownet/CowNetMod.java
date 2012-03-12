package us.fitzpatricksr.cownet;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class CowNetMod extends JavaPlugin {
    private static final String COWNET = "cownet";
    private final Logger logger = Logger.getLogger("Minecraft");

    @Override
    public void onDisable() {
        logger.info("CowNetMod is now disabled!");
        getServer().getScheduler().cancelAllTasks();
    }

    @Override
    public void onEnable() {
        this.getConfig().options().copyDefaults(true);
        if (getConfig().getBoolean("cownet.enable", false)) {
            logger.info("CowNetMod enabled.");
            new StarveCommand(this, COWNET, "starve");
            new BounceCommand(this, COWNET, "bounce");
            new NoSwearing(this, COWNET, "noswearing");
            new ExplodingSheep(this, COWNET, "tntsheep");
            new LoginHistory(this, COWNET, "logins");
            new Plots(this, COWNET, "plot");
//            new FlingPortal(this, COWNET, "flingportal");
        } else {
            logger.info("CowNetMod disabled");
            getPluginLoader().disablePlugin(this);
        }
        this.saveConfig();
    }
}

