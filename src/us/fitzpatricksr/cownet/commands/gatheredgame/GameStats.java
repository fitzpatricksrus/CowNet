package us.fitzpatricksr.cownet.commands.gatheredgame;

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
This class keeps track of who is part of a game and who is not.
It also keeps track of the player history persistently.

A game is either started or unstarted.  When unstarted, players can
be freely added and removed without any stats implications.

When the game starts, the listener is called with all the players
participating and all players will have a game attributed to their
stats.  No additional players may be added once the game has started.

Any players explicitly removed from the game once it is started
will accumulate a loss.   All players still in the game when
it ends will accumulate a win.

Instances of this object can be reused.
 */
public class GameStats extends CowNetConfig {
	@CowNetThingy.Setting
	private static int maxRecentWinners = 5;

	/*
	This class is a general bag for stats.  It's main structure is a multi-level
	hash table that maps playerName->statKey->value
	It also has a single, fixed length list that can be used to keep track
	of recent winners/losers.
	 */

	private String fileName;

	public GameStats(JavaPlugin plugin, String fileName) {
		super(plugin);
		this.fileName = fileName;
	}

	@Override
	protected String getFileName() {
		return fileName;
	}

	public void accumulate(String playerName, String statsName, int value) {
		String configKey = configKeyFor(playerName, statsName);
		int oldValue = getConfigValue(configKey, 0);
		updateConfigValue(configKey, oldValue + value);
	}

	public int getStat(String playerName, String statsName) {
		String configKey = configKeyFor(playerName, statsName);
		return getConfigValue(configKey, 0);
	}

	public Map<String, Integer> getStats(String playerName) {
		ConfigurationSection stats = getConfigurationSection(configKeyFor(playerName));
		if (stats == null) {
			return Collections.EMPTY_MAP;
		} else {
			HashMap<String, Integer> result = new HashMap<String, Integer>();
			for (String key : stats.getKeys(false)) {
				result.put(key, stats.getInt(key, 0));
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

	public void addRecentWinner(String playerName) {
		LinkedList<String> recentWinners = new LinkedList<String>();
		recentWinners.addAll(getStringList("recentWinners", Collections.EMPTY_LIST));
		if (recentWinners.size() >= maxRecentWinners) {
			recentWinners.removeLast();
		}
		recentWinners.addFirst(playerName);
		updateConfigValue("recentWinners", recentWinners);
	}

	public List<String> getRecentWinners() {
		return getStringList("recentWinners", Collections.EMPTY_LIST);
	}

	public Map<String, Integer> getStatSummary(String statName) {
		HashMap<String, Integer> result = new HashMap<String, Integer>();
		for (String playerName : getPlayerNames()) {
			if (hasConfigValue(configKeyFor(playerName, statName))) {
				result.put(playerName, getStat(playerName, statName));
			}
		}
		return result;
	}

	private String configKeyFor(String playerName) {
		return "players." + playerName.toLowerCase();
	}

	private String configKeyFor(String playerName, String statName) {
		return configKeyFor(playerName) + "." + statName.toLowerCase();
	}
}
