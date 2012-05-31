package us.fitzpatricksr.cownet.commands;

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.CowNetThingy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

public class Logins extends CowNetThingy implements Listener {
	enum Filter {
		IN,
		OUT,
		INOUT,
		CHAT,
		ALL;

		public boolean shouldDisplayEntry(LogEntry entry, String filter) {
			return ((this == ALL) || (this == IN || this == INOUT) && entry.isJoin()) ||
					((this == OUT || this == INOUT) && entry.isQuit()) ||
					((this == CHAT) && entry.isChat(filter));
		}
	}

	private static final String JOIN_STRING = "<JOIN>";
	private static final String QUIT_STRING = "<QUIT>";
	@Setting
	private int maxQueueSize = 5000;
	@Setting
	private int maxDisplayedToPlayer = 10;
	private HashMap<String, GameMode> gameModeSave = new HashMap<String, GameMode>();
	private PrintWriter log;
	private LinkedList<LogEntry> recentLogEntries = new LinkedList<LogEntry>();

	public Logins(JavaPlugin plugin, String permissionRoot) {
		super(plugin, permissionRoot);
	}

	@Override
	protected void onEnable() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(getLogFile()));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				LogEntry entry = new LogEntry(line);
				queueEntry(entry);
			}
			reader.close();
			logInfo("Restored " + recentLogEntries.size() + " log entries");
			log = new PrintWriter(new BufferedWriter(new FileWriter(getLogFile(), true)));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDisable() {
		if (log != null) {
			log.close();
			log = null;
		}
	}

	@Override
	protected String getHelpString(CommandSender sender) {
		return "usage: <command> [-in|-out|-inout] | [<filter>]";
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerLogin(PlayerLoginEvent event) {
		if (log != null) {
			LogEntry entry = new LogEntry().forUser(event.getPlayer()).forMessage(JOIN_STRING);
			log.println(entry.toString());
			log.flush();
			queueEntry(entry);
		}
		setGameMode(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (log != null) {
			LogEntry entry = new LogEntry().forUser(event.getPlayer()).forMessage(QUIT_STRING);
			log.println(entry.toString());
			log.flush();
			queueEntry(entry);
		}
		saveGameMode(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChat(PlayerChatEvent event) {
		if (event.isCancelled()) return;
		if (log != null) {
			LogEntry entry = new LogEntry().forUser(event.getPlayer()).forMessage(event.getMessage());
			log.println(entry.toString() + (event.isCancelled() ? " (CANCELED)" : ""));
			log.flush();
			queueEntry(entry);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
		if (event.isCancelled()) return;
		if (log != null) {
			LogEntry entry = new LogEntry().forUser(event.getPlayer()).forMessage(event.getMessage());
			log.println(entry.toString());
			log.flush();
			queueEntry(entry);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerCommand(PlayerGameModeChangeEvent event) {
		if (event.isCancelled()) return;
		if (log != null) {
			LogEntry entry = new LogEntry().forUser(event.getPlayer()).forMessage("set game mode: " + event.getNewGameMode());
			log.println(entry.toString());
			log.flush();
			queueEntry(entry);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (event.isCancelled()) return;
		if (log != null) {
			LogEntry entry = new LogEntry().forUser(event.getPlayer()).forMessage("Teleported: " + event.getPlayer().getWorld().getName());
			log.println(entry.toString());
			log.flush();
			queueEntry(entry);
		}
	}

	@CowCommand(opOnly = true)
	protected boolean doLogins(CommandSender player) {
		return doLogins(player, "-inout");
	}

	@CowCommand(opOnly = true)
	protected boolean doLogins(CommandSender player, String filterString) {
		Filter filterType;
		// it's -in, -out, or a filter
		if (filterString.equalsIgnoreCase("-in")) {
			filterType = Filter.IN;
		} else if (filterString.equalsIgnoreCase("-out")) {
			filterType = Filter.OUT;
		} else if (filterString.equalsIgnoreCase("-inout")) {
			filterType = Filter.INOUT;
		} else if (filterString.equalsIgnoreCase("-all")) {
			filterType = Filter.ALL;
		} else {
			filterType = Filter.CHAT;
		}

		int count = 0;
		for (LogEntry entry : recentLogEntries) {
			if (filterType.shouldDisplayEntry(entry, filterString)) {
				if (player instanceof Player) {
					player.sendMessage(entry.toHumanReadableString());
					if (++count > maxDisplayedToPlayer) return true;
				} else {
					logInfo(entry.toHumanReadableString());
				}
			}
		}

		return true;
	}

	private File getLogFile() throws IOException {
		File folder = getPlugin().getDataFolder();
		if (!folder.exists()) {
			folder.mkdir();
		}
		File file = new File(folder, "ChatLog-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".txt");
		if (!file.exists()) {
			file.createNewFile();
		}
		return file;
	}

	private void queueEntry(LogEntry e) {
		recentLogEntries.addLast(e);
		if (recentLogEntries.size() > maxQueueSize) {
			recentLogEntries.removeFirst();
		}
	}

	private static class LogEntry {
		private static DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		private String playerName;
		private String worldName;
		private String time;
		private String message;

		public LogEntry() {
			time = dateFormat.format(new Date());
		}

		public LogEntry(String line) {
			fromString(line);
		}

		public LogEntry forUser(Player p) {
			playerName = p.getName();
			worldName = p.getWorld().getName();
			return this;
		}

		public LogEntry forMessage(String message) {
			this.message = message;
			return this;
		}

		public void fromString(String line) {
			String[] args = line.split("\\|");
			time = args[0];
			worldName = args[1];
			playerName = args[2];
			message = args[3];
		}

		public String toString() {
			return time + "|" + worldName + "|" + playerName + "|" + message;
		}

		private String toHumanReadableString() {
			return time + " [" + worldName + "] " + playerName + ": " + message;
		}

		public boolean isJoin() {
			return JOIN_STRING.equalsIgnoreCase(message);
		}

		public boolean isQuit() {
			return QUIT_STRING.equalsIgnoreCase(message);
		}

		public boolean isChat() {
			return isChat(null);
		}

		public boolean isChat(String filter) {
			if (isJoin() || isQuit()) return false;
			return (filter == null) || toHumanReadableString().contains(filter) || filter.equals("-chat");
		}

		public String getMessage() {
			return message;
		}
	}

	//--------------------------------------------------
	private void setGameMode(Player player) {
		GameMode oldMode = gameModeSave.get(player.getPlayerListName());
		if (oldMode != null) {
			player.setGameMode(oldMode);
			logInfo("Restoring game mode for " + player.getPlayerListName() + " " + oldMode);
		}
	}

	private void saveGameMode(Player player) {
		gameModeSave.put(player.getPlayerListName(), player.getGameMode());
	}

}
