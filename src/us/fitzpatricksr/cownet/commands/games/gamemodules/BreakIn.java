package us.fitzpatricksr.cownet.commands.games.gamemodules;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import us.fitzpatricksr.cownet.CowNetMod;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.framework.GameContext;
import us.fitzpatricksr.cownet.commands.games.framework.GameModule;
import us.fitzpatricksr.cownet.commands.games.utils.SpawnAndLoungeUtils;
import us.fitzpatricksr.cownet.commands.games.utils.Team;

import java.util.Random;

/**
 * Two teams.
 * Each team has a "bell" at their spawn.
 * The other player has to get to the other teams spawn and ring the bell to gain a point.
 * The team at the end with the most points wins.
 * <p/>
 * BreakIn-lounge-blue
 * BreakIn-lounge-read
 * BreakIn-spawn-blue
 * BreakIn-spawn-red
 */

public class BreakIn implements Listener, GameModule {
    private Random rand = new Random();
    private GameContext context;
    private SpawnAndLoungeUtils spawnUtils;

    @CowNetThingy.Setting
    private int breakInSpawnJiggle = 5;
    @CowNetThingy.Setting
    private int breakInLoungeDuration = 10; // 30 second loung
    @CowNetThingy.Setting
    private int breakInGameDuration = 60 * 3; // 3 minutes max game length
    @CowNetThingy.Setting
    private int breakInMinPlayers = 2; // 3 minutes max game length

    @Override
    public String getName() {
        return "BreakIn";
    }

    @Override
    public int getLoungeDuration() {
        return breakInLoungeDuration;
    }

    @Override
    public int getGameDuration() {
        return breakInGameDuration;
    }

    @Override
    public int getMinPlayers() {
        return breakInMinPlayers;
    }

    @Override
    public boolean isTeamGame() {
        return true;
    }

    @Override
    public void startup(GameContext context) {
        this.context = context;
        CowNetMod plugin = context.getCowNet().getPlugin();
        spawnUtils = new SpawnAndLoungeUtils(plugin, getName(), breakInSpawnJiggle);
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
        context.broadcastToAllPlayers("A game of Break In is gathering.");
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
        context.broadcastToAllPlayers("Break In has begun.");
        for (String player : context.getPlayers()) {
            playerEnteredGame(player);
        }
        buildBases();
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
        context.broadcastToAllPlayers("Break In has eneded.");
    }

    // --------------------------------------------------------------
    // ---- Event handlers

    @EventHandler(ignoreCancelled = true)
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

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!context.isLounging()) return;
        Location loc = event.getBlock().getLocation();
        Player player = event.getPlayer();
        Team team = context.getPlayerTeam(player.getName());
        Team base = getLocationTeam(loc);

        if (base != null) {
            if (team != base) {
                smokeScreenEffect(loc);
                // someone broke the other base.  Give them points and send them back to spawn
                context.addWin(player.getName());
            } else {
                // some dummy broke his own base.
                loc.getWorld().strikeLightning(player.getLocation());
                event.setCancelled(true);
                context.addLoss(player.getName());
            }
        }
    }


    private boolean playerIsInGame(String playerName) {
        return context.getPlayers().contains(playerName);
    }

    // --------------------------------------------------------------
    // ---- Event handlers

    private void buildBases() {
        buildBase(Team.BLUE);
        buildBase(Team.RED);
    }

    private void buildBase(Team team) {
        getBaseLocation(team).getBlock().setType(team.getMaterial());
    }

    private Team getLocationTeam(Location loc) {
        if (loc.equals(getBaseLocation(Team.RED))) {
            return Team.RED;
        } else if (loc.equals(getBaseLocation(Team.BLUE))) {
            return Team.BLUE;
        } else {
            return null;
        }
    }

    private Location getBaseLocation(Team team) {
        Location baseLocation = spawnUtils.getTeamSpawnPoint(team.toString());
        baseLocation.add(0, 1, 0);
        return baseLocation;
    }

    private void smokeScreenEffect(Location location) {
        for (int i = 0; i < 10; i++) {
            location.getWorld().playEffect(location, Effect.SMOKE, rand.nextInt(9));
        }
    }
}
