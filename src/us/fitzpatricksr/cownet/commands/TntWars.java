package us.fitzpatricksr.cownet.commands;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import us.fitzpatricksr.cownet.CowNetMod;
import us.fitzpatricksr.cownet.commands.gatheredgame.GatheredGame;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

/**
 */
public class TntWars extends GatheredGame implements org.bukkit.event.Listener {
	private static final long GAME_FREQUENCY = 4;  // 5 times a second?
	private static final String KILLS_KEY = "kills";
	private static final String DEATHS_KEY = "deaths";
	private static final String BOMBS_PLACED_KEY = "bombsPlaced";
	private final Random rand = new Random();

	@Setting
	private int minPlayers = 2;
	@Setting
	private String acclimatingWarpName = "tntWarsAcclimating";
	@Setting
	private String spawnWarpName = "tntWarsSpawn";
	@Setting
	private int maxBlockPlacements = 1;
	@Setting
	private long explosionDelay = 5 * 1000; // 3 seconds
	@Setting
	private int explosionRadius = 6;
	// manual setting
	private Material explosiveBlockType = Material.TNT;

	private HashMap<String, LinkedList<BombPlacement>> placements;
	private int gameTaskId;

	// --------------------------------------------------------------
	// ---- Settings management

	@Override
	// reload any settings not handled by @Setting
	protected void reloadManualSettings() throws Exception {
		this.explosiveBlockType = Material.matchMaterial(getConfigValue("explosiveBlockType", explosiveBlockType.toString()));
	}

	// return any custom settings that are not handled by @Settings code
	protected HashMap<String, String> getManualSettings() {
		HashMap<String, String> result = new HashMap<String, String>();
		result.put("explosiveBlockType", explosiveBlockType.toString());
		return result;
	}

	// update a setting that was not handled by @Setting and return true if it has been updated.
	protected boolean updateManualSetting(String settingName, String settingValue) {
		if (settingName.equalsIgnoreCase("explosiveBlockType")) {
			explosiveBlockType = Material.valueOf(settingValue);
			updateConfigValue("explosiveBlockType", settingValue);
		} else {
			return false;
		}
		return true;
	}

	@Override
	protected String getGameName() {
		return "TntWars";
	}

	@Override
	protected int getMinPlayers() {
		return minPlayers;
	}

	// --------------------------------------------------------------
	// ---- game state transitions

	@Override
	protected void handleGathering() {
		debugInfo("handleGathering");
		broadcastToAllOnlinePlayers("A game is gathering.  To join use /" + getTrigger() + " join.");
	}

	@Override
	protected void handleAcclimating() {
		debugInfo("handleAcclimating");
		broadcastToAllOnlinePlayers("All the players are ready.  The games are about to start.");
		// teleport everyone to the lobby for acclimation
		Location loc = getWarpPoint(acclimatingWarpName);
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
		// teleport everyone to the spawn point to start the game
		Location loc = getWarpPoint(spawnWarpName);
		if (loc != null) {
			Server server = getPlugin().getServer();
			for (String playerName : getActivePlayers()) {
				Player player = server.getPlayer(playerName);
				player.teleport(loc);
			}
		}
		placements = new HashMap<String, LinkedList<BombPlacement>>();
		Set<String> players = getActivePlayers();
		for (String playerName : players) {
			placements.put(playerName, new LinkedList<BombPlacement>());
		}
	}

