package us.fitzpatricksr.cownet.commands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import us.fitzpatricksr.cownet.commands.games.GameStats;
import us.fitzpatricksr.cownet.commands.games.GameStatsMemory;
import us.fitzpatricksr.cownet.commands.games.GatheredGame;
import us.fitzpatricksr.cownet.utils.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 */
public class SnowWars extends GatheredGame implements org.bukkit.event.Listener {
	private static final String KILLS_KEY = "kills";
	private static final String DEATHS_KEY = "deaths";
	private static final String SNOW_THROWN_KEY = "snowThrown";
	private final Random rand = new Random();

	@Setting
	private int minPlayers = 2;
	@Setting
	private String loungeWarpName = "snowWarsLounge";
	@Setting
	private String spawnWarpName = "snowWarsSpawn";
	@Setting
	private int refillRate = 60;    // how often a player's supply is topped off
	@Setting
	private int refillSize = 5;        // how often a player's supply is topped off
	@Setting
	private int spawnJiggle = 5;

	private GameStats tempStats;
	private int gameTaskId;


	// --------------------------------------------------------------
	// ---- Settings management

	@Override
	protected String getGameName() {
		return "SnowWars";
	}

	@Override
	protected int getMinPlayers() {
		return minPlayers;
	}

	@Override
	protected String[] getHelpText(CommandSender player) {
		return new String[] {
				"usage: /" + getTrigger() + " join | info | quit | tp <player> | start",
				"   join - join the games",
				"   info - what's the state of the current game?",
				"   quit - chicken out and just watch",
				"   tp <player> - transport to a player, if you're a spectator",
				"   start - just get things started already!",
				"   scores - how players did last game.",
				"   stats <player> - see someone's lifetime stats.",
				"   leaders <kills | deaths | throws | stealth | accuracy> - Global rank in a category."
		};
	}

	// --------------------------------------------------------------
	// ---- user commands

	@CowCommand
	private boolean doScores(CommandSender sender) {
		if (tempStats != null) {
			sender.sendMessage("Scores for most recent game: ");
			for (String playerName : tempStats.getPlayerNames()) {
				double k = tempStats.getStat(playerName, KILLS_KEY);
				double d = tempStats.getStat(playerName, DEATHS_KEY);
				double b = tempStats.getStat(playerName, SNOW_THROWN_KEY);
				double accuracy = (b != 0) ? k / b : 0;
				double stealth = (d != 0) ? k / d : k;
				sender.sendMessage("  " + StringUtils.fitToColumnSize(playerName, 10) + " accuracy = " + StringUtils.fitToColumnSize(Double.toString(accuracy), 5) + " stealth = " + StringUtils.fitToColumnSize(Double.toString(stealth), 5));
			}
		}
		return true;
	}

	@CowCommand
	private boolean doStats(Player player) {
		return doStats(player, player.getName());
	}

	@CowCommand
	private boolean doStats(CommandSender player, String playerName) {
		double k = getHistoricStats().getStat(playerName, KILLS_KEY);
		double d = getHistoricStats().getStat(playerName, DEATHS_KEY);
		double b = getHistoricStats().getStat(playerName, SNOW_THROWN_KEY);
		double accuracy = (b != 0) ? k / b : 0;
		double stealth = (d != 0) ? k / d : k;
		player.sendMessage("Your stats: ");
		player.sendMessage("  you pegged: " + k);
		player.sendMessage("  you were hit: " + d);
		player.sendMessage("  you threw: " + b);
		player.sendMessage("  accuracy = " + StringUtils.fitToColumnSize(Double.toString(accuracy * 100), 5) + "%");
		player.sendMessage("  stealth = " + StringUtils.fitToColumnSize(Double.toString(stealth * 100), 5) + "%");
		return true;
	}

	@CowCommand
	private boolean doLeadersKills(CommandSender sender) {
		sender.sendMessage("Top killers: ");
		dumpLeaders(sender, getHistoricStats().getStatSummary(KILLS_KEY));
		return true;
	}

	@CowCommand
	private boolean doLeadersDeaths(CommandSender sender) {
		sender.sendMessage("Most likely to die: ");
		dumpLeaders(sender, getHistoricStats().getStatSummary(DEATHS_KEY));
		return true;
	}

	@CowCommand
	private boolean doLeadersThrows(CommandSender sender) {
		sender.sendMessage("Top throwers: ");
		dumpLeaders(sender, getHistoricStats().getStatSummary(SNOW_THROWN_KEY));
		return true;
	}

