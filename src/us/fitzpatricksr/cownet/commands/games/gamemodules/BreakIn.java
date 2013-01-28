package us.fitzpatricksr.cownet.commands.games.gamemodules;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.framework.BasicGameModule;
import us.fitzpatricksr.cownet.commands.games.utils.Team;

/**
 * Two teams.
 * Each team has a "bell" at their spawn.
 * The other player has to get to the other teams spawn and ring the bell to gain a point.
 * The team at the end with the most points wins.
 */

public class BreakIn extends BasicGameModule {
    @CowNetThingy.Setting
    private int breakInLoungeDuration = 10; // 30 second loung
    @CowNetThingy.Setting
    private int breakInGameDuration = 60 * 3; // 3 minutes max game length
    @CowNetThingy.Setting
    private int breakInMinPlayers = 2; // 3 minutes max game length

    @Override
    public String getName() {
        return "BreakIn";
    }

    @Override
    public int getLoungeDuration() {
        return breakInLoungeDuration;
    }

    @Override
    public int getGameDuration() {
        return breakInGameDuration;
    }

    @Override
    public int getMinPlayers() {
        return breakInMinPlayers;
    }

    @Override
    public boolean isTeamGame() {
        return true;
    }

    @Override
    public void gameStarted() {
        buildBases();
        super.gameStarted();
    }

    @Override
    public void playerLeftGame(String playerName) {
        //check to see if the player is the flag carrier
        super.playerLeftGame(playerName);
    }

    // --------------------------------------------------------------
    // ---- Event handlers

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!context.isLounging()) return;
        Location loc = event.getBlock().getLocation();
        Player player = event.getPlayer();
        Team team = context.getPlayerTeam(player.getName());
        Team base = getLocationTeam(loc);

        if (base != null) {
            if (team != base) {
                smokeScreenEffect(loc);
                // someone broke the other base.  Give them points and send them back to spawn
                context.addWin(player.getName());
            } else {
                // some dummy broke his own base.
                loc.getWorld().strikeLightning(player.getLocation());
                event.setCancelled(true);
                context.addLoss(player.getName());
            }
        }
    }

    // --------------------------------------------------------------
    // ---- Event handlers

    private void buildBases() {
        buildBase(Team.BLUE);
        buildBase(Team.RED);
    }

    private void buildBase(Team team) {
        getBaseLocation(team).getBlock().setType(team.getMaterial());
    }

    private Team getLocationTeam(Location loc) {
        if (loc.equals(getBaseLocation(Team.RED))) {
            return Team.RED;
        } else if (loc.equals(getBaseLocation(Team.BLUE))) {
            return Team.BLUE;
        } else {
            return null;
        }
    }

    private Location getBaseLocation(Team team) {
        Location baseLocation = spawnUtils.getTeamSpawnPoint(team.toString());
        baseLocation.add(0, 1, 0);
        return baseLocation;
    }

    private void smokeScreenEffect(Location location) {
        for (int i = 0; i < 10; i++) {
            location.getWorld().playEffect(location, Effect.SMOKE, rand.nextInt(9));
        }
    }


}
