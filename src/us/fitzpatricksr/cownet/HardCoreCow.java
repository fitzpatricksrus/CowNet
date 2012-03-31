package us.fitzpatricksr.cownet;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVDestination;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.SafeTTeleporter;
import com.onarandombox.MultiverseCore.destination.DestinationFactory;
import com.onarandombox.MultiverseCore.enums.TeleportResult;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetConfig;
import us.fitzpatricksr.cownet.utils.CowNetThingy;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/*
    The world is generated.  Player can visit and look all they want until they touch
    something, then they are in the game.  As soon as the world has at least one player
    in the game it needs to maintain at least one live player.  If at any time the number
    of live players goes to 0, the world regenerates.

    Timeouts.  Urg.
    If player is dead past timeout, remove them from dead queue.  If they queues are now
    empty, regen the world.  So, if they're a ghost and they try to do something, they
    will be removed from dead because of the timeout and re-added to because of the interaction.
    If they were just dead for a long time, we just remove them.  It's passive and only happens
    when the config is touched.  It's a reaper process, not something that's active.
 */
public class HardCoreCow extends CowNetThingy implements Listener {
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int REAPER_FREQUENCY = 20 * 30; // 30 seconds
    private HardCoreState config;
    private String worldName = "HardCoreCow";
    private int safeDistance = 10;
    private MultiverseCore mvPlugin;
    private static long liveTimeout = 7 * 24 * 60 * 60;  //live players keep things going for 7 days.
    private static long deathDuration = 60;  //default time before you are removed from game in seconds.
    private static double timeOutGrowth = 2.0; //the rate at which timeout increases.
    private Difficulty difficulty = Difficulty.HARD;
    private double monsterBoost = 1.0d;

    public HardCoreCow(JavaPlugin plugin, String permissionRoot, String trigger) {
        super(plugin, permissionRoot, trigger);
        if (isEnabled()) {
            reload();
            PluginManager pm = plugin.getServer().getPluginManager();
            pm.registerEvents(this, plugin);
            getPlugin().getServer().getScheduler().scheduleAsyncRepeatingTask(
                    getPlugin(),
                    new Runnable() {
                        public void run() {
                            config.reapDeadPlayers();
                        }
                    },
                    REAPER_FREQUENCY,
                    REAPER_FREQUENCY);
        }
    }

    @Override
    protected void reload() {
        if (mvPlugin != null) mvPlugin.decrementPluginCount();
        try {
            worldName = getConfigString("worldname", worldName);
            safeDistance = getConfigInt("safedistance", safeDistance);
            liveTimeout = getConfigLong("livetimeout", liveTimeout);
            deathDuration = getConfigLong("deathduration", deathDuration);
            timeOutGrowth = getConfigDouble("timeoutgrowth", timeOutGrowth);
            difficulty = Difficulty.valueOf(getConfigString("dificulty", difficulty.toString()));
            monsterBoost = Double.valueOf(getConfigString("monsterBoost", Double.toString(monsterBoost)));
            config = new HardCoreState(getPlugin(), getTrigger() + ".yml");
            config.loadConfig();
            mvPlugin = (MultiverseCore) getPlugin().getServer().getPluginManager().getPlugin("Multiverse-Core");
            if (mvPlugin == null) {
                logInfo("Could not find Multiverse-Core plugin.  Disabling self");
                disable();
            } else {
                //TODO: hey jf - there needs to be a way to do a teardown on disable.
                mvPlugin.incrementPluginCount();
            }
            logInfo("worldname:" + worldName);
            logInfo("safeDistance:" + safeDistance);
            logInfo("liveTimeout:" + liveTimeout);
            logInfo("deathDuration:" + deathDuration);
            logInfo("timeOutGrowth:" + timeOutGrowth);
            logInfo("difficulty:" + difficulty);
            logInfo("monsterBoost:" + monsterBoost);
        } catch (IOException e) {
            e.printStackTrace();
            disable();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            disable();
        }
    }

