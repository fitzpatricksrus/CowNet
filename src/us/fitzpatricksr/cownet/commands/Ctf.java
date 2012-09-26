package us.fitzpatricksr.cownet.commands;

import us.fitzpatricksr.cownet.commands.games.PhasedGame;
import us.fitzpatricksr.cownet.commands.games.TeamState;

import java.util.HashSet;

public class Ctf extends PhasedGame implements org.bukkit.event.Listener {
	@Setting
	private int minPlayers = 2;
	@Setting
	private String[] teamNames = new String[] {
			"RED",
			"BLUE"
	};

	private HashSet<String> gatheredPlayers;
	private TeamState teams;

	//-----------------------------------------------------------
	// Gathering events
	@Override
	protected void handleBeginGathering() {
		broadcastToAllOnlinePlayers("CTF: Gathering for a game of CTF has begun.");
		gatheredPlayers = new HashSet<String>();
	}

	@Override
	protected void handlePlayerEnteredGathering(String playerName) {
		// announce to players that someone entered the game.
		broadcastToAllOnlinePlayers("CTF: " + playerName + " has joined the game.");
		// add that player to the pool of gathered players
		gatheredPlayers.add(playerName);
	}

	@Override
	protected void handlePlayerLeftGathering(String playerName) {
		// announce to players that someone left the game.
		broadcastToAllOnlinePlayers("CTF: " + playerName + " has left the game.");
		// remove that player to the pool of gathered players
		gatheredPlayers.remove(playerName);
	}

	@Override
	protected void handleEndGathering() {
		if (gatheredPlayers.size() < minPlayers) {
			broadcastToAllOnlinePlayers("CTF: Game canceled due to lack of players.");
			cancelGame();
		} else {
			// allocate everyone to a team
			teams = new TeamState(teamNames);
			for (String playerName : gatheredPlayers) {
				String team = teams.getSmallestTeam();
				teams.setPlayerTeam(playerName, team);
				broadcastToAllOnlinePlayers("CTF: " + playerName + " is on the " + team + " team.");
			}
		}
	}

	//-----------------------------------------------------------
	// lounging events
	@Override
	protected void handleBeginLounging() {
		// teleport everyone to the lounge
		// broadcast results.
	}

	@Override
	protected void handlePlayerEnteredLounge(String playerName) throws PlayerCantBeAddedException {
		// allocate player to the team with the fewest people
		// teleport player to the lounge
		// broadcast allocation
	}

	@Override
	protected void handlePlayerLeftLounge(String playerName) {
		// if teams are out of balance, broadcast message asking someone to change
	}

	@Override
	protected void handleEndLounging() {
		// if teams are balanced, broadcast final teams
		// if teams are not balanced, rebalance them (or fail?)
	}

	//-----------------------------------------------------------
	// Game events
	@Override
	protected void handleBeginGame() {
		// teleport everyone to their base
	}

	@Override
	protected void handlePlayerEnteredGame(String playerName) throws PlayerCantBeAddedException {
		// allocate player to their previous team or to team with fewest players
		// announce player entered.
	}

	@Override
	protected void handlePlayerLeftGame(String playerName) {
		// announce player left.
	}

	@Override
	protected void handleEndGame() {
		// announce winning team
		// increase stats for everyone on that team.
		gatheredPlayers = null;
		teams = null;
	}

	@Override
	protected void handleCancelGame() {
		// announce failure.
		gatheredPlayers = null;
		teams = null;
	}

}
