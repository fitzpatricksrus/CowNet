package us.fitzpatricksr.cownet.commands.games.framework;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import us.fitzpatricksr.cownet.CowNetMod;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.utils.SpawnAndLoungeUtils;

import java.util.Random;

/**
 * Basic game that does nothing but more players to spawn etc.
 * <p/>
 * The main behavior this provides is managing spawn and lounge
 * locations.  It also provides some convenient status messages
 * to players.
 */

public abstract class BasicGameModule implements Listener, GameModule {
    protected final Random rand = new Random();
    protected GameContext context;
    protected SpawnAndLoungeUtils spawnUtils;

    @CowNetThingy.Setting
    private int genericSpawnJiggle = 5;

    /*
    The following must be implemented by the subclass

    public String getName()
    public int getLoungeDuration()
    public int getGameDuration()
    public int getMinPlayers()
    public boolean isTeamGame()
   */

    @Override
    public void startup(GameContext context) {
        this.context = context;
        CowNetMod plugin = context.getCowNet().getPlugin();
        spawnUtils = new SpawnAndLoungeUtils(plugin, getName(), genericSpawnJiggle);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void shutdown() {
        HandlerList.unregisterAll(this);
        this.context = null;
        spawnUtils = null;
    }

    @Override
    public void loungeStarted() {
        context.broadcastToAllPlayers("A game of " + getName() + " is gathering.");
        for (String playerName : context.getPlayers()) {
            playerEnteredLounge(playerName);
        }
    }

    @Override
    public void playerEnteredLounge(String playerName) {
        Location lounge = spawnUtils.getPlayerLoungePoint();
        if (lounge != null) {
            Player player = context.getPlayer(playerName);
            player.teleport(lounge);
        } else {
            context.debugInfo("Could not find lounge");
        }
        context.broadcastToAllPlayers(playerName + " is on the " + context.getPlayerTeam(playerName) + " team.");
    }

    @Override
    public void playerLeftLounge(String playerName) {
        context.broadcastToAllPlayers(playerName + " left the game.");
    }

    @Override
    public void loungeEnded() {
    }

    @Override
    public void gameStarted() {
        context.broadcastToAllPlayers(getName() + " has begun.");
        for (String player : context.getPlayers()) {
            playerEnteredGame(player);
        }
    }

    @Override
    public void playerEnteredGame(String playerName) {
        Location spawn = spawnUtils.getPlayerSpawnPoint();
        if (spawn != null) {
            Player player = context.getPlayer(playerName);
            player.teleport(spawn);
        } else {
            context.debugInfo("Could not find spawn");
        }
        context.broadcastToAllPlayers(playerName + " is on the " + context.getPlayerTeam(playerName) + " team.");
    }

    @Override
    public void playerLeftGame(String playerName) {
        context.broadcastToAllPlayers(playerName + " has left the game.");
    }

    @Override
    public void gameEnded() {
        context.broadcastToAllPlayers(getName() + " has ended.");
    }

    // --------------------------------------------------------------
    // ---- Event handlers

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // register a loss and teleport back to spawn point
        Player player = event.getPlayer();
        String playerName = player.getName();
        if (playerIsInGame(playerName)) {
            // Just teleport the person back to spawn here.
            // losses and announcements are done when the player is killed.
            Location loc = (context.isLounging()) ? spawnUtils.getPlayerLoungePoint() : spawnUtils.getPlayerSpawnPoint();
            if (loc != null) {
                // hey jf - you need to jiggle this a bit or everyone will be on top of each other
                // have the player respawn in the game spawn
                event.setRespawnLocation(loc);
            }
        }
    }

    protected boolean playerIsInGame(String playerName) {
        return context.getPlayers().contains(playerName);
    }
}
