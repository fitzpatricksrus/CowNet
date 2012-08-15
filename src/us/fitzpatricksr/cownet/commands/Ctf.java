package us.fitzpatricksr.cownet.commands;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.fitzpatricksr.cownet.commands.games.GameStats;
import us.fitzpatricksr.cownet.commands.games.TeamGame;

import java.util.HashMap;
import java.util.Random;

/**
 */
public class Ctf extends TeamGame implements org.bukkit.event.Listener {
	private final Random rand = new Random();

	// manual setting
	private Material redFlagBlockType = Material.RED_MUSHROOM;
	// manual setting
	private Material blueFlagBlockType = Material.BROWN_MUSHROOM;
	private GameStats tempStats;


	// --------------------------------------------------------------
	// ---- Settings management

	@Override
	// reload any settings not handled by @Setting
	protected void reloadManualSettings() throws Exception {
		super.reloadManualSettings();
		this.redFlagBlockType = Material.matchMaterial(getConfigValue("redFlagBlockType", redFlagBlockType.toString()));
		this.blueFlagBlockType = Material.matchMaterial(getConfigValue("blueFlagBlockType", blueFlagBlockType.toString()));
	}

	// return any custom settings that are not handled by @Settings code
	protected HashMap<String, String> getManualSettings() {
		HashMap<String, String> result = super.getManualSettings();
		result.put("redFlagBlockType", redFlagBlockType.toString());
		result.put("blueFlagBlockType", blueFlagBlockType.toString());
		return result;
	}

	// update a setting that was not handled by @Setting and return true if it has been updated.
	protected boolean updateManualSetting(String settingName, String settingValue) {
		if (settingName.equalsIgnoreCase("redFlagBlockType")) {
			redFlagBlockType = Material.valueOf(settingValue);
			updateConfigValue("redFlagBlockType", settingValue);
			return true;
		} else if (settingName.equalsIgnoreCase("blueFlagBlockType")) {
			blueFlagBlockType = Material.valueOf(settingValue);
			updateConfigValue("blueFlagBlockType", settingValue);
			return true;
		} else {
			return super.updateManualSetting(settingName, settingValue);
		}
	}

	@Override
	protected String getGameName() {
		return "CaptureTheFlag";
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
	// ---- Settings management

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
		super.handlePlayerLeft(playerName);
		// if that player had the flag, restore it to the spawn point
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
		/* return the team that owns this flag */
		public int getTeam() {
			return 0;
		}

		/* return the player currently holding this flag.  Null if at flag position */
		public String getOwner() {
			return null;

		}

		/* Change the owner of this flag.  */
		public void setOwner(String playerName) {

		}
	}


}