    @Override
    protected String getHelpString(Player player) {
        PlayerState ps = config.getPlayerState(player.getName());
        long timeOut = (ps != null) ? ps.getSecondsTillTimeout() : deathDuration;
        return "usage: hardcore (go | info | revive <player> | regen)  " +
                "HardCore is played with no mods.  You're on your own.  " +
                "Type /HardCore (or /hc) to enter and exit HardCore world.  " +
                "The leave, you must be close to the spawn point.  " +
                "You're officially in the game once you interact with something.  " +
                "Until then you are an observer.  After you die,  " +
                "you are a ghost for a " + durationString(timeOut) + " minutes and can only observer.  " +
                "The time you are a ghost increases with each death.  " +
                "If everyone is dead at the same time, the world regens.  If you don't " +
                "play for " + durationString(timeOut) + " minutes, you're no longer considered in the game.";
    }

    @Override
    protected boolean handleCommand(Player player, Command cmd, String[] args) {
        // subcommands
        //  regen
        //  revive <player>
        //  info
        //  go
        //  --- empty takes you to the hardcore world
        if (args.length == 0) {
            return goHardCore(player);
        } else if (args.length == 1) {
            if ("info".equalsIgnoreCase(args[0]) || "list".equalsIgnoreCase(args[0])) {
                return goInfo(player);
            } else if ("regen".equalsIgnoreCase(args[0])) {
                return goRegen(player);
            } else if ("go".equalsIgnoreCase((args[0]))) {
                return goHardCore(player);
            }
        } else if (args.length == 2) {
            if ("revive".equalsIgnoreCase(args[0])) {
                return goRevive(player, args[1]);
            }
        }

        return false;
    }

    private boolean goHardCore(Player player) {
        if (isHardCoreWorld(player.getWorld())) {
            //Player is on HARD CORE world already and wants to leave.
            //if they are close to spawn we will rescue them
            World world = player.getWorld();
            Location spawn = world.getSpawnLocation();
            Location loc = player.getLocation();
            double diff = spawn.distance(loc);
            if ((diff < safeDistance) || config.isDead(player.getName()) || hasPermissions(player, "canalwaysescape")) {
                World exitPlace = mvPlugin.getMVWorldManager().getSpawnWorld().getCBWorld();
                SafeTTeleporter teleporter = mvPlugin.getSafeTTeleporter();
                teleporter.safelyTeleport(null, player, exitPlace.getSpawnLocation(), true);
            } else {
                player.sendMessage("You need to make a HARD CORE effort to get closer to the spawn point.");
            }
        } else {
            // player is in some other world and wants to go HARD CORE
            MVWorldManager mgr = mvPlugin.getMVWorldManager();
            if (!mgr.isMVWorld(worldName) && !config.generateNewWorld()) {
                //this is an error.  Error message sent to console already.
                player.sendMessage("Something is wrong with HARD CORE.  You can't be transported at the moment.");
                return true;
            }
            String playerName = player.getName();
            if (config.isDead(playerName)) {
                player.sendMessage("You're dead, so you will roam the world as a ghost for the next " +
                        config.getSecondsTillTimeout(playerName) + " minutes.");
            }
            SafeTTeleporter teleporter = mvPlugin.getSafeTTeleporter();
            DestinationFactory destFactory = mvPlugin.getDestFactory();
            MVDestination destination = destFactory.getDestination(worldName);
            TeleportResult result = teleporter.safelyTeleport(player, player, destination);
            switch (result) {
                case FAIL_PERMISSION:
                    player.sendMessage("You don't have permissions to go to " + worldName);
                    break;
                case FAIL_UNSAFE:
                    player.sendMessage("Can't find a safe spawn location for you.");
                    break;
                case FAIL_TOO_POOR:
                    player.sendMessage("You don't have enough money.");
                    break;
                case FAIL_INVALID:
                    player.sendMessage(worldName + " is temporarily out of service.");
                    break;
                case SUCCESS:
                    player.sendMessage("Good luck.");
                    break;
                case FAIL_OTHER:
                default:
                    player.sendMessage("Something went wrong.  Something.  Stuff.");
                    break;
            }
        }
        return true;
    }

