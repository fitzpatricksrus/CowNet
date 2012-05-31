package us.fitzpatricksr.cownet.commands.hungergames;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetConfig;
import us.fitzpatricksr.cownet.utils.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

public class GameHistory extends CowNetConfig {
	private static final int MAX_RECENT_WINNERS = 5;
	private LinkedList<String> recentWinners = new LinkedList<String>();
	private HashMap<String, Integer> playerWins = new HashMap<String, Integer>();
	private HashMap<String, Integer> playerLosses = new HashMap<String, Integer>();

	private transient String fileName;

	public GameHistory(JavaPlugin plugin, String fileName) {
		super(plugin);
		this.fileName = fileName;
	}

	@Override
	protected String getFileName() {
		return fileName;
	}

	public void registerWinFor(Player player) {
		String name = player.getDisplayName().toLowerCase();
		Integer i = playerWins.get(name);
		if (i == null) {
			playerWins.put(name, 1);
		} else {
			playerWins.put(name, 1 + i);
		}
		recentWinners.addFirst(name);
		if (recentWinners.size() > MAX_RECENT_WINNERS) {
			recentWinners.removeLast();
		}
		saveConfig();
	}

	public void registerLossFor(Player player) {
		String name = player.getDisplayName().toLowerCase();
		Integer i = playerLosses.get(name);
		if (i == null) {
			playerLosses.put(name, 1);
		} else {
			playerLosses.put(name, 1 + i);
		}
		saveConfig();
	}

	public int getPlayerWins(Player player) {
		return getPlayerWins(player.getDisplayName());
	}

	public int getPlayerWins(String player) {
		Integer i = playerWins.get(player.toLowerCase());
		return (i == null) ? 0 : i;
	}

	public int getPlayerLosses(Player player) {
		return getPlayerLosses(player.getDisplayName());
	}

	public int getPlayerLosses(String player) {
		Integer i = playerLosses.get(player.toLowerCase());
		return (i == null) ? 0 : i;
	}

	public double getPlayerAverage(Player player) {
		return getPlayerAverage(player.getDisplayName());
	}

	public double getPlayerAverage(String player) {
		double total = getPlayerWins(player) + getPlayerLosses(player);
		if (total == 0) {
			return 0;
		} else {
			return getPlayerWins(player) / total;
		}
	}

	public void dumpRecentHistory(CommandSender playerToDumpTo) {
		playerToDumpTo.sendMessage("Recent winners:" + StringUtils.flatten(recentWinners));
	}

	public void dumpLeaderBoard(CommandSender playerToDumpTo) {
		HashSet<String> players = new HashSet<String>();
		players.addAll(playerWins.keySet());
		players.addAll(playerLosses.keySet());
		LinkedList<String> result = new LinkedList<String>();
		result.addAll(players);
		Collections.sort(result, new Comparator<String>() {
			@Override
			public int compare(String s, String s1) {
				if (getPlayerAverage(s) < getPlayerAverage(s1)) {
					// better average
					return 1;
				} else if (getPlayerAverage(s) > getPlayerAverage(s1)) {
					// worse average
					return -1;
				} else {
					// same average, so favor person with most plays
					int sTotal = getPlayerWins(s) + getPlayerLosses(s);
					int s1Total = getPlayerWins(s1) + getPlayerLosses(s1);
					if (sTotal < s1Total) {
						return 1;
					}
					if (sTotal > s1Total) {
						return -1;
					} else {
						// OK, just do it in alphabetical order
						return s.compareTo(s1);
					}
				}
			}
		});
		playerToDumpTo.sendMessage("Leader board:");
		for (String playerName : result) {
			String avgString = Double.toString(getPlayerAverage(playerName) * 100);
			int totalPlays = getPlayerWins(playerName) + getPlayerLosses(playerName);
			playerToDumpTo.sendMessage("    " + StringUtils.fitToColumnSize(avgString, 5) + "% of " + StringUtils.fitToColumnSize(Integer.toString(totalPlays), 3) + ": " + playerName);
		}
	}

	public void loadConfig() throws IOException, InvalidConfigurationException {
		super.loadConfig();
		recentWinners = new LinkedList<String>();
		recentWinners.addAll(getStringList("recentWinners"));
		playerWins = new HashMap<String, Integer>();
		playerLosses = new HashMap<String, Integer>();
		if (get("playerWins") != null) {
			Map<String, Object> winsTemp = ((ConfigurationSection) get("playerWins")).getValues(false);
			for (String key : winsTemp.keySet()) {
				playerWins.put(key, (Integer) winsTemp.get(key));
			}
		}
		if (get("playerLosses") != null) {
			Map<String, Object> lossTemp = ((ConfigurationSection) get("playerLosses")).getValues(false);
			for (String key : lossTemp.keySet()) {
				playerLosses.put(key, (Integer) lossTemp.get(key));
			}
		}
	}

	public void saveConfig() {
		set("recentWinners", recentWinners);
		set("playerWins", playerWins);
		set("playerLosses", playerLosses);
		try {
			super.saveConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
