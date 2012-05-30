package us.fitzpatricksr.cownet;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetThingy;

import java.util.logging.Logger;

public class CowNetMod extends JavaPlugin {
	private static final String COWNET = "cownet";
	private final Logger logger = Logger.getLogger("Minecraft");
	private Plot plot;
	private CowNetThingy[] commands;

	@Override
	public void onLoad() {
		for (CowNetThingy thingy : getCommandList()) {
			thingy.reloadSettings();
		}
	}

	@Override
	public void onDisable() {
		for (CowNetThingy thingy : getCommandList()) {
			thingy.onDisable();
		}
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
		} else {
			logger.info("CowNetMod disabled");
			getPluginLoader().disablePlugin(this);
		}
		this.saveConfig();
		for (CowNetThingy thingy : getCommandList()) {
			thingy.onEnable();
		}
	}

	private CowNetThingy[] getCommandList() {
		if (commands == null) {
			NoSwearing noSwearingMod = new NoSwearing(this, COWNET);
			plot = new Plot(this, COWNET, noSwearingMod);
			commands = new CowNetThingy[] {new Starve(this, COWNET), new Bounce(this, COWNET), new TntSheep(this, COWNET), new Logins(this, COWNET), noSwearingMod, plot, new Rank(this, COWNET), new Timber(this, COWNET), new HardCore(this, COWNET), new HungerGames(this, COWNET), new Nickname(this, COWNET),
					//            new FlingPortal(this, COWNET, "flingportal");
					new Hide(this, COWNET), new Snapshot(this, COWNET),};
		}
		return commands;
	}

	@Override
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
		logger.info("CowNetMod is the terrain generator for " + worldName + "  id:" + id);
		return plot.getDefaultWorldGenerator(worldName, id);
	}

}

