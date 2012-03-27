package us.fitzpatricksr.cownet;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVDestination;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.SafeTTeleporter;
import com.onarandombox.MultiverseCore.destination.DestinationFactory;
import com.onarandombox.MultiverseCore.enums.TeleportResult;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetConfig;
import us.fitzpatricksr.cownet.utils.CowNetThingy;
import us.fitzpatricksr.cownet.utils.StringUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class HardCoreCow extends CowNetThingy implements Listener {
    private HardCoreState config;
    private MultiverseCore mvPlugin;

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
        /*
        Configuration looks like this:
        hardcorecow.creationDate: creationDate
        hardcorecow.worldname: nameOfWorld
        hardcorecow.liveplayers: player1,player2,player3
        hardcorecow.deadplayers: player1,player2,player3
         */
        if (mvPlugin != null) mvPlugin.decrementPluginCount();
        try {
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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            disable();
        }
    }

    @Override
    protected String getHelpString(Player player) {
        return "usage: hardcore (go | info | revive <player> | regen)";
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
        MVWorldManager mgr = mvPlugin.getMVWorldManager();
        if (!mgr.isMVWorld(config.getWorldName()) && !generateNewWorld(player)) {
            return true;
        }
        if (config.playerIsDead(player.getName())) {
            player.sendMessage("You're dead, so you will roam the world as a ghost.");
        }
        SafeTTeleporter teleporter = mvPlugin.getSafeTTeleporter();
        DestinationFactory destFactory = mvPlugin.getDestFactory();
        MVDestination destination = destFactory.getDestination(config.getWorldName());
        TeleportResult result = teleporter.safelyTeleport(player, player, destination);
        switch (result) {
            case FAIL_PERMISSION:
                player.sendMessage("You don't have permissions to go to " + config.getWorldName());
                break;
            case FAIL_UNSAFE:
                player.sendMessage("Can't find a safe spawn location for you.");
                break;
            case FAIL_TOO_POOR:
                player.sendMessage("You don't have enough money.");
                break;
            case FAIL_INVALID:
                player.sendMessage(config.getWorldName() + " is temporarily out of service.");
                break;
            case SUCCESS:
                player.sendMessage("Good luck.");
                break;
            case FAIL_OTHER:
            default:
                player.sendMessage("Something went wrong.  Something.  Stuff.");
                break;
        }
        return true;
    }

    private boolean goInfo(Player player) {
        if (config.playerIsDead(player.getName())) {
            player.sendMessage("You're dead.  Let's establish that right off the bat.");
        } else {
            player.sendMessage("You're still breathing.  That's a good thing.");
        }
        player.sendMessage("  World: " + config.getWorldName());
        player.sendMessage("  Created: " + config.getCreationDate());
        player.sendMessage("  Dead players: " + StringUtils.flatten(config.getDeadPlayers()));
        player.sendMessage("  Live players: " + StringUtils.flatten(config.getLivePlayers()));
        return true;
    }

    private boolean goRegen(Player player) {
        if (!hasPermissions(player, "regen")) {
            player.sendMessage("Sorry.  You don't have permissions.");
            return true;
        }
        generateNewWorld(player);
        return true;
    }

    private boolean generateNewWorld(Player player) {
        MVWorldManager mgr = mvPlugin.getMVWorldManager();
        if (mgr.isMVWorld(config.getWorldName())) {
            if (!mgr.deleteWorld(config.getWorldName())) {
                player.sendMessage("Agh!  Can't regen " + config.getWorldName());
                return false;
            }
        }
        if (mgr.addWorld(config.getWorldName(),
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
            player.sendMessage(config.getWorldName() + " has been regenerated.");
            return true;
        } else {
            player.sendMessage("Oh No's! " + config.getWorldName() + " don wurk.");
            return false;
        }
    }

    private boolean goRevive(Player player, String arg) {
        if (!hasPermissions(player, "revive")) {
            player.sendMessage("Sorry.  You don't have permissions.");
        } else if (!config.playerIsDead(arg)) {
            player.sendMessage(arg + " isn't quite dead.  I mean, not completely.  Not yet.");
        } else if (config.markPlayerLive(arg)) {
            player.sendMessage("Kay. Fine. " + arg + " can has " + config.getWorldName());
        } else {
            player.sendMessage("Oh No's!  " + arg + " no can has!");
        }
        return true;
    }


    //
    // Persistent state methods (ex. live vs. dead)
    //
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private class HardCoreState extends CowNetConfig {
        private HashSet<String> livePlayers = new HashSet<String>();
        private HashSet<String> deadPlayers = new HashSet<String>();
        private String worldName = "HardCoreCow";
        private String creationDate = "unknown";

        public HardCoreState(JavaPlugin plugin, String name) {
            super(plugin, name);
        }

        public void loadConfig() throws IOException, InvalidConfigurationException {
            super.loadConfig();
            worldName = getString("worldname", worldName);
            creationDate = getString("creationdate", creationDate);

            String liveString = getString("liveplayers", "");
            livePlayers.clear();
            livePlayers.addAll(Arrays.asList(StringUtils.unflatten(liveString)));

            String deadString = getString("deadplayers", "");
            deadPlayers.clear();
            deadPlayers.addAll(Arrays.asList(StringUtils.unflatten(deadString)));

            debug("Restored:");
        }

        public void saveConfig() throws IOException {
            logInfo("saveConfig");
            set("worldname", worldName);
            set("creationdate", creationDate);
            set("liveplayers", StringUtils.flatten(livePlayers));
            set("deadplayers", StringUtils.flatten(deadPlayers));
            super.saveConfig();
            debug("Saving...");
        }

        public boolean playerIsDead(Player player) {
            return playerIsDead(player.getName());
        }

        public boolean playerIsDead(String player) {
            logInfo("playerIsDead(" + player + ") = " + deadPlayers.contains(player));
            return deadPlayers.contains(player);
        }

        public Set<String> getLivePlayers() {
            return livePlayers;
        }

        public Set<String> getDeadPlayers() {
            return deadPlayers;
        }

        public boolean markPlayerDead(Player p) {
            return markPlayerDead(p.getName());
        }

        public boolean markPlayerDead(String p) {
            if (deadPlayers.contains(p)) return true;
            deadPlayers.add(p);
            livePlayers.remove(p);
            try {
                saveConfig();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                deadPlayers.remove(p);
                livePlayers.add(p);
                return false;
            }
        }

        public boolean markPlayerLive(Player p) {
            return markPlayerLive(p.getName());
        }

        private boolean markPlayerLive(String p) {
            if (livePlayers.contains(p)) return true;
            deadPlayers.remove(p);
            livePlayers.add(p);
            try {
                saveConfig();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                deadPlayers.add(p);
                livePlayers.remove(p);
                return false;
            }
        }

        public String getWorldName() {
            return worldName;
        }

        public String getCreationDate() {
            return creationDate;
        }

        public void resetWorldState() throws IOException {
            livePlayers.clear();
            deadPlayers.clear();
            creationDate = dateFormat.format(new Date());
            //worldName = worldName;
            saveConfig();
        }

        private void debug(String msg) {
            logInfo(msg);
            logInfo("  World: " + worldName);
            logInfo("  Created: " + creationDate);
            logInfo("  Dead players: " + StringUtils.flatten(deadPlayers));
            logInfo("  Live players: " + StringUtils.flatten(livePlayers));
        }
    }

    // --- add players to the live player list


    // --- Stop Ghosts from doing things
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        logInfo("onPlayerInteract");
        if (event.isCancelled()) return;
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (config.playerIsDead(event.getPlayer())) {
            logInfo("onBlockPlace: dead");
            event.setCancelled(true);
        } else {
            logInfo("onBlockPlace: mark as live");
            config.markPlayerLive(event.getPlayer());
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        if (config.playerIsDead(event.getPlayer())) {
            logInfo("onBlockPlace: dead");
            event.setCancelled(true);
        } else {
            logInfo("onBlockPlace: mark as live");
            config.markPlayerLive(event.getPlayer());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        logInfo("onBlockBreak");
        if (event.isCancelled()) return;
        if (config.playerIsDead(event.getPlayer())) {
            logInfo("onBlockPlace: dead");
            event.setCancelled(true);
        } else {
            logInfo("onBlockPlace: mark as live");
            config.markPlayerLive(event.getPlayer());
        }
    }
}

