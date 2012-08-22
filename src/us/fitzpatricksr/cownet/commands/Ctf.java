package us.fitzpatricksr.cownet.commands;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.Wool;
import us.fitzpatricksr.cownet.commands.games.GameStats;
import us.fitzpatricksr.cownet.commands.games.TeamGame;

import java.util.Random;

/**
 */
public class Ctf extends TeamGame implements org.bukkit.event.Listener {
	private final Random rand = new Random();

	@Setting
	private int sizeOfHomeBase = 5;
	// manual setting...some day
	private ItemStack flagBlockColors[] = new ItemStack[] {
			new ItemStack(Material.AIR, 1),
			new ItemStack(Material.WOOL, 1, new Wool(DyeColor.BLUE).getData()),
			new ItemStack(Material.WOOL, 1, new Wool(DyeColor.RED).getData()),
	};
	private Flag[] flags = new Flag[getTeamCount()];
	private GameStats tempStats;


	// --------------------------------------------------------------
	// ---- Settings management

	@Override
	protected String getGameName() {
		return "CaptureTheFlag";
	}

	protected String[] getTeamNames() {
		return new String[] {
				"Spectator",
				"Red",
				"Blue"
		};
	}

	@Override
	protected String[] getHelpText(CommandSender player) {
		return new String[] {
				"usage: /" + getTrigger() + " join | info | quit | tp <player> | start",
				"   join - join the games",
				"   info - what's the state of the current game?",
				"   quit - chicken out and just watch",
				"   tp <player> - transport to a player, if you're a spectator",
				"   start - just get things started already!",
				"   scores - how players did last game.",
				"   stats <player> - see someone's lifetime stats.",
				"   leaders <kills | deaths | bombs | stealth | accuracy> - Global rank in a category."
		};
	}

	// --------------------------------------------------------------
	// ---- user commands

	@CowCommand
	private boolean doScores(CommandSender sender) {
		if (tempStats != null) {
		}
		return true;
	}

	@CowCommand
	private boolean doStats(Player player) {
		return doStats(player, player.getName());
	}

	@CowCommand
	private boolean doStats(CommandSender player, String playerName) {
		return true;
	}

