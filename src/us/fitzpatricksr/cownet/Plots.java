package us.fitzpatricksr.cownet;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import uk.co.jacekk.bukkit.infiniteplots.InfinitePlotsGenerator;
import us.fitzpatricksr.cownet.plotsclaims.PlayerCenteredClaim;
import us.fitzpatricksr.cownet.plotsclaims.InfinitePlotClaim;

import java.util.ArrayList;
import java.util.List;

public class Plots extends CowNetThingy {
    private WorldGuardPlugin worldGuard;
    private PlayerCenteredClaim pcc;
    private InfinitePlotClaim ipc;
    private int maxPlots = 4;

    public interface AbstractClaim {
        //define the region of the claim
        public ProtectedRegion defineClaim(Player p, String name);
        //after it has been claimed, we can build it out a bit and make it look nice.
        public void constructClaim(Player p, String name);
    }

    public interface AbstractDecorator {
        public void decorateClaim(Player p, String name, ProtectedRegion region);
        public void undecorateClaim(Player p, String name, ProtectedRegion region);
    }

    public Plots(JavaPlugin plugin, String permissionRoot, String trigger) {
        super(plugin, permissionRoot, trigger);
        if (isEnabled()) {
            //get WorldGuard and WorldEdit plugins
            Server server = plugin.getServer();
            if(server == null){
                throw new RuntimeException("getServer() is null");
            }
            PluginManager pluginManager = server.getPluginManager();
            if(pluginManager == null){
                throw new RuntimeException("server.getPluginManager() is null");
            }
            Plugin worldPlugin = pluginManager.getPlugin("WorldGuard");
            if(worldPlugin == null || !(worldPlugin instanceof WorldGuardPlugin)){
                throw new RuntimeException("WorldGuard must be loaded first");
            }
            worldGuard = (WorldGuardPlugin) worldPlugin;

            this.maxPlots = getConfigInt("maxPlots", maxPlots);
            
            this.pcc = new PlayerCenteredClaim(this);
            this.ipc = new InfinitePlotClaim(worldGuard);
        }
    }

    @Override
    protected String getHelpString(Player player) {
        return "usage: plot [claim <name>,release <name>,share <name><player>]";
    }

