package us.fitzpatricksr.cownet;

import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.commands.*;

import java.util.logging.Logger;

public class CowNetMod extends JavaPlugin {
    private static final String COWNET = "cownet";
    private final Logger logger = Logger.getLogger("Minecraft");
    private Plot plot;
    private CowNetThingy[] commands;

    public CowNetMod() {
        NoSwearing noSwearingMod = new NoSwearing();
        CowPerms cowPerms = new CowPerms();
        plot = new Plot(noSwearingMod);
        commands = new CowNetThingy[]{
                new Starve(),
                new Bounce(),
                new TntSheep(),
                new Logins(),
                noSwearingMod,
                plot,
                new Timber(),
                new HardCore(),
                new HungerGames(),
                // new Nickname(),
                // new FlingPortal(, "flingportal");
                new Hide(),
                // new Snapshot(),
                // new Jail(),
                cowPerms,
                new CowRank(cowPerms),
                new CowWarp(),
                new TntWars(),
                new SnowWars(),
                new Panic()
        };
    }

    @Override
    public void onDisable() {
        // cancel all tasks for this plugin
        getServer().getScheduler().cancelTasks(this);
        for (CowNetThingy thingy : commands) {
            thingy.onDisable();
        }
        logger.info("CowNetMod is now disabled!");
    }

    @Override
    public void onEnable() {
        if (getResource("config.yml") == null) {
            this.saveConfig();
            this.getConfig().options().copyDefaults(true);
            this.saveConfig();
        }
        if (getConfig().getBoolean("cownet.enable", true)) {
            logger.info("CowNetMod enabled.");
            for (CowNetThingy thingy : commands) {
                thingy.setPlugin(this);
                thingy.setPermissionRoot(COWNET);
                if (thingy.getConfigValue("enable", false)) {
                    thingy.logInfo(thingy.getTrigger() + " enabled");
                    thingy.loadCommands();
                    try {
                        thingy.reloadSettings();
                        thingy.onEnable();
                        if (thingy instanceof Listener) {
                            getServer().getPluginManager().registerEvents((Listener) thingy, this);
                        }
                        getCommand(thingy.getTrigger()).setExecutor(thingy);
                    } catch (Exception e) {
                        e.printStackTrace();
                        thingy.logInfo(thingy.getTrigger() + " disabled");
                    }
                } else {
                    thingy.logInfo(thingy.getTrigger() + " disabled");
                }
            }
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

    public CowNetThingy getThingy(String name) {
        for (CowNetThingy thingy : commands) {
            if (thingy.getTrigger().equals(name)) {
                return thingy;
            }
        }
        return null;
    }
}

