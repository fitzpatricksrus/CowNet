package us.fitzpatricksr.cownet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetThingy;
import us.fitzpatricksr.cownet.utils.SettingsTwiddler;

import java.util.LinkedList;

public class Timber extends CowNetThingy {
    @SettingsTwiddler.Setting
    private String worlds = "ALL";

    public Timber(JavaPlugin plugin, String permissionRoot, String trigger) {
        super(plugin, permissionRoot, trigger);
        if (isEnabled()) {
            reload();
            plugin.getServer().getPluginManager().registerEvents(
                    new TreeListener(),
                    plugin);
        }
    }

    @Override
    public void reload() {
        worlds = getConfigString("worlds", worlds);
    }

    @Override
    protected String getHelpString(CommandSender sender) {
        return "usage: timber";
    }

    @Override
    protected boolean handleCommand(Player player, Command cmd, String[] args) {
        if (!hasPermissions(player)) {
            player.sendMessage("Sorry, you don't have permissions");
            return false;
        } else {
            player.sendMessage("Huh?  Srsly?  Commands for timber?  Chop down a tree you lazy slog!");
            return false;
        }
    }

    public boolean doStuff(CommandSender sender, Command cmd) {
        sender.sendMessage("Yup");
        return true;
    }

    public boolean doStuff(CommandSender sender, Command cmd, String arg1) {
        sender.sendMessage("Yup(" + arg1 + ")");
        return true;
    }

    public boolean doStuff(CommandSender sender, Command cmd, String arg1, String arg2) {
        sender.sendMessage("Yup(" + arg1 + "," + arg2 + ")");
        return true;
    }

    private class TreeListener implements Listener {
        @SuppressWarnings("unused")
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onBlockBreak(BlockBreakEvent event) {
            if (event.isCancelled()) return;
            String worldName = event.getPlayer().getWorld().getName().toLowerCase();
            if (worlds.equalsIgnoreCase("ALL") || worlds.contains(worldName)) {
                Block dropBlock = event.getBlock();
                Location dropLoc = dropBlock.getLocation();
                Location loc = dropLoc.clone();
                LinkedList<ItemStack> drops = new LinkedList<ItemStack>();
                while (loc.getBlock().getType() == Material.LOG) {
                    drops.addAll(loc.getBlock().getDrops());
                    loc.getBlock().setType(Material.AIR);
                    loc.setY(loc.getY() + 1);
                }
                if (drops.size() > 0) {
                    for (ItemStack drop : drops) {
                        dropBlock.getWorld().dropItemNaturally(dropLoc, drop);
                    }
                }
            }
        }
    }
}
