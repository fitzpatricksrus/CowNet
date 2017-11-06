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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.framework.BasicGameModule;
import us.fitzpatricksr.cownet.commands.games.utils.inventory.BookUtils;

/**
 */
public class SnowWars extends BasicGameModule {
    private int gameTaskId = 0;

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
    public void playerEnteredLounge(String playerName) {
        setupPlayerInventory(playerName);
        super.playerEnteredLounge(playerName);
    }

    @Override
    public void playerLeftLounge(String playerName) {
        clearPlayerInventory(playerName);
        super.playerLeftLounge(playerName);
    }

    @Override
    public void gameStarted() {
        super.gameStarted();
        startRefillTask();
    }

    @Override
    public void playerEnteredGame(String playerName) {
        setupPlayerInventory(playerName);
        super.playerEnteredGame(playerName);
        giveSnow(playerName);
    }

    @Override
    public void playerLeftGame(String playerName) {
        removeSnow(playerName);
        clearPlayerInventory(playerName);
        super.playerLeftGame(playerName);
    }

    @Override
    public void gameEnded() {
        stopRefillTask();
        for (String player : context.getPlayers()) {
            removeSnow(player);
        }
        super.gameEnded();
    }

    private void setupPlayerInventory(String playerName) {
        Player player = context.getPlayer(playerName);
        player.getInventory().clear();
        player.getInventory().addItem(BookUtils.createBook(
                "SnowWars Rules", "Frosty", new String[]{
                "Hit a player on the other team with snowballs and you score a point.\n\n" +
                        "Hit your team members and you loose a point.",
                "Snowballs replenish 5 at a time every few seconds.\n\n" +
                        "Games are 3 minutes long."
        }));
    }

    private void clearPlayerInventory(String playerName) {
        Player player = context.getPlayer(playerName);
        player.getInventory().clear();
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
                    LivingEntity snowSource = snowball._INVALID_getShooter();
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
    public void onPlayerQuit(PlayerQuitEvent event) {
        context.debugInfo("PlayerQuitEvent");
        String playerName = event.getPlayer().getName();
        removeSnow(playerName);
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
