package us.fitzpatricksr.cownet.commands.games;

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

	public static class PlayerCantJoinException extends Exception {
		public PlayerCantJoinException(String reason) {
			super(reason);
		}
	}

	public interface Listener {
		public void playerJoined(String playerName) throws PlayerCantJoinException;

		public void playerLeft(String playerName);
	}

	private static final Listener DUMMY_LISTENER = new Listener() {  //stub listener so we always have one.
		@Override
		public void playerJoined(String playerName) throws PlayerCantJoinException {
		}

		@Override
		public void playerLeft(String playerName) {
		}
	};

	// what game is a particular player in?   playerName -> game
	// this is statis shared state in order to limit players to one
	// game at a time.
	private static HashMap<String, String> globalPlayerGames = new HashMap<String, String>();

	// who's in a particular game?  game -> list of players
	private HashSet<String> participating = new HashSet<String>();
	// who get's notified when players enter and leave games
	private Listener listener;
	// a reference to the list of public games to use.  If it's a local game, use a local list.
	private HashMap<String, String> playerGames;
	// The name of the game.   Null if this is a local game.
	private String gameName;

	public PlayerGameState(Listener newListener) {
		this.gameName = null;
		this.playerGames = new HashMap<String, String>();
		this.listener = (newListener != null) ? newListener : DUMMY_LISTENER;
	}

	public PlayerGameState(String gameName, Listener newListener) {
		this.gameName = gameName;
		this.playerGames = globalPlayerGames;
		this.listener = (newListener != null) ? newListener : DUMMY_LISTENER;
	}

	/*
	This class is a general bag for stats.  It's main structure is a multi-level
	hash table that maps playerName->statKey->value
	It also has a single, fixed length list that can be used to keep track
	of recent winners/losers.
	*/

	public void setListener(Listener newListener) {
		if (newListener == null) {
			listener = DUMMY_LISTENER;
		}
	}

	public void resetGame() {
		for (String player : participating) {
			// player was in this game, so remove the entry for this player.
			playerGames.remove(player);
		}
		participating.clear();
	}

	/* add a player to the game.  return true if player was added   */
	public void addPlayer(String playerName) throws PlayerCantJoinException {
		if (!playerGames.containsKey(playerName)) {
			// not in another game and not banned
			listener.playerJoined(playerName);
			// game said this player is allowed to join, so add them to the mix.
			participating.add(playerName);
			playerGames.put(playerName, gameName);
		} else {
			throw new PlayerCantJoinException(playerName + " is already in a game. (" + playerGames.get(playerName) + ")");
		}
	}

	/* remove a player from the game.  If the game has already started
	* this player accumulates a loss.  listener.playerLeft() is called
	* if the game has stared. */
	public void removePlayer(String playerName) {
		if (participating.contains(playerName)) {
			participating.remove(playerName);
			playerGames.remove(playerName);
			listener.playerLeft(playerName);
		}
	}

	public Set<String> getPlayers() {
		return participating;
	}

	public String getGameOfPlayer(String playerName) {
		return playerGames.get(playerName);
	}

	public PlayerState getPlayerState(String playerName) {
		if (participating.contains(playerName)) {
			return PlayerState.ALIVE;
		} else {
			return PlayerState.WATCHING;
		}
	}
}