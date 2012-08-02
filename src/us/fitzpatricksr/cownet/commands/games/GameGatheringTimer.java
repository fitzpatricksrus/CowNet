package us.fitzpatricksr.cownet.commands.games;

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

	private static enum GamePhase {
		GATHERING,      //gathering tributes
		LOUNGING,       //players are in the arena but can't do anything yet.
		IN_PROGRESS,    //started but not over yet
		ENDED           //the game is over
	}

	public interface Listener {
		public void gameGathering();

		public void gameLounging();

		public void gameInProgress();

		public void gameEnded();

		public void gameCanceled();

		public void announceGather(long time);

		public void announceLounging(long time);

		public void announceWindDown(long time);
	}

	@CowNetThingy.Setting
	public static long timeToGather = 1 * 60 * 1000; // 1 minute
	@CowNetThingy.Setting
	public static long timeToLounge = 10 * 1000; // 10 seconds
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
			return "Game Status: IN PROGRESS.  " + getTimeUntilEnd() / 1000 + " seconds to go.";
		} else {
			return "Game Status: Eneded.";
		}
	}

	public boolean isGathering() {
		return gameState == GamePhase.GATHERING;
	}

	public boolean isLounging() {
		return gameState == GamePhase.LOUNGING;
	}

	public boolean isInProgress() {
		return gameState == GamePhase.IN_PROGRESS;
	}

	public boolean isGameOn() {
		GamePhase phase = gameState;
		return phase == GamePhase.IN_PROGRESS || phase == GamePhase.LOUNGING;
	}

	public boolean isEnded() {
		return gameState == GamePhase.ENDED;
	}

	private long getTimeToGather() {  // private because it is not state aware
		return Math.max((timeToGather + time) - System.currentTimeMillis(), 0);
	}

	private long getTimeToLounge() {  // private because it is not state aware
		return Math.max((timeToLounge + time) - System.currentTimeMillis(), 0);
	}

	private long getTimeUntilEnd() {  // private because it is not state aware
		return Math.max((timeToRun + time) - System.currentTimeMillis(), 0);
	}

	/* start lounging if we haven't already done so */
	public void startLounging() {
		if (gameState == GamePhase.GATHERING) {
			gameState = GamePhase.LOUNGING;
			time = System.currentTimeMillis();
			listener.gameLounging();
		}
	}

	/* start the game if it isn't already started */
	public void startGame() {
		startLounging();  // don't skip lounging phase!
		if (gameState == GamePhase.LOUNGING) {
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

	private void gameWatcher() {
		if (gameState == GamePhase.GATHERING) {
			if (getTimeToGather() <= 0) {
				startLounging();
			} else {
				long timeToWait = getTimeToGather() / 1000;
				if (timeToWait % 10 == 0 || timeToWait < 10) {
					listener.announceGather(timeToWait);
				}
			}
		}
		// we check each state in case some state lasts for 0 time
		if (gameState == GamePhase.LOUNGING) {
			if (getTimeToLounge() <= 0) {
				startGame();
			} else {
				long timeToWait = getTimeToLounge() / 1000;
				listener.announceLounging(timeToWait);
			}
		}
		if (gameState == GamePhase.IN_PROGRESS) {
			if (getTimeUntilEnd() <= 0) {
				endGame();
			} else {
				long timeToWait = getTimeUntilEnd() / 1000;
				if (timeToWait % 30 == 0 && timeToWait > 10) {
					listener.announceWindDown(timeToWait);
				} else if (timeToWait <= 10) {
					listener.announceWindDown(timeToWait);
				}
			}
		}
		if (gameState == GamePhase.ENDED) {
			// gamestate here should be ENDED, so kill the timer
			if (gatherTaskId != 0) { // gatherTaskId should never be 0 here
				plugin.getServer().getScheduler().cancelTask(gatherTaskId);
				gatherTaskId = 0;
			} else {
				// gameWatcher() should only be called by the watcher thread whose ID
				// is stored in gatherTaskId.  If gatherTaskId == 0, then something
				// is calling this method other than the thread or the thread
				// wasn't killed properly.
				try {
					throw new Exception();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}