    private boolean goInfo(Player player) {
        if (!hasPermissions(player, "info")) {
            player.sendMessage("Sorry, you don't have permission.");
            return true;
        }
        String playerName = player.getName();
        String duration = durationString(config.getSecondsTillTimeout(playerName));
        if (config.isDead(playerName)) {
            player.sendMessage("You're dead for " + duration + ".  Not very HARD CORE.");
        } else if (config.isLive(playerName)) {
            player.sendMessage("You've been very HARD CORE up 'till now.");
            player.sendMessage("You need to do something in " + duration + " to stay in the game.");
        } else {
            player.sendMessage("You are not in the HARD CORE game.  Type /hc and do something to enter.");
        }
        player.sendMessage("  World: " + worldName);
        player.sendMessage("  Created: " + config.getCreationDate());
        player.sendMessage("  Dead players: ");
        for (PlayerState p : config.getDeadPlayers()) {
            player.sendMessage("    " + p.name + "  Deaths:" + p.deathCount);
        }
        player.sendMessage("  Live players: ");
        for (PlayerState p : config.getLivePlayers()) {
            player.sendMessage("    " + p.name + "  Deaths:" + p.deathCount);
        }
        return true;
    }

    private boolean goRegen(Player player) {
        if (!hasPermissions(player, "regen")) {
            player.sendMessage("Sorry, you're not HARD CORE enough.  Come back when you're more HARD CORE.");
            return true;
        }
        if (config.generateNewWorld()) {
            player.sendMessage(worldName + " has been regenerated HARDer and more CORE than ever.");
        } else {
            player.sendMessage(worldName + " is too HARD CORE to be regenerated.");
        }
        return true;
    }

    private boolean goRevive(Player player, String arg) {
        if (!hasPermissions(player, "revive")) {
            player.sendMessage("Sorry, you're not HARD CORE enough to revive other players.");
        } else if (!config.isDead(arg)) {
            player.sendMessage(arg + " is still going at it HARD CORE and isn't dead.");
        } else {
            config.markPlayerUndead(arg);
            player.sendMessage(arg + " has been revived, be is still not as HARD CORE as you.");
        }
        return true;
    }


