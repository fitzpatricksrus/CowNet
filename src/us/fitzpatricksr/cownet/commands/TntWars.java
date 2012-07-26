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
import org.bukkit.inventory.ItemStack;
import us.fitzpatricksr.cownet.CowNetMod;
import us.fitzpatricksr.cownet.commands.gatheredgame.GatheredGame;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 */
public class TntWars extends GatheredGame {
	@Setting
	private String acclimatingWarpName = "tntWarsAcclimating";
	@Setting
	private String spawnWarpName = "tntWarsSpawn";
	@Setting
	private int maxBlockPlacements = 1;
	@Setting
	private long explosionDelay = 3 * 1000; // 3 seconds
	@Setting
	private long explosionPower = 4;
	// manual setting
	private Material explosiveBlockType = Material.TNT;

	private HashMap<String, LinkedList<BombPlacement>> placements;
	private int gameTaskId;
	private static final long GAME_FREQUENCY = 4;  // 5 times a second?

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

	// --------------------------------------------------------------
	// ---- game state transitions

	@Override
	protected void handleGathering() {

	}

	@Override
	protected void handleAcclimating() {
		// teleport everyone to the lobby for acclimation
		CowNetMod plugin = (CowNetMod) getPlugin();
		CowWarp warpThingy = (CowWarp) plugin.getThingy("CowWarp");
		Location loc = warpThingy.getWarpLocation(acclimatingWarpName);
		if (loc != null) {
			Server server = getPlugin().getServer();
			for (String playerName : getActivePlayers()) {
				Player player = server.getPlayer(playerName);
				player.teleport(loc);
			}
		}
	}

	@Override
	protected void handleInProgress() {
		// teleport everyone to the spawn point to start the game
		CowNetMod plugin = (CowNetMod) getPlugin();
		CowWarp warpThingy = (CowWarp) plugin.getThingy("CowWarp");
		Location loc = warpThingy.getWarpLocation(spawnWarpName);
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
			Player player = getPlugin().getServer().getPlayer(playerName);
			placements.put(playerName, new LinkedList<BombPlacement>());
		}
	}

	@Override
	protected void handleEnded() {
		placements = null;
	}

	@Override
	protected void handleFailed() {
		placements = null;
	}

	@Override
	protected boolean handlePlayerAdded(String playerName) {
		// just add anyone who wants to be added
		return true;
	}

	@Override
	protected void handlePlayerLeft(String playerName) {
		// should remove their bombs
		// remove that player's tnt
		placements.remove(playerName);
	}

	public boolean gameIsInProgress() {
		return placements != null;
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
				placementList.addLast(new BombPlacement(loc));
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
		return result;
	}

	private class BombPlacement {
		public Location location;
		public long blockPlacedTime;

		public BombPlacement(Location loc) {
			location = loc;
			blockPlacedTime = System.currentTimeMillis();
			location.getWorld().playEffect(location, Effect.SMOKE, 2);
		}

		public boolean shouldExplode() {
			return System.currentTimeMillis() - blockPlacedTime > explosionDelay;
		}

		public void doExplosion() {
			location.getWorld().createExplosion(location, explosionPower, false);
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
					ItemStack itemInHand = player.getItemInHand();
					itemInHand.setAmount(itemInHand.getAmount() - 1);
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
			CowNetMod plugin = (CowNetMod) getPlugin();
			CowWarp warpThingy = (CowWarp) plugin.getThingy("CowWarp");
			Location loc = warpThingy.getWarpLocation(spawnWarpName);
			if (loc != null) {
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
		debugInfo("startBombWatcher");
		gameTaskId = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
			public void run() {
				bombWatcher();
			}
		}, GAME_FREQUENCY, GAME_FREQUENCY);
	}

	private void stopBombWatcher() {
		debugInfo("stopBombWatcher");
		if (gameTaskId != 0) {
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
}