    @Override
    protected boolean onCommand(Player player, Command cmd, String[] args) {
        if (args.length < 1) {
            return false;
        } else if (hasPermissions(player)) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("claim")) {
                return claim(player, cmd, args);
            } else if (subCmd.equals("release")) {
                return release(player, cmd, args);
            } else if (subCmd.equals("share")) {
                return share(player, cmd, args);
            } else if (subCmd.equals("unshare")) {
                return unshare(player, cmd, args);
            } else {
                player.sendMessage("Unknown command: "+args[0]);
                return true;
            }
        } else {
            player.sendMessage("You don't have permission to claim land.");
            return false;
        }
    }

    private boolean share(Player player, Command cmd, String[] args) {
        BukkitPlayer wgPlayer = new BukkitPlayer(worldGuard, player);
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());

        if ((args.length != 2)) {
            player.sendMessage("You must specify a player to share with.");
            return true;
        }

        String playerName = args[1];

        OfflinePlayer offPlayer = getPlugin().getServer().getOfflinePlayer(playerName);
        if (offPlayer == null) {
            player.sendMessage("Don't know any player named "+playerName);
            return true;
        }
        Player otherPlayer = offPlayer.getPlayer();
        if (otherPlayer == null) {
            player.sendMessage("Don't know any player named "+playerName);
            return true;
        }
        BukkitPlayer playerToShareWith = new BukkitPlayer(worldGuard, otherPlayer);

        ApplicableRegionSet regions = regionManager.getApplicableRegions(player.getLocation());
        if (regions.size() == 0) {
            player.sendMessage("Nothing to share.");
        } else {
            for (ProtectedRegion region : regions) {
                if (region.isOwner(wgPlayer)) {
                    region.getMembers().addPlayer(playerToShareWith);
                    if (saveRegions(regionManager)) {
                        player.sendMessage("Sharing "+region.getId()+" with "+ playerName);
                    } else {
                        player.sendMessage("Could not share region for unknown reasons.");
                    }
                } else {
                    player.sendMessage("Could not share region "+region.getId()+" because you don't own it.");
                }
            }
        }
        return true;
    }

    private boolean unshare(Player player, Command cmd, String[] args) {
        BukkitPlayer wgPlayer = new BukkitPlayer(worldGuard, player);
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());

        if ((args.length != 2)) {
            player.sendMessage("You must specify a player to unshare with.");
            return true;
        }

        String playerName = args[1];

        OfflinePlayer offPlayer = getPlugin().getServer().getOfflinePlayer(playerName);
        if (offPlayer == null) {
            player.sendMessage("Don't know any player named "+playerName);
            return true;
        }
        Player otherPlayer = offPlayer.getPlayer();
        if (otherPlayer == null) {
            player.sendMessage("Don't know any player named "+playerName);
            return true;
        }
        BukkitPlayer playerToShareWith = new BukkitPlayer(worldGuard, otherPlayer);

        ApplicableRegionSet regions = regionManager.getApplicableRegions(player.getLocation());
        if (regions.size() == 0) {
            player.sendMessage("Nothing to unshare.");
        } else {
            for (ProtectedRegion region : regions) {
                if (region.isOwner(wgPlayer)) {
                    region.getMembers().removePlayer(playerToShareWith);
                    if (saveRegions(regionManager)) {
                        player.sendMessage("No longer sharing "+region.getId()+" with "+ playerName);
                    } else {
                        player.sendMessage("Could not unshare region for unknown reasons.");
                    }
                } else {
                    player.sendMessage("Could not unshare region "+region.getId()+" because you don't own it.");
                }
            }
        }
        return true;
    }

    private boolean release(Player player, Command cmd, String[] args) {
        if (args.length != 1) {
            return false;
        }

        BukkitPlayer wgPlayer = new BukkitPlayer(worldGuard, player);
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());

        ApplicableRegionSet regions = regionManager.getApplicableRegions(player.getLocation());
        if (regions.size() == 0) {
            player.sendMessage("Nothing to release.");
        } else {
            for (ProtectedRegion region : regions) {
                if (region.isOwner(wgPlayer)) {
                    regionManager.removeRegion(region.getId());
                    if (saveRegions(regionManager)) {
                        player.sendMessage("Releasing region "+region.getId());
                    } else {
                        player.sendMessage("Could not release region for unknown reasons.");
                    }
                } else {
                    player.sendMessage("Could not release region "+region.getId()+" because you don't own it.");
                }
            }
        }
        return true;
    }

    private boolean claim(Player player, Command cmd, String[] args) {
        BukkitPlayer wgPlayer = new BukkitPlayer(worldGuard, player);
        RegionManager regionManager = worldGuard.getRegionManager(player.getWorld());

        if ((args.length != 2) || !ProtectedRegion.isValidId(args[1])) {
            player.sendMessage("You must specify a valid name for the claim: /plot claim <name>");
            return true;
        }

        String claimName = args[1];
        if (regionManager.hasRegion(claimName)) {
            player.sendMessage("There's already a claim with that name.  Please choose another name.");
            return true;
        }

        //does this player reached his maximum number of plots.
        if (regionManager.getRegionCountOfPlayer(wgPlayer) > maxPlots) {
            player.sendMessage("You've exceeded the maximum of "+maxPlots+" allowed plots");
            return true;
        }

        //does the region on unclaimed land?
        AbstractClaim claim = getClaimType(player);
        ProtectedRegion region = claim.defineClaim(player, claimName);
        ProtectedRegion conflict = getConflictingRegions(regionManager, region, wgPlayer);
        if (conflict != null) {
            String owners = conflict.getOwners().toPlayersString();
            String name = conflict.getId();
            player.sendMessage("Sorry, this overlaps \""+name+"\" owned by "+owners);
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
        region.setFlag(DefaultFlag.GREET_MESSAGE, "Now entering "+claimName+" owned by "+wgPlayer.getName());
        region.setFlag(DefaultFlag.FAREWELL_MESSAGE, "Now leaving " + claimName);

        // looks good, so let's twiddle as needed.
        claim.constructClaim(player, claimName);
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
        } catch (ProtectionDatabaseException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private AbstractClaim getClaimType(Player p) {
        World w = p.getWorld();
        ChunkGenerator cg = w.getGenerator();
        if (cg instanceof InfinitePlotsGenerator) {
            // InfinitePlotsClaim
            logInfo("Claiming using InfinitePlotsClaim");
            return ipc;
        } else {
            // player centered claim
            logInfo("Claiming using PlayerCenterClaim");
            return pcc;
        }
    }

    public ProtectedRegion getConflictingRegions(RegionManager mgr, ProtectedRegion checkRegion, LocalPlayer player) {
        ApplicableRegionSet appRegions = mgr.getApplicableRegions(checkRegion);
        for (ProtectedRegion region : appRegions) {
            if (!region.getOwners().contains(player)) {
                return region;
            }
        }
        return null;
    }
}

