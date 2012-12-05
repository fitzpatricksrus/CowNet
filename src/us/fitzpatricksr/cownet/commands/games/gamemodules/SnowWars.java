package us.fitzpatricksr.cownet.commands.games.gamemodules;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.framework.GameContext;
import us.fitzpatricksr.cownet.commands.games.framework.GameModule;
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
    private static int spawnJiggle = 5;
    @CowNetThingy.Setting
    private static int refillRate = 60;    // how often a player's supply is topped off
    @CowNetThingy.Setting
    private static int refillSize = 5;     // how many we give them per minute

    @Override
    public String getName() {
        return "SnowWars";
    }

    @Override
    public void startup(GameContext context) {
        this.context = context;
        spawnUtils = new SpawnAndLoungeUtils(context.getCowNet(), getName(), spawnJiggle);
    }

    @Override
    public void shutdown(GameContext context) {
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
        Location lounge = spawnUtils.getPlayerLoungePoint();
        Player player = context.getPlayer(playerName);
        player.teleport(lounge);
    }

    @Override
    public void playerLeftLounge(String playerName) {
    }

    @Override
    public void loungeEnded() {
    }

    @Override
    public void gameStarted() {
        for (String player : context.getPlayers()) {
            playerEnteredGame(player);
        }
        startRefillTask();
    }

    @Override
    public void playerEnteredGame(String playerName) {
        Location spawn = spawnUtils.getPlayerSpawnPoint();
        Player player = context.getPlayer(playerName);
        player.teleport(spawn);
        giveSnow(playerName);
    }

    @Override
    public void playerLeftGame(String playerName) {
        removeSnow(playerName);
    }

    @Override
    public void gameEnded() {
        stopRefillTask();
        for (String player : context.getPlayers()) {
            removeSnow(player);
        }
    }

    // --------------------------------------------------------------
    // ---- Event handlers

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!context.isGaming()) return;
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
                            event.setDamage(0);
                            smokeScreenEffect(victim.getLocation());
                            context.addWin(shooter.getName());
                            context.addLoss(victim.getName());
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
            gameTaskId = context.getCowNet().getServer().getScheduler().scheduleSyncRepeatingTask(context.getCowNet(), new Runnable() {
                public void run() {
                    for (String playerName : context.getPlayers()) {
                        giveSnow(playerName);
                    }
                }
            }, refillRate, refillRate);
        }
    }

    private void stopRefillTask() {
        if (gameTaskId != 0) {
            context.debugInfo("stopRefillTask");
            context.getCowNet().getServer().getScheduler().cancelTask(gameTaskId);
            gameTaskId = 0;
        }
    }


    private void giveSnow(String playerName) {
        //give everyone snow in hand
        Player player = context.getPlayer(playerName);
        ItemStack oldItemInHand = player.getItemInHand();
        ItemStack itemInHand = new ItemStack(Material.SNOW_BALL, refillSize);
        player.setItemInHand(itemInHand);
        player.getInventory().addItem(oldItemInHand);
        player.updateInventory();
    }

    private void removeSnow(String playerName) {
        Player player = context.getPlayer(playerName);
        Inventory inventory = player.getInventory();
        int slot = inventory.first(Material.SNOW_BALL);
        ItemStack stack = inventory.getItem(slot);
        stack.setAmount(stack.getAmount() - 1);
        inventory.setItem(slot, stack);
        player.updateInventory();
    }

    private void smokeScreenEffect(Location location) {
        for (int i = 0; i < 10; i++) {
            location.getWorld().playEffect(location, Effect.SMOKE, rand.nextInt(9));
        }
    }
}
