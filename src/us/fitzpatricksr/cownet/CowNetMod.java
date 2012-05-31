package us.fitzpatricksr.cownet;

import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.commands.Bounce;
import us.fitzpatricksr.cownet.commands.HardCore;
import us.fitzpatricksr.cownet.commands.Hide;
import us.fitzpatricksr.cownet.commands.HungerGames;
import us.fitzpatricksr.cownet.commands.Logins;
import us.fitzpatricksr.cownet.commands.Nickname;
import us.fitzpatricksr.cownet.commands.NoSwearing;
import us.fitzpatricksr.cownet.commands.Plot;
import us.fitzpatricksr.cownet.commands.Rank;
import us.fitzpatricksr.cownet.commands.Snapshot;
import us.fitzpatricksr.cownet.commands.Starve;
import us.fitzpatricksr.cownet.commands.Timber;
import us.fitzpatricksr.cownet.commands.TntSheep;

import java.util.logging.Logger;

public class CowNetMod extends JavaPlugin {
	private static final String COWNET = "cownet";
	private final Logger logger = Logger.getLogger("Minecraft");
	private Plot plot;
	private CowNetThingy[] commands;

	public CowNetMod() {
		NoSwearing noSwearingMod = new NoSwearing(this, COWNET);
		plot = new Plot(this, COWNET, noSwearingMod);
		commands = new CowNetThingy[] {
				new Starve(this, COWNET),
				new Bounce(this, COWNET),
				new TntSheep(this, COWNET),
				new Logins(this, COWNET),
				noSwearingMod,
				plot,
				new Rank(this, COWNET),
				new Timber(this, COWNET),
				new HardCore(this, COWNET),
				new HungerGames(this, COWNET),
				new Nickname(this, COWNET),
				// new FlingPortal(this, COWNET, "flingportal");
				new Hide(this, COWNET),
				new Snapshot(this, COWNET),
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
		}
		this.getConfig().options().copyDefaults(true);
		if (getConfig().getBoolean("cownet.enable", true)) {
			logger.info("CowNetMod enabled.");
			for (CowNetThingy thingy : commands) {
				if (thingy.isEnabled()) {
					thingy.logInfo(thingy.getTrigger() + " enabled");
					thingy.loadCommands();
					thingy.reloadSettings();
					thingy.onEnable();
					if (thingy instanceof Listener) {
						getServer().getPluginManager().registerEvents((Listener) thingy, this);
					}
					getCommand(thingy.getTrigger()).setExecutor(thingy);
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

}

