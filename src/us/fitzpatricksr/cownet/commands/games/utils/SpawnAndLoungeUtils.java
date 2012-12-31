package us.fitzpatricksr.cownet.commands.games.utils;

import org.bukkit.Location;
import us.fitzpatricksr.cownet.CowNetMod;
import us.fitzpatricksr.cownet.commands.CowWarp;

import java.util.Random;

/**
 */
public class SpawnAndLoungeUtils {
    private Random rand = new Random();
    private int spawnJiggle = 5;
    private String moduleName;
    private CowNetMod thingy;

    public SpawnAndLoungeUtils(CowNetMod thingy, String moduleName, int spawnJiggle) {
        this.spawnJiggle = spawnJiggle;
        this.thingy = thingy;
        this.moduleName = moduleName;
    }

    //-----------------------------------------------------------
    // warp utilities

    // point-module
    // point
    public final Location getTeamSpawnPoint() {
        Location result = getWarpPoint("spawn-" + moduleName, 0);
        if (result == null) {
            result = getWarpPoint("spawn", 0);
        }
        return result;
    }

    // point-module-team
    // point-team
    // point-module
    // point
    public final Location getTeamSpawnPoint(String team) {
        Location result = getWarpPoint("spawn-" + moduleName + "-" + team, 0);
        if (result == null) {
            result = getWarpPoint("spawn-" + team, 0);
        }
        if (result == null) {
            result = getTeamSpawnPoint();
        }
        return result;
    }

    public final Location getPlayerSpawnPoint() {
        Location result = getWarpPoint("spawn-" + moduleName, spawnJiggle);
        if (result == null) {
            result = getWarpPoint("spawn", spawnJiggle);
        }
        return result;
    }

    public final Location getPlayerSpawnPoint(String team) {
        Location result = getWarpPoint("spawn-" + moduleName + "-" + team, spawnJiggle);
        if (result == null) {
            result = getWarpPoint("spawn-" + team, spawnJiggle);
        }
        if (result == null) {
            result = getTeamSpawnPoint();
        }
        return result;
    }


    // point-module
    // point
    public final Location getTeamLoungePoint() {
        Location result = getWarpPoint("lounge-" + moduleName, 0);
        if (result == null) {
            result = getWarpPoint("lounge", 0);
        }
        return result;
    }

    // point-module-team
    // point-team
    // point-module
    // point
    public final Location getTeamLoungePoint(String team) {
        Location result = getWarpPoint("lounge-" + moduleName + "-" + team, 0);
        if (result == null) {
            result = getWarpPoint("lounge-" + team, 0);
        }
        if (result == null) {
            result = getTeamLoungePoint();
        }
        return result;
    }

    public final Location getPlayerLoungePoint() {
        Location result = getWarpPoint("lounge-" + moduleName, spawnJiggle);
        if (result == null) {
            result = getWarpPoint("lounge", spawnJiggle);
        }
        return result;
    }

    public final Location getPlayerLoungePoint(String team) {
        Location result = getWarpPoint("lounge-" + moduleName + "-" + team, spawnJiggle);
        if (result == null) {
            result = getWarpPoint("lounge-" + team, spawnJiggle);
        }
        if (result == null) {
            result = getPlayerLoungePoint();
        }
        return result;
    }

    public final Location getWarpPoint(String warpName, int jiggle) {
        CowWarp warpThingy = (CowWarp) thingy.getThingy("cowwarp");
        return jigglePoint(warpThingy.getWarpLocation(warpName), jiggle);
    }

    public final Location jigglePoint(Location loc) {
        return jigglePoint(loc, spawnJiggle);
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
}
