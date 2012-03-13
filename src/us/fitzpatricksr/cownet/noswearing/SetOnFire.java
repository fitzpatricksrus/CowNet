package us.fitzpatricksr.cownet.noswearing;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.NoSwearing.Consequence;

// set them on fire
public class SetOnFire implements Consequence {
	private int fireTicks;
	public SetOnFire(JavaPlugin plugin, String trigger) {
        FileConfiguration config = plugin.getConfig();
        fireTicks = config.getInt(trigger+".fireTicks", 5) * 20; // 5 seconds
	}
	public void handleBadWord(Player player, String word) {
		player.setFireTicks(fireTicks);
		player.sendMessage("You've been very naughty!  Saying words like "+word+"!");
	}
}