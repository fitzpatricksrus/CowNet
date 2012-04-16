package us.fitzpatricksr.cownet;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetThingy;

import java.util.Random;

/*
    enter "lobby" to invite people
    when enough people have entered the lobby announce 10 seconds
    teleported and games begin
    random items around teleport
    no reg health
    goes until all dead
    people not in games can watch and donate 1 item per minute
    cannon when people die
    last person to leave wins
    persistant stats
    if you join the game late, you get health = the weakest player

    games have 3 states
       unstarted - nothing in progress
       gathering - someone is waiting for the games to start
       started - the games are underway
 */
public class HungerGames extends CowNetThingy implements Listener {
    private String gameWorldName = "HungerGames";
    private int safeDistance = 10;
    private int maxDistance = 1000;
    private MultiverseCore mvPlugin;
    private boolean allowFly = false;
    private boolean allowXRay = false;
    private long timeToGather = 1 * 60 * 1000; // 1 minutes
    private long timeBetweenGifts = 1 * 60 * 1000;
    private int minTributes = 3;

    public HungerGames(JavaPlugin plugin, String permissionRoot, String trigger) {
        super(plugin, permissionRoot, trigger);
        if (isEnabled()) {
            reload();
            PluginManager pm = plugin.getServer().getPluginManager();
            pm.registerEvents(this, plugin);
        }
    }

    @Override
    protected void reload() {
        if (mvPlugin != null) mvPlugin.decrementPluginCount();
        gameWorldName = getConfigString("worldname", gameWorldName);
        safeDistance = getConfigInt("safedistance", safeDistance);
        maxDistance = getConfigInt("maxDistance", maxDistance);
        allowFly = getConfigBoolean("allowFly", allowFly);
        allowXRay = getConfigBoolean("allowXRay", allowXRay);
        timeToGather = getConfigLong("timeToGather", timeToGather);
        timeBetweenGifts = getConfigLong("timeBetweenGifts", timeBetweenGifts);
        minTributes = getConfigInt("minTributes", minTributes);
        mvPlugin = (MultiverseCore) getPlugin().getServer().getPluginManager().getPlugin("Multiverse-Core");
        if (mvPlugin == null) {
            logInfo("Could not find Multiverse-Core plugin.  Disabling self");
            disable();
        } else {
            mvPlugin.incrementPluginCount();
        }
        logInfo("gameWorldName:" + gameWorldName);
        logInfo("safeDistance:" + safeDistance);
        logInfo("maxDistance:" + maxDistance);
        logInfo("allowFly:" + allowFly);
        logInfo("timeToGather:" + timeToGather);
        logInfo("timeBetweenGifts:" + timeBetweenGifts);
        logInfo("minTributes:" + minTributes);
    }

    @EventHandler
    protected void handlePluginDisabled(PluginDisableEvent event) {
        if (event.getPlugin() == getPlugin()) {
            mvPlugin.decrementPluginCount();
        }
    }

    @Override
    protected String getHelpString(CommandSender player) {
        return "usage: /hungergames or /hg";
    }

    @Override
    protected boolean handleCommand(CommandSender sender, Command cmd, String[] args) {
        // subcommands
        //  quit
        //  info
        //  donate <player> <item>
        //  --- empty takes you to the hardcore world
        if (args.length == 1) {
            if ("info".equalsIgnoreCase(args[0])
                    || "list".equalsIgnoreCase(args[0])
                    || "stats".equalsIgnoreCase(args[0])) {
                return goInfo(sender);
            } else if ("quit".equalsIgnoreCase(args[0])) {
                return goQuit(sender);
            }
        } else if (args.length == 2) {
            if ("donate".equalsIgnoreCase(args[0])) {
                return goDonate(sender, args[1]);
            }
        }
        return super.handleCommand(sender, cmd, args);
    }


    // ---- Event handlers

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        handleWorldChange(event.getPlayer(), event.getFrom(), event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        handleWorldChange(event.getPlayer(), event.getTo().getWorld(), event.getFrom().getWorld());
    }

