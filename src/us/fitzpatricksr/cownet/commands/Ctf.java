package us.fitzpatricksr.cownet.commands;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.Wool;
import us.fitzpatricksr.cownet.CowNetMod;
import us.fitzpatricksr.cownet.commands.games.PhasedGame;
import us.fitzpatricksr.cownet.commands.games.TeamState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class Ctf extends PhasedGame implements org.bukkit.event.Listener {
    private final Random rand = new Random();

    @Setting
    private boolean allowJoinGameInProgress = true;
    @Setting
    private int minPlayers = 2;
    @Setting
    private String loungeWarpName = "teamLounge-";
    @Setting
    private String spawnWarpName = "teamSpawn-";
    @Setting
    private int spawnJiggle = 5;

    private String[] teamNames = new String[]{
            "RED",
            "BLUE"
    };
    private HashMap<String, ItemStack> flagBlockColors = new HashMap<String, ItemStack>();

    {
        flagBlockColors.put("RED", new ItemStack(Material.WOOL, 1, new Wool(DyeColor.RED).getData()));
        flagBlockColors.put("BLUE", new ItemStack(Material.WOOL, 1, new Wool(DyeColor.BLUE).getData()));
    }

    ;

    private HashSet<String> gatheredPlayers;
    private TeamState teams;
    private BiMap<String, String> flagCarriers; // teamName -> playerName

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
        for (String playerName : gatheredPlayers) {
            loungeAPlayer(playerName);
        }
    }

    @Override
    protected void handlePlayerEnteredLounge(String playerName) throws PlayerCantBeAddedException {
        // allocate player to the team with the fewest people
        String team = teams.getSmallestTeam();
        teams.setPlayerTeam(playerName, team);
        // broadcast player joining
        broadcastToAllOnlinePlayers("CTF: " + playerName + " is on the " + team + " team.");
        // teleport player to the lounge
        loungeAPlayer(playerName);
    }

    @Override
    protected void handlePlayerLeftLounge(String playerName) {
        // announce to players that someone left the game.
        broadcastToAllOnlinePlayers("CTF: " + playerName + " has left the game.");
        // remove player from the records so they will re-added randomly if they rejoin
        teams.setPlayerTeam(playerName, null);
        gatheredPlayers.remove(playerName);
        // TODO hey jf - if teams are way out of balance, what do we do?
    }

    @Override
    protected void handleEndLounging() {
    }

    //-----------------------------------------------------------
    // Game events
    @Override
    protected void handleBeginGame() {
        if (gatheredPlayers.size() < minPlayers) {
            broadcastToAllOnlinePlayers("CTF: Game canceled due to lack of players.");
            cancelGame();
        } else {
            // if teams are not balanced, re-balance them (or fail?)
            // TODO hey jf - if teams are way out of balance, what do we do?
            // if teams are balanced, broadcast final teams
            for (String teamName : teams.getTeamNames()) {
                StringBuilder msg = new StringBuilder();
                msg.append("CTF:  The ");
                msg.append(teamName);
                msg.append(" team is: ");
                for (String playerName : teams.getPlayersForTeam(teamName)) {
                    msg.append(playerName);
                    msg.append("  ");
                }
                broadcastToAllOnlinePlayers(msg.toString());
            }
        }

        initializeTeamFlags();

        // teleport everyone to their base
        for (String playerName : gatheredPlayers) {
            spawnAPlayer(playerName);
        }
    }

    @Override
    protected void handlePlayerEnteredGame(String playerName) throws PlayerCantBeAddedException {
        // allocate player to their previous team or to team with fewest players
        // announce player entered.
        String team = teams.getPlayerTeam(playerName);
        if (team != null) {
            // broadcast player re-joining
            broadcastToAllOnlinePlayers("CTF: " + playerName + " has rejoined the " + team + " team.");
        } else if (allowJoinGameInProgress) {
            // broadcast player joining
            team = teams.getSmallestTeam();
            teams.setPlayerTeam(playerName, team);
            broadcastToAllOnlinePlayers("CTF: " + playerName + " is on the " + team + " team.");
        } else {
            throw new PlayerCantBeAddedException("You can't join a game that is in progress.");
        }
        // teleport player to the lounge
        spawnAPlayer(playerName);
    }

    @Override
    protected void handlePlayerLeftGame(String playerName) {
        // announce to players that someone left the game.
        broadcastToAllOnlinePlayers("CTF: " + playerName + " has left the game.");
        if (gatheredPlayers.size() < 1) {
            // if everyone left, just cancel the game
            cancelGame();
            broadcastToAllOnlinePlayers("CTF: all players left the game so it has been canceled without a winner.");
        }
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

    //-----------------------------------------------------------
    // warp utilities
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
        if (loc == null) {
            // THIS SHOULD NEVER HAPPEN!  All players are put on a team
            // when gathering ends and if they enter during lounging or a
            // game that is in progress.
            // TODO hey jf -   This is defensive.  If this stack trace doesn't show up in practice, this code should be removed.
            // hey jf - what happens if they aren't on a team.  How did they get here?
            logStackTrace("OMG!  There's a player without a team trying to spawn.");
            // I guess all we can do at this point is put them on a team...
            String team = teams.getSmallestTeam();
            teams.setPlayerTeam(playerName, team);
            broadcastToAllOnlinePlayers("CTF: " + playerName + " is on the " + team + " team.");
            loc = getPlayerSpawnPoint(playerName);
        }
        Player player = getPlayer(playerName);
        player.teleport(loc);
    }

    protected final Location getTeamSpawnPoint(String team) {
        return getWarpPoint(spawnWarpName + team, 0);
    }

    protected final Location getPlayerSpawnPoint(String playerName) {
        String team = teams.getPlayerTeam(playerName);
        Location loc = getTeamSpawnPoint(team);
        return jigglePoint(loc, spawnJiggle);
    }

    protected final Location getTeamLoungePoint(String team) {
        return getWarpPoint(loungeWarpName + team, 0);
    }

    protected final Location getPlayerLoungePoint(String playerName) {
        String team = teams.getPlayerTeam(playerName);
        Location loc = getTeamLoungePoint(team);
        return jigglePoint(loc, spawnJiggle);
    }

    protected final Location getWarpPoint(String warpName, int jiggle) {
        CowNetMod plugin = (CowNetMod) getPlugin();
        CowWarp warpThingy = (CowWarp) plugin.getThingy("cowwarp");
        return jigglePoint(warpThingy.getWarpLocation(warpName), jiggle);
    }

    protected final Location jigglePoint(Location loc, int jiggle) {
        if (loc != null) {
            if (jiggle > 0) {
                int dx = rand.nextInt(jiggle * 2 + 1) - jiggle - 1; // -5..5
                int dz = rand.nextInt(jiggle * 2 + 1) - jiggle - 1; // -5..5
                loc.add(dx, 0, dz);
                loc = loc.getWorld().getHighestBlockAt(loc).getLocation();
                loc.add(0, 1, 0);
            }
        }
        return loc;
    }

    protected final Player getPlayer(String playerName) {
        Server server = getPlugin().getServer();
        return server.getPlayer(playerName);
    }

    //-----------------------------------------------------------
    // flag management
    // The flag is a block of colored wool that sits directly above the team spawn point.

    protected void initializeTeamFlags() {
        // reset flag carriers so nobody is carrying the flags
        flagCarriers = HashBiMap.create();
        //TODO put flag block at the team spawn point
    }

    protected String getFlagByPlayer(String playerName) {
        return flagCarriers.inverse().get(playerName);
    }

    // Return the name of the player carrying the specified flag.  Null if that flag is home.
    protected String getFlagCarrier(String teamName) {
        return flagCarriers.get(teamName);
    }

    // Give the specified team flag to the designated player.  If player is null, return it home.
    protected void setFlagCarrier(String teamName, String playerName) {
        // remove flag from the current flag carrier
        String oldCarrier = getFlagCarrier(teamName);
        if (oldCarrier != null) {
            // if the specified player is already the carrier then we're done.  Optimization.
            if (oldCarrier.equals(playerName)) return;
            Player player = getPlayer(getFlagCarrier(teamName));
            PlayerInventory inv = player.getInventory();
            inv.setHelmet(null);
        }

        // Give the flag to the new player
        if (playerName == null) {
            // we're sending the flag home
            //TODO place the flag block at the team spawn position
        } else {
            // someone is now carrying the flag
            Player player = getPlayer(playerName);
            PlayerInventory inv = player.getInventory();
            ItemStack helmet = inv.getArmorContents()[3]; // getHelmet() wouldn't give me non-helmet blocks
            // if they already have a helmet, remove it and put it in their inventory
            if (helmet != null && helmet.getType() != Material.AIR) {
                inv.addItem(helmet);
            }
            inv.setHelmet(flagBlockColors.get(teamName));
            //TODO remove the flag from the team spawn position
        }
    }
}