	@Override
	protected void handleEnded() {
		debugInfo("handleEnded");
		stopBombWatcher();
		placements = null;
		try {
			getStats().saveConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
		broadcastToAllOnlinePlayers("The game has ended.");
	}

	@Override
	protected void handleFailed() {
		debugInfo("handleFailed");
		stopBombWatcher();
		placements = null;
		broadcastToAllOnlinePlayers("The game has been canceled.");
	}

	@Override
	protected boolean handlePlayerAdded(String playerName) {
		// just add anyone who wants to be added
		debugInfo("handlePlayerAdded");
		broadcastToAllOnlinePlayers(playerName + " has joined the game.");
		return true;
	}

	@Override
	protected void handlePlayerLeft(String playerName) {
		// should remove their bombs
		// remove that player's tnt
		debugInfo("handlePlayerLeft");
		if (placements != null) {
			// placements is only non-null if the game has begun.
			placements.remove(playerName);
		}
		broadcastToAllOnlinePlayers(playerName + " has left the game.");
	}

	public boolean gameIsInProgress() {
		return placements != null;
	}

	private Location getWarpPoint(String warpName) {
		CowNetMod plugin = (CowNetMod) getPlugin();
		CowWarp warpThingy = (CowWarp) plugin.getThingy("cowwarp");
		return warpThingy.getWarpLocation(warpName);
	}

	// --------------------------------------------------------------
	// ---- explosive mgmt

	/* place a bomb at players current location? */
	private boolean placeBomb(Player player, Location loc) {
		String playerName = player.getName();
		if (playerIsAlive(playerName)) {
			// check to see if they've already placed the maximum number of blocks.
			LinkedList<BombPlacement> placementList = placements.get(playerName);
			if (placementList.size() < maxBlockPlacements) {
				// place a bomb
				placementList.addLast(new BombPlacement(player, loc));
				startBombWatcher();
				getStats().accumulate(player.getName(), BOMBS_PLACED_KEY, 1);
				return true;
			}
		}
		return false;
	}

	private LinkedList<BombPlacement> getBombsToExplode() {
		LinkedList<BombPlacement> result = new LinkedList<BombPlacement>();
		for (String playerName : placements.keySet()) {
			LinkedList<BombPlacement> bombPlacements = placements.get(playerName);
			while ((bombPlacements.size() > 0) && bombPlacements.getFirst().shouldExplode()) {
				result.add(bombPlacements.getFirst());
				bombPlacements.removeFirst();
			}
		}
		if (placements.size() <= 0) {
			stopBombWatcher();
		}
		return result;
	}

	private class BombPlacement {
		public Player placer;
		public Location location;
		public long blockPlacedTime;

		public BombPlacement(Player placer, Location loc) {
			this.placer = placer;
			this.location = loc;
			blockPlacedTime = System.currentTimeMillis();
			location.getWorld().playEffect(location, Effect.ENDER_SIGNAL, 2);
		}

		public boolean shouldExplode() {
			return System.currentTimeMillis() - blockPlacedTime > explosionDelay;
		}

		public void doExplosion() {
			placer.sendMessage("Boom!");
			location.getWorld().createExplosion(location, 0F);
			Server server = getPlugin().getServer();
			long radiusSquared = explosionRadius * explosionRadius;
			for (String playerName : getActivePlayers()) {
				Player player = server.getPlayer(playerName);
				if (player != null) {
					Location playerLocation = player.getLocation();
					if (location.distanceSquared(playerLocation) < radiusSquared) {
						//this player is in the blast zone.
						//chalk up a kill and a death.
						getStats().accumulate(placer.getName(), KILLS_KEY, 1);
						getStats().accumulate(playerName, DEATHS_KEY, 1);
						Location dest = getWarpPoint(spawnWarpName);
						player.teleport(dest);
						for (int i = 0; i < 10; i++) {
							location.getWorld().playEffect(dest, Effect.SMOKE, rand.nextInt(9));
						}
						player.sendMessage("You were blown up by " + placer.getDisplayName());
						placer.sendMessage("You blew up " + player.getDisplayName());
					}
				}
			}
			//			location.getWorld().createExplosion(location, explosionPower, false);
		}
	}

	// --------------------------------------------------------------
	// ---- Event handlers

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!gameIsInProgress()) return;
		debugInfo("BlockPlaceEvent");
		// if it's an explosive block, update state
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (playerIsAlive(playerName)) {
			// hey jf - do you want to look at the block in hand or block placed?
			if (event.getBlock().getType().equals(explosiveBlockType)) {
				// if they already have an unexploded block, just cancel event
				if (placeBomb(player, event.getBlock().getLocation())) {
					// remove the item in hand (ex. decrement count by one)
					// ItemStack itemInHand = player.getItemInHand();
					// itemInHand.setAmount(itemInHand.getAmount() - 1);
				} else {
					player.sendMessage("You already have the maximum number of explosive placed.");
				}
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (!gameIsInProgress()) return;
		// register a loss and teleport back to spawn point
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (playerIsAlive(playerName)) {
			// Just teleport the person back to spawn here.
			// losses and announcements are done when the player is killed.
			Location loc = getWarpPoint(spawnWarpName);
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

	private void startBombWatcher() {
		if (gameTaskId == 0) {
			debugInfo("startBombWatcher");
			gameTaskId = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
				public void run() {
					bombWatcher();
				}
			}, GAME_FREQUENCY, GAME_FREQUENCY);
		}
	}

	private void stopBombWatcher() {
		if (gameTaskId != 0) {
			debugInfo("stopBombWatcher");
			getPlugin().getServer().getScheduler().cancelTask(gameTaskId);
			gameTaskId = 0;
		}
	}

	private void bombWatcher() {
		//debugInfo("bombWatcher");
		for (BombPlacement bomb : getBombsToExplode()) {
			bomb.doExplosion();
		}
	}


	/*
		private void fireBall(Location loc) {
			final HashSet<Block> fires = new HashSet<Block>();
			for (int x = loc.getBlockX()-1; x <= loc.getBlockX()+1; x++) {
				for (int y = loc.getBlockY()-1; y <= loc.getBlockY()+1; y++) {
					for (int z = loc.getBlockZ()-1; z <= loc.getBlockZ()+1; z++) {
						if (loc.getWorld().getBlockTypeIdAt(x,y,z) == 0) {
							Block b = loc.getWorld().getBlockAt(x,y,z);
							b.setTypeIdAndData(Material.FIRE.getId(), (byte)15, false);
							fires.add(b);
						}
					}
				}
			}
			fireball.remove();
			if (fires.size() > 0) {
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					@Override
					public void run() {
						for (Block b : fires) {
							if (b.getType() == Material.FIRE) {
								b.setType(Material.AIR);
							}
						}
					}
				}, 20);
			}
		} */
}
