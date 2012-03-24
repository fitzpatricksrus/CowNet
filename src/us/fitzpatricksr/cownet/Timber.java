package us.fitzpatricksr.cownet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedList;

public class Timber extends CowNetThingy {
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
    }

    @Override
    protected String getHelpString(Player player) {
        return "usage: timber";
    }

    @Override
    protected boolean onCommand(Player player, Command cmd, String[] args) {
        if (!hasPermissions(player)) {
            player.sendMessage("Sorry, you don't have permissions");
            return false;
        } else {
            player.sendMessage("Huh?  Srsly?  Commands for timber?  Chop down a tree you lazy slog!");
            return false;
        }
    }

    private class TreeListener implements Listener {
        @SuppressWarnings("unused")
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onBlockBreak(BlockBreakEvent event) {
            if (event.isCancelled()) return;
            Block dropBlock = event.getBlock();
            Location dropLoc = dropBlock.getLocation();
            Location loc = dropLoc.clone();
            LinkedList<ItemStack> drops = new LinkedList();
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
