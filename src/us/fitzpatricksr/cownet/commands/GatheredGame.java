package us.fitzpatricksr.cownet.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.gatheredgame.GamePhaseState;
import us.fitzpatricksr.cownet.commands.gatheredgame.GamePlayerState;
import us.fitzpatricksr.cownet.commands.gatheredgame.GamePlayerState.PlayerState;
import us.fitzpatricksr.cownet.utils.StringUtils;

import java.util.HashMap;
import java.util.Random;

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
public class GatheredGame extends CowNetThingy implements Listener, GamePhaseState.GameStateListener, GamePlayerState.GamePlayerListener {
	private static final int GAME_WATCHER_FREQUENCY = 20 * 1; // 1 second
	private final Random rand = new Random();

	//game state
	private GamePlayerState playerState;                        //stats and stuff
	private GamePhaseState gameState;                             //the state of the game

	@Override
	protected void onEnable() throws Exception {
		playerState = new GamePlayerState(getPlugin(), getTrigger() + "-stats.yml", this);
		playerState.loadConfig();
		getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
			public void run() {
				gameWatcher();
			}
		}, GAME_WATCHER_FREQUENCY, GAME_WATCHER_FREQUENCY);
	}

	@Override
	protected void reloadManualSettings() throws Exception {
		reloadAutoSettings(GamePhaseState.class);
		reloadAutoSettings(GamePlayerState.class);
	}

	@Override
	protected HashMap<String, String> getManualSettings() {
		HashMap<String, String> result = getSettingValueMapFor(GamePhaseState.class);
		result.putAll(getSettingValueMapFor(GamePlayerState.class));
		return result;
	}

	@Override
	protected boolean updateManualSetting(String settingName, String settingValue) {
		return setAutoSettingValue(GamePhaseState.class, settingName, settingValue) || setAutoSettingValue(GamePlayerState.class, settingName, settingValue);
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
		sender.sendMessage(gameState.getGameStatusMessage());
		for (Player player : getPlugin().getServer().getOnlinePlayers()) {
			sender.sendMessage("  " + player.getDisplayName() + playerState.getPlayerState(player.getName()));
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
	// ---- Game watcher moves the game forward through different stages

	private void gameWatcher() {
		debugInfo(gameState.getGameStatusMessage());
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

	private void broadcast(String msg) {
		for (Player player : getPlugin().getServer().getOnlinePlayers()) {
			player.sendMessage(msg);
		}
	}

	@Override
	public void gameGathering() {
	}

	@Override
	public void gameAcclimating() {
	}

	@Override
	public void gameInProgress() {
		// at this point, we don't need the game state anymore.
		gameState = null;
	}

	@Override
	public void playerJoined(String playerName) {
		if (gameState == null) {
			// start the timer for the game to begin.  i.e. put it in gathering mode.
			gameState = new GamePhaseState(this);
		}
	}

	@Override
	public void playerLeft(String playerName) {
		// default behavior is to end the game when one player is left.
		if (playerState.livePlayerCount() == 1) {
			playerState.endGame();
		}
	}
}


