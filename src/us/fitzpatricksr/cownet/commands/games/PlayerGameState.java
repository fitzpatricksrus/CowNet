package us.fitzpatricksr.cownet.commands.games;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/*
This class keeps track of who is part of what game.  It keeps track of the
games players are in globally in order to limit the players to one
game at a time.
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
		public boolean playerCanJoin(String playerName);

		public void playerJoined(String playerName);

		public void playerLeft(String playerName);
	}

	private static final Listener DUMMY_LISTENER = new Listener() {  //stub listener so we always have one.
		@Override
		public boolean playerCanJoin(String playerName) {
			return true;
		}

		@Override
		public void playerJoined(String playerName) {
		}

		@Override
		public void playerLeft(String playerName) {
		}
	};

	// what game is a particular player in?   playerName -> gameName
	// this is static shared state in order to limit players to one
	// game at a time.
	private static final HashMap<String, String> playerGames = new HashMap<String, String>();

	// who's in a particular game?  game -> list of players
	private HashSet<String> participating = new HashSet<String>();
	// who gets notified when players enter and leave games
	private Listener listener;
	// The name of the game.   Null if this is a local game.
	private String gameName;

	public PlayerGameState(String gameName, Listener newListener) {
		this.gameName = gameName;
		this.listener = (newListener != null) ? newListener : DUMMY_LISTENER;
	}

	public void setListener(Listener newListener) {
		if (newListener == null) {
			listener = DUMMY_LISTENER;
		} else {
			listener = newListener;
		}
	}

	public void resetGame() {
		for (String player : participating) {
			// player was in this game, so remove the entry for this player.
			playerGames.remove(player);
		}
		participating.clear();
	}

	/* add a player to the game.  return true if player is then in the game   */
	public boolean addPlayer(String playerName) {
		String playersCurrentGame = playerGames.get(playerName);
		if (playersCurrentGame == null) {
			if (listener.playerCanJoin(playerName)) {
				// not in another game and not banned
				listener.playerJoined(playerName);
				// game said this player is allowed to join, so add them to the mix.
				participating.add(playerName);
				playerGames.put(playerName, gameName);
				return true;
			} else {
				// listener says player can't join
				return false;
			}
		} else {
			// player is already in a game.  OK if it's this one.  :-)
			return gameName.equals(playersCurrentGame);
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