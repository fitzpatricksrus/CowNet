package us.fitzpatricksr.cownet.commands.games;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.fitzpatricksr.cownet.CowNetMod;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.CowWarp;
import us.fitzpatricksr.cownet.commands.games.PlayerGameState.PlayerState;

import java.util.HashMap;
import java.util.Random;
import java.util.Set;

public abstract class GatheredGame extends CowNetThingy {
	private final Random rand = new Random();

	//game state
	private GameGatheringTimer gameState;                       //the state of the game
	private PlayerGameState playerState;                        //state of players in the game
	private GameStatsFile statsFile;                            //game statsFile that we load and save

	protected abstract String getGameName();

	protected abstract int getMinPlayers();

	@Override
	protected void onEnable() throws Exception {
		playerState = new PlayerGameState(getGameName(), new CallbackStub());
		statsFile = new GameStatsFile(getPlugin(), getGameName() + ".yml");
		statsFile.loadConfig();
	}

	@Override
	protected void reloadManualSettings() throws Exception {
		reloadAutoSettings(GameStatsFile.class);
		reloadAutoSettings(GameGatheringTimer.class);
		reloadAutoSettings(PlayerGameState.class);
	}

	@Override
	protected HashMap<String, String> getManualSettings() {
		HashMap<String, String> result = getSettingValueMapFor(GameGatheringTimer.class);
		result.putAll(getSettingValueMapFor(PlayerGameState.class));
		result.putAll(getSettingValueMapFor(GameStatsFile.class));
		return result;
	}

