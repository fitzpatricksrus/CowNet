package us.fitzpatricksr.cownet.commands;

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
import us.fitzpatricksr.cownet.commands.games.gamemodules.SnowWars;
import us.fitzpatricksr.cownet.commands.games.gamemodules.TntWars;

import java.util.HashMap;

/*
    battle
    battle join
    battle team
 */
public class Panic extends CowNetThingy implements Listener {

    private SimpleGameController controller;
    private GameModule[] modules = new GameModule[]{
            new SnowWars(),
            new TntWars()
    };

    public Panic() {
    }

    @Override
    protected void onEnable() throws Exception {
        controller = new SimpleGameController(this, modules);
        controller.startup();
    }

    @Override
    protected void onDisable() {
        controller.shutdown();
        controller = null;
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        debugInfo("onPlayerTeleport");
        // this is how you officially leave the game
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        // don't let players in the game do this
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // if player is not in the game, make sure they are not
        // teleported to the game world.  Else, let the games figure it out
        debugInfo("onPlayerJoin");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        debugInfo("onPlayerQuit");
    }
}


