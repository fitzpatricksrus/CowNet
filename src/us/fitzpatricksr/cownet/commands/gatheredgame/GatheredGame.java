package us.fitzpatricksr.cownet.commands.gatheredgame;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.gatheredgame.PlayerGameState.PlayerState;

import java.util.HashMap;
import java.util.Set;

/*
    the game world is off limits until a game starts
    the first person to enter starts the gathering
    gathering continues for a specified period of time
    all players who entered after that time are teleported in and their inventory cleared.
    after the last player dies the world is regenerated.

    games have 3 states
       ended - nothing in progress
       gathering - someone is waiting for the games to start
       inprogress - the games are underway

    for each person
       enum { player, deadPlayer, sponsor } gameState
       boolean lastTribute
 */
public class GatheredGame extends CowNetThingy implements org.bukkit.event.Listener {
	private static final String STATS_KILLS = "kills";
	private static final String STATS_DEATHS = "deaths";
	private static final String STATS_BOMBS = "bombs";

	//game state
	private GameGatheringTimer gameState;                       //the state of the game
	private PlayerGameState playerState;                        //state of players in the game
	private GameStats stats;                                    //game stats that we load and save

	// hey jf - this should be abstract
	protected String getGameName() {
		return "GatheredGame";
	}

	// hey jf - this should be abstract
	protected int getMinPlayers() {
		return 2;
	}

	@Override
	protected void onEnable() throws Exception {
		playerState = new PlayerGameState(getGameName());
		playerState.addListener(new CallbackStub());
		stats = new GameStats(getPlugin(), getGameName() + ".yml");
		stats.loadConfig();
	}

	@Override
	protected void reloadManualSettings() throws Exception {
		reloadAutoSettings(GameStats.class);
		reloadAutoSettings(GameGatheringTimer.class);
		reloadAutoSettings(PlayerGameState.class);
	}

	@Override
	protected HashMap<String, String> getManualSettings() {
		HashMap<String, String> result = getSettingValueMapFor(GameGatheringTimer.class);
		result.putAll(getSettingValueMapFor(PlayerGameState.class));
		result.putAll(getSettingValueMapFor(GameStats.class));
		return result;
	}

	@Override
	protected boolean updateManualSetting(String settingName, String settingValue) {
		return setAutoSettingValue(GameGatheringTimer.class, settingName, settingValue) || setAutoSettingValue(PlayerGameState.class, settingName, settingValue) || setAutoSettingValue(GameStats.class, settingName, settingValue);
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
				"   stats - see how you stack up against others."
		};
	}

	//------------------------------------------------
	//  command handlers
	//

	@CowCommand
	private boolean doStats(CommandSender sender) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			String name = player.getName();
			//			player.sendMessage("Your wins: " + playerGameState.getPlayerWins(name) + "   " + StringUtils.fitToColumnSize(Double.toString(playerGameState.getPlayerAverage(name) * 100), 5) + "%");
		}
		//		playerGameState.dumpRecentHistory(sender);
		//		playerGameState.dumpLeaderBoard(sender);
		return true;
	}

	@CowCommand
	private boolean doJoin(Player player) {
		if (playerState.addPlayer(player.getName())) {
			broadcastToAllOnlinePlayers(player.getName() + " has joined the game.");
		} else {
			player.sendMessage("You aren't allowed to join right now.  You can only watch.");
		}
		return true;
	}

	@CowCommand
	private boolean doStart(Player player) {
		if (gameState != null) {
			if (gameState.isGathering()) {
				gameState.startAcclimating();
			} else if (gameState.isAcclimating()) {
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
		public void gameAcclimating() {
			if (playerState.getPlayers().size() < getMinPlayers()) {
				// game failed to gather enough players
				gameState.cancelGame();
				playerState.resetGame();
				handleFailed();
				debugInfo("gameAcclimating() - game failed.");
			} else {
				// just forward the callback
				handleAcclimating();
				debugInfo("gameAcclimating() - game starting.");
			}
		}

		@Override
		public void gameInProgress() {
			// at this point, we don't need the game state anymore.
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
		public boolean playerJoined(String playerName) {
			if (handlePlayerAdded(playerName)) {
				if (gameState == null) {
					// start the timer for the game to begin.  i.e. put it in gathering mode.
					gameState = new GameGatheringTimer(getPlugin(), new CallbackStub());
					debugInfo("playerJoined - startGatheringTimer");
				}
				return true;
			} else {
				return false;
			}
		}

		@Override
		public void playerLeft(String playerName) {
			handlePlayerLeft(playerName);
			if (gameState != null && gameState.isInProgress()) {
				if (playerState.getPlayers().size() < getMinPlayers()) {
					gameState.endGame();
					playerState.resetGame();
					handleEnded();
					gameState = null;
					debugInfo("playerLeft - endingGame");
				}
			}
		}

		@Override
		public void playerBanned(String playerName) {
			handlePlayerBanned(playerName);
		}

		@Override
		public void playerUnbanned(String playerName) {
			handlePlayerUnbanned(playerName);
		}

		@Override
		public void announceGather(long time) {
			handleAnnounceGather(time);
		}

		@Override
		public void announceAcclimate(long time) {
			handleAnnounceAcclimate(time);
		}

		@Override
		public void announceWindDown(long time) {
			handleAnnounceWindDown(time);
		}
	}

	// --------------------------------------------------------------
	// ---- subclass interface

	protected void handleGathering() {
	}

	protected void handleAcclimating() {
	}

	protected void handleInProgress() {
	}

	protected void handleEnded() {
	}

	protected void handleFailed() {
	}

	protected boolean handlePlayerAdded(String playerName) {
		return true;
	}

	protected void handlePlayerLeft(String playerName) {
	}

	protected void handlePlayerBanned(String playerName) {
	}

	protected void handlePlayerUnbanned(String playerName) {
	}

	protected void handleAnnounceGather(long time) {
		broadcastToAllOnlinePlayers("Gathering for the games ends in " + time + " seconds");
	}

	protected void handleAnnounceAcclimate(long time) {
		broadcastToAllOnlinePlayers("The game starts in " + time + " seconds");
	}

	protected void handleAnnounceWindDown(long time) {
		broadcastToAllOnlinePlayers("The game ends in " + time + " seconds");
	}

	protected final GameStats getStats() {
		return stats;
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

	protected final boolean playerIsAlive(String playerName) {
		return playerState.getPlayerState(playerName) == PlayerState.ALIVE;
	}

	protected final boolean playerIsDead(String playerName) {
		return playerState.getPlayerState(playerName) == PlayerState.BANNED;
	}

	protected final boolean playerIsWatching(String playerName) {
		return playerState.getPlayerState(playerName) == PlayerState.WATCHING;
	}

	protected final void broadcastToAllOnlinePlayers(String msg) {
		for (Player player : getPlugin().getServer().getOnlinePlayers()) {
			player.sendMessage(msg);
		}
	}
}


