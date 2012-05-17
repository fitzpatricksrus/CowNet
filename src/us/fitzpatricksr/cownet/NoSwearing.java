package us.fitzpatricksr.cownet;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.noswearing.CreepThemOut;
import us.fitzpatricksr.cownet.noswearing.Launch;
import us.fitzpatricksr.cownet.noswearing.Lightning;
import us.fitzpatricksr.cownet.noswearing.SetOnFire;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class NoSwearing implements Listener {
    private Random rand = new Random();
    private Logger logger = Logger.getLogger("Minecraft");
    private String permissionNode;
    private String[] bannedPhrases;
    private Consequence[] consequences;

    public NoSwearing(JavaPlugin plugin, String permissionRoot, String trigger) {
        FileConfiguration config = plugin.getConfig();
        if (config.getBoolean(trigger + ".enable")) {
            this.permissionNode = permissionRoot + "." + trigger + ".allowed";
            String fileName = config.getString(trigger + "badwords.txt", "badwords.txt");
            bannedPhrases = loadBadWords(plugin, fileName, trigger);
            consequences = new Consequence[]{
                    new SetOnFire(plugin, trigger),
                    new CreepThemOut(plugin, trigger),
                    new Launch(plugin, trigger),
                    new Lightning(plugin, trigger)
            };
            PluginManager pm = plugin.getServer().getPluginManager();
            pm.registerEvents(this, plugin);
            logger.info(trigger + " enable");
        } else {
            logger.info("CowNet - " + trigger + ".enable: false");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(permissionNode)) {
            return;
        }

        if (scanForBadWords(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }

    public boolean scanForBadWords(Player player, String textToScan) {
        String text = textToScan.toLowerCase();
        for (String phrase : bannedPhrases) {
            if (text.matches(phrase)) {
                String word = phrase.replace("(", "").replace(")", "").replace(".*", "");
                consequences[rand.nextInt(consequences.length)].handleBadWord(player, word);
                return true;
            } else {
                //logger.info("Didn't see word '"+bannedPhrase+"'");
            }
        }
        return false;
    }

    // interface for all consequences
    public static interface Consequence {
        public void handleBadWord(Player player, String word);
    }

    private String[] loadBadWords(JavaPlugin plugin, String fileName, String trigger) {
        List<String> words = new LinkedList<String>();
        try {
            InputStream inStream = plugin.getResource(fileName);
            BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
            String line = in.readLine();
            while (line != null) {
                words.add(line.toLowerCase());
                line = in.readLine();
            }
            return words.toArray(new String[words.size()]);
        } catch (IOException e) {
            e.printStackTrace();
            logger.severe(trigger + " could not load bad word list.");
            return new String[0];
        }
    }
}
