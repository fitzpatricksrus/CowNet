package us.fitzpatricksr.cownet.commands.games.framework;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.CowNetThingy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Logger;

/*
lounge, then game.
*/
public class SimpleGameController implements GameContext {
    private Logger logger = Logger.getLogger("Minecraft");
    private Random rand = new Random();
    private CowNetThingy mod;
    private boolean isLounging;
    private HashMap<String, Team> players;
    private int currentModule;
    private GameModule[] modules;
    private int gameTimerTaskId;

    @CowNetThingy.Setting
    private static boolean isDebug = false;

    public SimpleGameController(CowNetThingy mod, GameModule[] modules) {
        this.mod = mod;
        this.isLounging = true;   // lounging
        this.players = new HashMap<String, Team>();
        this.modules = modules;
        this.currentModule = 0;
        this.gameTimerTaskId = 0;
    }

    public void startup() {
        this.modules[currentModule].startup(this);
        this.modules[currentModule].loungeStarted();
        startTimerTask();
    }

    public void shutdown() {
        stopTimerTask();
        endLounging();
        modules[currentModule].gameEnded();
        isLounging = true;
        modules[currentModule].shutdown(this);
    }

    //---------------------------------------------------------------------
    // GameContext interface

    @Override
    public CowNetThingy getCowNet() {
        return mod;
    }

    @Override
    public boolean isLounging() {
        return isLounging;
    }

    @Override
    public void endLounging() {
        debugInfo("endLounging");
        if (isLounging) {
            stopTimerTask();
            modules[currentModule].loungeEnded();
            isLounging = false;
            balanceTeams();
            modules[currentModule].gameStarted();
            startTimerTask();
        } else {
            // lounging already over, so nothing to do
        }
    }

    @Override
    public boolean isGaming() {
        return !isLounging;
    }

    @Override
    public void endGame() {
        debugInfo("endGame");
        if (isLounging) {
            // to end the game, we must cleanly end the lounge first
            endLounging();
        }
        stopTimerTask();    //stop game timer
        modules[currentModule].gameEnded();
        modules[currentModule].shutdown(this);
        isLounging = true;
        currentModule = (currentModule + 1) % modules.length;
        modules[currentModule].startup(this);
        modules[currentModule].loungeStarted();
        startTimerTask();   //start lounge timer
    }

    private void stopTimerTask() {
        if (gameTimerTaskId != 0) {
            debugInfo("stopTimerTask");
            getCowNet().getPlugin().getServer().getScheduler().cancelTask(gameTimerTaskId);
            gameTimerTaskId = 0;
        }
    }

    private void startTimerTask() {
        stopTimerTask();
        long maxTime = isLounging ? modules[currentModule].getLoungeDuration() * 20 : modules[currentModule].getGameDuration() * 20;
        if (maxTime > 0) {
            debugInfo("startTimerTask");
            JavaPlugin plugin = getCowNet().getPlugin();
            gameTimerTaskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    endGame();
                }
            }, maxTime);
        }
    }

    //---------------------------------------------------------------------
    // Team and player management

    @Override
    public Collection<String> getPlayers() {
        return players.keySet();
    }

    @Override
    public void broadcastToAllPlayers(String message) {
        for (String playerName : players.keySet()) {
            Player player = getPlayer(playerName);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    @Override
    public Player getPlayer(String playerName) {
        return mod.getPlugin().getServer().getPlayer(playerName);
    }

    @Override
    public Team getPlayerTeam(String playerName) {
        return players.get(playerName);
    }

    public void addPlayer(String playerName) {
        if (!players.containsKey(playerName)) {
            int red = Collections.frequency(players.values(), Team.RED);
            int blue = Collections.frequency(players.values(), Team.BLUE);
            if (red < blue) {
                players.put(playerName, Team.RED);
            } else if (blue < red) {
                players.put(playerName, Team.BLUE);
            } else {
                players.put(playerName, rand.nextBoolean() ? Team.RED : Team.BLUE);
            }
            playerEntered(playerName);
        }
    }

    public void removePlayer(String playerName) {
        if (players.containsKey(playerName)) {
            players.remove(playerName);
            playerLeft(playerName);
        }
    }

    private void playerEntered(String playerName) {
        if (isLounging()) {
            modules[currentModule].playerEnteredLounge(playerName);
        } else {
            modules[currentModule].playerEnteredGame(playerName);
        }
    }

    private void playerLeft(String playerName) {
        if (isLounging()) {
            modules[currentModule].playerLeftLounge(playerName);
        } else {
            modules[currentModule].playerLeftGame(playerName);
        }
    }

    public boolean changePlayerTeam(String playerName, Team team) {
        Team t = getPlayerTeam(playerName);

        // if already on this team, do nothing.
        if (t == team) return true;

        int red = Collections.frequency(players.values(), Team.RED);
        int blue = Collections.frequency(players.values(), Team.BLUE);

        if (team == Team.RED) {
            red += 1;
            blue -= 1;
        } else {
            red -= 1;
            blue += 1;
        }
        int sizeDiff = Math.abs(red - blue);
        if (sizeDiff > 1) {
            // teams would be out of balance, so fail
            return false;
        } else {
            players.remove(playerName);
            playerLeft(playerName);
            players.put(playerName, team);
            playerEntered(playerName);
            return true;
        }
    }

    public void balanceTeams() {
        // remove players that have left the server or the game world
        for (String playerName : players.keySet()) {
            if (getPlayer(playerName) == null) {
                // player has left the server.  remove them from their team
                players.remove(playerName);
            }
        }

        // make sure teams are balanced
        int red = Collections.frequency(players.values(), Team.RED);
        int blue = Collections.frequency(players.values(), Team.BLUE);
        while (Math.abs(red - blue) > 1) {
            for (String playerName : players.keySet()) {
                if (red > blue) {
                    if (players.get(playerName) == Team.RED) {
                        players.put(playerName, Team.BLUE);
                        break;
                    }
                } else {
                    if (players.get(playerName) == Team.BLUE) {
                        players.put(playerName, Team.RED);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void addWin(String playerName) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addLoss(String playerName) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void debugInfo(String message) {
        if (isDebug) {
            logger.info(message);
        }
    }
}
