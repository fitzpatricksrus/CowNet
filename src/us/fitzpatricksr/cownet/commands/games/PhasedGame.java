package us.fitzpatricksr.cownet.commands.games;

import us.fitzpatricksr.cownet.CowNetThingy;

/**
 * handleBeingGathering
 * handlePlayerEnteredGathering
 * handlePlayerLeftGathering
 * handleEndGathering
 * handleBeingLounging
 * handlePlayerEnteredLounge
 * handlePlayerLeftLounge
 * handleEndLounging
 * handleBeingGame
 * handlePlayerEnteredGame
 * handlePlayerLeftGame
 * handleEndGame
 * handleCancelGame
 */
public class PhasedGame extends CowNetThingy {

	protected static class PlayerCantBeAddedException extends Exception {
		public final String reason;

		public PlayerCantBeAddedException(String reason) {
			this.reason = reason;
		}
	}


	//-----------------------------------------------------------
	// Player management
	//
	// These methods don't keep track of the players, only ensure
	// that the proper callbacks are made and that gathering
	// begins when the first player enters the game.  Subclasses
	// should keep track of players as needed.  This allows
	// subclasses to organize players as they see fit.
	// The first player added to the game starts the gathering timer.

	public final void addPlayer(String playerName) throws PlayerCantBeAddedException {
		if (timer.isGathering()) {
			handlePlayerEnteredGathering(playerName);
		} else if (timer.isLounging()) {
			handlePlayerEnteredLounge(playerName);
		} else if (timer.isGameOn()) {
			handlePlayerEnteredGame(playerName);
		}
		startGathering();
	}

	public final void removePlayer(String playerName) {
		if (timer.isGathering()) {
			handlePlayerLeftGathering(playerName);
		} else if (timer.isLounging()) {
			handlePlayerLeftLounge(playerName);
		} else if (timer.isGameOn()) {
			handlePlayerLeftGame(playerName);
		}
	}

	//-----------------------------------------------------------
	// Game management
	//
	// The first player added to the game starts the gathering timer.
	// Since PhasedGame does not track individual players or know
	// conditions that will terminate the game this is left to
	// subclasses to determine.  endGame() and cancelGame()
	// allow subclasses to stop the gathering timer and clear
	// game state.

	public final void startLounging() {
		if (timer != null) {
			timer.startLounging();
		}
	}

	public final void startGame() {
		if (timer != null) {
			timer.startGame();
		}
	}

	public final void endGame() {
		if (timer != null) {
			timer.endGame();
			timer = null;
		}
	}

	public final void cancelGame() {
		if (timer != null) {
			timer.cancelGame();
			timer = null;
		}
	}

	//-----------------------------------------------------------
	// Gathering events
	protected void handleBeingGathering() {
	}

	protected void handlePlayerEnteredGathering(String playerName) throws PlayerCantBeAddedException {
	}

	protected void handlePlayerLeftGathering(String playerName) {
	}

	protected void handleEndGathering() {
	}

	//-----------------------------------------------------------
	// lounging events
	protected void handleBeginLounging() {
	}

	protected void handlePlayerEnteredLounge(String playerName) throws PlayerCantBeAddedException {
	}

	protected void handlePlayerLeftLounge(String playerName) {
	}

	protected void handleEndLounging() {
	}

	//-----------------------------------------------------------
	// Game events
	protected void handleBeginGame() {
	}

	protected void handlePlayerEnteredGame(String playerName) throws PlayerCantBeAddedException {
	}

	protected void handlePlayerLeftGame(String playerName) {
	}

	protected void handleEndGame() {
	}

	protected void handleCancelGame() {
	}

	//-----------------------------------------------------------
	// Game phase control
	private GameGatheringTimer timer;

	private void startGathering() {
		if (timer != null) return; // only start a new timer if one isn't already running.
		timer = new GameGatheringTimer(getPlugin(), new GameGatheringTimer.Listener() {
			@Override
			public void gameGathering() {
				handleBeingGathering();
			}

			@Override
			public void gameLounging() {
				handleEndGathering();
				handleBeginLounging();
			}

			@Override
			public void gameInProgress() {
				handleEndLounging();
				handleBeginGame();
			}

			@Override
			public void gameEnded() {
				handleEndGame();
			}

			@Override
			public void gameCanceled() {
				handleCancelGame();
			}

			@Override
			public void announceGather(long time) {
				broadcastToAllOnlinePlayers(timer.getGameStatusMessage());
			}

			@Override
			public void announceLounging(long time) {
				broadcastToAllOnlinePlayers(timer.getGameStatusMessage());
			}

			@Override
			public void announceWindDown(long time) {
				broadcastToAllOnlinePlayers(timer.getGameStatusMessage());
			}
		});
	}
}
