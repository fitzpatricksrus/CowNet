package us.fitzpatricksr.cownet.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import us.fitzpatricksr.cownet.CowNetThingy;

public class Nickname extends CowNetThingy implements Listener {
	@Override
	protected String getHelpString(CommandSender sender) {
		return "usage: Nickname";
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		String nickname = getConfigValue(player.getName().toLowerCase(), player.getDisplayName());
		logInfo(player.getName() + "'s nickname is " + nickname);
		player.setDisplayName(nickname);
		player.setPlayerListName(nickname);
		event.setJoinMessage(nickname + " joined the game");
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		String nickname = getConfigValue(player.getName().toLowerCase(), player.getDisplayName());
		event.setQuitMessage(nickname + " left the game");
	}
}
