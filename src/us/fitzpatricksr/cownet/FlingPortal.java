package us.fitzpatricksr.cownet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import us.fitzpatricksr.cownet.utils.CowNetThingy;

import java.util.HashMap;


public class FlingPortal extends CowNetThingy implements org.bukkit.event.Listener {
    private static final Vector LAUNCH_VECTOR = new Vector(0, 5, 0);
    private HashMap<Player, Long> playersInFlight = new HashMap<Player, Long>();

    public FlingPortal(JavaPlugin plugin, String permissionRoot, String trigger) {
        super(plugin, permissionRoot, trigger);
        if (isEnabled()) {
            logInfo("adding listeners...");
            plugin.getServer().getPluginManager().registerEvents(
                    this,
                    plugin);
            plugin.getServer().getPluginManager().registerEvents(
                    this,
                    plugin);
        }
    }

    @Override
    protected String getHelpString(CommandSender sender) {
        return "usage: put a redstone torch below a glass block and stand on it.";
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private void onPlayerMove(PlayerMoveEvent event) {
        // if we moved over a fling portal then here we go...
        if (shouldFlingPlayer(event.getPlayer())) {
            fling(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (playerIsInFlight(player)) {
                EntityDamageEvent.DamageCause cause = event.getCause();
                if (cause.equals(EntityDamageEvent.DamageCause.FALL)) {
                    logInfo("absorbing fall damage");
                    playersInFlight.remove(player);
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean shouldFlingPlayer(Player player) {
        if (player.getLocation().getBlockY() > 127) return false;
        if (playerIsInFlight(player)) return false;
        Location loc = player.getLocation().clone();
        loc.setY(loc.getY() - 1);
        Block topBlock = loc.getBlock();
        if (topBlock.getType().equals(Material.GLASS)) {
            //OK, it's a glass block.  Does it have a redstone torch underneath?
            loc.setY(loc.getY() - 1);
            topBlock = loc.getBlock();
            boolean result = topBlock.getType().equals(Material.REDSTONE_TORCH_ON);
//            logInfo("Fling: "+result+" "+topBlock.getTypeId()+" "+topBlock.getType());
            return result;
        } else {
//            logInfo("x="+loc.getBlockX()+" y="+loc.getBlockY()+" z="+loc.getBlockZ()+" typeid="+topBlock.getTypeId()+" type="+topBlock.getType());
        }
        return false;
    }

    private void fling(final Player player) {
        playersInFlight.put(player, System.currentTimeMillis());
        final int nyan = getPlugin().getServer().getScheduler().scheduleAsyncRepeatingTask(
                getPlugin(),
                new Runnable() {
                    public void run() {
                        player.setVelocity(LAUNCH_VECTOR);
                    }
                },
                5,
                5);

        getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
            public void run() {
                Location destination = getPlayerDestination(player);
                logInfo("Fling " + player.getName() + " to: " + destination);
                player.teleport(destination);
                getPlugin().getServer().getScheduler().cancelTask(nyan);
            }// end of run
        }, 100); // end of teleport
    }

    private boolean playerIsInFlight(Player p) {
        Long timeStamp = playersInFlight.get(p);
        if (timeStamp == null) {
            return false;
        } else {
            long duration = System.currentTimeMillis() - timeStamp;
            if (duration < 30 * 1000) {
                return true;
            } else {
                // they've been in flight too long.  Let them suffer.
                playersInFlight.remove(p);
                return false;
            }
        }
    }

    private Location getPlayerDestination(Player player) {
//        long seed = player.getName().hashCode() & 0x0FFFF;
        long seed = 0;

        Location loc = player.getLocation();

        return new Location(
                loc.getWorld(),
                rotateBytes(loc.getBlockX()) ^ seed,
                300,
                rotateBytes(loc.getBlockZ()) ^ seed,
                loc.getYaw(),
                loc.getPitch());
    }

    private long rotateBytes(long value) {
        Short s = (short) value;
        return Short.reverseBytes(s);
    }

    private Location getPlayerDestinationx(Player player) {
        final long MASK = 0x0FFF;
        long seed = player.getName().hashCode() & MASK;
        Location loc = player.getLocation();

        return new Location(
                loc.getWorld(),
                loc.getBlockX() ^ seed,
                300,
                loc.getBlockZ() ^ seed,
                loc.getYaw(),
                loc.getPitch());
    }
}
