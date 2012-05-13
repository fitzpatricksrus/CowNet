package us.fitzpatricksr.cownet;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetThingy;

public class Nickname extends CowNetThingy implements Listener {
    public Nickname(JavaPlugin plugin, String permissionRoot) {
        super(plugin, permissionRoot);
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(this, plugin);
    }

    @Override
    protected String getHelpString(CommandSender sender) {
        return "usage: Nickname";
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String nickname = getConfigString(player.getName().toLowerCase(), player.getDisplayName());
        logInfo(player.getName() + "'s nickname is " + nickname);
        player.setDisplayName(nickname);
        player.setPlayerListName(nickname);
        event.setJoinMessage(nickname + " joined the game");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String nickname = getConfigString(player.getName().toLowerCase(), player.getDisplayName());
        event.setQuitMessage(nickname + " left the game");
    }
}
