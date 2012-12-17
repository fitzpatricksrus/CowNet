package us.fitzpatricksr.cownet.commands;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.framework.GameContext;
import us.fitzpatricksr.cownet.commands.games.framework.GameModule;
import us.fitzpatricksr.cownet.commands.games.framework.SimpleGameController;
import us.fitzpatricksr.cownet.commands.games.gamemodules.TestModule;

import java.util.HashMap;

/*
    battle
    battle join
    battle team

    You leave the game when you leave the server or when
    you teleport out of the game world.  We could preserve
    the players team across these events but it mucks up
    the works and doesn't seem worth the trouble.
 */
public class Panic extends CowNetThingy implements Listener {

    @Setting
    private String panicWorldName = "PanicLand";

    private SimpleGameController controller;
    private GameModule[] modules = new GameModule[]{
            new TestModule("TestModule1"),
            new TestModule("TestModule2"),
            new TestModule("TestModule3"),
//            new SnowWars(),
//            new TntWars()
    };

    public Panic() {
        controller = new SimpleGameController(this, modules);
    }

    @Override
    protected void onEnable() throws Exception {
        controller.startup();
    }

    @Override
    protected void onDisable() {
        controller.shutdown();
    }

    @Override
    protected String[] getHelpText(CommandSender player) {
        return new String[]{
                "usage: panic [battle | team | changeteam]"
        };
    }

    @Override
    protected void reloadManualSettings() throws Exception {
        reloadAutoSettings(controller);
        for (GameModule module : modules) {
            reloadAutoSettings(module);
        }
    }

    @Override
    protected HashMap<String, String> getManualSettings() {
        HashMap<String, String> result = getSettingValueMapFor(controller);
        for (GameModule module : modules) {
            result.putAll(getSettingValueMapFor(module));
        }
        return result;
    }

    @Override
    protected boolean updateManualSetting(String settingName, String settingValue) {
        boolean result = setAutoSettingValue(controller, settingName, settingValue);
        for (GameModule module : modules) {
            result = setAutoSettingValue(module, settingName, settingValue) || result;
        }
        return result;
    }

    //--------------------------------------------------
    //  Command handlers
    //

    @CowCommand
    protected boolean doBattle(Player player) {
        controller.addPlayer(player.getName());
        return true;
    }

    @CowCommand
    protected boolean doTeam(Player player) {
        return doTeam(player, player.getName());
    }

    @CowCommand
    protected boolean doTeam(CommandSender sender, String playerName) {
        GameContext.Team team = controller.getPlayerTeam(playerName);
        sender.sendMessage(playerName + " is on the " + team + " team.");
        return true;
    }

    @CowCommand
    protected boolean doChangeteam(Player player) {
        return doChangeteam(player, player.getName());
    }

    @CowCommand
    protected boolean doChangeteam(CommandSender sender, String playerName) {
        GameContext.Team team = controller.getPlayerTeam(playerName).otherTeam();
        if (controller.changePlayerTeam(playerName, team)) {
            broadcastToAllOnlinePlayers(playerName + " is now on the " + team + " team.");
        } else {
            sender.sendMessage("Sorry.  Changing teams would make the teams unbalanced.");
        }
        return true;
    }

    @CowCommand(permission = "info")
    protected boolean doInfo(CommandSender player) {
        for (String playerName : controller.getPlayers()) {
            player.sendMessage(playerName + ": " + controller.getPlayerTeam(playerName));
        }
        return true;
    }

    // ---- Event handlers

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        debugInfo("onPlayerChangedWorld");
        // this is how you officially leave the game
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        debugInfo("onPlayerTeleport");
        String toWorld = event.getTo().getWorld().getName();
        String fromWorld = event.getFrom().getWorld().getName();
        if (toWorld.equalsIgnoreCase(fromWorld)) return;
        if (!toWorld.equalsIgnoreCase(fromWorld)) {
            Player player = event.getPlayer();
            if (toWorld.equalsIgnoreCase(panicWorldName)) {
                controller.addPlayer(player.getName());
            } else if (fromWorld.equalsIgnoreCase(panicWorldName)) {
                if (controller.isGaming()) {
                    // you can't leave in the middle of a game
                    event.setCancelled(true);
                } else {
                    controller.removePlayer(player.getName());
                }
            }
        }
        // this is how you officially leave the game
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        // don't let players in the game do this
        if (event.getNewGameMode() == GameMode.CREATIVE) {
            Player player = event.getPlayer();
            if (controller.getPlayers().contains(player.getName())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // if player is not in the game, make sure they are not
        // teleported to the game world.
        debugInfo("onPlayerJoin");
        Player player = event.getPlayer();
        if (controller.playerIsInGame(player.getName())) {
            controller.addPlayer(player.getName());
        } else {
            // OK, so they were in a game a long time ago and left before the game
            // ended but bukkit still thinks they are in the game world.  We
            // want to put them in a better spot.
            World safeWorld = getPlugin().getServer().getWorlds().get(0);
            Location location = safeWorld.getSpawnLocation();
            player.teleport(location);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        debugInfo("onPlayerQuit");
        controller.removePlayer(event.getPlayer().getName());
        // As an alternative we could just let the end of game logic
        // remove this player from the team so that if they rejoined they
        // would be on the same team.  However, we'd then have to decide
        // how this affected new players entering mid-game, so we just
        // punt on it all and assume people wont quit unless they have
        // to and aren't trying to game the system.
    }
}


