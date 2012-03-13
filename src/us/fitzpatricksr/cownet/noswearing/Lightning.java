package us.fitzpatricksr.cownet.noswearing;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.NoSwearing.Consequence;

// set them on fire
public class Lightning implements Consequence {
	private int damage;
	public Lightning(JavaPlugin plugin, String trigger) {
        FileConfiguration config = plugin.getConfig();
        damage = config.getInt(trigger+".lightningDamage", 5);
	}
	public void handleBadWord(Player player, String word) {
		player.getWorld().strikeLightningEffect(player.getLocation());
		player.damage(damage, player);
		player.sendMessage("I call down lightning for saying words like "+word+"!");
	}
}