    private void handleWorldChange(Player p, World fromWorld, World toWorld) {
        if (isGameWorld(toWorld)) {
            //unstarted - start the reaping timer and make announcements
            //gathering - player is added/removed from the games
            //started - you're a sponsor only
        } else if (isGameWorld(toWorld)) {
            // if it's a player who left this world, they quit, so there may be a winner
            //   restore inventory if needed
        } else {
            // to and from non-game worlds, we don't care
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMoved(PlayerMoveEvent event) {
        // if sponsor, you can't do anything
        Player player = event.getPlayer();
        if (!playerIsInGame(player)) return;
        // if in the game, keep within game bounds.
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // if sponsor, you can't do anything
        Player player = event.getPlayer();
        if (!playerIsInGame(player)) {
            event.setCancelled(true);
            player.sendMessage("You can't interfere with the game directly.");
        } else {

        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // if sponsor, you can't do anything
        Player player = event.getPlayer();
        if (!playerIsInGame(player)) {
            event.setCancelled(true);
            player.sendMessage("YYou can't interfere with the game directly.");
        } else {

        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // if sponsor, you can't do anything
        Player player = event.getPlayer();
        if (!playerIsInGame(player)) {
            event.setCancelled(true);
            player.sendMessage("You can't interfere with the game directly.");
        } else {

        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!playerIsInGame(player)) {
            event.setCancelled(true);
            player.sendMessage("You can't interfere with the game directly.");
        } else {
            //
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!playerIsInGame(player)) {
            if (playerIsSponsor(player)) {
                if (System.currentTimeMillis() - timeOfLastSponsorship(player) <= timeBetweenGifts) {
                    event.setCancelled(true);
                    player.sendMessage("You can only give gifts once every " + timeBetweenGifts / 1000 / 60 + " minutes.");
                }
            } else {
                event.setCancelled(true);
                player.sendMessage("As a dead player, you can't interfere with the game directly.");
            }
        } else {
            // players who are in the game can drop whatever they want
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (playerIsInGame(player)) {
            event.setCancelled(true);
            player.sendMessage("You can't change the gamemode of players in the game.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        // if sponsor, you can't do anything
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(EntityDeathEvent event) {
        // mark player a non-sponsor and fire the cannon
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        // if sponsor, they can't be targeted
        Entity entity = event.getTarget();
        if (!isGameWorld(entity.getWorld())) return;
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!playerIsInGame(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleEntityCollision(VehicleEntityCollisionEvent event) {
        // if sponsor, you can't do anything
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // tell them about the games
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // if in the games, there may be a winner
        // restore inventory
    }

    // -- utility methods
    private long timeOfLastSponsorship(Player player) {
        return 0;
    }

    private boolean playerIsInGame(Player p) {
        return false;
    }

    private boolean playerIsSponsor(Player p) {
        return false;
    }

    private boolean playerIsOutOfGame(Player p) {
        return false;
    }

    private boolean isGameWorld(World w) {
        return isGameWorld(w.getName());
    }

    private boolean isGameWorld(String worldName) {
        return gameWorldName.equalsIgnoreCase(worldName);
    }

    private boolean regenIsAlreadyScheduled = false;

    private boolean generateNewWorld(String worldName) {
        logInfo("generateNewWorld " + worldName);
        if (regenIsAlreadyScheduled) {
            logInfo("generateNewWorld " + worldName + " aborted.  Regen already in progress.");
            return true;
        }
        try {
            regenIsAlreadyScheduled = true;
            MVWorldManager mgr = mvPlugin.getMVWorldManager();
            if (mgr.isMVWorld(worldName)) {
                mgr.removePlayersFromWorld(worldName);
                if (!mgr.deleteWorld(worldName)) {
                    logInfo("Agh!  Can't regen " + worldName);
                    return false;
                }
            }
            //TODO hey jf - can we create this world in another thread?
            if (mgr.addWorld(worldName,
                    World.Environment.NORMAL,
                    "" + (new Random().nextLong()),
                    WorldType.NORMAL,
                    true,
                    null,
                    true)) {
                config.resetWorldState();
                World w = mgr.getMVWorld(worldName).getCBWorld();
                w.setDifficulty(difficulty);
                w.setTicksPerMonsterSpawns((int) (w.getTicksPerMonsterSpawns() * monsterBoost) + 1);
                logInfo(worldName + " has been regenerated.");
                return true;
            } else {
                logInfo("Oh No's! " + worldName + " don wurk.");
                return false;
            }
        } finally {
            regenIsAlreadyScheduled = false;
        }
    }

}


