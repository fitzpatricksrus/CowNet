package us.fitzpatricksr.cownet.commands.gatheredgame;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.utils.CowNetConfig;
import us.fitzpatricksr.cownet.utils.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/*
This class keeps track of who is part of a game and who is not.
It also keeps track of the player history persistently.

A game is either started or unstarted.  When unstarted, players can
be freely added and removed without any stats implications.

When the game starts, the listener is called with all the players
participating and all players will have a game attributed to their
stats.  No additional players may be added once the game has started.

Any players explicitly removed from the game once it is started
will accumulate a loss.   All players still in the game when
it ends will accumulate a win.

Instances of this object can be reused.
 */
public class GamePlayerState extends CowNetConfig {
	public enum PlayerState {
		ALIVE("a player"),
		DEAD("an ex-player"),
		WATCHING("a spectator");

		private String printString;

		PlayerState(String printString) {
			this.printString = printString;
		}

		public String toString() {
			return printString;
		}

		public String toString(Player player) {
			return player.getDisplayName() + ": " + toString();
		}
	}

	public interface GamePlayerListener {
		public void playerJoined(String playerName);

		public void playerLeft(String playerName);
	}


	@CowNetThingy.Setting
	private static int maxRecentWinners = 5;

	private String fileName;
	private HashSet<String> participating = new HashSet<String>();
	private HashSet<String> dead = new HashSet<String>();
	private HashMap<String, Integer> playerPlays = new HashMap<String, Integer>();
	private HashMap<String, Integer> playerLosses = new HashMap<String, Integer>();
	private LinkedList<String> recentWinners = new LinkedList<String>();
	private boolean isStarted = false;
	private GamePlayerListener listener;

	public GamePlayerState(JavaPlugin plugin, String fileName, GamePlayerListener listener) {
		super(plugin);
		this.fileName = fileName;
		this.listener = listener;
	}

	@Override
	protected String getFileName() {
		return fileName;
	}

	/* add a player to the game if the game hasn't started yet.  */
	public void addPlayer(String player) {
		if (!participating.contains(player)) {
			if (!isStarted) {
				participating.add(player);
				dead.remove(player);
			}
			listener.playerJoined(player);
		}
	}

	/* remove a player from the game.  If the game has already started
	* this player accumulates a loss.  listener.playerLeft() is called
	* if the game has stared. */
	public void removePlayer(String player) {
		if (participating.contains(player)) {
			if (!isStarted) {
				participating.remove(player);
				dead.remove(player);
			} else {
				participating.remove(player);
				dead.add(player);
				playerLosses.put(player, 1 + playerLosses.get(player));
			}
			listener.playerLeft(player);
		}
	}

	public boolean isStarted() {
		return isStarted;
	}

	/* return TRUE if the player was in the game when it started */
	public boolean containsPlayer(String player) {
		return participating.contains(player) || dead.contains(player);
	}

	public int livePlayerCount() {
		return participating.size();
	}

	public Set<String> getPlayers() {
		return participating;
	}

	/* call this when the game starts so all players are marked as participating in stats */
	public void startGame() {
		if (!isStarted) {
			isStarted = true;
			for (String name : participating) {
				if (playerPlays.containsKey(name)) {
					playerPlays.put(name, 1 + playerPlays.get(name));
				} else {
					playerPlays.put(name, 1);
				}
				if (!playerLosses.containsKey(name)) {
					// just populate a row here to make things easier later.
					playerLosses.put(name, 0);
				}
			}
		}
	}

	/* abort the game. */
	public void abortGame() {
		isStarted = false;
		participating.clear();
		dead.clear();
	}

	/* end the game, set the recent winners. */
	public void endGame() {
		if (isStarted) {
			// add current players to the recent winners list
			for (String name : participating) {
				if (recentWinners.size() >= maxRecentWinners) {
					recentWinners.removeLast();
					recentWinners.addFirst(name);
				}
			}
			isStarted = false;
		}
		participating.clear();
		dead.clear();

	}

