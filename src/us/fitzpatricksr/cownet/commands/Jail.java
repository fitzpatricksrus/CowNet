package us.fitzpatricksr.cownet.commands;

import cosine.boseconomy.BOSEconomy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import us.fitzpatricksr.cownet.CowNetThingy;

import java.util.Random;

public class Jail extends CowNetThingy {
	@Setting
	private int escapeFee = 500;

	private BOSEconomy economy;
	private Random rand = new Random();

	@Override
	protected void onEnable() throws Exception {
		Plugin econ = getPlugin().getServer().getPluginManager().getPlugin("BOSEconomy");
		if (econ instanceof BOSEconomy) {
			this.economy = (BOSEconomy) econ;
			logInfo("Found BOSEconomy.  Plot economy enable.");
		} else {
			logInfo("Could not find BOSEconomy.");
		}
	}

	@Override
	protected String getHelpString(CommandSender sender) {
		return "usage: jail | jailbreak";
	}

	@CowCommand
	protected boolean doJail(Player player) {
		Location oldItemsLocation = player.getLocation();
		player.sendMessage("Your inventory has been placed in a chest at your last location.");
		player.sendMessage("You need " + escapeFee + " before you can escape.");
		player.sendMessage("When you think you have enough money, type /jailbreak.");
		Location spawn = player.getWorld().getSpawnLocation();
		String worldName = player.getWorld().getName();
		String baseNode = "worlds." + worldName;
		int x = getConfigValue(baseNode + ".X", spawn.getBlockX());
		int y = getConfigValue(baseNode + ".Y", spawn.getBlockY());
		int z = getConfigValue(baseNode + ".Z", spawn.getBlockZ());
		Location jailLocation = new Location(player.getWorld(), x, y, z);
		player.teleport(jailLocation);
		spawnGift(jailLocation);

		//save the player's inventory in a chest and clear it.
		Block c = oldItemsLocation.getBlock();
		c.setType(Material.CHEST);
		Chest chest = (Chest) c.getState();
		Inventory inv = chest.getInventory();
		inv.setContents(player.getInventory().getContents());
		player.getInventory().clear();
		return true;
	}

	private static EntityType[] types = new EntityType[] {
			EntityType.CHICKEN,
			EntityType.COW,
			EntityType.EGG,
			EntityType.OCELOT,
			EntityType.PIG,
			EntityType.SHEEP,
			EntityType.WOLF
	};

	private void spawnGift(Location loc) {
		int t = rand.nextInt(types.length);
		loc.add(0, 2, 0);
		loc.getWorld().spawnCreature(loc, types[t]);
	}

	@CowCommand
	protected boolean doJailbreak(Player player) {
		if (economy.getPlayerMoneyDouble(player.getName()) > escapeFee) {
			economy.addPlayerMoney(player.getName(), (double) -escapeFee, true);
			player.teleport(player.getWorld().getSpawnLocation());
			player.sendMessage("You've been charged " + escapeFee + " to escape.");
		}
		return true;
	}

	@CowCommand(permission = "jailor")
	protected boolean doJail(CommandSender sender, String playerName) {
		Player bad = getPlugin().getServer().getPlayer(playerName);
		if (bad != null) {
			bad.sendMessage("You've been sent to jail.");
			return doJail(bad);
		} else {
			sender.sendMessage("Player not found.");
			return true;
		}
	}

	@CowCommand(opOnly = true)
	protected boolean doJailSet(Player player) {
		Location jailLocation = player.getLocation();
		String worldName = player.getWorld().getName();
		String baseNode = "worlds." + worldName;
		updateConfigValue(baseNode + ".X", jailLocation.getBlockX());
		updateConfigValue(baseNode + ".Y", jailLocation.getBlockY());
		updateConfigValue(baseNode + ".Z", jailLocation.getBlockZ());
		saveConfiguration();
		player.sendMessage("New jail set for " + worldName);
		return true;
	}

	@CowCommand(opOnly = true)
	protected boolean doJailList(CommandSender player) {
		for (World w : getPlugin().getServer().getWorlds()) {
			String node = "worlds." + w.getName() + ".X";
			if (getConfigValue(node, Integer.MAX_VALUE) != Integer.MAX_VALUE) {
				player.sendMessage("  " + w.getName());
			}
		}
		return true;
	}
}

