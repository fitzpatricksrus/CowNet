package us.fitzpatricksr.cownet.noswearing;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import us.fitzpatricksr.cownet.NoSwearing.Consequence;

// launch
public class Launch implements Consequence {
    private int launchVelocity;

    public Launch(JavaPlugin plugin, String trigger) {
        FileConfiguration config = plugin.getConfig();
        launchVelocity = config.getInt(trigger + ".launchVelocity", 5);
    }

    public void handleBadWord(Player player, String word) {
        player.setVelocity(new Vector(0, launchVelocity, 0));
        player.sendMessage("You say " + word + " and I fling you!");
    }
}