	@CowCommand
	private boolean doLeadersAccuracy(CommandSender sender) {
		sender.sendMessage("Most accurate: ");
		HashMap<String, Double> accuracy = new HashMap<String, Double>();
		for (String playerName : getHistoricStats().getPlayerNames()) {
			double k = getHistoricStats().getStat(playerName, KILLS_KEY);
			double b = getHistoricStats().getStat(playerName, SNOW_THROWN_KEY);
			double a = (b != 0) ? k / b : 0;
			accuracy.put(playerName, a * 100);
		}
		dumpLeaders(sender, accuracy);
		return true;
	}

	@CowCommand
	private boolean doLeadersStealth(CommandSender sender) {
		sender.sendMessage("Top stealthy: ");
		HashMap<String, Double> stealth = new HashMap<String, Double>();
		for (String playerName : getHistoricStats().getPlayerNames()) {
			double k = getHistoricStats().getStat(playerName, KILLS_KEY);
			double d = getHistoricStats().getStat(playerName, DEATHS_KEY);
			double s = (d != 0) ? k / d : k;
			stealth.put(playerName, s * 100);
		}
		dumpLeaders(sender, stealth);
		return true;
	}

	private void dumpLeaders(CommandSender sender, Map<String, Double> map) {
		TreeMap<Double, String> sortedMap = new TreeMap<Double, String>();
		for (Map.Entry<String, Double> entry : map.entrySet()) {
			sortedMap.put(entry.getValue(), entry.getKey());
		}
		for (Map.Entry entry : sortedMap.entrySet()) {
			sender.sendMessage("  " + StringUtils.fitToColumnSize(entry.getValue().toString(), 15) + ": " + StringUtils.fitToColumnSize(entry.getKey().toString(), 5));
		}
	}

	// --------------------------------------------------------------
	// ---- game state transitions

	@Override
	protected void handleGathering() {
		debugInfo("handleGathering");
		broadcastToAllOnlinePlayers("A game is gathering.  To join use /" + getTrigger() + " join.");
	}

	@Override
	protected void handleLounging() {
		debugInfo("handleLounging");
		broadcastToAllOnlinePlayers("All the players are ready.  The games are about to start.");
		// teleport everyone to the lounge
		Location loc = getWarpPoint(loungeWarpName, spawnJiggle);
		if (loc != null) {
			// hey jf - you need to jiggle this a bit or everyone will be on top of each other
			Server server = getPlugin().getServer();
			for (String playerName : getActivePlayers()) {
				Player player = server.getPlayer(playerName);
				player.teleport(loc);
			}
		}
	}

	@Override
	protected void handleInProgress() {
		debugInfo("handleInProgress");
		broadcastToAllOnlinePlayers("Let the games begin!");
		tempStats = new GameStatsMemory();

		// teleport everyone to the spawn point to start the game
		Location loc = getWarpPoint(spawnWarpName, spawnJiggle);
		if (loc != null) {
			Server server = getPlugin().getServer();
			for (String playerName : getActivePlayers()) {
				Player player = server.getPlayer(playerName);
				player.teleport(loc);
				giveSnow(playerName);
			}
		}

		startRefillTask();
	}

