package us.fitzpatricksr.cownet.commands;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.fitzpatricksr.cownet.commands.games.GameStats;
import us.fitzpatricksr.cownet.commands.games.TeamGame;
import us.fitzpatricksr.cownet.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 */
public class Ctf extends TeamGame implements org.bukkit.event.Listener {
	private static final String KILLS_KEY = "kills";
	private static final String DEATHS_KEY = "deaths";
	private static final String BOMBS_PLACED_KEY = "bombsPlaced";
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
			sender.sendMessage("Scores for most recent game: ");
			for (String playerName : tempStats.getPlayerNames()) {
				double k = tempStats.getStat(playerName, KILLS_KEY);
				double d = tempStats.getStat(playerName, DEATHS_KEY);
				double b = tempStats.getStat(playerName, BOMBS_PLACED_KEY);
				double accuracy = (b != 0) ? k / b : 0;
				double stealth = (d != 0) ? k / d : k;
				sender.sendMessage("  " + StringUtils.fitToColumnSize(playerName, 10) + " accuracy = " + StringUtils.fitToColumnSize(Double.toString(accuracy), 5) + " stealth = " + StringUtils.fitToColumnSize(Double.toString(stealth), 5));
			}
		}
		return true;
	}

	@CowCommand
	private boolean doStats(Player player) {
		return doStats(player, player.getName());
	}

	@CowCommand
	private boolean doStats(CommandSender player, String playerName) {
		double k = getHistoricStats().getStat(playerName, KILLS_KEY);
		double d = getHistoricStats().getStat(playerName, DEATHS_KEY);
		double b = getHistoricStats().getStat(playerName, BOMBS_PLACED_KEY);
		double accuracy = (b != 0) ? k / b : 0;
		double stealth = (d != 0) ? k / d : k;
		player.sendMessage("Your stats: ");
		player.sendMessage("  kills: " + k);
		player.sendMessage("  deaths: " + d);
		player.sendMessage("  bombs: " + b);
		player.sendMessage("  accuracy = " + StringUtils.fitToColumnSize(Double.toString(accuracy * 100), 5) + "%");
		player.sendMessage("  stealth = " + StringUtils.fitToColumnSize(Double.toString(stealth * 100), 5) + "%");
		return true;
	}

	@CowCommand
	private boolean doLeadersKills(CommandSender sender) {
		sender.sendMessage("Top killers: ");
		dumpLeaders(sender, getHistoricStats().getStatSummary(KILLS_KEY));
		return true;
	}

	@CowCommand
	private boolean doLeadersDeaths(CommandSender sender) {
		sender.sendMessage("Most likely to die: ");
		dumpLeaders(sender, getHistoricStats().getStatSummary(DEATHS_KEY));
		return true;
	}

	@CowCommand
	private boolean doLeadersBombs(CommandSender sender) {
		sender.sendMessage("Top bombers: ");
		dumpLeaders(sender, getHistoricStats().getStatSummary(BOMBS_PLACED_KEY));
		return true;
	}

	@CowCommand
	private boolean doLeadersAccuracy(CommandSender sender) {
		sender.sendMessage("Most accurate: ");
		HashMap<String, Double> accuracy = new HashMap<String, Double>();
		for (String playerName : getHistoricStats().getPlayerNames()) {
			double k = getHistoricStats().getStat(playerName, KILLS_KEY);
			double b = getHistoricStats().getStat(playerName, BOMBS_PLACED_KEY);
			double a = (b != 0) ? k / b : 0;
			accuracy.put(playerName, a * 100);
		}
		dumpLeaders(sender, accuracy);
		return true;
	}

	@CowCommand
	private boolean doLeadersStealth(CommandSender sender) {
		sender.sendMessage("Top stealthy: ");
		HashMap<String, Double> stealth = new HashMap<String, Double>();
		for (String playerName : getHistoricStats().getPlayerNames()) {
			double k = getHistoricStats().getStat(playerName, KILLS_KEY);
			double d = getHistoricStats().getStat(playerName, DEATHS_KEY);
			double s = (d != 0) ? k / d : k;
			stealth.put(playerName, s * 100);
		}
		dumpLeaders(sender, stealth);
		return true;
	}

	private void dumpLeaders(CommandSender sender, Map<String, Double> map) {
		TreeMap<Double, String> sortedMap = new TreeMap<Double, String>();
		for (Map.Entry<String, Double> entry : map.entrySet()) {
			sortedMap.put(entry.getValue(), entry.getKey());
		}
		for (Map.Entry entry : sortedMap.entrySet()) {
			sender.sendMessage("  " + StringUtils.fitToColumnSize(entry.getValue().toString(), 15) + ": " + StringUtils.fitToColumnSize(entry.getKey().toString(), 5));
		}
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
	}

	@Override
	protected void handleInProgress() {
		super.handleInProgress();
	}

	@Override
	protected void handleEnded() {
		super.handleEnded();
	}

	@Override
	protected void handleFailed() {
		super.handleFailed();
	}

	@Override
	protected void handlePlayerAdded(String playerName) {
		super.handlePlayerAdded(playerName);
	}

	@Override
	protected void handlePlayerLeft(String playerName) {
		super.handlePlayerLeft(playerName);
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
		public Team getTeam() {

		}

		/* return the player currently holding this flag.  Null if at flag position */
		public String getOwner() {

		}

		/* Change the owner of this flag.  */
		public void setOwner(String playerName) {

		}
	}


}
