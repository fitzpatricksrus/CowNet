package us.fitzpatricksr.cownet.commands.games;

import com.google.common.collect.HashMultimap;

import java.util.Collection;
import java.util.HashMap;

/*
This class is a simple bidirectional map between team and player.
 */
public class TeamState {
    //-----------------------------------------------------------
    // team methods
    private HashMultimap<String, String> teams = HashMultimap.create();        // teamName -> playerName[]
    private HashMap<String, String> team = new HashMap<String, String>();    // playerName -> teamName
    private String[] teamNames;

    public TeamState(String[] teamNames) {
        this.teamNames = teamNames;
    }

    public String[] getTeamNames() {
        return teamNames;
    }

    public Collection<String> getPlayersForTeam(String teamName) {
        return teams.get(teamName);
    }

    public String getPlayerTeam(String playerName) {
        return team.get(playerName);
    }

    public void setPlayerTeam(String playerName, String newTeam) {
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

    public void clearTeams() {
        teams = HashMultimap.create();
        team = new HashMap<String, String>();
    }

    public String getSmallestTeam() {
        int smallestSize = Integer.MAX_VALUE;
        String smallestName = "Whaaa!";
        for (String teamName : teamNames) {
            int teamSize = teams.get(teamName).size();
            if (teamSize < smallestSize) {
                smallestSize = teamSize;
                smallestName = teamName;
            }
        }
        return smallestName;
    }
}