	@Override
	protected void handleEnded() {
		debugInfo("handleEnded");
		stopRefillTask();
		for (String player : getActivePlayers()) {
			removeSnow(player);
		}
		try {
			getHistoricStats().saveConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
		broadcastToAllOnlinePlayers("The game has ended.");
		for (String playerName : getActivePlayers()) {
			Player player = getPlayer(playerName);
			player.sendMessage("Scores this game:");
			doScores(player);
		}
	}

	@Override
	protected void handleFailed() {
		debugInfo("handleFailed");
		stopRefillTask();
		for (String player : getActivePlayers()) {
			removeSnow(player);
		}
		try {
			getHistoricStats().saveConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
		broadcastToAllOnlinePlayers("The game has been canceled.");
	}

	@Override
	protected void handlePlayerAdded(String playerName) {
		// just add anyone who wants to be added
		debugInfo("handlePlayerAdded");
		broadcastToAllOnlinePlayers(playerName + " has joined the game.");
		if (isGameLounging()) {
			Location warp = getWarpPoint(loungeWarpName, spawnJiggle);
			if (warp != null) {
				getPlayer(playerName).teleport(warp);
			}
		} else if (isGameInProgress()) {
			Player player = getPlayer(playerName);
			Location warp = getWarpPoint(spawnWarpName, spawnJiggle);
			if (warp != null) {
				player.teleport(warp);
			}
			ItemStack itemInHand = new ItemStack(Material.SNOW_BALL, refillSize);
			player.setItemInHand(itemInHand);
		}
	}

	@Override
	protected void handlePlayerLeft(String playerName) {
		// should remove their bombs
		// remove that player's tnt
		debugInfo("handlePlayerLeft");
		broadcastToAllOnlinePlayers(playerName + " has left the game.");
		if (!isGameGathering()) {
			// we've already given them TNT, we need to remove at least 1 TNT from their inventory
			removeSnow(playerName);
		}
	}

	public boolean gameIsInProgress() {
		return isGameInProgress();
	}

	private void giveSnow(String playerName) {
		//give everyone TNT in hand
		Player player = getPlayer(playerName);
		ItemStack oldItemInHand = player.getItemInHand();
		ItemStack itemInHand = new ItemStack(Material.SNOW_BALL, refillSize);
		player.setItemInHand(itemInHand);
		player.getInventory().addItem(oldItemInHand);
		player.updateInventory();
	}

	private void removeSnow(String playerName) {
		Player player = getPlayer(playerName);
		Inventory inventory = player.getInventory();
		int slot = inventory.first(Material.SNOW_BALL);
		ItemStack stack = inventory.getItem(slot);
		stack.setAmount(stack.getAmount() - 1);
		inventory.setItem(slot, stack);
		player.updateInventory();
	}

	// --------------------------------------------------------------
	// ---- Stats

	private void accumulatStats(String playerName, String statName, int amount) {
		getHistoricStats().accumulate(playerName, statName, amount);
		tempStats.accumulate(playerName, statName, amount);
	}

	// --------------------------------------------------------------
	// ---- Event handlers

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (!gameIsInProgress()) return;
		debugInfo("onEntityDamagedByEntity");

		if (event.getDamager() instanceof Snowball) {
			Snowball snowball = (Snowball) event.getDamager();
			Entity hitBySnowball = event.getEntity();
			if (hitBySnowball instanceof Player) {
				Player victim = (Player) hitBySnowball;
				if (isPlayerAlive(victim.getName())) {
					ProjectileSource snowSource = snowball.getShooter();
					if (snowSource instanceof Player) {
						Player shooter = (Player) snowSource;
						if (isPlayerAlive(shooter.getName())) {
							// OK, someone got plastered.  Accumulate stats.
							event.setDamage(0);
							accumulatStats(victim.getName(), DEATHS_KEY, 1);
							//you only get a kill if you don't kill yourself
							accumulatStats(shooter.getName(), KILLS_KEY, 1);
						}
					}
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onProjectileHit(ProjectileHitEvent event) {
		Projectile entity = event.getEntity();
		ProjectileSource shooter = entity.getShooter();
		if (entity instanceof Snowball && shooter instanceof Player) {
			Player source = (Player) shooter;
			if (isPlayerAlive(source.getName())) {
				accumulatStats(source.getName(), SNOW_THROWN_KEY, 1);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (!gameIsInProgress()) return;
		// register a loss and teleport back to spawn point
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (isPlayerAlive(playerName)) {
			// Just teleport the person back to spawn here.
			// losses and announcements are done when the player is killed.
			Location loc = getWarpPoint(spawnWarpName, spawnJiggle);
			if (loc != null) {
				// hey jf - you need to jiggle this a bit or everyone will be on top of each other
				// have the player respawn in the game spawn
				event.setRespawnLocation(loc);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		debugInfo("PlayerQuitEvent");
		String playerName = event.getPlayer().getName();
		// remove player from game.  this will cause handlePlayerLeft to be called.
		removePlayerFromGame(playerName);
	}

	// --------------------------------------------------------------
	// ---- Event handlers

	private void startRefillTask() {
		if (gameTaskId == 0) {
			debugInfo("startRefillTask");
			gameTaskId = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
				public void run() {
					for (String playerName : getActivePlayers()) {
						giveSnow(playerName);
					}
				}
			}, refillRate, refillRate);
		}
	}

	private void stopRefillTask() {
		if (gameTaskId != 0) {
			debugInfo("stopRefillTask");
			getPlugin().getServer().getScheduler().cancelTask(gameTaskId);
			gameTaskId = 0;
		}
	}
}
