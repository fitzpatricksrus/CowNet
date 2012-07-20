package us.fitzpatricksr.cownet.commands.gatheredgame;

import us.fitzpatricksr.cownet.CowNetThingy;

/**
 * This class simply manages the state transition changes in the game.  It doesn't keep track of
 * players, winners, losers or much else.  There is a simple callback interface that is used
 * to tell clients when the game progresses from one state to the next.  The whole class is
 * driven by clients calling the getGameState() method.
 * <p/>
 */
public class GameGatheringTimer {
	public static enum GamePhase {
		GATHERING,      //gathering tributes
		ACCLIMATING,     //players are in the arena but can't do anything yet.
		IN_PROGRESS,    //started but not over yet
	}

	public interface GameStateListener {
		public void gameGathering();

		public void gameAcclimating();

		public void gameInProgress();
	}

	@CowNetThingy.Setting
	public static long timeToGather = 1 * 60 * 1000; // 1 minute
	@CowNetThingy.Setting
	public static long timeToAcclimate = 10 * 1000; // 10 seconds

	private GameStateListener listener;
	private long time = System.currentTimeMillis(); // time when the current phase started
	private GamePhase gameState = GamePhase.GATHERING;

	public GameGatheringTimer(GameStateListener listener) {
		this.listener = listener;
		this.listener.gameGathering();
	}

	public String getGameStatusMessage() {
		if (isGathering()) {
			long timeToWait = getTimeToGather() / 1000;
			return "Game Status: Gathering.  The games will start in " + timeToWait + " seconds";
		} else if (isGameOn()) {
			return "Game Status: IN PROGRESS";
		} else {
			return "Game Status: UNKNOWN.  What's up with that?";
		}
	}

	public boolean isGathering() {
		return getGameState() == GamePhase.GATHERING;
	}

	public boolean isGameOn() {
		GamePhase phase = getGameState();
		return phase == GamePhase.IN_PROGRESS || phase == GamePhase.ACCLIMATING;
	}

	public boolean isInProgress() {
		return getGameState() == GamePhase.IN_PROGRESS;
	}

	public boolean isAcclimating() {
		return getGameState() == GamePhase.ACCLIMATING;
	}

	public long getTimeToGather() {
		return Math.max((timeToGather + time) - System.currentTimeMillis(), 0);
	}

	public long getTimeToAcclimate() {
		return Math.max((timeToAcclimate + time) - System.currentTimeMillis(), 0);
	}

	/* end the GATHERING phase early */
	public void endGathering() {
		if (gameState == GamePhase.GATHERING) {
			startAcclimating();
		}
	}

	private void startAcclimating() {
		gameState = GamePhase.ACCLIMATING;
		time = System.currentTimeMillis();
		listener.gameAcclimating();
	}

	private GamePhase getGameState() {
		return gameState;
	}

	public void tick() {
		if (gameState == GamePhase.GATHERING) {
			if (getTimeToGather() <= 0) {
				startAcclimating();
			}
		} else if (gameState == GamePhase.ACCLIMATING) {
			if (getTimeToAcclimate() <= 0) {
				gameState = GamePhase.IN_PROGRESS;
				time = System.currentTimeMillis();
				listener.gameInProgress();
			}
		}
	}
}