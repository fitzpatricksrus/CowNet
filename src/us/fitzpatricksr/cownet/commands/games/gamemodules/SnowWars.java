package us.fitzpatricksr.cownet.commands.games.gamemodules;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.CowNetMod;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.framework.GameContext;
import us.fitzpatricksr.cownet.commands.games.framework.GameModule;
import us.fitzpatricksr.cownet.commands.games.utils.InventoryUtils;
import us.fitzpatricksr.cownet.commands.games.utils.SpawnAndLoungeUtils;

import java.util.Random;

/**
 */
public class SnowWars implements org.bukkit.event.Listener, GameModule {
    private Random rand = new Random();
    private GameContext context;
    private SpawnAndLoungeUtils spawnUtils;
    private int gameTaskId = 0;

    @CowNetThingy.Setting
    private int snowWarsSpawnJiggle = 5;
    @CowNetThingy.Setting
    private int snowWarsRefillRate = 60;    // how often a player's supply is topped off
    @CowNetThingy.Setting
    private int snowWarsRefillSize = 5;     // how many we give them per minute
    @CowNetThingy.Setting
    private int snowWarsLoungeDuration = 10; // 30 second lounge
    @CowNetThingy.Setting
    private int snowWarsGameDuration = 60 * 3; // 3 minutes max game length
    @CowNetThingy.Setting
    private int snowWarsFireTicks = 10;  // 1/2 second when you get hit
    @CowNetThingy.Setting
    private int snowWarsMinPlayers = 1;  // 1/2 second when you get hit

    @Override
    public String getName() {
        return "SnowWars";
    }

    @Override
    public int getLoungeDuration() {
        return snowWarsLoungeDuration;
    }

    @Override
    public int getGameDuration() {
        return snowWarsGameDuration;
    }

    @Override
    public int getMinPlayers() {
        return snowWarsMinPlayers;
    }

    @Override
    public boolean isTeamGame() {
        return true;
    }

    @Override
    public void startup(GameContext context) {
        this.context = context;
        CowNetMod plugin = context.getCowNet().getPlugin();
        spawnUtils = new SpawnAndLoungeUtils(plugin, getName(), snowWarsSpawnJiggle);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void shutdown() {
        HandlerList.unregisterAll(this);
        this.context = null;
        spawnUtils = null;
    }

    @Override
    public void loungeStarted() {
        for (String playerName : context.getPlayers()) {
            playerEnteredLounge(playerName);
        }
    }

    @Override
    public void playerEnteredLounge(String playerName) {
        Player player = context.getPlayer(playerName);
        player.getInventory().clear();
        player.getInventory().addItem(InventoryUtils.createBook(
                "SnowWars Rules", "Frosty", new String[]{
                "Hit a player on the other team with snowballs and you score a point.\n\n" +
                        "Hit your team members and you loose a point.",
                "Snowballs replenish 5 at a time every few seconds.\n\n" +
                        "Games are 3 minutes long."
        }));
        Location lounge = spawnUtils.getPlayerLoungePoint();
        if (lounge != null) {
            player.teleport(lounge);
        } else {
            context.debugInfo("Could not find lounge.");
        }
        context.broadcastToAllPlayers(playerName + " is on the " + context.getPlayerTeam(playerName) + " team.");
    }

    @Override
    public void playerLeftLounge(String playerName) {
        context.broadcastToAllPlayers(playerName + " left the game.");
    }

    @Override
    public void loungeEnded() {
    }

    @Override
    public void gameStarted() {
        context.broadcastToAllPlayers("SnowWars has begun.");
        for (String player : context.getPlayers()) {
            playerEnteredGame(player);
        }
        startRefillTask();
    }

    @Override
    public void playerEnteredGame(String playerName) {
        Location spawn = spawnUtils.getPlayerSpawnPoint();
        if (spawn != null) {
            Player player = context.getPlayer(playerName);
            player.teleport(spawn);
        } else {
            context.debugInfo("Could not find spawn");
        }
        giveSnow(playerName);
        context.broadcastToAllPlayers(playerName + " is on the " + context.getPlayerTeam(playerName) + " team.");
    }

    @Override
    public void playerLeftGame(String playerName) {
        removeSnow(playerName);
        context.broadcastToAllPlayers(playerName + " has left the game.");
    }

    @Override
    public void gameEnded() {
        context.broadcastToAllPlayers("SnowWars has eneded.");
        stopRefillTask();
        for (String player : context.getPlayers()) {
            removeSnow(player);
        }
    }

    // --------------------------------------------------------------
    // ---- Event handlers

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!context.isLounging()) return;
        context.debugInfo("onEntityDamagedByEntity");

