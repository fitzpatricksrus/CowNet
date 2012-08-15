package us.fitzpatricksr.cownet.commands.games;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 */
public class TeamGame extends GatheredGame implements Listener {
	private final Random rand = new Random();

	@Setting
	private int minPlayers = 2;
	@Setting
	private boolean forceBalanceTeams = true;
	@Setting
	private String loungeWarpName = "teamLounge-";
	@Setting
	private String spawnWarpName = "teamSpawn-";
	@Setting
	private int spawnJiggle = 5;
	@Setting
	private int maxTeamsOutOfBalance = 1;

	// map from playername -> team
	private HashMap<String, Integer> teamOfPlayer;

	// map from team -> playerName[]
	private HashMap<Integer, HashSet<String>> playersOnTeam;

	// --------------------------------------------------------------
	// ---- Settings management

	protected String[] getTeamNames() {
		return new String[] {
				"Spectator",
				"Red",
				"Blue"
		};
	}

	protected final int getTeamCount() {
		return getTeamNames().length;
	}

	@Override
	protected String getGameName() {
		return "TeamGame";
	}

	@Override
	protected int getMinPlayers() {
		return minPlayers;
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

	//------------------------------------------------
	//  command handlers
	//

	/*
	Print what team a player is on.  /game team
	 */
	@CowCommand
	private boolean doTeam(Player player) {
		return doTeam(player, player.getName());
	}

	/*
	Print what team a player is on.  /game team <playerName>
	 */
	@CowCommand
	private boolean doTeam(CommandSender player, String playerName) {
		if (getActivePlayers().contains(playerName)) {
			int team = getPlayerTeam(playerName);
			if (team == 0) {
				player.sendMessage("You are not on a team.");
			} else {
				player.sendMessage("You are on the " + getTeamNames()[team] + " team.");
			}
		} else {
			player.sendMessage("You are not currently in the game.");
		}
		return true;
	}

	/*
	Set the team a player is on.  /game join team red
	 */
	@CowCommand
	private boolean doJoinTeam(Player player, String teamName) {
		String playerName = player.getName();
		if (getActivePlayers().contains(playerName)) {
			try {
				int newTeam = getTeamByName(teamName);
				if (canPlayerJoinTeam(playerName, newTeam)) {
					setPlayerTeam(playerName, newTeam);
					player.sendMessage("You are now on the " + getTeamNames()[newTeam] + " team.");
				} else {
					player.sendMessage("Sorry, that would make the teams unbalanced.");
				}
			} catch (IllegalArgumentException e) {
				player.sendMessage("There is no team with that name.");
			}
		} else {
			player.sendMessage("You are not currently in the game.");
		}
		return true;
	}

	// --------------------------------------------------------------
	// ---- team management

	private int getTeamByName(String teamName) {
		String[] names = getTeamNames();
		for (int ndx = 0; ndx < names.length; ndx++) {
			if (names[ndx].equalsIgnoreCase(teamName)) {
				return ndx;
			}
		}
		return 0;
	}

	private Set<String> getTeamMembers(int ndx) {
		return playersOnTeam.get(ndx);
	}

	private int getPlayerTeam(String playerName) {
		if (teamOfPlayer.containsKey(playerName)) {
			return teamOfPlayer.get(playerName);
		} else {
			return 0;
		}
	}

	private boolean canPlayerJoinTeam(String playerName, int newTeam) {
		if (isGameInProgress()) {
			if (getPlayerTeam(playerName) > 0) {
				// you can't change teams once the games have started
				return false;
			} else {
				// you can only join a team who's size is < average size
				// we ignore spectators here.
				// this should only be hit by new players joining a game in progress
				int totalPlayersOnTeams = 0;
				for (int i = 1; i < getTeamCount(); i++) {
					totalPlayersOnTeams += getTeamMembers(i).size();
				}
				double averageSize = totalPlayersOnTeams / (getTeamCount() - 1);
				return getTeamMembers(newTeam).size() < averageSize;
			}
		} else {
			// do whatever you want before the game starts or after it ends.
			return true;
		}
	}

	private boolean setPlayerTeam(String playerName, int newTeam) {
		int currentTeam = getPlayerTeam(playerName);
		if (currentTeam == newTeam) return true;

		if (canPlayerJoinTeam(playerName, newTeam)) {
			getTeamMembers(currentTeam).remove(playerName);
			getTeamMembers(newTeam).add(playerName);
			teamOfPlayer.put(playerName, newTeam);
			handlePlayerJoinedTeam(playerName, currentTeam, newTeam);
			return true;
		} else {
			return false;
		}
	}

	private int addPlayerToRandomTeam(String playerName) {
		if (getPlayerTeam(playerName) == 0) {
			int newTeam = 1;
			for (int i = 2; i < getTeamCount(); i++) {
				if (getTeamMembers(i).size() < getTeamMembers(newTeam).size()) {
					newTeam = i;
				}
			}
			setPlayerTeam(playerName, newTeam);
		}
		return getPlayerTeam(playerName);
	}

	private void resetTeams() {
		// generate empty team data structures here
		teamOfPlayer = new HashMap<String, Integer>();
		playersOnTeam = new HashMap<Integer, HashSet<String>>();
		for (int i = 0; i < getTeamCount(); i++) {
			playersOnTeam.put(i, new HashSet<String>());
		}
	}

	protected void handlePlayerJoinedTeam(String playerName, int oldTeam, int newTeam) {
		// called when a player's team is set or changed.
	}

	// --------------------------------------------------------------
	// ---- game state transitions

	@Override
	protected void handleGathering() {
		debugInfo("handleGathering");
		broadcastToAllOnlinePlayers("A game is gathering.  To join use /" + getTrigger() + " join.");
		resetTeams();
	}

	@Override
	protected void handleLounging() {
		debugInfo("handleLounging");
		broadcastToAllOnlinePlayers("All the players are ready.  The games are about to start.");
		// put players on random team
		for (String playerName : getActivePlayers()) {
			loungeAPlayer(playerName);
			broadcastToAllOnlinePlayers(playerName + " is on the " + getPlayerTeam(playerName) + " team.");
		}
		for (String playerName : getActivePlayers()) {
			getPlayer(playerName).sendMessage("** You are on the " + getPlayerTeam(playerName) + " team.");
		}
	}

	@Override
	protected void handleInProgress() {
		debugInfo("handleInProgress");

		//TODO hey jf - we need to make sure the teams are still balanced here.

		broadcastToAllOnlinePlayers("Let the games begin!");
		for (String playerName : getActivePlayers()) {
			spawnAPlayer(playerName);
		}
	}

	@Override
	protected void handleEnded() {
		debugInfo("handleEnded");
		broadcastToAllOnlinePlayers("The game has ended.");
		resetTeams();
	}

	@Override
	protected void handleFailed() {
		debugInfo("handleFailed");
		broadcastToAllOnlinePlayers("The game has been canceled.");
		resetTeams();
	}

	@Override
	protected void handlePlayerAdded(String playerName) {
		// just add anyone who wants to be added
		debugInfo("handlePlayerAdded");
		if (isGameGathering()) {
			// don't do anything if we're still gather players.  We don't assign teams
			// until we're lounging.
		} else if (isGameEnded()) {
			// once the game is over, you're a spectator.  Do we even care?
			setPlayerTeam(playerName, 0);
		} else if (getPlayerTeam(playerName) == 0) {
			int team = addPlayerToRandomTeam(playerName);
			String teamName = getTeamNames()[getPlayerTeam(playerName)];
			broadcastToAllOnlinePlayers(playerName + " has joined the " + teamName + " team.");
			getPlayer(playerName).sendMessage("You are on the " + teamName + " team.");
			if (isGameLounging()) {
				loungeAPlayer(playerName);
			} else if (isGameInProgress()) {
				spawnAPlayer(playerName);
			}
		} else {
			String teamName = getTeamNames()[getPlayerTeam(playerName)];
			broadcastToAllOnlinePlayers(playerName + " has re-joined the " + teamName + " team.");
			getPlayer(playerName).sendMessage("You are on the " + teamName + " team.");
		}
	}

	@Override
	protected void handlePlayerLeft(String playerName) {
		// should remove their bombs
		// remove that player's tnt
		debugInfo("handlePlayerLeft");
		broadcastToAllOnlinePlayers(playerName + " has left the game.");
		// we don't remove the player from the team so that if they rejoin,
		// they will rejoin the same team they were on.
	}

	protected final void loungeAPlayer(String playerName) {
		Location loc = getPlayerLoungePoint(playerName);
		if (loc != null) {
			Player player = getPlayer(playerName);
			player.teleport(loc);
		} else {
			// hey jf - what happens if they aren't on a team.  How did they get here?
			logInfo("OMG!  There's a player without a team trying to lounge.");
		}
	}

	protected final void spawnAPlayer(String playerName) {
		Location loc = getPlayerSpawnPoint(playerName);
		if (loc != null) {
			Player player = getPlayer(playerName);
			player.teleport(loc);
		} else {
			// hey jf - what happens if they aren't on a team.  How did they get here?
			logInfo("OMG!  There's a player without a team trying to spawn.");
		}
	}

	protected final Location getTeamSpawnPoint(int team) {
		return getWarpPoint(spawnWarpName + getTeamNames()[team], 0);
	}

	protected final Location getPlayerSpawnPoint(String playerName) {
		int team = getPlayerTeam(playerName);
		Location loc = getTeamSpawnPoint(team);
		return jigglePoint(loc, spawnJiggle);
	}

	protected final Location getTeamLoungePoint(int team) {
		return getWarpPoint(loungeWarpName + getTeamNames()[team], 0);
	}

	protected final Location getPlayerLoungePoint(String playerName) {
		Player player = getPlayer(playerName);
		int team = getPlayerTeam(playerName);
		Location loc = getTeamLoungePoint(team);
		return jigglePoint(loc, spawnJiggle);
	}

	// --------------------------------------------------------------
	// ---- Event handlers

	@EventHandler(ignoreCancelled = true)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (!isGameInProgress()) return;
		// register a loss and teleport back to spawn point
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (playerIsAlive(playerName)) {
			Location loc = getPlayerSpawnPoint(playerName);
			if (loc != null) {
				// Just teleport the person back to spawn here.
				// losses and announcements are done when the player is killed.
				event.setRespawnLocation(loc);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		debugInfo("PlayerQuitEvent");
		String playerName = event.getPlayer().getName();
		// remove player from game.  this will cause handlePlayerLeft to be called.
		removePlayerFromGame(playerName);
	}
}
