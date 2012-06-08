package us.fitzpatricksr.cownet.commands;

import cosine.boseconomy.BOSEconomy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.utils.CowNetConfig;

import java.io.IOException;
import java.util.Random;

public class Jail extends CowNetThingy {
	@Setting
	private int escapeFee = 500;
	@Setting
	private String configFileName = "jail.yml";

	private Random rand = new Random();
	private CowNetConfig config;

	@Override
	protected String getHelpString(CommandSender sender) {
		return "usage: jail | jailbreak";
	}

	@Override
	protected void onEnable() throws IOException, InvalidConfigurationException {
		config = new CowNetConfig(getPlugin(), configFileName);
		config.setPathSeparator('/');
		config.loadConfig();
	}

	@Override
	public void onDisable() {
		config = null;
	}

	@CowCommand
	protected boolean doJail(Player player) {
		Location oldItemsLocation = player.getLocation();
		String playerNode = playerNodeName(player.getName());
		String worldNode = worldNodeName(player.getWorld().getName());
		if (config.getValue(playerNode, false)) {
			// player is already in jail
			player.sendMessage("You're already in jail!");
			player.sendMessage("Try /jailbreak");
		} else {
			player.sendMessage("Your inventory has been placed in a chest at your last location.");
			player.sendMessage("You need " + escapeFee + " " + currency() + " before you can escape.");
			player.sendMessage("When you think you have enough money, type /jailbreak.");
			Location spawn = player.getWorld().getSpawnLocation();
			Location jailLocation = getLocation(worldNode, spawn);
			player.teleport(jailLocation);
			spawnGift(jailLocation);

			//save the player's inventory in a chest and clear it.
			config.updateValue(playerNode, true);
			setLocation(playerNode, oldItemsLocation);
			/*			Block c = oldItemsLocation.getBlock();
						c.setType(Material.CHEST);
						Chest chest = (Chest) c.getState();
						Inventory inv = chest.getInventory();
						inv.setContents(player.getInventory().getContents()); */
			player.getInventory().clear();

			BOSEconomy economy = getEconomy();
			if (economy != null) {
				economy.setPlayerMoney(player.getName(), 0.0d, true);
			}
			saveState();
		}
		return true;
	}

	private static EntityType[] types = new EntityType[] {
			EntityType.CHICKEN,
			EntityType.COW,
			EntityType.PIG,
			EntityType.SHEEP,
	};

	private void spawnGift(Location loc) {
		int t = rand.nextInt(types.length);
		loc.add(0, 2, 0);
		loc.getWorld().spawnCreature(loc, types[t]);
	}

	@CowCommand
	protected boolean doJailbreak(Player player) {
		String playerNode = playerNodeName(player.getName());
		if (!config.getValue(playerNode, false)) {
			player.sendMessage("You aren't in jail.");
		} else {
			Location loc = getLocation(playerNode, player.getWorld().getSpawnLocation());
			BOSEconomy economy = getEconomy();
			if (economy != null) {
				if (economy.getPlayerMoneyDouble(player.getName()) >= escapeFee) {
					economy.addPlayerMoney(player.getName(), (double) -escapeFee, true);
					player.teleport(loc);
					player.sendMessage("You've been charged " + escapeFee + " " + currency() + " to escape.");
					config.updateValue(playerNode, false);
					saveState();
				} else {
					double needed = escapeFee - economy.getPlayerMoneyDouble(player.getName());
					player.sendMessage("You don't have enough money.  You need " + needed + " more " + currency());
				}
			} else {
				// no economy so it's a free for all!
				player.teleport(loc);
				config.updateValue(playerNode, false);
				saveState();
			}
		}
		return true;
	}

	@CowCommand(opOnly = true)
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

	@CowCommand
	protected boolean doJailInfo(Player player) {
		String playerNode = playerNodeName(player.getName());
		if (!config.getValue(playerNode, false)) {
			player.sendMessage("You aren't in jail.");
		} else {
			player.sendMessage("You are in jail.");
			BOSEconomy econ = getEconomy();
			if (econ != null) {
				double needed = escapeFee - econ.getPlayerMoneyDouble(player.getName());
				if (needed <= 0) {
					player.sendMessage("You have enough money to leave at any time.");
				} else {
					player.sendMessage("You need " + needed + " more " + currency() + " to leave.");
				}
			}
		}
		return true;
	}

	@CowCommand(opOnly = true)
	protected boolean doJailSet(Player player) {
		Location jailLocation = player.getLocation();
		String worldName = player.getWorld().getName();
		String baseNode = worldNodeName(worldName);
		setLocation(baseNode, jailLocation);
		saveState();
		player.sendMessage("New jail set for " + worldName);
		return true;
	}

	@CowCommand(opOnly = true)
	protected boolean doJailList(CommandSender player) {
		for (World w : getPlugin().getServer().getWorlds()) {
			String node = worldNodeName(w.getName()) + ".X";
			if (config.getValue(node, Integer.MAX_VALUE) != Integer.MAX_VALUE) {
				player.sendMessage("  " + w.getName());
			}
		}
		return true;
	}

	private String currency() {
		return getEconomy().getMoneyNamePlural();
	}

	private BOSEconomy getEconomy() {
		Plugin econ = getPlugin().getServer().getPluginManager().getPlugin("BOSEconomy");
		if (econ instanceof BOSEconomy) {
			return (BOSEconomy) econ;
		} else {
			return null;
		}
	}

	private String playerNodeName(String playerName) {
		return "players/" + playerName;
	}

	private String worldNodeName(String worldName) {
		return "worlds/" + worldName;
	}

	private boolean hasLocation(String name) {
		return config.hasValue(name + ".X");

	}

	private Location getLocation(String name, Location defaultValue) {
		int x = config.getValue(name + ".X", defaultValue.getBlockX());
		int y = config.getValue(name + ".Y", defaultValue.getBlockY());
		int z = config.getValue(name + ".Z", defaultValue.getBlockZ());
		return new Location(defaultValue.getWorld(), x, y, z);
	}

	public void setLocation(String name, Location value) {
		config.updateValue(name + ".X", value.getBlockX());
		config.updateValue(name + ".Y", value.getBlockY());
		config.updateValue(name + ".Z", value.getBlockZ());
	}

	private void saveState() {
		try {
			config.saveConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

