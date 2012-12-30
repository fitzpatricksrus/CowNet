package us.fitzpatricksr.cownet.commands.games.framework;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.utils.StatusBoard;
import us.fitzpatricksr.cownet.utils.StringUtils;

import java.util.*;

/*
lounge, then game.
*/
public class SimpleGameController implements GameContext {
    private Random rand = new Random();
    private CowNetThingy mod;
    private boolean isLounging;
    private HashMap<String, Team> players;
    private int currentModule;
    private GameModule[] modules;
    private int gameTimerTaskId;
    private StatusBoard status;

    @CowNetThingy.Setting
    private int minPlayers = 1;

    public SimpleGameController(CowNetThingy mod, GameModule[] modules) {
        this.mod = mod;
        this.isLounging = true;   // lounging
        this.players = new HashMap<String, Team>();
        this.modules = modules;
        this.currentModule = 0;
        this.gameTimerTaskId = 0;
        this.status = new StatusBoard(4,
                "Game: %10s Team: %4s Score: %d",
                "Blue Team: %s",
                " Red Team: %s");
    }

    public void startup() {
        GameModule module = this.modules[currentModule];
        module.startup(newDebugWrapper(this, module.getName()));
        module.loungeStarted();
        startTimerTask();
    }

    public void shutdown() {
        GameModule module = this.modules[currentModule];
        stopTimerTask();
        endLounging();
        module.gameEnded();
        setLounging(true);
        module.shutdown();
    }

    private static GameContext newDebugWrapper(final GameContext context, final String tag) {
        return new GameContext() {

            @Override
            public CowNetThingy getCowNet() {
//                debugInfo("getCowNet");
                return context.getCowNet();
            }

            @Override
            public boolean isLounging() {
//                debugInfo("isLounging");
                return context.isLounging();
            }

            @Override
            public void endLounging() {
//                debugInfo("endLounging");
                context.endLounging();
            }

            @Override
            public boolean isGaming() {
//                debugInfo("isGaming");
                return context.isGaming();
            }

            @Override
            public void endGame() {
//                debugInfo("endGame");
                context.endGame();
            }

            @Override
            public Collection<String> getPlayers() {
//                debugInfo("getPlayers");
                return context.getPlayers();
            }

            @Override
            public void broadcastToAllPlayers(String message) {
//                debugInfo("broadcastToAllPlayers");
                context.broadcastToAllPlayers(message);
            }

            @Override
            public Player getPlayer(String playerName) {
//                debugInfo("getPlayer");
                return context.getPlayer(playerName);
            }

            @Override
            public Team getPlayerTeam(String playerName) {
//                debugInfo("getPlayerTeam");
                return context.getPlayerTeam(playerName);
            }

            @Override
            public Set<String> getPlayersOnTeam(Team team) {
                return context.getPlayersOnTeam(team);
            }

            @Override
            public void addWin(String playerName) {
//                debugInfo("addWin");
                context.addWin(playerName);
            }

            @Override
            public void addLoss(String playerName) {
//                debugInfo("addLoss");
                context.addLoss(playerName);
            }

            @Override
            public void debugInfo(String message) {
                context.debugInfo(tag + ": " + message);
            }
        };
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
        dumpDebugInfo("endLounging");
        stopTimerTask();
        if (isLounging) {
            if (getPlayers().size() >= minPlayers) {
                modules[currentModule].loungeEnded();
                setLounging(false);
                balanceTeams();
                modules[currentModule].gameStarted();
            } else {
                dumpDebugInfo("  - not enough players.  continue lounging");
                // not enough players.  continue to lounge
            }
        } else {
            dumpDebugInfo("  - Hmm....not lounging");
            // lounging already over, so nothing to do
        }
        startTimerTask();
    }

    @Override
    public boolean isGaming() {
        return !isLounging;
    }

    private void setLounging(boolean isLounging) {
        dumpDebugInfo("lounging: " + isLounging);
        this.isLounging = isLounging;
    }

    @Override
    public void endGame() {
        dumpDebugInfo("endGame");
        stopTimerTask();
        if (isLounging) {
            if (getPlayers().size() < minPlayers) {
                // OK, we're lounging and still don't have enough people
                // to have a real game.  No point in stopping lounging.
                dumpDebugInfo("  - not enough players to start a new game.");
                startTimerTask();   //start lounge timer
                return;
            } else {
                // to end the game, we must cleanly end the lounge first
                modules[currentModule].loungeEnded();
                setLounging(false);
                modules[currentModule].gameStarted();
            }
        }
        modules[currentModule].gameEnded();
        modules[currentModule].shutdown();
        setLounging(true);
        currentModule = (currentModule + 1) % modules.length;
        modules[currentModule].startup(newDebugWrapper(this, modules[currentModule].getName()));
        modules[currentModule].loungeStarted();
        startTimerTask();   //start lounge timer
    }

