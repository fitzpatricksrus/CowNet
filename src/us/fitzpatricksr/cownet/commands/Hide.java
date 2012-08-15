package us.fitzpatricksr.cownet.commands;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import us.fitzpatricksr.cownet.CowNetThingy;

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

	@Override
	protected String getHelpString(CommandSender sender) {
		if (sender.isOp()) {
			return "usage: hide [ on | off | check | join | quit ]";
		} else {
			return "";
		}
	}

	public boolean doHide(Player player) {
		toggleHiddenState(player);
		return true;
	}

	@CowCommand
	public boolean doOn(Player player) {
		hidePlayer(player, true);
		return true;
	}

	@CowCommand
	public boolean doOff(Player player) {
		unhidePlayer(player, true);
		return true;
	}

	@CowCommand
	public boolean doCheck(Player player) {
		player.sendMessage((isHidden(player) ? "You are hidden" : "You are visible"));
		return true;
	}

	@CowCommand
	public boolean doJoin(Player player) {
		fakeJoin(player);
		return true;
	}

	@CowCommand
	public boolean doQuit(Player player) {
		fakeQuit(player);
		return true;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
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

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onEntityTarget(EntityTargetEvent event) {
		if ((event.getTarget() instanceof Player) && isHidden((Player) event.getTarget())) {
			event.setCancelled(true);
			debugInfo("Canceled target.");
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		if (isHidden(event.getPlayer())) {
			event.setCancelled(true);
			debugInfo("Canceled chat.");
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBucketFill(PlayerBucketFillEvent event) {
		if (isHidden(event.getPlayer())) {
			event.setCancelled(true);
			debugInfo("Canceled bucket fill.");
		}
	}

	/*	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
		public void onPlayerInteract(PlayerInteractEvent event) {
			if (isHidden(event.getPlayer())) {
				debugInfo("Canceled interaction: ");
				event.setCancelled(true);
			}
		} */

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if (isHidden(event.getPlayer())) {
			event.setCancelled(true);
			debugInfo("Canceled pickup.");
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onShear(PlayerShearEntityEvent event) {
		if (isHidden(event.getPlayer())) {
			event.setCancelled(true);
			debugInfo("Canceled shear.");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (isHidden(event.getPlayer())) {
			event.setCancelled(true);
			debugInfo("Canceled block place.");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (isHidden(event.getPlayer())) {
			event.setCancelled(true);
			debugInfo("Canceled block break.");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPickupItem(PlayerPickupItemEvent event) {
		if (isHidden(event.getPlayer())) {
			event.setCancelled(true);
			debugInfo("Canceled pickup item.");
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (isHidden(event.getPlayer())) {
			event.setCancelled(true);
			debugInfo("Canceled drop items.");
		}
	}

	// ------------------------------

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (hasPermissions(player, "joinHidden")) {
			debugInfo(player.getName() + " joined hidden");
			hidePlayer(player, false);
			player.sendMessage("Joining hidden...");
			event.setJoinMessage(null);
		}
		hideInvisiblePlayersFrom(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (isHidden(player)) {
			unhidePlayer(player, false);
			event.setQuitMessage(null);
		}
	}

	public void hidePlayer(Player p, boolean poof) {
		if (isHidden(p)) return;
		invisiblePlayers.put(p.getName(), p);
		if (poof) smokeScreenEffect(p.getLocation());
		for (Player other : getPlugin().getServer().getOnlinePlayers()) {
			if (!other.equals(p)) {
				if (!hasPermissions(other, "seesInvisible")) {
					debugInfo("Hiding " + p.getName() + " from " + other.getName());
					other.hidePlayer(p);
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
				p.hidePlayer(other);
			}
		}
	}

	public void unhidePlayer(Player p, boolean poof) {
		if (!isHidden(p)) return;
		invisiblePlayers.remove(p.getName());
		if (poof) smokeScreenEffect(p.getLocation());
		for (Player other : getPlugin().getServer().getOnlinePlayers()) {
			if (!other.equals(p)) {
				other.showPlayer(p);
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