    @EventHandler
    protected void handlePluginDisabled(PluginDisableEvent event) {
        if (event.getPlugin() == getPlugin()) {
            mvPlugin.decrementPluginCount();
            config.saveConfig();
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // set up the proper player settings for HARD CORE
        Player player = event.getPlayer();
        if (isHardCoreWorld(event.getPlayer().getWorld())) {
            // teleported to hardcore
            if (!hasPermissions(player, "keepop")) {
                player.setOp(false);
                player.setAllowFlight(false);
                player.setGameMode(GameMode.SURVIVAL);
                //TODO hey jf - this is where you want to disable zombe mod
            }
        } else if (isHardCoreWorld(event.getFrom())) {
            // teleported from hardcore
            player.setAllowFlight(true);
            if (hasPermissions(player, "keepop")) {
                player.setOp(true);
            } else {
                player.setOp(false);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // --- Stop Ghosts from doing things
        if (event.isCancelled()) return;
        if (!isHardCoreWorld(event.getPlayer().getWorld())) return;
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR) {
            return;
        }
        String playerName = event.getPlayer().getName();
        if (config.isDead(playerName)) {
            debugInfo("Ghost event");
            event.getPlayer().sendMessage("Your're dead for " + durationString(config.getSecondsTillTimeout(playerName)));
            event.setCancelled(true);
        } else {
            config.playerActivity(playerName);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        if (!isHardCoreWorld(event.getPlayer().getWorld())) return;
        String playerName = event.getPlayer().getName();
        if (config.isDead(playerName)) {
            debugInfo("Ghost event");
            event.getPlayer().sendMessage("Your're dead for " + durationString(config.getSecondsTillTimeout(playerName)));
            event.setCancelled(true);
        } else {
            config.playerActivity(playerName);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        if (!isHardCoreWorld(event.getPlayer().getWorld())) return;
        String playerName = event.getPlayer().getName();
        if (config.isDead(playerName)) {
            debugInfo("Ghost event");
            event.getPlayer().sendMessage("Your're dead for " + durationString(config.getSecondsTillTimeout(playerName)));
            event.setCancelled(true);
        } else {
            config.playerActivity(playerName);
        }
    }

    @EventHandler
    public void onPlayerDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!isHardCoreWorld(player.getWorld())) return;
            String playerName = player.getName();
            config.markPlayerDead(playerName);
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.isCancelled()) return;
        if (isHardCoreWorld(event.getPlayer().getWorld())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("Can't change game modes.  " + worldName + " is HARD CORE.");
        }
    }

    private boolean isHardCoreWorld(World w) {
        return worldName.equalsIgnoreCase(w.getName());
    }

    private static String durationString(long duration) {
        return String.format("%02d:%02d:%02d", duration / 3600, (duration % 3600) / 60, (duration % 60));
    }

    //
    // Persistent state methods (ex. live vs. dead)
    //

    /*
       Configuration looks like this:
       hardcorecow.creationDate: creationDate
       hardcorecow.liveplayers: player1,player2,player3
       hardcorecow.deadplayers: player1,player2,player3
    */
    private class HardCoreState extends CowNetConfig {
        private boolean regenIsAlreadyScheduled = false;
        private Map<String, PlayerState> allPlayers = new HashMap<String, PlayerState>();
        private String creationDate = "unknown";

        public HardCoreState(JavaPlugin plugin, String name) {
            super(plugin, name);
        }

        public void loadConfig() throws IOException, InvalidConfigurationException {
            super.loadConfig();
            creationDate = getString("creationdate", creationDate);

            allPlayers.clear();

            if (get("players") instanceof MemorySection) {
                MemorySection section = (MemorySection) get("players");
                for (String key : section.getKeys(false)) {
                    PlayerState ps = (PlayerState) section.get(key);
                    allPlayers.put(key.toLowerCase(), ps);
                }
            }
        }

        public void saveConfig() {
            debugInfo("saveConfig()");
            set("creationdate", creationDate);
            set("players", allPlayers);
            try {
                super.saveConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /*
        Mark as player as dead and return true if that was the last
        active player.
         */
        public void markPlayerDead(String player) {
            if (isLive(player)) {
                // this player was live in the game, so mark them dead.
                getPlayerState(player).setIsDead();
                saveConfig();
                debug("markPlayerUndead " + player);
            }
        }

        /*
        This method moves someone from the dead queue to the live
        queue ONLY.  It doesn't add them to the live queue
        if they are not already there.
         */
        public void markPlayerUndead(String player) {
            if (!isDead(player)) return;
            getPlayerState(player).setIsLive();
            saveConfig();
            debug("markPlayerUndead " + player);
        }

        public boolean isDead(String player) {
            PlayerState ps = getPlayerState(player);
            return (ps != null) && !ps.isLive;
        }

        public boolean isLive(String player) {
            PlayerState ps = getPlayerState(player);
            return (ps != null) && ps.isLive;
        }

        public boolean isUnknown(String player) {
            return getPlayerState(player) == null;
        }

        /*
        This is the primary way to put a player into the game and to
        keep them there.
         */
        public void playerActivity(String player) {
            if (isDead(player)) return;
            PlayerState ps = getPlayerState(player);
            if (ps == null) {
                // automagically add players
                allPlayers.put(player.toLowerCase(), new PlayerState(player));
            } else {
                ps.noteActivity();
            }
        }

        private PlayerState getPlayerState(String name) {
            return allPlayers.get(name.toLowerCase());
        }

        public Set<PlayerState> getLivePlayers() {
            Set<PlayerState> result = new HashSet<PlayerState>();
            for (PlayerState player : allPlayers.values()) {
                if (isLive(player.name)) {
                    result.add(player);
                }
            }
            return result;
        }

        public Set<PlayerState> getDeadPlayers() {
            Set<PlayerState> result = new HashSet<PlayerState>();
            for (PlayerState player : allPlayers.values()) {
                if (isDead(player.name)) {
                    result.add(player);
                }
            }
            return result;
        }

        public long getSecondsTillTimeout(String name) {
            PlayerState ps = getPlayerState(name);
            if (ps == null) return 0;
            return ps.getSecondsTillTimeout();
        }

        public String getCreationDate() {
            return creationDate;
        }

        public void resetWorldState() {
            creationDate = dateFormat.format(new Date());
            allPlayers.clear();
            saveConfig();
            debug("resetWorldState");
        }

        private void reapDeadPlayers() {
            if (allPlayers.size() == 0) return;
            LinkedList<String> playersToStomp = new LinkedList<String>();
            for (PlayerState player : allPlayers.values()) {
                if (player.getSecondsTillTimeout() == 0) {
                    // It's been so long since we've seen this player we don't
                    // count them as live anymore.
                    playersToStomp.add(player.name.toLowerCase());
                    debugInfo("Stomping on " + player);
                } else {
                    debugInfo("Timeremaining: " + player.getSecondsTillTimeout() + "  " + player);
                }
            }
            for (String playerName : playersToStomp) {
                PlayerState ps = allPlayers.get(playerName);
                if (ps.isLive) {
                    allPlayers.remove(playerName);
                } else {
                    ps.setIsLive();
                }
            }
            config.saveConfig();
            debug("Reaping complete");
            if (getLivePlayers().size() == 0) {
                // if the last live player was removed, regen the world
                getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
                    public void run() {
                        generateNewWorld();
                        resetWorldState();
                        getPlugin().getServer().broadcastMessage("Seems like " + worldName + " was too HARD CORE.  " +
                                "It's been regenerated to be a bit more fluffy for you softies.");
                    }// end of run
                }, 40);
            }
        }

        private boolean generateNewWorld() {
            if (regenIsAlreadyScheduled) return true;
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

        private void debug(String msg) {
            debugInfo(msg);
            debugInfo("  World: " + worldName);
            debugInfo("  Created: " + creationDate);
            debugInfo("  Players: ");
            for (String name : allPlayers.keySet()) {
                PlayerState ps = allPlayers.get(name);
                debugInfo("    " + ps.toString());
            }
        }
    }

    static {
        ConfigurationSerialization.registerClass(PlayerState.class);
    }

    @SerializableAs("PlayerState")
    public static class PlayerState implements ConfigurationSerializable {
        public String name;
        public long lastActivity;    // either time when player should be removed from allPlayers list
        public boolean isLive;
        public int deathCount;

        public void setIsLive() {
            // state has changed
            isLive = true;
            System.err.println(this);
        }

        public void setIsDead() {
            isLive = false;
            deathCount++;
            noteActivity();
            System.err.println(this);
            try {
                throw new Exception();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void noteActivity() {
            lastActivity = System.currentTimeMillis();
            System.err.println(this);
        }

        public long getSecondsTillTimeout() {
            long secondsElapsed = (System.currentTimeMillis() - lastActivity) / 1000;
            if (isLive) {
                long timeRequired = liveTimeout / (deathCount + 1);
                long timeLeft = timeRequired - secondsElapsed;
                System.err.println(name + " LIVE elapsed time:" + secondsElapsed + "  time required: " + timeRequired + "  time left:" + timeLeft);
                return Math.max(0, timeLeft);
            } else {
                long timeRequired = (long) (deathDuration * Math.pow(timeOutGrowth, deathCount - 1));
                long timeLeft = timeRequired - secondsElapsed;
                System.err.println(name + " DEAD elapsed time:" + secondsElapsed + "  time required: " + timeRequired + "  time left:" + timeLeft);
                return Math.max(0, timeLeft);
            }
        }

        public PlayerState(String name) {
            this.name = name;
            this.isLive = true;
            noteActivity();
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(name);
            builder.append("  ");
            builder.append((isLive) ? "live  " : "dead  ");
            builder.append("deathCount:" + deathCount);
            builder.append("  ");
            builder.append(dateFormat.format(new Date(lastActivity)));
            return builder.toString();
        }

        // --- serialize/deserialize support
        public static PlayerState deserialize(Map<String, Object> args) {
            return new PlayerState(args);
        }

        public static PlayerState valueOf(Map<String, Object> map) {
            return new PlayerState(map);
        }

        public PlayerState(Map<String, Object> map) {
            CowNetConfig.deserialize(this, map);
        }

        @Override
        public Map<String, Object> serialize() {
            return CowNetConfig.serialize(this);
        }
    }

    // --- Hachky test
    public static void main(String[] args) {
        PlayerState test = new PlayerState("Player1");
        Map<String, Object> map = test.serialize();
        for (String key : map.keySet()) {
            System.out.println(key);
            System.out.println("  " + map.get(key));
        }
        test = new PlayerState(map);
        System.out.println(test);

        test.setIsLive();
        test.getSecondsTillTimeout();
        test.noteActivity();
        test.getSecondsTillTimeout();
        test.setIsDead();
        test.getSecondsTillTimeout();
    }
}