    private void stopTimerTask() {
        if (gameTimerTaskId != 0) {
            dumpDebugInfo("stopTimerTask");
            getCowNet().getPlugin().getServer().getScheduler().cancelTask(gameTimerTaskId);
            gameTimerTaskId = 0;
        }
    }

    private void startTimerTask() {
        stopTimerTask();
        long maxTime = isLounging ? modules[currentModule].getLoungeDuration() * 20 : modules[currentModule].getGameDuration() * 20;
        if (maxTime > 0) {
            debugInfo("startTimerTask: " + maxTime);
            JavaPlugin plugin = getCowNet().getPlugin();
            if (isLounging) {
                gameTimerTaskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        endLounging();
                    }
                }, maxTime);
            } else {
                gameTimerTaskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        endGame();
                    }
                }, maxTime);
            }
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
        // Game: gameName   Team: team name   Score: ##
        // Red:
        // Blue:
        // messages

        String gameName = modules[currentModule].getName();
        if (message != null) {
            status.chat(message);
        }
        status.format(1, StringUtils.flatten(getPlayersOnTeam(Team.RED)));
        status.format(2, StringUtils.flatten(getPlayersOnTeam(Team.BLUE)));
        for (String playerName : players.keySet()) {
            Player player = getPlayer(playerName);
            if (player != null) {
                Team team = players.get(playerName);
                status.format(0, gameName, team, 0);
                for (String line : status.getStatusLines()) {
                    player.sendMessage(line);
                }
            }
        }
    }

    public void broadcastChat(String playerName, String message) {
        broadcastToAllPlayers(playerName + ": " + message);
    }

    @Override
    public Player getPlayer(String playerName) {
        return mod.getPlugin().getServer().getPlayer(playerName);
    }

    @Override
    public Team getPlayerTeam(String playerName) {
        return players.get(playerName);
    }

    @Override
    public Set<String> getPlayersOnTeam(Team team) {
        HashSet<String> result = new HashSet<String>();
        for (String playerName : players.keySet()) {
            if (team == getPlayerTeam(playerName)) {
                result.add(playerName);
            }
        }
        return result;
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
            dumpDebugInfo("addPlayer(" + playerName + ") : " + players.get(playerName));
        }
        playerEntered(playerName);
    }

    public void removePlayer(String playerName) {
        // NOTE: this method is rarely needed since players are automatically
        // removed from the game when they leave the server
        if (players.containsKey(playerName)) {
            dumpDebugInfo("removePlayer(" + playerName + ")");
            players.remove(playerName);
            playerLeft(playerName);
        }
    }

    private void playerEntered(String playerName) {
        if (isLounging()) {
            dumpDebugInfo("playerEntered(" + playerName + ") lounge");
            modules[currentModule].playerEnteredLounge(playerName);
        } else {
            dumpDebugInfo("playerEntered(" + playerName + ") game");
            modules[currentModule].playerEnteredGame(playerName);
        }

        // add a hat
        fixTeamSuits();
    }

    private void playerLeft(String playerName) {
        if (isLounging()) {
            dumpDebugInfo("playerLeft(" + playerName + ") lounge");
            modules[currentModule].playerLeftLounge(playerName);
        } else {
            dumpDebugInfo("playerLeft(" + playerName + ") game");
            modules[currentModule].playerLeftGame(playerName);
        }

        // remove the hat.
        PlayerInventory inv = getPlayer(playerName).getInventory();
        inv.setHelmet(null);
    }

    public boolean playerIsInGame(String playerName) {
        return getPlayerTeam(playerName) != null;
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
            fixTeamSuits();
            return true;
        }
    }

    public void balanceTeams() {
        // remove players that have left the server or the game world
        for (String playerName : players.keySet()) {
            if (getPlayer(playerName) == null) {
                // player has left the server.  remove them from their team and the game
                debugInfo("Player no longer on server: " + playerName);
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
        fixTeamSuits();
    }

    private void fixTeamSuits() {
        if (modules[currentModule].isTeamGame()) {
            for (String playerName : players.keySet()) {
                // someone is now carrying the flag
                Player player = getPlayer(playerName);
                PlayerInventory inv = player.getInventory();
                ItemStack helmet = inv.getArmorContents()[3]; // getHelmet() wouldn't give me non-helmet blocks
                // if they already have a helmet, remove it and put it in their inventory
                if (helmet != null && helmet.getType() != Material.AIR) {
                    inv.addItem(helmet);
                }
                Team team = getPlayerTeam(player.getName());
                inv.setHelmet(team.getWool());
            }
        }
    }

    @Override
    public void addWin(String playerName) {
    }

    @Override
    public void addLoss(String playerName) {
    }

    public void dumpDebugInfo(String message) {
        debugInfo(message);
        debugInfo(" - module:" + modules[currentModule].getName());
    }

    @Override
    public void debugInfo(String message) {
        getCowNet().debugInfo(message);
    }
}
