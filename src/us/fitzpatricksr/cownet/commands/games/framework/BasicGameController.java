package us.fitzpatricksr.cownet.commands.games.framework;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.GameStatsFile;
import us.fitzpatricksr.cownet.commands.games.utils.StatusBoard;
import us.fitzpatricksr.cownet.commands.games.utils.Team;

import java.io.IOException;
import java.util.*;

/*
Gather players until we have at least one game that can be started, then start it.
*/
public class BasicGameController implements GameContext {
    private Random rand = new Random();
    private CowNetThingy mod;
    private boolean isLounging;
    private HashMap<String, Team> players;
    private GameModule currentModule;
    private GameModule[] modules;
    private int gameTimerTaskId;
    private StatusBoard status;
    private GameStatsFile statsFile;
    private HashMap<String, Integer> wins;

    public BasicGameController(CowNetThingy mod, GameModule[] modules) {
        this.mod = mod;
        this.isLounging = true;   // lounging
        this.players = new HashMap<String, Team>();
        this.modules = modules;
        this.currentModule = null;
        this.gameTimerTaskId = 0;
        this.status = new StatusBoard(this);
    }

    public void startup(GameStatsFile statsFile) {
        this.statsFile = statsFile;
        currentModule = new GatheringModule();
        status.enable();
    }

    public void shutdown() {
        GameModule module = this.getCurrentModule();
        stopTimerTask();
        status.disable();
        endLounging();
        module.gameEnded();
        setLounging(true);
        module.shutdown();
    }

    //---------------------------------------------------------------------
    // GameContext interface

    @Override
    public CowNetThingy getCowNet() {
        return mod;
    }

    @Override
    public String getGameName() {
        return getCurrentModule().getName();
    }

