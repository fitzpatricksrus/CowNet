package us.fitzpatricksr.cownet;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetThingy;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Hide extends CowNetThingy implements Listener {
    //show/hide
    //join/leave
    //chat
    //interactions

    private Map<String, Player> invisiblePlayers = new HashMap<String, Player>();
    private Random rand = new Random();

    public Hide(JavaPlugin plugin, String permissionRoot, String trigger) {
        super(plugin, permissionRoot, trigger);
        if (isEnabled()) {
            reload();
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }
    }

    @Override
    protected void reload() {
    }

    @Override
    protected String getHelpString(CommandSender sender) {
        return "usage: hide [ on | off | check | join | quit ]";
    }

    @Override
    protected boolean handleCommand(Player player, Command cmd, String[] args) {
        if (hasPermissions(player)) {
            if (args.length == 0) {
                toggleHiddenState(player);
                return true;
            } else if (args.length == 1) {
                String subCmd = args[0];
                if (subCmd.equalsIgnoreCase("on")) {
                    hidePlayer(player, true);
                    return true;
                } else if (subCmd.equalsIgnoreCase("off")) {
                    unhidePlayer(player, true);
                    return true;
                } else if (subCmd.equalsIgnoreCase("check")) {
                    player.sendMessage((isHidden(player) ? "You are hidden" : "You are visible"));
                    return true;
                } else if (subCmd.equalsIgnoreCase("join")) {
                    fakeJoin(player);
                    return true;
                } else if (subCmd.equalsIgnoreCase("quit")) {
                    fakeQuit(player);
                    return true;
                }
            }
            return false;
        } else {
            player.sendMessage("You don't have permission.");
            return true;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        final Entity smacked = event.getEntity();
        if (smacked instanceof Player) {
            final Player player = (Player) smacked;
            if (isHidden(player)) {
                event.setCancelled(true);
                debugInfo("Canceled damage to player.");
            }
        }
        if (event instanceof EntityDamageByEntityEvent) {
            final EntityDamageByEntityEvent ev = (EntityDamageByEntityEvent) event;
            final Entity damager = ev.getDamager();
            if (damager instanceof Player) {
                final Player player = (Player) damager;
                if (isHidden(player)) {
                    event.setCancelled(true);
                    debugInfo("Canceled damage cause by player.");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if ((event.getTarget() instanceof Player) && isHidden((Player) event.getTarget())) {
            event.setCancelled(true);
            debugInfo("Canceled target.");
        } else {
            if (isDebug()) {
                if (!(event.getTarget() instanceof Player)) {
                    debugInfo("Didn't cancel TARGET - not a player: " + event.getTarget().getClass().getName());
                } else {
                    debugInfo("Didn't cancel TARGET to player " + ((Player) event.getTarget()).getName());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayer(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (hasPermissions(player, "joinHidden")) {
            hidePlayer(player, false);
            player.sendMessage("Joining hidden...");
            event.setJoinMessage(null);
        }
        hideInvisiblePlayersFrom(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(PlayerChatEvent event) {
        if (isHidden(event.getPlayer())) {
            event.setCancelled(true);
            debugInfo("Canceled chat.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (isHidden(event.getPlayer())) {
            event.setCancelled(true);
            debugInfo("Canceled bucket fill.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isHidden(event.getPlayer())) {
            event.setCancelled(true);
            debugInfo("Canceled interaction.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (isHidden(event.getPlayer())) {
            event.setCancelled(true);
            debugInfo("Canceled pickup.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (isHidden(event.getPlayer())) {
            unhidePlayer(event.getPlayer(), false);
            event.setQuitMessage(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        if (isHidden(event.getPlayer())) {
            event.setCancelled(true);
            debugInfo("Canceled shear.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        hideInvisiblePlayersFrom(event.getPlayer());
    }

    public void hidePlayer(Player p, boolean poof) {
        if (isHidden(p)) return;
        invisiblePlayers.put(p.getName(), p);
        World w = p.getWorld();
        if (poof) smokeScreenEffect(p.getLocation());
        for (Player other : w.getPlayers()) {
            if (!other.equals(p)) {
                if (!hasPermissions(other, "seesInvisible")) {
                    debugInfo("Hiding " + p.getName() + " from " + other.getName());
                    if (other.canSee(p)) {
                        other.hidePlayer(p);
                    }
                }
            }
        }
        p.sendMessage("You are now hidden");
    }

    public void hideInvisiblePlayersFrom(Player p) {
        if (hasPermissions(p, "seesInvisible")) return;
        for (Player other : invisiblePlayers.values()) {
            if (!other.equals(p)) {
                debugInfo("Hiding " + other.getName() + " from " + p.getName());
                if (p.canSee(other)) {
                    p.hidePlayer(other);
                }
            }
        }
    }

    public void unhidePlayer(Player p, boolean poof) {
        if (!isHidden(p)) return;
        invisiblePlayers.remove(p.getName());
        World w = p.getWorld();
        if (poof) smokeScreenEffect(p.getLocation());
        for (Player other : w.getPlayers()) {
            if (!other.equals(p)) {
                if (!other.canSee(p)) {
                    other.showPlayer(p);
                }
            }
        }
        p.sendMessage("You are now unhidden");
    }

    public boolean isHidden(Player p) {
        return invisiblePlayers.get(p.getName()) != null;
    }

    private boolean toggleHiddenState(Player player) {
        if (isHidden(player)) {
            unhidePlayer(player, true);
            return false;
        } else {
            hidePlayer(player, true);
            return true;
        }
    }

    private void smokeScreenEffect(Location location) {
        for (int i = 0; i < 10; i++) {
            location.getWorld().playEffect(location, Effect.SMOKE, rand.nextInt(9));
        }
    }

    private void fakeJoin(Player player) {
        getPlugin().getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + " joined the game.");
        unhidePlayer(player, false);
    }

    private void fakeQuit(Player player) {
        getPlugin().getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + " left the game.");
        hidePlayer(player, false);
    }
}
