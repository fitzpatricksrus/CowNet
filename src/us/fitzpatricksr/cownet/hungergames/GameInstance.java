package us.fitzpatricksr.cownet.hungergames;

import org.bukkit.entity.Player;
import org.easymock.EasyMock;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class GameInstance {
    private static enum GamePhase {
        UNSTARTED,      //havent started yet
        GATHERING,      //gathering tributes
        ACCLIMATING,     //players are in the arena but can't do anything yet.
        IN_PROGRESS,    //started but not over yet
        ENDED,          //it ran and it's over
        FAILED,         //didn't gather enough players
    }

    public static long timeToGather = 1 * 60 * 1000; // 1 minutesprivate long firstPlayerJoinTime = 0;private java.util.HashMap<org.bukkit.entity.Player,us.fitzpatricksr.cownet.HungerGames.PlayerInfo> gameInfo = new java.util.HashMap<org.bukkit.entity.Player,us.fitzpatricksr.cownet.HungerGames.PlayerInfo>();	public GameInstance(us.fitzpatricksr.cownet.HungerGames hungerGames)	{		this.hungerGames = hungerGames;	}private java.util.List<us.fitzpatricksr.cownet.HungerGames.PlayerInfo> getPlayersInGame() {
    public static long timeToAcclimate = 10 * 1000; // 1 minutesprivate long firstPlayerJoinTime = 0;private java.util.HashMap<org.bukkit.entity.Player,us.fitzpatricksr.cownet.HungerGames.PlayerInfo> gameInfo = new java.util.HashMap<org.bukkit.entity.Player,us.fitzpatricksr.cownet.HungerGames.PlayerInfo>();	public GameInstance(us.fitzpatricksr.cownet.HungerGames hungerGames)	{		this.hungerGames = hungerGames;	}private java.util.List<us.fitzpatricksr.cownet.HungerGames.PlayerInfo> getPlayersInGame() {
    public static int minTributes = 2;

    private long firstPlayerJoinTime = 0;
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

    void testPlayerListSupport() {
        List<PlayerInfo> infos = getPlayersInGame();
        GamePhase phase = getGameState();
        Player mockPlayer1 = EasyMock.createMock(Player.class);

        //test gathering state
        addPlayerToGame(mockPlayer1);
        infos = getPlayersInGame();
        phase = getGameState();

        //test failure to gather enough people
        firstPlayerJoinTime = 1;
        phase = getGameState();
        firstPlayerJoinTime = System.currentTimeMillis();

        //test adding two people while gathering
        Player mockPlayer2 = EasyMock.createMock(Player.class);
        addPlayerToGame(mockPlayer2);
        addPlayerToGame(mockPlayer1);
        infos = getPlayersInGame();
        phase = getGameState();

        //test finished gathering
        firstPlayerJoinTime = 1;
        phase = getGameState();

        //test one player leaves game.  should be ended
        removePlayerFromGame(mockPlayer1);
        infos = getPlayersInGame();
        phase = getGameState();
    }

    public static void main(String[] args) {
        GameInstance games = new GameInstance();
        games.testPlayerListSupport();
    }

}