    @Override
    public boolean isLounging() {
        return isLounging;
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
    public void endLounging() {
        dumpDebugInfo("endLounging");

        if (!isLounging) {
            dumpDebugInfo("  - Hmm....not lounging");
            // lounging already over, so nothing to do
        } else {
            stopTimerTask();
            GameModule module = getCurrentModule();
            module.loungeEnded();
            if (players.size() < module.getMinPlayers()) {
                // leave it lounging. go to the next game and lounge again.
                broadcastToAllPlayers("Not enough players for " + getCurrentModule().getName() + ".  Game canceled.");
                selectNewCurrentGameModule();
            } else {
                setLounging(false);
                clearScores();  // clear scores from previous game.
                balanceTeams();
                getCurrentModule().gameStarted();
            }
            startTimerTask();
        }
    }

    @Override
    public void endGame() {
        dumpDebugInfo("endGame");
        stopTimerTask();
        if (isLounging) {
            // if we're lounging, clean it up before we move to next game
            // skip gameStarted/gameEnded phases
            getCurrentModule().loungeEnded();
            dumpDebugInfo("  - was lounging, so just moving to next game.");
        } else {
            getCurrentModule().gameEnded();
            awardPoints();
            setLounging(true);
        }
        selectNewCurrentGameModule();
        getCurrentModule().loungeStarted();
        startTimerTask();   //start lounge timer
    }

    private void selectNewCurrentGameModule() {
        getCurrentModule().shutdown();
        currentModule = null; //force to select a new module
        getCurrentModule().startup(new DebugGameContext(this, getCurrentModule().getName()));
    }

    private GameModule getCurrentModule() {
        if (currentModule == null) {
            // nothing is running right now, select a module.
            int base = rand.nextInt(modules.length);
            for (int i = 0; i < modules.length; i++) {
                int ndx = (base + i) % modules.length;
                currentModule = modules[ndx];
                if (players.size() >= modules[ndx].getMinPlayers()) {
                    return currentModule;
                }
            }
            // we didn't find a runnable module.  So, return the GatheringModule
            currentModule = new GatheringModule();
            return currentModule;
        } else {
            return currentModule;
        }
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
        JavaPlugin plugin = getCowNet().getPlugin();
        if (isLounging) {
            int maxLoungeDuration = getCurrentModule().getLoungeDuration() * 20;
            debugInfo("startTimerTask: " + maxLoungeDuration);
            if (maxLoungeDuration > 0) {
                gameTimerTaskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        endLounging();
                    }
                }, maxLoungeDuration);
            } else {
                endLounging();
            }
        } else {
            int maxGameDuration = getCurrentModule().getLoungeDuration() * 20;
            debugInfo("startTimerTask: " + maxGameDuration);
            if (maxGameDuration > 0) {
                gameTimerTaskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        endGame();
                    }
                }, maxGameDuration);
            } else {
                endGame();
            }
        }

    }

    //---------------------------------------------------------------------
    // status display

    @Override
    public void broadcastToAllPlayers(String message) {
        status.sendMessageToAll(message);
    }

    @Override
    public void sendToPlayer(String playerName, String message) {
        status.sendMessageToPlayer(playerName, message);
    }

    public void broadcastChat(String playerName, String message) {
        Team team = getPlayerTeam(playerName);
        status.sendMessageToTeam(getPlayerTeam(playerName),
                team.getChatColor() + playerName + ": " + ChatColor.RESET + message);
    }

    //---------------------------------------------------------------------
    // Team and player management

    @Override
    public Collection<String> getPlayers() {
        return players.keySet();
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
            // if this is the first player, start the timer
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
        if (players.containsKey(playerName)) {
            dumpDebugInfo("removePlayer(" + playerName + ")");
            players.remove(playerName);
            playerLeft(playerName);
        }
    }

    private void playerEntered(String playerName) {
        if (isLounging()) {
            dumpDebugInfo("playerEntered(" + playerName + ") lounge");
            getCurrentModule().playerEnteredLounge(playerName);
        } else {
            dumpDebugInfo("playerEntered(" + playerName + ") game");
            getCurrentModule().playerEnteredGame(playerName);
        }

        // add a hat
        fixTeamSuits();
    }

    private void playerLeft(String playerName) {
        if (isLounging()) {
            dumpDebugInfo("playerLeft(" + playerName + ") lounge");
            getCurrentModule().playerLeftLounge(playerName);
        } else {
            dumpDebugInfo("playerLeft(" + playerName + ") game");
            getCurrentModule().playerLeftGame(playerName);
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
            status.updateForAll();
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
        status.updateForAll();
    }

    private void fixTeamSuits() {
        if (getCurrentModule().isTeamGame()) {
            for (String playerName : players.keySet()) {
                // someone is now carrying the flag
                Player player = getPlayer(playerName);
                PlayerInventory inv = player.getInventory();
                ItemStack helmet = inv.getArmorContents()[2]; // getHelmet() wouldn't give me non-helmet blocks
                // if they already have a helmet, remove it and put it in their inventory
                if (helmet != null && helmet.getType() != Material.AIR) {
                    inv.addItem(helmet);
                }
                Team team = getPlayerTeam(player.getName());
                inv.setChestplate(team.getWool());
                player.setDisplayName(
                        (team == Team.BLUE ? ChatColor.BLUE : ChatColor.RED) + playerName);
            }
        }
    }

    //---------------------------------------------------------------------
    // Scoring

    private void clearScores() {
        wins = new HashMap<String, Integer>();
    }

    @Override
    public int getScore(String playerName) {
        return wins.containsKey(playerName) ? wins.get(playerName) : 0;
    }

    @Override
    public void addWin(String playerName) {
        wins.put(playerName, getScore(playerName) + 1);
        status.updateFor(playerName);
    }

    @Override
    public void addLoss(String playerName) {
        wins.put(playerName, getScore(playerName) - 1);
        status.updateFor(playerName);
    }

    private void awardPoints() {
        Team winningTeam = status.getWinningTeam();
        String winningPlayer = status.getWinningPlayer();
        statsFile.accumulate(winningPlayer, "wins", 1);
        for (String playerName : getPlayersOnTeam(winningTeam)) {
            statsFile.accumulate(playerName, "teamWins", 1);
        }

        broadcastToAllPlayers(winningPlayer + " was the winning player.");
        broadcastToAllPlayers(winningTeam + " was the winning team.");
        try {
            statsFile.saveConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //---------------------------------------------------------------------
    // utilities

    public void dumpDebugInfo(String message) {
        debugInfo(message);
        debugInfo(" - module:" + getCurrentModule().getName());
    }

    @Override
    public void debugInfo(String message) {
        getCowNet().debugInfo(message);
    }

    //---------------------------------------------------------------------
    // module used to gather players for the games
    //
    // this module just sits until someone enters the game.
    // it then exits, forcing the controller to select
    // a game to be run.  This module will run when there are
    // zero players and is the default module if no
    // other modules can be run.  This allows the rest
    // of the controller to just treat this like a normal module
    // that can ignore the gather player phase.

    public class GatheringModule implements GameModule {
        private GameContext context;

        public GatheringModule() {
        }

        @Override
        public String getName() {
            return "Waiting...";
        }

        @Override
        public int getLoungeDuration() {
            // we'll sit and lounge until we find a game that we can run
            // we lounge so previous scores are still available.
            return Integer.MAX_VALUE;
        }

        @Override
        public int getGameDuration() {
            return 0;
        }

        @Override
        public int getMinPlayers() {
            return 0;
        }

        @Override
        public boolean isTeamGame() {
            return true;
        }

        @Override
        public void startup(GameContext context) {
            this.context = context;
        }

        @Override
        public void shutdown() {
        }

        @Override
        public void loungeStarted() {
            // If this got called, that means that there weren't any schedulable modules.
            // No point in running the timer if we're not gathering players.
            stopTimerTask();
        }

        @Override
        public void playerEnteredLounge(String playerName) {
            // It looks like we have at least 1 player, so force controller
            // to pick a module and start a new game.
            context.endGame();
        }

        @Override
        public void playerLeftLounge(String playerName) {
        }

        @Override
        public void loungeEnded() {
        }

        @Override
        public void gameStarted() {
        }

        @Override
        public void playerEnteredGame(String playerName) {
        }

        @Override
        public void playerLeftGame(String playerName) {
        }

        @Override
        public void gameEnded() {
        }
    }

}