	@Override
	protected boolean updateManualSetting(String settingName, String settingValue) {
		return setAutoSettingValue(GameGatheringTimer.class, settingName, settingValue) || setAutoSettingValue(PlayerGameState.class, settingName, settingValue) || setAutoSettingValue(GameStatsFile.class, settingName, settingValue);
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
				"   statsFile - see how you stack up against others."
		};
	}

	//------------------------------------------------
	//  command handlers
	//

	@CowCommand
	private boolean doJoin(Player player) {
		if (!playerState.addPlayer(player.getName())) {
			player.sendMessage("You aren't allowed to join right now.");
		}
		return true;
	}

	@CowCommand
	private boolean doStart(Player player) {
		if (gameState != null) {
			if (gameState.isGathering()) {
				gameState.startLounging();
			} else if (gameState.isLounging()) {
				gameState.startGame();
			} else if (gameState.isGameOn()) {
				player.sendMessage("The game has already begun.");
			}
		} else {
			player.sendMessage("No players have joined the game.  Not much fun without players...");
		}
		return true;
	}

	@CowCommand
	private boolean doInfo(CommandSender sender) {
		if (gameState != null) {
			sender.sendMessage(gameState.getGameStatusMessage());
		}
		for (Player player : getPlugin().getServer().getOnlinePlayers()) {
			sender.sendMessage("  " + player.getDisplayName() + " - " + playerState.getPlayerState(player.getName()));
		}
		return true;
	}

	@CowCommand
	private boolean doQuit(Player player) {
		String name = player.getName();
		if (playerState.getPlayerState(name).equals(PlayerState.ALIVE)) {
			playerState.removePlayer(name);
		} else {
			player.sendMessage("You are not currently in the game.");
		}
		return true;
	}

	@CowCommand
	private boolean doTp(Player sender, String destName) {
		return doTeleport(sender, destName);
	}

	@CowCommand
	private boolean doTeleport(Player sender, String destName) {
		// we only allow non-players to teleport to live players.
		if (playerState.getPlayerState(sender.getName()) == PlayerState.ALIVE) {
			sender.sendMessage("Players are not allowed to teleport.");
		} else if (playerState.getPlayerState(destName) == PlayerState.ALIVE) {
			Location loc = getPlugin().getServer().getPlayer(destName).getLocation();
			sender.teleport(loc);
		} else {
			sender.sendMessage("Can't find " + destName + " in the game.");
		}
		return false;
	}

	// --------------------------------------------------------------
	// ---- callbacks

	// I put these in this small wrapper class so the callback methods would not be part
	// of this class' interface.
	private class CallbackStub implements GameGatheringTimer.Listener, PlayerGameState.Listener {
		@Override
		public void gameGathering() {
			debugInfo("gameGathered()");
			handleGathering();
		}

		@Override
		public void gameLounging() {
			if (playerState.getPlayers().size() < getMinPlayers()) {
				// game failed to gather enough players
				gameState.cancelGame();
				debugInfo("gameLounging() - game failed.");
			} else {
				// just forward the callback
				handleLounging();
				debugInfo("gameLounging() - game starting.");
			}
		}

		@Override
		public void gameInProgress() {
			debugInfo("gameInProgress()");
			handleInProgress();
		}

		@Override
		public void gameCanceled() {
			debugInfo("gameCanceled()");
			handleFailed();
			playerState.resetGame();
			gameState = null;
		}

		@Override
		public void gameEnded() {
			debugInfo("gameEnded()");
			handleEnded();
			playerState.resetGame();
			gameState = null;
		}

		@Override
		public boolean playerCanJoin(String playerName) {
			return handleCanAddPlayer(playerName);
		}

		@Override
		public void playerJoined(String playerName) {
			handlePlayerAdded(playerName);
			if (gameState == null) {
				// start the timer for the game to begin.  i.e. put it in gathering mode.
				gameState = new GameGatheringTimer(getPlugin(), new CallbackStub());
				debugInfo("playerJoined - startGatheringTimer");
			}
		}

		@Override
		public void playerLeft(String playerName) {
			handlePlayerLeft(playerName);
			if (gameState.isInProgress()) {
				if (playerState.getPlayers().size() < getMinPlayers()) {
					gameState.endGame();
					debugInfo("playerLeft - endingGame");
				}
			}
		}

		@Override
		public void announceGather(long time) {
			handleAnnounceGather(time);
		}

		@Override
		public void announceLounging(long time) {
			handleAnnounceLounging(time);
		}

		@Override
		public void announceWindDown(long time) {
			handleAnnounceWindDown(time);
		}
	}

	// --------------------------------------------------------------
	// ---- subclass interface

	// -- notification routines.
	protected void handleGathering() {
	}

	protected void handleLounging() {
	}

	protected void handleInProgress() {
	}

	protected void handleEnded() {
	}

	protected void handleFailed() {
	}

	protected boolean handleCanAddPlayer(String playerName) {
		return true;
	}

	protected void handlePlayerAdded(String playerName) {
	}

	protected void handlePlayerLeft(String playerName) {
	}

	protected void handleAnnounceGather(long time) {
		broadcastToAllOnlinePlayers("Gathering for the games ends in " + time + " seconds");
	}

	protected void handleAnnounceLounging(long time) {
		broadcastToAllOnlinePlayers("The game starts in " + time + " seconds");
	}

	protected void handleAnnounceWindDown(long time) {
		broadcastToAllOnlinePlayers("The game ends in " + time + " seconds");
	}

	// -- routines for subclasses to inspect and modify game flow.
	protected final GameStatsFile getHistoricStats() {
		return statsFile;
	}

	protected final void addPlayerToGame(String playerName) {
		playerState.addPlayer(playerName);
	}

	protected final void removePlayerFromGame(String playerName) {
		playerState.removePlayer(playerName);
	}

	protected final Set<String> getActivePlayers() {
		return playerState.getPlayers();
	}

	protected final String getPlayerGame(String playerName) {
		return playerState.getGameOfPlayer(playerName);
	}

	protected final boolean playerIsAlive(String playerName) {
		return playerState.getPlayerState(playerName) == PlayerState.ALIVE;
	}

	protected final boolean playerIsWatching(String playerName) {
		return playerState.getPlayerState(playerName) == PlayerState.WATCHING;
	}

	protected final boolean isGameGathering() {
		return (gameState != null) && gameState.isGathering();
	}

	protected final boolean isGameLounging() {
		return (gameState != null) && gameState.isLounging();
	}

	protected final boolean isGameInProgress() {
		return (gameState != null) && gameState.isInProgress();
	}

	protected final boolean isGameEnded() {
		return (gameState != null) && gameState.isEnded();
	}

	protected final void broadcastToAllOnlinePlayers(String msg) {
		for (Player player : getPlugin().getServer().getOnlinePlayers()) {
			player.sendMessage(msg);
		}
	}

	// --------------------------------------------------------------
	// ---- Utility methods

	protected final Location getWarpPoint(String warpName, int jiggle) {
		CowNetMod plugin = (CowNetMod) getPlugin();
		CowWarp warpThingy = (CowWarp) plugin.getThingy("cowwarp");
		return jigglePoint(warpThingy.getWarpLocation(warpName), jiggle);
	}

	protected final Location jigglePoint(Location loc, int jiggle) {
		if (loc != null) {
			if (jiggle > 0) {
				int dx = rand.nextInt(jiggle * 2 + 1) - jiggle - 1; // -5..5
				int dz = rand.nextInt(jiggle * 2 + 1) - jiggle - 1; // -5..5
				loc.add(dx, 0, dz);
				loc = loc.getWorld().getHighestBlockAt(loc).getLocation();
				loc.add(0, 1, 0);
			}
		}
		return loc;
	}

	protected final Player getPlayer(String playerName) {
		Server server = getPlugin().getServer();
		return server.getPlayer(playerName);
	}
}


