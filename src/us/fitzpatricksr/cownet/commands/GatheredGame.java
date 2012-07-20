package us.fitzpatricksr.cownet.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.gatheredgame.GameGatheringTimer;
import us.fitzpatricksr.cownet.commands.gatheredgame.GamePlayerState;
import us.fitzpatricksr.cownet.commands.gatheredgame.GamePlayerState.PlayerState;
import us.fitzpatricksr.cownet.utils.DebugProxy;
import us.fitzpatricksr.cownet.utils.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
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
public class GatheredGame extends CowNetThingy implements Listener {
	private static final int GAME_WATCHER_FREQUENCY = 20 * 1; // 1 second
	private final Random rand = new Random();

	@Setting
	private int minPlayers = 2;

	//game state
	private GamePlayerState playerState;                        //stats and stuff
	private GameGatheringTimer gameState;                       //the state of the game
	private int gatherTaskId = 0;

	@Override
	protected void onEnable() throws Exception {
		playerState = new GamePlayerState(getPlugin(), getTrigger() + "-stats.yml", (GamePlayerState.GamePlayerListener) DebugProxy.newInstance(new CallbackStub()));
		//				new CallbackStub());
		playerState.loadConfig();
	}

	@Override
	protected void reloadManualSettings() throws Exception {
		reloadAutoSettings(GameGatheringTimer.class);
		reloadAutoSettings(GamePlayerState.class);
	}

	@Override
	protected HashMap<String, String> getManualSettings() {
		HashMap<String, String> result = getSettingValueMapFor(GameGatheringTimer.class);
		result.putAll(getSettingValueMapFor(GamePlayerState.class));
		return result;
	}

	@Override
	protected boolean updateManualSetting(String settingName, String settingValue) {
		return setAutoSettingValue(GameGatheringTimer.class, settingName, settingValue) || setAutoSettingValue(GamePlayerState.class, settingName, settingValue);
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
			player.sendMessage("Your wins: " + playerState.getPlayerWins(name) + "   " + StringUtils.fitToColumnSize(Double.toString(playerState.getPlayerAverage(name) * 100), 5) + "%");
		}
		playerState.dumpRecentHistory(sender);
		playerState.dumpLeaderBoard(sender);
		return true;
	}

	@CowCommand
	private boolean doIt(Player player) {
		return doJoin(player);
	}

	@CowCommand
	private boolean doJoin(Player player) {
		if (playerState.isStarted()) {
			player.sendMessage("You can't join a game in progress.  You can only watch.");
		} else {
			playerState.addPlayer(player.getName());
			broadcast(player.getDisplayName() + " is " + playerState.getPlayerState(player.getName()));
			doInfo(player);
		}
		return true;
	}

	@CowCommand
	private boolean doStart(Player player) {
		if (gameState != null) {
			if (gameState.isGathering()) {
				gameState.endGathering();
			} else if (gameState.isGameOn()) {
				player.sendMessage("The game has already begun.");
			}
		} else {
			player.sendMessage("No players have joined the game.");
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

	private void broadcast(String msg) {
		for (Player player : getPlugin().getServer().getOnlinePlayers()) {
			player.sendMessage(msg);
		}
	}

	// --------------------------------------------------------------
	// ---- callbacks

	// I put these in this small wrapper class so the callback methods would not be part
	// of this class' interface.
	private class CallbackStub implements GameGatheringTimer.GameStateListener, GamePlayerState.GamePlayerListener {
		@Override
		public void gameGathering() {
			debugInfo("gameGathered()");
			handleGathering();
		}

		@Override
		public void gameAcclimating() {
			if (playerState.livePlayerCount() < minPlayers) {
				// game failed to gather enough players
				stopGatheringTimer();
				playerState.abortGame();
				handleFailed();
				debugInfo("gameAcclimating() - game failed.");
			} else {
				// everyone in the game will now register a win or loss.
				// no new players.
				playerState.startGame();
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
		public void playerJoined(String playerName) {
			handlePlayerAdded(playerName);
			if (gameState == null) {
				// start the timer for the game to begin.  i.e. put it in gathering mode.
				startGatheringTimer();
				debugInfo("playerJoined - startGatheringTimer");
			}
		}

		@Override
		public void playerLeft(String playerName) {
			handlePlayerLeft(playerName);
			if (gameState != null && gameState.isInProgress()) {
				if (playerState.livePlayerCount() < minPlayers) {
					stopGatheringTimer();
					playerState.endGame();
					handleEnded();
					try {
						playerState.saveConfig();
					} catch (IOException e) {
						e.printStackTrace();
					}
					debugInfo("playerLeft - endingGame");
				}
			}
		}
	}

	// --------------------------------------------------------------
	// ---- Game watcher moves the game forward through different stages

	private void gameWatcher() {
		if (gameState != null) {
			gameState.tick();
			if (gameState != null) {
				debugInfo("gameWatcher: " + gameState.getGameStatusMessage());
				if (gameState.isGathering()) {
					long timeToWait = gameState.getTimeToGather() / 1000;
					if (timeToWait % 10 == 0 || timeToWait < 10) {
						broadcast("Gathering for the games ends in " + timeToWait + " seconds");
					}
				} else {
					if (gameState.isAcclimating()) {
						long timeToWait = gameState.getTimeToAcclimate() / 1000;
						broadcast("The games start in " + timeToWait + " seconds");
					}
				}
			}
		}
	}

	private void startGatheringTimer() {
		debugInfo("startGatheringTimer()");
		if (gameState == null) {
			// start the timer for the game to begin.  i.e. put it in gathering mode.
			gameState = new GameGatheringTimer((GameGatheringTimer.GameStateListener) DebugProxy.newInstance(new CallbackStub()));
			gatherTaskId = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
				public void run() {
					gameWatcher();
				}
			}, GAME_WATCHER_FREQUENCY, GAME_WATCHER_FREQUENCY);
		}
	}

	private void stopGatheringTimer() {
		debugInfo("stopGatheringTimer()");
		if (gameState != null) {
			getPlugin().getServer().getScheduler().cancelTask(gatherTaskId);
			gameState = null;
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

	protected void handlePlayerAdded(String playerName) {

	}

	protected void handlePlayerLeft(String playerName) {

	}

	protected Set<String> getActivePlayers() {
		return playerState.getPlayers();
	}
}


