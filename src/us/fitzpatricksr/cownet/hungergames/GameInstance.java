package us.fitzpatricksr.cownet.hungergames;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * A GameInstance represents a single instance of a game being played.  I goes through multiple distinct phases:
 * unstarted -> gathering -> failed* | acclimating -> inprogress -> ended
 * <p/>
 * This class is terribly hacky.  I tries to define the state transitions by just the number of people in the game
 * and the time since the first player joined instead of explicitly keeping state.  Don't know why this made
 * sense, but since it works I don't see any reason to change it.  I'm sure it can be simplified.
 */
public class GameInstance {
    private static enum GamePhase {
        UNSTARTED,      //havent started yet
        GATHERING,      //gathering tributes
        ACCLIMATING,     //players are in the arena but can't do anything yet.
        IN_PROGRESS,    //started but not over yet
        ENDED,          //it ran and it's over
        FAILED,         //didn't gather enough players
    }

    public static long timeToGather = 1 * 60 * 1000; // 1 minute
    public static long timeToAcclimate = 10 * 1000; // 10 seconds
    public static int minTributes = 2;

    private long firstPlayerJoinTime = 0;
    private int gameSize = 0; //the most people who have ever been in the games
    private HashMap<Player, PlayerInfo> gameInfo = new HashMap<Player, PlayerInfo>();

    public String getGameStatusMessage() {
        if (isEnded()) {
            return "Game Status: ENDED.";
        } else if (isFailed()) {
            return "Game Status: Canceled due to lack of tributes.";
        } else if (isGathering()) {
            long timeToWait = getTimeToGather() / 1000;
            return "Game Status: Gathering.  The games will start in " + timeToWait + " seconds";
        } else if (isGameOn()) {
            return "Game Status: IN PROGRESS";
        } else if (isUnstarted()) {
            return "Game Status: Waiting for first tribute";
        } else {
            return "Game Status: UNKNOWN.  What's up with that?";
        }
    }

    public boolean isUnstarted() {
        return getGameState() == GamePhase.UNSTARTED;
    }

    public boolean isEnded() {
        return getGameState() == GamePhase.ENDED;
    }

    public boolean isFailed() {
        return getGameState() == GamePhase.FAILED;
    }

    public boolean isGathering() {
        return getGameState() == GamePhase.GATHERING;
    }

    public boolean isGameOn() {
        GamePhase phase = getGameState();
        return phase == GamePhase.IN_PROGRESS ||
                phase == GamePhase.ACCLIMATING;
    }

    public boolean isInProgress() {
        return getGameState() == GamePhase.IN_PROGRESS;
    }

    public boolean isAcclimating() {
        return getGameState() == GamePhase.ACCLIMATING;
    }

    public List<PlayerInfo> getPlayersInGame() {
        LinkedList<PlayerInfo> result = new LinkedList<PlayerInfo>();
        for (PlayerInfo p : gameInfo.values()) {
            if (p.isInGame()) {
                result.add(p);
            }
        }

        return result;
    }

    public void addPlayerToGame(Player player) {
        PlayerInfo info = getPlayerInfo(player);
        info.setIsInGame();
        if (firstPlayerJoinTime == 0) {
            firstPlayerJoinTime = System.currentTimeMillis();
        }
    }

    public void removePlayerFromGame(Player player) {
        if (getPlayerInfo(player).isInGame()) {
            getPlayerInfo(player).setIsOutOfGame();
        }
    }

    public PlayerInfo getPlayerInfo(Player p) {
        PlayerInfo info = gameInfo.get(p);
        if (info == null) {
            info = new PlayerInfo(p);
            gameInfo.put(p, info);
        }
        return info;
    }

    public long getTimeToGather() {
        return Math.max(timeToGather - timeSinceFirstPlayer(), 0);
    }

    public long getTimeToAcclimate() {
        return Math.max(timeToGather + timeToAcclimate - timeSinceFirstPlayer(), 0);
    }

    private long timeSinceFirstPlayer() {
        return (firstPlayerJoinTime != 0) ? System.currentTimeMillis() - firstPlayerJoinTime : -1;
    }

    // the number of people who were in the games when it started.
    public int getNumberOfGamePlayers() {
        return gameSize;
    }

    public void startNow() {
        firstPlayerJoinTime = System.currentTimeMillis() - timeToGather;
    }

    private GamePhase getGameState() {
        int livePlayerCount = getPlayersInGame().size();
        long time = timeSinceFirstPlayer();
        if (time == -1) {
            return GamePhase.UNSTARTED;
        } else if (time < timeToGather) {
            if (livePlayerCount < 1) {
                return GamePhase.FAILED;
            } else {
                return GamePhase.GATHERING;
            }
        } else if (time < timeToGather + timeToAcclimate) {
            gameSize = Math.max(0, livePlayerCount);
            if (livePlayerCount < minTributes) {
                return GamePhase.FAILED;
            } else {
                return GamePhase.ACCLIMATING;
            }
        } else {
            if (livePlayerCount < minTributes) {
                // we have a winner...maybe...unless they both died at once.
                return GamePhase.ENDED;
            } else {
                return GamePhase.IN_PROGRESS;
            }
        }
    }
}