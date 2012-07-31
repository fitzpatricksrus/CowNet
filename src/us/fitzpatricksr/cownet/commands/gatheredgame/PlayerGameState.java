package us.fitzpatricksr.cownet.commands.gatheredgame;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/*
This class keeps track of who is part of what game.  Eventually
it will limit the number of games a player can be in to 1.
 */
public class PlayerGameState {
	public enum PlayerState {
		ALIVE("a player"),
		WATCHING("a spectator");

		private String printString;

		private PlayerState(String printString) {
			this.printString = printString;
		}

		public String toString() {
			return printString;
		}

		public String toString(Player player) {
			return player.getDisplayName() + ": " + toString();
		}
	}

	public interface Listener {
		public boolean playerJoined(String playerName, String teamName);

		public void playerLeft(String playerName);
	}

	private String name;

	public PlayerGameState(String name) {
		this.name = name;
	}

	public void setListener(Listener listener) {
		setListener(name, listener);
	}

	public void removeListener() {
		removeListener(name);
	}

	public void resetGame() {
		resetGame(name);
	}

	public boolean addPlayer(String playerName) {
		return addPlayer(name, playerName, "");
	}

	public boolean addPlayer(String playerName, String teamName) {
		return addPlayer(name, playerName, teamName);
	}

	public void removePlayer(String playerName) {
		removePlayer(name, playerName);
	}

	public Set<String> getPlayers() {
		return getPlayers(name);
	}

	public String getGameOfPlayer(String playerName) {
		return playerGames.get(playerName);
	}

	public String getTeamOfPlayer(String playerName) {
		if (getGameOfPlayer(playerName).equals(name)) {
			return playerTeams.get(playerName);
		} else {
			return null;
		}
	}

	public PlayerState getPlayerState(String playerName) {
		return getPlayerState(name, playerName);
	}

	/*
	This class is a general bag for stats.  It's main structure is a multi-level
	hash table that maps playerName->statKey->value
	It also has a single, fixed length list that can be used to keep track
	of recent winners/losers.
	*/

	// who's in a particular game?  game -> list of players
	private static HashMap<String, HashSet<String>> participating = new HashMap<String, HashSet<String>>();
	// what game is a particular player in?   playerName -> game
	private static HashMap<String, String> playerGames = new HashMap<String, String>();
	// what team is a particular player on?
	private static HashMap<String, String> playerTeams = new HashMap<String, String>();
	// who get's notified when players enter and leave games
	private static HashMap<String, Listener> listeners = new HashMap<String, Listener>();

	private static void setListener(String gameName, Listener listener) {
		listeners.put(gameName, listener);
		participating.put(gameName, new HashSet<String>());
	}

	private static void removeListener(String gameName) {
		resetGame(gameName);
		listeners.remove(gameName);
		participating.remove(gameName);
	}

	private static void resetGame(String gameName) {
		for (String player : participating.get(gameName)) {
			// player was in this game, so remove the entry for this player.
			playerGames.remove(player);
			playerTeams.remove(player);
		}
		participating.get(gameName).clear();
	}

	/* add a player to the game.  return true if player was added   */
	private static boolean addPlayer(String gameName, String playerName, String teamName) {
		if (!playerGames.containsKey(playerName)) {
			// not in another game and not banned
			if (listeners.get(gameName).playerJoined(playerName, teamName)) {
				// game said this player is allowed to join, so add them to the mix.
				participating.get(gameName).add(playerName);
				playerGames.put(playerName, gameName);
				playerTeams.put(playerName, teamName);
				return true;
			}
		}
		return false;
	}

	/* remove a player from the game.  If the game has already started
	* this player accumulates a loss.  listener.playerLeft() is called
	* if the game has stared. */
	private static void removePlayer(String gameName, String playerName) {
		if (participating.get(gameName).contains(playerName)) {
			participating.get(gameName).remove(playerName);
			playerGames.remove(playerName);
			playerTeams.remove(playerName);
			listeners.get(gameName).playerLeft(playerName);
		}
	}

	private static Set<String> getPlayers(String gameName) {
		return participating.get(gameName);
	}

	private static PlayerState getPlayerState(String gameName, String playerName) {
		if (participating.get(gameName).contains(playerName)) {
			return PlayerState.ALIVE;
		} else {
			return PlayerState.WATCHING;
		}
	}

	private String getPlayerTeam(String playerName) {
		return playerTeams.get(playerName);
	}
}