	// --------------------------------------------------------------
	// ---- event handling

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		debugInfo("BlockBreakEvent");
		if (!isGameInProgress()) return;
		Player player = event.getPlayer();
		int team = getPlayerTeam(player.getName());
		Location blockLocation = event.getBlock().getLocation();
		for (int i = 0; i < getTeamCount(); i++) {
			if (flags[i].isFlagBlock(blockLocation)) {
				if (getPlayerTeam(player.getName()) > 0) {
					// OK, it's a flag block.  If it's for another team, give it to the player.
					if (flags[i].getTeam() != team) {
						flags[i].setOwner(player.getName());
					}
				}
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMoved(PlayerMoveEvent event) {
		debugInfo("BlockBreakEvent");
		if (!isGameInProgress()) return;
		// check to see if the player has delivered the flag to their base.
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (getPlayerTeam(playerName) > 0) {
			for (int i = 0; i < getTeamCount(); i++) {
				if (flags[i].getOwner().equals(playerName)) {
					// OK, this player owns a flag
					Location to = event.getTo();
					Location spawn = getPlayerSpawnPoint(playerName);
					double distance = to.distance(spawn);
					if (distance < sizeOfHomeBase) {
						// they win!
						for (int j = 0; j < getTeamCount(); j++) {
							flags[j].clear();
						}
						broadcastToAllOnlinePlayers("The " + getTeamNames()[i] + " WINS!");
					}
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		debugInfo("PlayerDeathEvent");
		if (!isGameInProgress()) return;
		Player player = event.getEntity();
		for (int i = 0; i < getTeamCount(); i++) {
			if (flags[i].getOwner().equals(player.getName())) {
				flags[i].setOwner(null);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		debugInfo("PlayerQuitEvent");
		if (!isGameInProgress()) return;
		Player player = event.getPlayer();
		for (int i = 0; i < getTeamCount(); i++) {
			if (flags[i].getOwner().equals(player.getName())) {
				flags[i].setOwner(null);
			}
		}
	}

	// --------------------------------------------------------------
	// ---- game state transitions

	@Override
	protected void handleGathering() {
		super.handleGathering();
	}

	@Override
	protected void handleLounging() {
		super.handleLounging();
		// might as well set up the arena here, since the players are out of the way.
	}

	@Override
	protected void handleInProgress() {
		super.handleInProgress();
		// generate the flag blocks for each team.
		for (int i = 1; i < getTeamCount(); i++) {
			flags[i] = new Flag(i);
			flags[i].setOwner(null);
		}
	}

	@Override
	protected void handleEnded() {
		// dump scores, clean up arena
		super.handleEnded();
	}

	@Override
	protected void handleFailed() {
		// clean up arena
		super.handleFailed();
	}

	@Override
	protected void handlePlayerAdded(String playerName) {
		super.handlePlayerAdded(playerName);
	}

	@Override
	protected void handlePlayerLeft(String playerName) {
		// remove the player's armor
		// if that player had the flag, restore it to the spawn point
		super.handlePlayerLeft(playerName);
	}

	protected void handlePlayerJoinedTeam(String playerName, int oldTeam, int newTeam) {
		// give the player the proper team armor
		setArmor(playerName, newTeam, true);
	}

	// --------------------------------------------------------------
	// ---- Stats

	private void accumulatStats(String playerName, String statName, int amount) {
		getHistoricStats().accumulate(playerName, statName, amount);
		tempStats.accumulate(playerName, statName, amount);
	}

	// --------------------------------------------------------------
	// ---- Flag management

	private class Flag {
		private String ownerName;
		private int team;

		public Flag(int team) {
			this.team = team;
		}

		/* return the team that owns this flag */
		public int getTeam() {
			return team;
		}

		/* return the player currently holding this flag.  Null if at flag position */
		public String getOwner() {
			return ownerName;

		}

		/* Change the owner of this flag.  */
		public void setOwner(String playerName) {
			if (ownerName != null) {
				setHat(ownerName, 0, false);
			}
			ownerName = playerName;
			if (ownerName != null) {
				setHat(ownerName, 0, false);
			} else {
				// no new owner, so move the block back to the block spawn point.
				Location spawnPoint = getTeamSpawnPoint(getTeam());
				spawnPoint.getWorld().dropItem(spawnPoint, flagBlockColors[getTeam()]);
			}
		}

		public void clear() {
			if (ownerName != null) {
				setHat(ownerName, 0, false);
			}
			getTeamSpawnPoint(getTeam()).getBlock().setType(Material.AIR);
			ownerName = null;
		}

		public boolean isFlagBlock(Location blockLocation) {
			return blockLocation.equals(getTeamSpawnPoint(getTeam()));
		}

		private void setHat(String playerName, int team, boolean saveOldHat) {
			if (team == 0) return;
			Player player = getPlayer(playerName);
			PlayerInventory inv = player.getInventory();
			ItemStack helmet = inv.getArmorContents()[3]; // getHelmet() wouldn't give me non-helmet blocks
			// if they already have a helmet, remove it and put it in their inventory
			if (helmet != null && helmet.getType() != Material.AIR && saveOldHat) {
				inv.addItem(helmet);
			}
			inv.setHelmet(flagBlockColors[team]);
		}
	}

	private void setArmor(String playerName, int team, boolean saveOldArmor) {
		if (team == 0) return;
		Player player = getPlayer(playerName);
		PlayerInventory inv = player.getInventory();
		ItemStack armor = inv.getArmorContents()[2]; // getArmor() wouldn't give me non-helmet blocks
		// if they already have a helmet, remove it and put it in their inventory
		if (armor != null && armor.getType() != Material.AIR && saveOldArmor) {
			inv.addItem(armor);
		}
		inv.setChestplate(flagBlockColors[team]);
	}

}
