package us.fitzpatricksr.cownet;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import org.bukkit.Difficulty;
import org.bukkit.Location;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetThingy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/*
    the game world is off limits until a game starts
    the first person to enter starts the gathering
    gathering continues for a specified period of time
    all players who entered after that time are teleported in and their inventory cleared.
    after the last player dies the world is regenerated.

    games have 3 states
       ended - nothing in progress
       gathering - someone is waiting for the games to start
       inprogress - the games are underway

    for each person
       enum { player, deadPlayer, sponsor } gameState
       boolean lastTribute
 */
public class HungerGames extends CowNetThingy implements Listener {
    private static final int GAME_WATCHER_FREQUENCY = 20 * 1; // 30 seconds

    private static enum GamePhase {
        ENDED,
        GATHERING,
        IN_PROGRESS
    }

    private static enum PlayerState {
        TRIBUTE,
        DEAD,
        SPONSOR
    }

    private String gameWorldName = "HungerGames";
    private int arenaSize = 1000;
    private MultiverseCore mvPlugin;
    private boolean allowFly = false;
    private boolean allowXRay = false;
    private long timeToGather = 1 * 60 * 1000; // 1 minutes
    private long timeBetweenGifts = 1 * 60 * 1000;
    private int minTributes = 3;

    private long firstPlayerJoinTime = 0;
    private HashMap<Player, PlayerInfo> gameInfo = new HashMap<Player, PlayerInfo>();

    public HungerGames(JavaPlugin plugin, String permissionRoot, String trigger) {
        super(plugin, permissionRoot, trigger);
        if (isEnabled()) {
            reload();
            PluginManager pm = plugin.getServer().getPluginManager();
            pm.registerEvents(this, plugin);
            getPlugin().getServer().getScheduler().scheduleAsyncRepeatingTask(
                    getPlugin(),
                    new Runnable() {
                        public void run() {
                            goGameWatcher();
                        }
                    },
                    GAME_WATCHER_FREQUENCY,
                    GAME_WATCHER_FREQUENCY);
        }
    }

    @Override
    protected void reload() {
        if (mvPlugin != null) mvPlugin.decrementPluginCount();
        gameWorldName = getConfigString("worldname", gameWorldName);
        arenaSize = getConfigInt("arenaSize", arenaSize);
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
        logInfo("arenaSize:" + arenaSize);
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
        return "usage: /hungergames or /hg   info | quit | donate";
    }

    @Override
    protected boolean handleCommand(CommandSender sender, Command cmd, String[] args) {
        // subcommands
        //  quit
        //  info
        //  donate <player> <item>
        //  --- empty takes you to the hardcore world
        if (args.length == 1 && (
                "info".equalsIgnoreCase(args[0])
                        || "list".equalsIgnoreCase(args[0])
                        || "stats".equalsIgnoreCase(args[0]))) {
            return goInfo(sender);
        }
        return false;
    }

    protected boolean handleCommand(Player sender, Command cmd, String[] args) {
        if (args.length == 1) {
            if ("quit".equalsIgnoreCase(args[0])) {
                return goQuit(sender);
            }
        } else if (args.length == 2) {
            if ("donate".equalsIgnoreCase(args[0])) {
                return goDonate(sender, args[0], args[1]);
            }
        }
        return false;
    }

    private boolean goInfo(CommandSender sender) {
        if (!hasPermissions(sender, "info")) {
            sender.sendMessage("You don't have permission.");
        } else {
            sender.sendMessage("Game stats:");
            for (Player player : getPlugin().getServer().getOnlinePlayers()) {
                PlayerInfo info = getPlayerInfo(player);
                sender.sendMessage("  " + info);
            }
        }
        return true;
    }

    private boolean goQuit(Player player) {
        if (playerIsInGame(player)) {
            removePlayerFromGame(player);
        }
        return true;
    }

    private boolean goDonate(Player sender, String tributeName, String itemName) {
        //todo hey jf - code this routine.
        return true;
    }


