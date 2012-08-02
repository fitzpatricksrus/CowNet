package us.fitzpatricksr.cownet.commands.games;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GameStatsMemory implements GameStats {
	private HashMap<String, HashMap<String, Double>> stats = new HashMap<String, HashMap<String, Double>>();

	public void accumulate(String playerName, String stat, double amount) {
		getStats(playerName).put(stat, getStat(playerName, stat) + amount);
	}

	@Override
	public double getStat(String playerName, String statsName) {
		Map<String, Double> playerStats = getStats(playerName);
		Double stat = playerStats.get(statsName);
		if (stat == null) {
			return 0;
		} else {
			return stat;
		}
	}

	@Override
	public Map<String, Double> getStats(String playerName) {
		HashMap<String, Double> playerStats = stats.get(playerName);
		if (playerStats == null) {
			playerStats = new HashMap<String, Double>();
			stats.put(playerName, playerStats);
		}
		return playerStats;
	}

	@Override
	public Set<String> getPlayerNames() {
		return stats.keySet();
	}

	@Override
	public Map<String, Double> getStatSummary(String statName) {
		HashMap<String, Double> result = new HashMap<String, Double>();
		for (String playerName : getPlayerNames()) {
			result.put(playerName, getStat(playerName, statName));
		}
		return result;
	}
}