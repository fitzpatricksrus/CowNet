package us.fitzpatricksr.cownet.commands.games;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import us.fitzpatricksr.cownet.CowNetMod;
import us.fitzpatricksr.cownet.commands.CowWarp;

import java.util.Random;

/**
 */
public class TeamGame extends GatheredGame implements Listener {
	private enum Team {
		NONE,
		RED,
		BLUE
	}

	private final Random rand = new Random();

	@Setting
	private String redTeamName = "red";
	@Setting
	private String blueTeamName = "blue";
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

	private PlayerGameState redTeam = new PlayerGameState();
	private PlayerGameState blueTeam = new PlayerGameState();


	// --------------------------------------------------------------
	// ---- Settings management

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
	Print what team a player is on.
	 */
	@CowCommand
	private boolean doTeam(Player player) {
		String playerName = player.getName();
		if (getActivePlayers().contains(playerName)) {
			Team t = addPlayerToRandomTeam(playerName);
			player.sendMessage("You are on the " + t + " team.");
		} else {
			player.sendMessage("You are not currently in the game.");
		}
		return true;
	}

	/*
	Set the team a player is on.
	 */
	@CowCommand
	private boolean doTeam(Player player, String teamName) {
		String playerName = player.getName();
		if (getActivePlayers().contains(playerName)) {
			Team newTeam;
			try {
				newTeam = Team.valueOf(teamName.toLowerCase());
				if (canPlayerJoinTeam(playerName, newTeam)) {
					setPlayerTeam(playerName, newTeam);
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

	private Team getPlayerTeam(String playerName) {
		if (redTeam.getPlayers().contains(playerName)) {
			return Team.RED;
		} else if (blueTeam.getPlayers().contains(playerName)) {
			return Team.BLUE;
		} else {
			return Team.NONE;
		}
	}

	private boolean canPlayerJoinTeam(String playerName, Team newTeam) {
		if (!forceBalanceTeams || (newTeam == getPlayerTeam(playerName)) || (newTeam == Team.NONE)) return true;
		int newRedTeamSize = redTeam.getPlayers().size();
		int newBlueTeamSize = blueTeam.getPlayers().size();
		if (blueTeam.getPlayers().contains(playerName)) {
			// this player would need to leave the red team to join blue
			newBlueTeamSize = newBlueTeamSize - 1;
			newRedTeamSize = newRedTeamSize + 1;
		} else {
			newBlueTeamSize = newBlueTeamSize + 1;
			newRedTeamSize = newRedTeamSize - 1;
		}
		return (Math.abs(newBlueTeamSize - newRedTeamSize) <= maxTeamsOutOfBalance);
	}

	private boolean setPlayerTeam(String playerName, Team newTeam) {
		if (canPlayerJoinTeam(playerName, newTeam)) {
			Team currentTeam = getPlayerTeam(playerName);
			if (currentTeam != newTeam) {
				if (redTeam.getPlayers().contains(playerName)) {
					redTeam.removePlayer(playerName);
				} else if (blueTeam.getPlayers().contains(playerName)) {
					blueTeam.removePlayer(playerName);
				}
				if (newTeam == Team.RED) {
					redTeam.addPlayer(playerName);
				} else if (newTeam == Team.BLUE) {
					blueTeam.addPlayer(playerName);
				}
			}
			return true;
		} else {
			return false;
		}
	}

	private Team addPlayerToRandomTeam(String playerName) {
		Team t = getPlayerTeam(playerName);
		if (t != Team.NONE) {
			return t;
		} else {
			if (redTeam.getPlayers().size() < blueTeam.getPlayers().size()) {
				redTeam.addPlayer(playerName);
				return Team.RED;
			} else if (redTeam.getPlayers().size() > blueTeam.getPlayers().size()) {
				blueTeam.addPlayer(playerName);
				return Team.BLUE;
			} else if (rand.nextBoolean()) {
				redTeam.addPlayer(playerName);
				return Team.RED;
			} else {
				blueTeam.addPlayer(playerName);
				return Team.BLUE;
			}
		}
	}

	// --------------------------------------------------------------
	// ---- game state transitions

	@Override
	protected void handleGathering() {
		debugInfo("handleGathering");
		broadcastToAllOnlinePlayers("A game is gathering.  To join use /" + getTrigger() + " join.");
	}

	@Override
	protected void handleLounging() {
		debugInfo("handleLounging");
		broadcastToAllOnlinePlayers("All the players are ready.  The games are about to start.");
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
		broadcastToAllOnlinePlayers("Let the games begin!");
		for (String playerName : getActivePlayers()) {
			spawnAPlayer(playerName);
		}
	}

	@Override
	protected void handleEnded() {
		debugInfo("handleEnded");
		broadcastToAllOnlinePlayers("The game has ended.");
		redTeam.resetGame();
		blueTeam.resetGame();
	}

	@Override
	protected void handleFailed() {
		debugInfo("handleFailed");
		broadcastToAllOnlinePlayers("The game has been canceled.");
		redTeam.resetGame();
		blueTeam.resetGame();
	}

	@Override
	protected void handlePlayerAdded(String playerName) {
		// just add anyone who wants to be added
		debugInfo("handlePlayerAdded");
		Team team = addPlayerToRandomTeam(playerName);
		broadcastToAllOnlinePlayers(playerName + " has joined the " + team + " team.");
		if (isGameLounging()) {
			loungeAPlayer(playerName);
		} else if (isGameInProgress()) {
			spawnAPlayer(playerName);
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

	protected void loungeAPlayer(String playerName) {
		Location blueLounge = getWarpPoint(loungeWarpName + blueTeamName);
		Location redLounge = getWarpPoint(loungeWarpName + redTeamName);
		Player player = getPlayer(playerName);
		Team team = getPlayerTeam(playerName);
		if (team.equals(Team.RED)) {
			if (redLounge != null) {
				player.teleport(redLounge);
			}
		} else if (team.equals(Team.BLUE)) {
			if (blueLounge != null) {
				player.teleport(blueLounge);
			}
		} else {
			// hey jf - what happens if they aren't on a team.  How did they get here?
			logInfo("OMG!  There's a player without a team trying to lounge.");
		}
	}

	protected void spawnAPlayer(String playerName) {
		Location blueSpawn = getWarpPoint(spawnWarpName + blueTeamName);
		Location redSpawn = getWarpPoint(spawnWarpName + redTeamName);
		Player player = getPlayer(playerName);
		Team team = getPlayerTeam(playerName);
		if (team.equals(Team.RED)) {
			if (redSpawn != null) {
				player.teleport(redSpawn);
			}
		} else if (team.equals(Team.BLUE)) {
			if (blueSpawn != null) {
				player.teleport(blueSpawn);
			}
		} else {
			// hey jf - what happens if they aren't on a team.  How did they get here?
			logInfo("OMG!  There's a player without a team trying to spawn.");
		}
	}

	private Location getWarpPoint(String warpName) {
		CowNetMod plugin = (CowNetMod) getPlugin();
		CowWarp warpThingy = (CowWarp) plugin.getThingy("cowwarp");
		Location loc = warpThingy.getWarpLocation(warpName);
		if (loc != null) {
			if (spawnJiggle > 0) {
				int dx = rand.nextInt(spawnJiggle * 2 + 1) - spawnJiggle - 1; // -5..5
				int dz = rand.nextInt(spawnJiggle * 2 + 1) - spawnJiggle - 1; // -5..5
				loc.add(dx, 0, dz);
				loc = loc.getWorld().getHighestBlockAt(loc).getLocation();
				loc.add(0, 1, 0);
			}
		}
		return loc;
	}

	private Player getPlayer(String playerName) {
		Server server = getPlugin().getServer();
		return server.getPlayer(playerName);
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
			// Just teleport the person back to spawn here.
			// losses and announcements are done when the player is killed.
			Location blueSpawn = getWarpPoint(spawnWarpName + blueTeamName);
			Location redSpawn = getWarpPoint(spawnWarpName + redTeamName);
			Team t = getPlayerTeam(playerName);
			if (t == Team.RED) {
				if (redSpawn != null) {
					event.setRespawnLocation(redSpawn);
				}
			} else if (t == Team.BLUE) {
				if (blueSpawn != null) {
					event.setRespawnLocation(blueSpawn);
				}
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
