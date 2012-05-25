package us.fitzpatricksr.cownet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetThingy;

import java.util.LinkedList;

public class Timber extends CowNetThingy {
	@Setting
	private String worlds = "ALL";

	public Timber(JavaPlugin plugin, String permissionRoot) {
		super(plugin, permissionRoot);
		if (isEnabled()) {
			reloadSettings();
			plugin.getServer().getPluginManager().registerEvents(new TreeListener(), plugin);
		}
	}

	@Override
	protected String getHelpString(CommandSender sender) {
		return "Timer doesn't have any useful commands.";
	}

	private class TreeListener implements Listener {
		@SuppressWarnings("unused")
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onBlockBreak(BlockBreakEvent event) {
			if (event.isCancelled()) return;
			String worldName = event.getPlayer().getWorld().getName().toLowerCase();
			if ((worlds.contains("ALL") || worlds.contains(worldName)) && (!worlds.contains("-" + worldName))) {
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
