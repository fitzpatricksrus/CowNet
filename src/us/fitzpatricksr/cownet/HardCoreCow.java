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
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetConfig;
import us.fitzpatricksr.cownet.utils.CowNetThingy;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class HardCoreCow extends CowNetThingy implements Listener {
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private HardCoreState config;
    private String worldName = "HardCoreCow";
    private int safeDistance = 5;
    private MultiverseCore mvPlugin;
    private int timeOut = 3 * 24 * 60;  //default time before you are removed from game in minutes.
    private Difficulty difficulty = Difficulty.HARD;
    private double monsterBoost = 1.0d;
    private boolean regenIsAlreadyScheduled = false;

    public HardCoreCow(JavaPlugin plugin, String permissionRoot, String trigger) {
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
        try {
            worldName = getConfigString("worldname", worldName);
            safeDistance = getConfigInt("safedistance", safeDistance);
            timeOut = getConfigInt("timeout", timeOut);
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
        } catch (IOException e) {
            e.printStackTrace();
            disable();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            disable();
        }
    }

    @EventHandler
    protected void handlePluginDisabled(PluginDisableEvent event) {
        if (event.getPlugin() == getPlugin()) {
            mvPlugin.decrementPluginCount();
            try {
                config.saveConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected String getHelpString(Player player) {
        return "usage: hardcore (go | info | revive <player> | regen)  " +
                "HardCore is played with no mods.  You're on your own.  " +
                "Type /HardCore (or /hc) to enter and exit HardCore world.  " +
                "The leave, you must be close to the spawn point.  " +
                "You're officially in the game once you interact with something.  " +
                "Until then you are an observer.  After you die,  " +
                "you can come back and observe, but not change things.  You're a ghost.  " +
                "Last person dies, the world regens.  If you don't " +
                "play for " + timeOut + " minutes, you're removed from the game " +
                "and you can re-enter if you previously died.";
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
            if ("info".equalsIgnoreCase(args[0])) {
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
        if (!hasPermissions(player, "go")) {
            player.sendMessage("Sorry, you don't have permission.");
            return true;
        }
        if (player.getWorld().getName().equalsIgnoreCase(worldName)) {
            //Player is on HARD CORE world aleady.
            //if they are close to spawn we will rescue them
            World world = player.getWorld();
            Location spawn = world.getSpawnLocation();
            Location loc = player.getLocation();
            double diff = spawn.distance(loc);
            if ((diff > safeDistance) && config.playerIsLive(player) && !hasPermissions(player, "canalwaysescape")) {
                player.sendMessage("You need to make a HARD CORE effort to get closer to the spawn point.");
            } else {
                World exitPlace = mvPlugin.getMVWorldManager().getSpawnWorld().getCBWorld();
                SafeTTeleporter teleporter = mvPlugin.getSafeTTeleporter();
                teleporter.safelyTeleport(null, player, exitPlace.getSpawnLocation(), true);
            }
        } else {
            // player is in some other world and wants to go HARD CORE
            MVWorldManager mgr = mvPlugin.getMVWorldManager();
            if (!mgr.isMVWorld(worldName) && !generateNewWorld()) {
                return true;
            }
            if (config.playerIsDead(player.getName())) {
                player.sendMessage("You're dead, so you will roam the world as a ghost.");
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
        if (config.playerIsDead(player.getName())) {
            player.sendMessage("You're dead.  Not very HARD CORE.");
        } else {
            player.sendMessage("You've been very HARD CORE up 'till now.");
        }
        player.sendMessage("  World: " + worldName);
        player.sendMessage("  Created: " + config.getCreationDate());
        player.sendMessage("  Dead players: ");
        for (String p : config.getDeadPlayers()) {
            player.sendMessage("    " + p + "  -  " + dateFormat.format(new Date(config.getPlayerLastActiveTime(p))));
        }
        player.sendMessage("  Live players: ");
        for (String p : config.getLivePlayers()) {
            player.sendMessage("    " + p + "  -  " + dateFormat.format(new Date(config.getPlayerLastActiveTime(p))));
        }
        return true;
    }

    private boolean goRegen(Player player) {
        if (!hasPermissions(player, "regen")) {
            player.sendMessage("Sorry, you're not HARD CORE enough.  Come back when you're more HARD CORE.");
            return true;
        }
        if (generateNewWorld()) {
            player.sendMessage(worldName + " has been regenerated HARDer and more CORE than ever.");
        } else {
            player.sendMessage(worldName + " is too HARD CORE to be regenerated.");
        }
        return true;
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
                try {
                    config.resetWorldState();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

    private boolean goRevive(Player player, String arg) {
        if (!hasPermissions(player, "revive")) {
            player.sendMessage("Sorry, you're not HARD CORE enough.");
        } else if (!config.playerIsDead(arg)) {
            player.sendMessage(arg + " is still going at it HARD CORE and isn't dead.");
        } else {
            config.markPlayerLive(arg);
        }
        return true;
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // set up the proper player settings for HARD CORE
        Player player = event.getPlayer();
        if (event.getPlayer().getWorld().getName().equalsIgnoreCase(worldName)) {
            // teleported to hardcore
            if (!hasPermissions(player, "keepop")) {
                player.setOp(false);
                player.setAllowFlight(false);
                player.setGameMode(GameMode.SURVIVAL);
                //hey jf - this is where you want to disable zombe mod
            }
        } else if (event.getFrom().getName().equalsIgnoreCase(worldName)) {
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
        if (!event.getPlayer().getWorld().getName().equalsIgnoreCase(worldName)) return;
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (config.playerIsDead(event.getPlayer())) {
            logInfo("Ghost event");
            event.setCancelled(true);
        } else {
            config.markPlayerLive(event.getPlayer());
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        if (!event.getPlayer().getWorld().getName().equalsIgnoreCase(worldName)) return;
        if (config.playerIsDead(event.getPlayer())) {
            logInfo("Ghost event");
            event.setCancelled(true);
        } else {
            config.markPlayerLive(event.getPlayer());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        if (!event.getPlayer().getWorld().getName().equalsIgnoreCase(worldName)) return;
        if (config.playerIsDead(event.getPlayer())) {
            logInfo("Ghost event");
            event.setCancelled(true);
        } else {
            config.markPlayerLive(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!player.getWorld().getName().equalsIgnoreCase(worldName)) return;
            config.markPlayerDead(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (regenIsAlreadyScheduled) return;
        if (config.getDeadPlayers().size() > 0 && config.getLivePlayers().size() == 0) {
            logInfo("Everyone's dead.  This world was too HARD CORE.  Generating a new world in 2 seconds.  Please wait...");
            getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
                public void run() {
                    generateNewWorld();
                    getPlugin().getServer().broadcastMessage("Seems like " + worldName + " was too HARD CORE.  " +
                            "It's been regenerated to be a bit more fluffy for you softies.");
                }// end of run
            }, 40);
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.isCancelled()) return;
        if (worldName.equalsIgnoreCase(event.getPlayer().getWorld().getName())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("Can't change game modes.  " + worldName + " is HARD CORE.");
        }
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
        private HashMap<String, PlayerState> livePlayers = new HashMap<String, PlayerState>();
        private HashMap<String, PlayerState> deadPlayers = new HashMap<String, PlayerState>();
        private Map<String, PlayerState> allPlayers = new HashMap<String, PlayerState>();
        private String creationDate = "unknown";

        //TODO hey jf - it would be nice if live players expired after a while so they
        // don't keep the hardcore world around forever.

        public HardCoreState(JavaPlugin plugin, String name) {
            super(plugin, name);
        }

        public void loadConfig() throws IOException, InvalidConfigurationException {
            super.loadConfig();
            creationDate = getString("creationdate", creationDate);

            allPlayers.clear();
            livePlayers.clear();
            deadPlayers.clear();

            Object lp = get("players");
            System.err.println(lp.getClass());
            if (get("players") instanceof MemorySection) {
                MemorySection section = (MemorySection) get("players");
                for (String key : section.getKeys(false)) {
                    PlayerState ps = (PlayerState) section.get(key);
                    allPlayers.put(key, ps);
                    if (ps.isLive) {
                        livePlayers.put(ps.name, ps);
                        System.err.println("live: " + ps.name);
                    } else {
                        deadPlayers.put(ps.name, ps);
                        System.err.println("dead: " + ps.name);
                    }
                }
            }
        }

        public void saveConfig() throws IOException {
            System.err.println("saveConfig()");
            set("creationdate", creationDate);
            set("players", allPlayers);
            System.err.println(get("players"));
            super.saveConfig();
            debug("Saving...");
        }

        public boolean playerIsDead(Player player) {
            return playerIsDead(player.getName());
        }

        public boolean playerIsDead(String player) {
            player = player.toLowerCase();
            return deadPlayers.get(player) != null;
        }

        public boolean playerIsLive(Player player) {
            return playerIsLive(player.getName());
        }

        public boolean playerIsLive(String player) {
            player = player.toLowerCase();
            return livePlayers.get(player) != null;
        }

        public Set<String> getLivePlayers() {
            return livePlayers.keySet();
        }

        public Set<String> getDeadPlayers() {
            return deadPlayers.keySet();
        }

        public long getPlayerLastActiveTime(String player) {
            PlayerState ps = allPlayers.get(player);
            if (ps == null) {
                return 0;
            } else {
                return ps.lastHardCore;
            }
        }

        public void markPlayerDead(Player p) {
            markPlayerDead(p.getName());
        }

        public void markPlayerDead(String p) {
            p = p.toLowerCase();
            if (deadPlayers.keySet().contains(p)) return;
            PlayerState state = livePlayers.get(p);
            state.setIsLive(false);
            deadPlayers.put(p, state);
            livePlayers.remove(p);
            try {
                saveConfig();
            } catch (IOException e) {
                e.printStackTrace();
                // OK, we didn't save it.  No biggie.
            }
        }

        public void markPlayerLive(Player p) {
            markPlayerLive(p.getName());
        }

        private void markPlayerLive(String p) {
            p = p.toLowerCase();
            PlayerState state = allPlayers.get(p);
            if (state == null) {
                state = new PlayerState(p);
                allPlayers.put(p, state);
                livePlayers.put(p, state);
            } else {
                state.setIsLive(true);
                if (!state.isLive) {
                    deadPlayers.remove(p);
                    livePlayers.put(p, state);
                    try {
                        saveConfig();
                    } catch (IOException e) {
                        e.printStackTrace();
                        // we didn't save, but whatever
                    }
                }
            }
        }

        public String getCreationDate() {
            return creationDate;
        }

        public void resetWorldState() throws IOException {
            livePlayers.clear();
            deadPlayers.clear();
            creationDate = dateFormat.format(new Date());
            saveConfig();
        }

        private void debug(String msg) {
            logInfo(msg);
            logInfo("  World: " + worldName);
            logInfo("  Created: " + creationDate);
            logInfo("  Players: ");
            for (String name : allPlayers.keySet()) {
                PlayerState ps = allPlayers.get(name);
                logInfo("    " + ps.toString());
            }
        }
    }

    {
        ConfigurationSerialization.registerClass(PlayerState.class);
    }

    @SerializableAs("PlayerState")
    public static class PlayerState implements ConfigurationSerializable {
        public String name;
        public long lastHardCore;
        public boolean isLive;

        public void setIsLive(boolean live) {
            isLive = live;
            lastHardCore = new Date().getTime();
        }

        public PlayerState(String name) {
            this.name = name;
            this.lastHardCore = new Date().getTime();
            this.isLive = true;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(name);
            builder.append("  ");
            builder.append((isLive) ? "live  " : "dead  ");
            builder.append(dateFormat.format(new Date(lastHardCore)));
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

    }
}

