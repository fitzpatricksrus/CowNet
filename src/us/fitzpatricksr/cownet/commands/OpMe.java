package us.fitzpatricksr.cownet.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import us.fitzpatricksr.cownet.CowNetThingy;


public class OpMe extends CowNetThingy implements Listener {
    @Setting
    private boolean loginop = true;   // allow people to login as op

    @Override
    protected String getHelpString(CommandSender sender) {
        return "usage: opme";
    }

    @CowCommand(permission = "canOp")
    protected boolean doOpme(Player player) {
        player.setOp(true);
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (loginop != true) {
            event.getPlayer().setOp(false);
        }
    }
}