        if (event.getDamager() instanceof Snowball) {
            Snowball snowball = (Snowball) event.getDamager();
            Entity hitBySnowball = event.getEntity();
            if (hitBySnowball instanceof Player) {
                Player victim = (Player) hitBySnowball;
                if (playerIsInGame(victim.getName())) {
                    LivingEntity snowSource = snowball.getShooter();
                    if (snowSource instanceof Player) {
                        Player shooter = (Player) snowSource;
                        if (playerIsInGame(shooter.getName())) {
                            // OK, someone got plastered.  Accumulate stats.  Play effect.
                            if (context.getPlayerTeam(shooter.getName()) == context.getPlayerTeam(victim.getName())) {
                                // Oops!  You hit someone on your team
                                context.sendToPlayer(shooter.getName(), "Oops!  You hit " + victim.getName() + " who is on your team!");
                                context.addLoss(shooter.getName());
                            } else {
                                context.sendToPlayer(shooter.getName(), "Direct hit on " + victim.getName());
                                context.addWin(shooter.getName());
                                context.addLoss(victim.getName());
                            }
                            event.setDamage(0);
                            smokeScreenEffect(victim.getLocation());
                            victim.setFireTicks(snowWarsFireTicks);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // register a loss and teleport back to spawn point
        Player player = event.getPlayer();
        String playerName = player.getName();
        if (playerIsInGame(playerName)) {
            // Just teleport the person back to spawn here.
            // losses and announcements are done when the player is killed.
            Location loc = (context.isLounging()) ? spawnUtils.getPlayerLoungePoint() : spawnUtils.getPlayerSpawnPoint();
            if (loc != null) {
                // hey jf - you need to jiggle this a bit or everyone will be on top of each other
                // have the player respawn in the game spawn
                event.setRespawnLocation(loc);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        context.debugInfo("PlayerQuitEvent");
        String playerName = event.getPlayer().getName();
        removeSnow(playerName);
    }

    private boolean playerIsInGame(String playerName) {
        return context.getPlayers().contains(playerName);
    }

    // --------------------------------------------------------------
    // ---- Event handlers

    private void startRefillTask() {
        if (gameTaskId == 0) {
            context.debugInfo("startRefillTask");
            JavaPlugin plugin = context.getCowNet().getPlugin();
            gameTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                public void run() {
                    for (String playerName : context.getPlayers()) {
                        giveSnow(playerName);
                    }
                }
            }, snowWarsRefillRate, snowWarsRefillRate);
        }
    }

    private void stopRefillTask() {
        if (gameTaskId != 0) {
            context.debugInfo("stopRefillTask");
            context.getCowNet().getPlugin().getServer().getScheduler().cancelTask(gameTaskId);
            gameTaskId = 0;
        }
    }

    private void giveSnow(String playerName) {
        //give everyone snow in hand
        Player player = context.getPlayer(playerName);
        PlayerInventory inventory = player.getInventory();
        ItemStack oldItem = inventory.getItem(0);
        ItemStack itemInHand = new ItemStack(Material.SNOW_BALL, snowWarsRefillSize);
        inventory.setItem(0, itemInHand);
        if (oldItem != null && oldItem.getType() != Material.SNOW_BALL) {
            inventory.addItem(oldItem);
        }
        player.updateInventory();
    }

    private void removeSnow(String playerName) {
        Player player = context.getPlayer(playerName);
        Inventory inventory = player.getInventory();
        int slot = inventory.first(Material.SNOW_BALL);
        if (slot >= 0) {
            ItemStack stack = inventory.getItem(slot);
            stack.setAmount(stack.getAmount() - 1);
            inventory.setItem(slot, stack);
            player.updateInventory();
        }
    }

    private void smokeScreenEffect(Location location) {
        for (int i = 0; i < 10; i++) {
            location.getWorld().playEffect(location, Effect.SMOKE, rand.nextInt(9));
        }
    }
}
