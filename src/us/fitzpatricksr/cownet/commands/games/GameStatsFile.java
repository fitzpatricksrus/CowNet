package us.fitzpatricksr.cownet.commands.games;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.utils.CowNetConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
	This class is a general bag for stats.  It's main structure is a multi-level
	hash table that maps playerName->statKey->value
	It also has a single, fixed length list that can be used to keep track
	of recent winners/losers.
*/
public class GameStatsFile extends CowNetConfig implements GameStats {
	@CowNetThingy.Setting
	private static int maxRecentWinners = 5;

	private String fileName;

	public GameStatsFile(JavaPlugin plugin, String fileName) {
		super(plugin);
		this.fileName = fileName;
	}

	@Override
	protected String getFileName() {
		return fileName;
	}

	public void accumulate(String playerName, String statsName, double value) {
		String configKey = configKeyFor(playerName, statsName);
		double oldValue = getConfigValue(configKey, 0);
		updateConfigValue(configKey, oldValue + value);
	}

	public double getStat(String playerName, String statsName) {
		String configKey = configKeyFor(playerName, statsName);
		return getConfigValue(configKey, 0d);
	}

	public Map<String, Double> getStats(String playerName) {
		ConfigurationSection stats = getConfigurationSection(configKeyFor(playerName));
		if (stats == null) {
			return Collections.EMPTY_MAP;
		} else {
			HashMap<String, Double> result = new HashMap<String, Double>();
			for (String key : stats.getKeys(false)) {
				result.put(key, stats.getDouble(key, 0));
			}
			return result;
		}
	}

	public Set<String> getPlayerNames() {
		ConfigurationSection stats = getConfigurationSection("players");
		if (stats == null) {
			return Collections.EMPTY_SET;
		} else {
			return stats.getKeys(false);
		}
	}

	public Map<String, Double> getStatSummary(String statName) {
		HashMap<String, Double> result = new HashMap<String, Double>();
		for (String playerName : getPlayerNames()) {
			if (hasConfigValue(configKeyFor(playerName, statName))) {
				result.put(playerName, getStat(playerName, statName));
			}
		}
		return result;
	}

	//-------------------------------------------------
	// leader boards

	public void addRecentWinner(String playerName) {
		addRecentLeader("recentWinners", playerName);
	}

	public void addRecentLeader(String leaderBoardName, String playerName) {
		LinkedList<String> recentWinners = new LinkedList<String>();
		recentWinners.addAll(getStringList(leaderBoardName, Collections.EMPTY_LIST));
		if (recentWinners.size() >= maxRecentWinners) {
			recentWinners.removeLast();
		}
		recentWinners.addFirst(playerName);
		updateConfigValue(leaderBoardName, recentWinners);
	}

	public List<String> getRecentWinners() {
		return getRecentLeaders("recentWinnders");
	}

	public List<String> getRecentLeaders(String leaderBoardName) {
		return getStringList(leaderBoardName, Collections.EMPTY_LIST);
	}

	public void clearRecentWinners() {
		clearRecentLeaders("recentWinnders");
	}

	public void clearRecentLeaders(String leaderBoardName) {
		updateConfigValue(leaderBoardName, Collections.EMPTY_LIST);
	}

	private String configKeyFor(String playerName) {
		return "players." + playerName.toLowerCase();
	}

	private String configKeyFor(String playerName, String statName) {
		return configKeyFor(playerName) + "." + statName.toLowerCase();
	}
}