	public PlayerState getPlayerState(String player) {
		if (participating.contains(player)) {
			return PlayerState.ALIVE;
		} else if (dead.contains(player)) {
			return PlayerState.DEAD;
		} else {
			return PlayerState.WATCHING;
		}
	}

	public int getPlayerWins(String player) {
		if (playerPlays.containsKey(player)) {
			return playerPlays.get(player) - playerLosses.get(player);
		} else {
			return 0;
		}
	}

	public int getPlayerLosses(String player) {
		if (playerPlays.containsKey(player)) {
			return playerLosses.get(player);
		} else {
			return 0;
		}
	}

	public int getPlayerPlays(String player) {
		if (playerPlays.containsKey(player)) {
			return playerPlays.get(player);
		} else {
			return 0;
		}
	}

	public double getPlayerAverage(String player) {
		return getPlayerWins(player) - getPlayerPlays(player);
	}

	@Override
	public void loadConfig() throws IOException, InvalidConfigurationException {
		super.loadConfig();
		recentWinners = new LinkedList<String>();
		recentWinners.addAll(getStringList("recentWinners", Collections.EMPTY_LIST));
		playerPlays = new HashMap<String, Integer>();
		playerLosses = new HashMap<String, Integer>();
		if (hasConfigValue("playerPlays")) {
			Map<String, Object> winsTemp = ((ConfigurationSection) getConfigValue("playerPlays", (Object) null)).getValues(false);
			for (String key : winsTemp.keySet()) {
				playerPlays.put(key, (Integer) winsTemp.get(key));
			}
		}
		if (hasConfigValue("playerLosses")) {
			Map<String, Object> lossTemp = ((ConfigurationSection) getConfigValue("playerLosses", (Object) null)).getValues(false);
			for (String key : lossTemp.keySet()) {
				playerLosses.put(key, (Integer) lossTemp.get(key));
			}
		}
	}

	@Override
	public void saveConfig() throws IOException {
		updateConfigValue("wins", playerPlays);
		updateConfigValue("losses", playerLosses);
		updateConfigValue("recentWinners", recentWinners);
		super.saveConfig();
	}


	public void dumpRecentHistory(CommandSender playerToDumpTo) {
		playerToDumpTo.sendMessage("Recent winners:" + StringUtils.flatten(recentWinners));
	}

	public void dumpLeaderBoard(CommandSender playerToDumpTo) {
		HashSet<String> players = new HashSet<String>();
		players.addAll(playerPlays.keySet());
		LinkedList<String> result = new LinkedList<String>();
		result.addAll(players);
		Collections.sort(result, new Comparator<String>() {
			@Override
			public int compare(String s, String s1) {
				if (getPlayerAverage(s) < getPlayerAverage(s1)) {
					// better average
					return 1;
				} else if (getPlayerAverage(s) > getPlayerAverage(s1)) {
					// worse average
					return -1;
				} else {
					// same average, so favor person with most plays
					int sTotal = getPlayerWins(s) + getPlayerLosses(s);
					int s1Total = getPlayerWins(s1) + getPlayerLosses(s1);
					if (sTotal < s1Total) {
						return 1;
					}
					if (sTotal > s1Total) {
						return -1;
					} else {
						// OK, just do it in alphabetical order
						return s.compareTo(s1);
					}
				}
			}
		});
		playerToDumpTo.sendMessage("Leader board:");
		for (String playerName : result) {
			String avgString = Double.toString(getPlayerAverage(playerName) * 100);
			int totalPlays = getPlayerWins(playerName) + getPlayerLosses(playerName);
			playerToDumpTo.sendMessage("    " + StringUtils.fitToColumnSize(avgString, 5) + "% of " + StringUtils.fitToColumnSize(Integer.toString(totalPlays), 3) + ": " + playerName);
		}
	}
}
