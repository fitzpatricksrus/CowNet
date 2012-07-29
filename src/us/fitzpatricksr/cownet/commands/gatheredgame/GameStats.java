package us.fitzpatricksr.cownet.commands.gatheredgame;

import java.util.Map;
import java.util.Set;

public interface GameStats {
	public void accumulate(String playerName, String statsName, double value);

	public double getStat(String playerName, String statsName);

	public Map<String, Double> getStats(String playerName);

	public Set<String> getPlayerNames();

	// return map player -> stat
	public Map<String, Double> getStatSummary(String statName);
}
