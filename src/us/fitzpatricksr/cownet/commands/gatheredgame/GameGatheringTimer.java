package us.fitzpatricksr.cownet.commands.gatheredgame;

import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.CowNetThingy;

/**
 * This class simply manages the state transition changes in the game.  It doesn't keep track of
 * players, winners, losers or much else.  There is a simple callback interface that is used
 * to tell clients when the game progresses from one state to the next.  The whole class is
 * driven by clients calling the getGameState() method.
 * <p/>
 */
public class GameGatheringTimer {
	private static final int GAME_WATCHER_FREQUENCY = 20 * 1; // 1 second

	public static enum GamePhase {
		GATHERING,      //gathering tributes
		ACCLIMATING,    //players are in the arena but can't do anything yet.
		IN_PROGRESS,    //started but not over yet
		ENDED            //the game is over
	}

	public interface Listener {
		public void gameGathering();

		public void gameAcclimating();

		public void gameInProgress();

		public void gameEnded();

		public void gameCanceled();

		public void announceGather(long time);

		public void announceAcclimate(long time);

		public void announceWindDown(long time);
	}

	// hey jf - these settings should be pushed up into the highest level client and passed down.
	@CowNetThingy.Setting
	public static long timeToGather = 1 * 60 * 1000; // 1 minute
	@CowNetThingy.Setting
	public static long timeToAcclimate = 10 * 1000; // 10 seconds
	@CowNetThingy.Setting
	public static long timeToRun = 2 * 60 * 1000; // 2 minute game

	private Listener listener;                      // client callback
	private long time = System.currentTimeMillis();       // time when the current phase started
	private GamePhase gameState = GamePhase.GATHERING;    // what stage of the game we're in
	private JavaPlugin plugin;
	private int gatherTaskId = 0;

	public GameGatheringTimer(JavaPlugin plugin, Listener listener) {
		this.plugin = plugin;
		this.listener = listener;
		this.listener.gameGathering();
		gatherTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() {
				gameWatcher();
			}
		}, GAME_WATCHER_FREQUENCY, GAME_WATCHER_FREQUENCY);
	}

	public String getGameStatusMessage() {
		if (isGathering()) {
			long timeToWait = getTimeToGather() / 1000;
			return "Game Status: Gathering.  The games will start in " + timeToWait + " seconds";
		} else if (isGameOn()) {
			return "Game Status: IN PROGRESS";
		} else {
			return "Game Status: Eneded.";
		}
	}

	public boolean isGathering() {
		return getGameState() == GamePhase.GATHERING;
	}

	public boolean isAcclimating() {
		return getGameState() == GamePhase.ACCLIMATING;
	}

	public boolean isInProgress() {
		return getGameState() == GamePhase.IN_PROGRESS;
	}

	public boolean isGameOn() {
		GamePhase phase = getGameState();
		return phase == GamePhase.IN_PROGRESS || phase == GamePhase.ACCLIMATING;
	}

	public boolean isEnded() {
		return getGameState() == GamePhase.ENDED;
	}

	public long getTimeToGather() {
		return Math.max((timeToGather + time) - System.currentTimeMillis(), 0);
	}

	public long getTimeToAcclimate() {
		return Math.max((timeToAcclimate + time) - System.currentTimeMillis(), 0);
	}

	public long getTimeUntilEnd() {
		return Math.max((timeToRun + time) - System.currentTimeMillis(), 0);
	}

	/* start acclimating if we haven't already done so */
	public void startAcclimating() {
		if (gameState == GamePhase.GATHERING) {
			gameState = GamePhase.ACCLIMATING;
			time = System.currentTimeMillis();
			listener.gameAcclimating();
		}
	}

	/* start the game if it isn't already started */
	public void startGame() {
		startAcclimating();
		if (gameState == GamePhase.ACCLIMATING) {
			gameState = GamePhase.IN_PROGRESS;
			time = System.currentTimeMillis();
			listener.gameInProgress();
		}
	}

	public void cancelGame() {
		gameState = GamePhase.ENDED;
		time = System.currentTimeMillis();
		listener.gameCanceled();
	}

	public void endGame() {
		gameState = GamePhase.ENDED;
		time = System.currentTimeMillis();
		listener.gameEnded();
	}

	private GamePhase getGameState() {
		return gameState;
	}

	private void gameWatcher() {
		if (gameState == GamePhase.GATHERING) {
			if (getTimeToGather() <= 0) {
				startAcclimating();
			} else {
				long timeToWait = getTimeToGather() / 1000;
				if (timeToWait % 10 == 0 || timeToWait < 10) {
					listener.announceGather(timeToWait);
				}
			}
		} else if (gameState == GamePhase.ACCLIMATING) {
			if (getTimeToAcclimate() <= 0) {
				startGame();
			} else {
				long timeToWait = getTimeToAcclimate() / 1000;
				listener.announceAcclimate(timeToWait);
			}
		} else if (gameState == GamePhase.IN_PROGRESS) {
			if (getTimeUntilEnd() <= 0) {
				endGame();
			} else {
				long timeToWait = getTimeUntilEnd() / 1000;
				if (timeToWait % 30 == 0 && timeToWait > 10) {
					listener.announceGather(timeToWait);
				} else if (timeToWait <= 10) {
					listener.announceWindDown(timeToWait);
				}
			}
		} else {
			// gamestate here should be ENDED, so kill the timer
			if (gatherTaskId != 0) {
				plugin.getServer().getScheduler().cancelTask(gatherTaskId);
				gatherTaskId = 0;
			}
		}
	}
}