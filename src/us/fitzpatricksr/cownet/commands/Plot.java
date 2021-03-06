package us.fitzpatricksr.cownet.commands;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.plots.InfinitePlotClaim;
import us.fitzpatricksr.cownet.commands.plots.PlayerCenteredClaim;
import us.fitzpatricksr.cownet.commands.plots.PlotsChunkGenerator;
import us.fitzpatricksr.cownet.utils.BlockUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Plot extends CowNetThingy {
    private Material plotBase = Material.STONE;
    private Material plotSurface = Material.GRASS;
    private Material plotPath = Material.DOUBLE_STEP;

    private WorldGuardPlugin worldGuard;
    private NoSwearing noSwearingMod;
    private PlayerCenteredClaim pcc;
    private InfinitePlotClaim ipc;

    @Setting
    private int plotSize = 64;
    @Setting
    private int plotHeight = 20;

    /**
     * Interface for different type of claim shapes and decorations
     */
    public interface AbstractClaim {
        //define the region of the claim
        public ProtectedRegion defineClaim(Player p, String name);

        //after it has been claimed, we can build it out a bit and make it look nice.
        public void decorateClaim(Player p, ProtectedRegion region);

        public void dedecorateClaim(Player p, ProtectedRegion region);
    }

    public Plot(NoSwearing noSwearingMod) {
        this.noSwearingMod = noSwearingMod;
    }

    @Override
    protected void onEnable() throws Exception {
        //get WorldGuard and WorldEdit plugins
        Plugin worldPlugin = getPlugin().getServer().getPluginManager().getPlugin("WorldGuard");
        if (worldPlugin == null || !(worldPlugin instanceof WorldGuardPlugin)) {
            throw new RuntimeException("WorldGuard must be loaded first");
        }
        worldGuard = (WorldGuardPlugin) worldPlugin;
        this.pcc = new PlayerCenteredClaim(this);
    }

    @Override
    // reload any settings not handled by @Setting
    protected void reloadManualSettings() throws Exception {
        this.plotBase = Material.matchMaterial(getConfigValue("plotBase", plotBase.toString()));
        this.plotSurface = Material.matchMaterial(getConfigValue("plotSurface", plotSurface.toString()));
        this.plotPath = Material.matchMaterial(getConfigValue("plotPath", plotPath.toString()));
        this.ipc = new InfinitePlotClaim(plotSize);
    }

    // return any custom settings that are not handled by @Settings code
    protected HashMap<String, String> getManualSettings() {
        HashMap<String, String> result = new HashMap<String, String>();
        result.put("plotBase", plotBase.toString());
        result.put("plotSurface", plotSurface.toString());
        result.put("plotPath", plotPath.toString());
        return result;
    }

    // update a setting that was not handled by @Setting and return true if it has been updated.
    protected boolean updateManualSetting(String settingName, String settingValue) {
        if (settingName.equalsIgnoreCase("plotBase")) {
            plotBase = Material.valueOf(settingValue);
            updateConfigValue("plotBase", settingValue);
        } else if (settingName.equalsIgnoreCase("plotSurface")) {
            plotSurface = Material.valueOf(settingValue);
            updateConfigValue("plotSurface", settingValue);
        } else if (settingName.equalsIgnoreCase("plotPath")) {
            plotPath = Material.valueOf(settingValue);
            updateConfigValue("plotPath", settingValue);
        } else {
            return false;
        }
        return true;
    }


    @Override
    protected String getHelpString(CommandSender sender) {
        return "usage: plot [ claim <plotName> | release | share <player> | unshare <player> | " + "info | list [player] | giveto <player> | tp <plotName> ]";
    }

    @CowCommand
    private boolean doShare(Player player, String playerName) {
        BukkitPlayer wgPlayer = new BukkitPlayer(worldGuard, player);
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());

        ApplicableRegionSet regions = regionManager.getApplicableRegions(player.getLocation());
        if (regions.size() == 0) {
            player.sendMessage("Nothing to share.");
        } else {
            for (ProtectedRegion region : regions) {
                if (player.isOp() || region.isOwner(wgPlayer)) {
                    region.getMembers().addPlayer(playerName);
                    if (saveRegions(regionManager)) {
                        player.sendMessage("Sharing " + region.getId() + " with " + playerName);
                    } else {
                        player.sendMessage("Could not share region for unknown reasons.");
                    }
                } else {
                    player.sendMessage("Could not share region " + region.getId() + " because you don't own it.");
                }
            }
        }
        return true;
    }

    @CowCommand
    private boolean doUnshare(Player player, String playerName) {
        BukkitPlayer wgPlayer = new BukkitPlayer(worldGuard, player);
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());

        ApplicableRegionSet regions = regionManager.getApplicableRegions(player.getLocation());
        if (regions.size() == 0) {
            player.sendMessage("Nothing to unshare.");
        } else {
            for (ProtectedRegion region : regions) {
                if (player.isOp() || region.isOwner(wgPlayer)) {
                    region.getMembers().removePlayer(playerName);
                    if (saveRegions(regionManager)) {
                        player.sendMessage("No longer sharing " + region.getId() + " with " + playerName);
                    } else {
                        player.sendMessage("Could not unshare region for unknown reasons.");
                    }
                } else {
                    player.sendMessage("Could not unshare region " + region.getId() + " because you don't own it.");
                }
            }
        }
        return true;
    }

    @CowCommand
    private boolean doClear(Player player) {
        return doRelease(player);
    }

    @CowCommand
    private boolean doRelease(Player player) {
        BukkitPlayer wgPlayer = new BukkitPlayer(worldGuard, player);
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());

        ApplicableRegionSet regions = regionManager.getApplicableRegions(player.getLocation());
        if (regions.size() == 0) {
            player.sendMessage("Nothing to release.");
        } else {
            for (ProtectedRegion region : regions) {
                if (player.isOp() || region.isOwner(wgPlayer)) {
                    regionManager.removeRegion(region.getId());
                    if (saveRegions(regionManager)) {
                        player.sendMessage("Releasing region " + region.getId());
                        getClaimType(player).dedecorateClaim(player, region);
                    } else {
                        player.sendMessage("Could not release region for unknown reasons.");
                    }
                } else {
                    player.sendMessage("Could not release region " + region.getId() + " because you don't own it.");
                }
            }
        }
        return true;
    }

    @CowCommand
    private boolean doInfo(Player player) {
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());

        ApplicableRegionSet regions = regionManager.getApplicableRegions(player.getLocation());
        if (regions.size() == 0) {
	        player.sendMessage("This region can be claimed.");
        } else {
            for (ProtectedRegion region : regions) {
                String name = region.getId();
                String owners = region.getOwners().toPlayersString();
                player.sendMessage("Plot name: " + name);
                player.sendMessage("    Owner: " + owners);
                if (region.getMembers().size() > 0) {
                    String members = region.getMembers().toPlayersString();
                    player.sendMessage("    Shared with: " + members);
                }
            }
            return true;
        }
        return true;
    }

    @CowCommand
    private boolean doList(Player player) {
        return doList(player, player.getName());
    }

    @CowCommand
    private boolean doList(Player player, String playerName) {
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());
        player.sendMessage("Plot claimed by " + playerName);
        for (Map.Entry<String, ProtectedRegion> entry : regionManager.getRegions().entrySet()) {
            for (String owner : entry.getValue().getOwners().getPlayers()) {
                if (owner.equalsIgnoreCase(playerName)) {
                    player.sendMessage("    " + entry.getKey());
                    break;
                }
            }
        }
        return true;
    }

    @CowCommand
    private boolean doTp(Player player, String plotName) {
        return doTeleport(player, plotName);
    }

    @CowCommand
    private boolean doTeleport(Player player, String plotName) {
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());
        ProtectedRegion region = regionManager.getRegion(plotName);
        if (region == null) {
            player.sendMessage("Can't find a plot named " + plotName);
            return true;
        }

        Vector middle = region.getMinimumPoint();
        middle = middle.add(region.getMaximumPoint());
        middle = middle.divide(2.0);

        Location dropPoint = BlockUtils.getHighestLandLocation(new Location(player.getWorld(), middle.getX() + 0.5, middle.getY(), middle.getZ() + 0.5));

        dropPoint.setY(dropPoint.getY() + 1); //above ground.  :-)
        player.sendMessage("Zooooop!   You're in " + plotName + ".");
        player.teleport(dropPoint);
        return true;
    }

    @CowCommand
    private boolean doGiveto(Player player, String playerName) {
        BukkitPlayer wgPlayer = new BukkitPlayer(worldGuard, player);
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());

        ApplicableRegionSet regions = regionManager.getApplicableRegions(player.getLocation());
        if (regions.size() == 0) {
            player.sendMessage("Nothing to give.");
        } else {
            for (ProtectedRegion region : regions) {
                if (player.isOp() || region.isOwner(wgPlayer)) {
                    // add designated player to owners and remove from sharing
                    region.getOwners().addPlayer(playerName);
                    region.getMembers().removePlayer(playerName);
                    // remove this player from owners and add to sharing
                    region.getOwners().removePlayer(player.getName());
                    region.getMembers().addPlayer(player.getName());
                    if (saveRegions(regionManager)) {
                        player.sendMessage("Gave " + region.getId() + " to " + playerName + ".  " +
                                "You are no longer an owner, but it's shared with you.");
                    } else {
                        player.sendMessage("Could not give away region for unknown reasons.");
                    }
                } else {
                    player.sendMessage("Could not give region " + region.getId() + " away because you don't own it.");
                }
            }
        }
        return true;
    }

    @CowCommand
    private boolean doAuto(Player player) {
        return doClaim(player);
    }

    @CowCommand
    private boolean doClaim(Player player) {
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());
        for (int i = 0; i < 100; i++) {
            String claimName = player.getDisplayName() + "'s Plot #" + i;
            if (!regionManager.hasRegion(claimName)) {
                return doClaim(player, claimName);
            }
        }
        player.sendMessage("You've reached the maximum number of plots.  Please release some old ones.");
        return true;
    }

    @CowCommand
    private boolean doClaim(Player player, String claimName) {
        BukkitPlayer wgPlayer = new BukkitPlayer(worldGuard, player);
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());

        if (!hasPermissions(player, "claim")) {
            player.sendMessage("You don't have permission to claim plots.");
            return true;
        }

        //make sure nobody uses bad language.
        if (noSwearingMod.scanForBadWords(player, claimName)) {
            return true;
        }

        //is there already a claim with that name?
        if (regionManager.hasRegion(claimName)) {
            player.sendMessage("There's already a claim with that name.  Please choose another name.");
            return true;
        }

        //does the region on unclaimed land?
        AbstractClaim claim = getClaimType(player);
        ProtectedRegion region = claim.defineClaim(player, claimName);
        List<ProtectedRegion> conflicts = getConflictingRegions(regionManager, region, wgPlayer);
        if (conflicts.size() > 0) {
            for (ProtectedRegion conflict : conflicts) {
                String owners = conflict.getOwners().toPlayersString();
                String name = conflict.getId();
                player.sendMessage("Sorry, this overlaps \"" + name + "\" owned by " + owners);
            }
            return true;
        }

        // set up owner and flags
        region.getOwners().addPlayer(wgPlayer);
        region.setFlag(DefaultFlag.PVP, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.MOB_DAMAGE, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.MOB_SPAWNING, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.CREEPER_EXPLOSION, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.ENDER_BUILD, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.GHAST_FIREBALL, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.TNT, StateFlag.State.DENY);
        region.setFlag(DefaultFlag.GREET_MESSAGE, "Now entering " + claimName + " owned by " + wgPlayer.getName());
        region.setFlag(DefaultFlag.FAREWELL_MESSAGE, "Now leaving " + claimName);

        // looks good, so let's twiddle as needed.
        claim.decorateClaim(player, region);
        regionManager.addRegion(region);
        if (saveRegions(regionManager)) {
	        player.sendMessage("You now own a plot named " + claimName);
        } else {
            player.sendMessage("You're claim was REJECTED by the county land manager.  Bummer.");
        }
        return true;
    }

    private boolean saveRegions(RegionManager mgr) {
        try {
            mgr.save();
            return true;
        } catch (StorageException e) {
	        e.printStackTrace();
	        return false;
        }
    }

    private AbstractClaim getClaimType(Player p) {
        World w = p.getWorld();
        ChunkGenerator cg = w.getGenerator();
        if (cg instanceof PlotsChunkGenerator) {
            // InfinitePlotsClaim
            logInfo("Claiming using InfinitePlotsClaim");
            return ipc;
        } else {
            // player centered claim
            if (cg != null) {
                logInfo("Claiming using PlayerCenterClaim.  Chunk generator was a " + cg.getClass().getName());
            } else {
                logInfo("Claiming using PlayerCenterClaim.");
            }
            return pcc;
        }
    }

    private List<ProtectedRegion> getConflictingRegions(RegionManager mgr, ProtectedRegion checkRegion, LocalPlayer player) {
        ArrayList<ProtectedRegion> result = new ArrayList<ProtectedRegion>();
        ApplicableRegionSet appRegions = mgr.getApplicableRegions(checkRegion);
        for (ProtectedRegion region : appRegions) {
            if (!region.getOwners().contains(player)) {
                result.add(region);
            }
        }
        return result;
    }

    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new PlotsChunkGenerator(plotSize, plotHeight, plotBase, plotSurface, plotPath);
    }
}

