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

    public final Location getTeamSpawnPoint() {
        return getPoint("spawn", 0);
    }

    public final Location getTeamSpawnPoint(String team) {
        return getPoint("spawn", team, 0);
    }

    public final Location getPlayerSpawnPoint() {
        return getPoint("spawn", spawnJiggle);
    }

    public final Location getPlayerSpawnPoint(String team) {
        return getPoint("spawn", team, spawnJiggle);
    }

    public final Location getTeamLoungePoint() {
        return getPoint("lounge", 0);
    }

    public final Location getTeamLoungePoint(String team) {
        return getPoint("lounge", team, 0);
    }

    public final Location getPlayerLoungePoint() {
        return getPoint("lounge", spawnJiggle);
    }

    public final Location getPlayerLoungePoint(String team) {
        return getPoint("lounge", team, spawnJiggle);
    }

    // point-module
    // point
    private final Location getPoint(String point, int jiggle) {
        Location result = getWarpPoint(point + "-" + moduleName, jiggle);
        if (result == null) {
            result = getWarpPoint(point, jiggle);
        }
        return result;
    }

    // point-module-team
    // point-team
    // point-module
    // point
    private final Location getPoint(String point, String team, int jiggle) {
        Location result = getWarpPoint(point + "-" + moduleName + "-" + team, jiggle);
        if (result == null) {
            result = getWarpPoint(point + "-" + team, jiggle);
        }
        if (result == null) {
            result = getTeamLoungePoint();
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