    // ---- Event handlers

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // do we need this or is it also a teleport event
//        handleWorldChange(event.getPlayer(), event.getFrom(), event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // nobody can go to the game world unless a game is underway
        if (isGameWorld(event.getTo().getWorld()) && (getGameState() != GamePhase.IN_PROGRESS)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("You can't teleport to the arena.  You must become a tribute using /hg");
        } else if (playerIsInGame(event.getPlayer())) {
            // tributes can't be teleported.  Sorry.
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMoved(PlayerMoveEvent event) {
        if (playerIsInGame(event.getPlayer()) && !isInArena(event.getTo())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (getGameState() != GamePhase.IN_PROGRESS) return;
        // if sponsor, you can't do anything
        Player player = event.getPlayer();
        if (!playerIsInGame(player) && isInArena(player.getLocation())) {
            event.setCancelled(true);
            player.sendMessage("You can't interfere with the game directly.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (getGameState() != GamePhase.IN_PROGRESS) return;
        // if sponsor, you can't do anything
        Player player = event.getPlayer();
        if (!playerIsInGame(player) && isInArena(player.getLocation())) {
            event.setCancelled(true);
            player.sendMessage("You can't interfere with the game directly.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (getGameState() != GamePhase.IN_PROGRESS) return;
        // if sponsor, you can't do anything
        Player player = event.getPlayer();
        if (!playerIsInGame(player) && isInArena(player.getLocation())) {
            event.setCancelled(true);
            player.sendMessage("You can't interfere with the game directly.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickupItem(PlayerPickupItemEvent event) {
        if (getGameState() != GamePhase.IN_PROGRESS) return;
        Player player = event.getPlayer();
        if (!playerIsInGame(player) && isInArena(player.getLocation())) {
            event.setCancelled(true);
            player.sendMessage("You can't interfere with the game directly.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (getGameState() != GamePhase.IN_PROGRESS) return;
        Player player = event.getPlayer();
        if (playerIsOutOfGame(player)) {
            event.setCancelled(true);
            player.sendMessage("You are out of the game and can't sponsor other players.");
        } else if (playerIsSponsor(player)) {
            if (!dropGiftToTribute(player)) {
                event.setCancelled(true);
                player.sendMessage("You can only give gifts once every " + timeBetweenGifts / 1000 / 60 + " minutes.");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (getGameState() != GamePhase.IN_PROGRESS) return;
        Player player = event.getPlayer();
        if (playerIsInGame(player)) {
            event.setCancelled(true);
            player.sendMessage("You can't change the gamemode of players in the game.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (getGameState() != GamePhase.IN_PROGRESS) return;
        // if sponsor, you can't do anything
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            // make sure sponsors can't be damaged
            Player player = (Player) entity;
            if (!playerIsInGame(player)) {
                event.setCancelled(true);
            }
        } else if (event instanceof EntityDamageByEntityEvent) {
            // make user non-players can't damage things in the arena
            final EntityDamageByEntityEvent target = (EntityDamageByEntityEvent) event;
            final Entity damager = target.getDamager();
            if (damager instanceof Player) {
                Player player = ((Player) damager);
                if (!playerIsInGame(player) && isInArena(player.getLocation())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (getGameState() != GamePhase.IN_PROGRESS) return;
        if (playerIsInGame(event.getEntity())) {
            removePlayerFromGame(event.getEntity());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (getGameState() != GamePhase.IN_PROGRESS) return;
        // if sponsor, they can't be targeted
        if (event.getTarget() instanceof Player) {
            Player player = (Player) event.getTarget();
            if (isInArena(player) && !playerIsInGame(player)) {
                event.setCancelled(true);
            }
        }
    }

/*    @EventHandler(ignoreCancelled = true)
    public void onVehicleEntityCollision(VehicleEntityCollisionEvent event) {
        if (getGameState() != GamePhase.IN_PROGRESS) return;
    } */

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (getGameState() != GamePhase.IN_PROGRESS) return;
        goInfo(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (getGameState() != GamePhase.IN_PROGRESS) return;
        if (playerIsInGame(event.getPlayer())) {
            removePlayerFromGame(event.getPlayer());
        }
    }

    // -- utility methods
    private boolean dropGiftToTribute(Player player) {
        return getPlayerInfo(player).dropGiftToTribute();
    }

    private boolean playerIsInGame(Player p) {
        return getPlayerInfo(p).isInGame();
    }

    private boolean playerIsSponsor(Player p) {
        return getPlayerInfo(p).isSponsor();
    }

    private boolean playerIsOutOfGame(Player p) {
        return getPlayerInfo(p).isOutOfGame();
    }

    private boolean isGameWorld(World w) {
        return isGameWorld(w.getName());
    }

    private boolean isGameWorld(String worldName) {
        return gameWorldName.equalsIgnoreCase(worldName);
    }

    private boolean isInArena(Player player) {
        return isInArena(player.getLocation());
    }

    private boolean isInArena(Location loc) {
        World w = loc.getWorld();
        if (isGameWorld(w)) {
            Location spawnLoc = w.getSpawnLocation();
            double distance = spawnLoc.distance(loc);
            return distance < arenaSize;
        } else {
            return false;
        }
    }

    // -- player list support
    private List<PlayerInfo> getPlayersInGame() {
        LinkedList<PlayerInfo> result = new LinkedList<PlayerInfo>();
        for (PlayerInfo p : gameInfo.values()) {
            if (p.isInGame()) {
                result.add(p);
            }
        }
        return result;
    }

    private void removePlayerFromGame(Player player) {
        if (playerIsInGame(player)) {
            player.getWorld().strikeLightningEffect(player.getLocation());
            player.getWorld().strikeLightningEffect(player.getLocation());
            player.setHealth(0);
            getPlayerInfo(player).setIsOutOfGame();
            List<PlayerInfo> livePlayers = getPlayersInGame();
            if (livePlayers.size() < 2) {
                PlayerInfo winner = livePlayers.get(0);
                for (Player p : getPlugin().getServer().getOnlinePlayers()) {
                    p.sendMessage("" + winner.getPlayer().getDisplayName() + " is the winner of the games.");
                }
                gameInfo.clear();
            }
        }
    }

    private PlayerInfo getPlayerInfo(Player p) {
        PlayerInfo info = gameInfo.get(p);
        if (info == null) {
            info = new PlayerInfo(p, PlayerState.SPONSOR);
            gameInfo.put(p, info);
        }
        return info;
    }

    private GamePhase getGameState() {
        List<PlayerInfo> livePlayers = getPlayersInGame();
        if (livePlayers.size() == 0) {
            firstPlayerJoinTime = 0;
            return GamePhase.ENDED;
        } else if (System.currentTimeMillis() - firstPlayerJoinTime > timeToGather && livePlayers.size() > 2) {
            return GamePhase.IN_PROGRESS;
        } else {
            return GamePhase.GATHERING;
        }
    }

    private void goGameWatcher() {
        //todo hey jf - code this routine
        GamePhase phase = getGameState();
        if (phase == GamePhase.IN_PROGRESS) {
            //if there are people that are not in the arena, teleport them there
        } else if (phase == GamePhase.GATHERING) {
            //if we don't have enough people, say so and extend gathering time if needed
            //if less than 10 seconds, do countdown.
        }
        // run every second
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
                World w = mgr.getMVWorld(worldName).getCBWorld();
                w.setDifficulty(Difficulty.HARD);
//                w.setTicksPerMonsterSpawns((int) (w.getTicksPerMonsterSpawns() * monsterBoost) + 1);
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

    private class PlayerInfo {
        private Player player;
        private PlayerState state;
        private long lastGiftTime = 0;

        public PlayerInfo(Player player, PlayerState state) {
            this.player = player;
            this.state = state;
        }

        public Player getPlayer() {
            return player;
        }

        public boolean dropGiftToTribute() {
            if (System.currentTimeMillis() - lastGiftTime <= timeBetweenGifts) {
                return false;
            } else {
                lastGiftTime = System.currentTimeMillis();
                return true;
            }
        }

        public boolean isInGame() {
            return state == PlayerState.TRIBUTE;
        }

        public void setIsInGame() {
            state = PlayerState.TRIBUTE;
        }

        public boolean isSponsor() {
            return state == PlayerState.SPONSOR;
        }

        public void setIsSponsor() {
            state = PlayerState.SPONSOR;
        }

        public boolean isOutOfGame() {
            return state == PlayerState.DEAD;
        }

        public void setIsOutOfGame() {
            state = PlayerState.DEAD;
        }

        public String toString() {
            //todo hey jf - do something here.  this is used for "info" command
            return null;
        }
    }
}


