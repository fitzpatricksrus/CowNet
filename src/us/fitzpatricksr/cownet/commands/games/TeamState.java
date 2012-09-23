package us.fitzpatricksr.cownet.commands.games;

import com.google.common.collect.HashMultimap;

import java.util.Collection;
import java.util.HashMap;

public class TeamState {
	//-----------------------------------------------------------
	// team methods
	private HashMultimap<String, String> teams = HashMultimap.create();
	private HashMap<String, String> team = new HashMap<String, String>();

	protected final Collection<String> getPlayersForTeam(String teamName) {
		return teams.get(teamName);
	}

	protected final String getPlayerTeam(String playerName) {
		return team.get(playerName);
	}

	protected final void setPlayerTeam(String playerName, String newTeam) {
		// remove player from their current team
		String oldTeam = team.get(playerName);
		if (oldTeam != null) {
			teams.remove(oldTeam, playerName);
			team.remove(playerName);
		}
		// add the player to a new team if desired.
		if (newTeam != null) {
			teams.put(newTeam, playerName);
			team.put(playerName, newTeam);
		}
	}

	protected final void clearTeams() {
		teams = HashMultimap.create();
		team = new HashMap<String, String>();
	}
}
