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
        return getTeamSpawnPoint(null);
    }

    public final Location getTeamSpawnPoint(String spawnWarpName) {
        return getWarpPoint(moduleName + "-spawn" + ((spawnWarpName != null) ? "-" + spawnWarpName : ""), 0);
    }

    public final Location getPlayerSpawnPoint() {
        return getPlayerSpawnPoint(null);
    }

    public final Location getPlayerSpawnPoint(String team) {
        Location loc = getTeamSpawnPoint(team);
        return jigglePoint(loc, spawnJiggle);
    }

    public final Location getTeamLoungePoint() {
        return getTeamLoungePoint(null);
    }

    public final Location getTeamLoungePoint(String loungeWarpName) {
        return getWarpPoint(moduleName + "-lounge" + ((loungeWarpName != null) ? "-" + loungeWarpName : ""), 0);
    }

    public final Location getPlayerLoungePoint() {
        return getTeamLoungePoint(null);
    }

    public final Location getPlayerLoungePoint(String team) {
        Location loc = getTeamLoungePoint(team);
        return jigglePoint(loc, spawnJiggle);
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
