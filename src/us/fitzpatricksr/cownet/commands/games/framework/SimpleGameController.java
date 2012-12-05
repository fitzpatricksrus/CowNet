package us.fitzpatricksr.cownet.commands.games.framework;

import org.bukkit.entity.Player;
import us.fitzpatricksr.cownet.CowNetMod;
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
    private CowNetMod mod;
    private boolean isGaming;
    private HashMap<String, Team> players;
    private int currentModule;
    private GameModule[] modules;

    @CowNetThingy.Setting
    private static boolean isDebug = false;

    public SimpleGameController(CowNetMod mod, GameModule[] modules) {
        this.mod = mod;
        this.isGaming = false;   // lounging
        this.players = new HashMap<String, Team>();
        this.modules = modules;
        this.currentModule = 0;
    }

    public void startup() {
        this.modules[currentModule].startup(this);
        this.modules[currentModule].loungeStarted();
    }

    public void shutdown() {
        endLounging();
        modules[currentModule].gameEnded();
        isGaming = false;
        modules[currentModule].shutdown(this);
    }

    //---------------------------------------------------------------------
    // GameContext interface

    @Override
    public CowNetMod getCowNet() {
        return mod;
    }

    @Override
    public boolean isLounging() {
        return !isGaming;
    }

    @Override
    public void endLounging() {
        if (isLounging()) {
            modules[currentModule].loungeEnded();
            isGaming = true;
            modules[currentModule].gameStarted();
        } else {
            // lounging already over, so nothing to do
        }
    }

    @Override
    public boolean isGaming() {
        return isGaming;
    }

    @Override
    public void endGame() {
        if (isLounging()) {
            // to end the game, we must cleanly end the lounge first
            endLounging();
        }
        modules[currentModule].gameEnded();
        isGaming = false;
        modules[currentModule].shutdown(this);
        currentModule = (currentModule + 1) % modules.length;
        modules[currentModule].startup(this);
        modules[currentModule].loungeStarted();
    }

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
        return mod.getServer().getPlayer(playerName);
    }

    @Override
    public Team getPlayerTeam(String playerName) {
        return players.get(playerName);
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

    //---------------------------------------------------------------------
    //

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
}
