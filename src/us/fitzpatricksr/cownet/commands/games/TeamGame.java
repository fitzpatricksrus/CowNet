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

	;

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

	private PlayerGameState redTeam = new PlayerGameState(new RedPlayerListener());
	private PlayerGameState blueTeam = new PlayerGameState(new BluePlayerListener());


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

	// --------------------------------------------------------------
	// ---- team management

	private Team addPlayerToRandomTeam(String playerName) {
		try {
			if (redTeam.getPlayers().contains(playerName)) {
				return Team.RED;
			} else if (blueTeam.getPlayers().contains(playerName)) {
				return Team.BLUE;
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
		} catch (PlayerGameState.PlayerCantJoinException e) {
			return Team.NONE;
		}
	}

	private class BluePlayerListener implements PlayerGameState.Listener {
		@Override
		public void playerJoined(String playerName) throws PlayerGameState.PlayerCantJoinException {
			if (forceBalanceTeams) {
				// if this would unbalance the teams, reject it
				int newBlueTeamSize = blueTeam.getPlayers().size() + 1;
				int newRedTeamSize = redTeam.getPlayers().size();
				if (redTeam.getPlayers().contains(playerName)) {
					// this player would need to leave the red team to join blue
					newRedTeamSize = newRedTeamSize - 1;
				}
				if (Math.abs(newBlueTeamSize - newRedTeamSize) > 1) {
					throw new PlayerGameState.PlayerCantJoinException("The teams would be uneven");
				}
			}
		}

		@Override
		public void playerLeft(String playerName) {
		}
	}

	private class RedPlayerListener implements PlayerGameState.Listener {
		@Override
		public void playerJoined(String playerName) throws PlayerGameState.PlayerCantJoinException {
			if (forceBalanceTeams) {
				// if this would unbalance the teams, reject it
				int newRedTeamSize = redTeam.getPlayers().size() + 1;
				int newBlueTeamSize = blueTeam.getPlayers().size();
				if (blueTeam.getPlayers().contains(playerName)) {
					// this player would need to leave the red team to join blue
					newBlueTeamSize = newBlueTeamSize - 1;
				}
				if (Math.abs(newBlueTeamSize - newRedTeamSize) > 1) {
					throw new PlayerGameState.PlayerCantJoinException("The teams would be uneven");
				}
			}
		}

		@Override
		public void playerLeft(String playerName) {
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
	protected void handlePlayerAdded(String playerName) throws PlayerGameState.PlayerCantJoinException {
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
		Team team = addPlayerToRandomTeam(playerName);
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
		}
	}

	protected void spawnAPlayer(String playerName) {
		Location blueSpawn = getWarpPoint(spawnWarpName + blueTeamName);
		Location redSpawn = getWarpPoint(spawnWarpName + redTeamName);
		Player player = getPlayer(playerName);
		Team team = addPlayerToRandomTeam(playerName);
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
			Team t = addPlayerToRandomTeam(playerName);
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
