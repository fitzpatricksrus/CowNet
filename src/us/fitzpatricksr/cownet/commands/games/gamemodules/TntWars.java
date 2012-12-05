package us.fitzpatricksr.cownet.commands.games.gamemodules;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.framework.GameContext;
import us.fitzpatricksr.cownet.commands.games.framework.GameModule;
import us.fitzpatricksr.cownet.commands.games.utils.SpawnAndLoungeUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

/**
 */
public class TntWars implements org.bukkit.event.Listener, GameModule {
    private static final long GAME_FREQUENCY = 4;  // 5 times a second?
    private final Random rand = new Random();
    private int gameTaskId = 0;
    private GameContext context;
    private SpawnAndLoungeUtils spawnUtils;
    private HashMap<String, LinkedList<BombPlacement>> placements;

    @CowNetThingy.Setting
    private static int maxBlockPlacements = 1;
    @CowNetThingy.Setting
    private static long explosionDelay = 5 * 1000; // 3 seconds
    @CowNetThingy.Setting
    private static int explosionRadius = 6;
    @CowNetThingy.Setting
    private static int explosivePower = 0;
    @CowNetThingy.Setting
    private static int spawnJiggle = 5;
    @CowNetThingy.Setting
    private static int refillRate = 60;    // how often a player's supply is topped off
    // manual setting
    private static Material explosiveBlockType = Material.TNT;

    @Override
    public String getName() {
        return "TntWars";
    }

    @Override
    public void startup(GameContext context) {
        this.context = context;
        spawnUtils = new SpawnAndLoungeUtils(context.getCowNet(), getName(), spawnJiggle);
        placements = new HashMap<String, LinkedList<BombPlacement>>();
        gameTaskId = 0;
        context.getCowNet().getServer().getPluginManager().registerEvents(this, context.getCowNet());
    }

    @Override
    public void shutdown(GameContext context) {
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
        giveTnt(playerName);
    }

    @Override
    public void playerLeftGame(String playerName) {
        removeTnt(playerName);
    }

    @Override
    public void gameEnded() {
        stopRefillTask();
        for (String player : context.getPlayers()) {
            removeTnt(player);
        }
    }

    // --------------------------------------------------------------
    // ---- Event handlers

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!context.isGaming()) return;
        context.debugInfo("BlockPlaceEvent");
        // if it's an explosive block, update state
        Player player = event.getPlayer();
        String playerName = player.getName();
        if (playerIsInGame(playerName)) {
            // hey jf - do you want to look at the block in hand or block placed?
            if (event.getBlock().getType().equals(explosiveBlockType)) {
                // if they already have an unexploded block, just cancel event
                if (placeBomb(player, event.getBlock().getLocation())) {
                    // remove the item in hand (ex. decrement count by one)
                    // ItemStack itemInHand = player.getItemInHand();
                    // itemInHand.setAmount(itemInHand.getAmount() - 1);
                } else {
                    player.sendMessage("You already have the maximum number of explosive placed.");
                }
                event.setCancelled(true);
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
        removeTnt(playerName);
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
                        giveTnt(playerName);
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

    private void giveTnt(String playerName) {
        //give everyone TNT in hand
        Player player = context.getPlayer(playerName);
        ItemStack oldItemInHand = player.getItemInHand();
        ItemStack itemInHand = new ItemStack(Material.TNT, 1);
        player.setItemInHand(itemInHand);
        player.getInventory().addItem(oldItemInHand);
        player.updateInventory();
    }

    private void removeTnt(String playerName) {
        Player player = context.getPlayer(playerName);
        Inventory inventory = player.getInventory();
        int slot = inventory.first(Material.TNT);
        ItemStack stack = inventory.getItem(slot);
        stack.setAmount(stack.getAmount() - 1);
        inventory.setItem(slot, stack);
        player.updateInventory();
    }

    // --------------------------------------------------------------
    // ---- explosive mgmt

    /* place a bomb at players current location? */
    private boolean placeBomb(Player player, Location loc) {
        String playerName = player.getName();
        // check to see if they've already placed the maximum number of blocks.
        LinkedList<BombPlacement> placementList = placements.get(playerName);
        if (placementList.size() < maxBlockPlacements) {
            // place a bomb
            placementList.addLast(new BombPlacement(player, loc));
            startBombWatcher();
//            accumulatStats(player.getName(), BOMBS_PLACED_KEY, 1);
            return true;
        }
        return false;
    }

    private LinkedList<BombPlacement> getBombsToExplode() {
        LinkedList<BombPlacement> result = new LinkedList<BombPlacement>();
        for (String playerName : placements.keySet()) {
            LinkedList<BombPlacement> bombPlacements = placements.get(playerName);
            while ((bombPlacements.size() > 0) && bombPlacements.getFirst().shouldExplode()) {
                result.add(bombPlacements.getFirst());
                bombPlacements.removeFirst();
            }
        }
        if (placements.size() <= 0) {
            stopBombWatcher();
        }
        return result;
    }

    private class BombPlacement {
        public Player placer;
        public Location location;
        public long blockPlacedTime;

        public BombPlacement(Player placer, Location loc) {
            this.placer = placer;
            this.location = loc;
            blockPlacedTime = System.currentTimeMillis();
            location.getWorld().playEffect(location, Effect.ENDER_SIGNAL, 2);
        }

        public boolean shouldExplode() {
            return System.currentTimeMillis() - blockPlacedTime > explosionDelay;
        }

        public void doExplosion() {
            placer.sendMessage("Boom!");
            location.getWorld().createExplosion(location, explosivePower);
            Server server = context.getCowNet().getServer();
            long radiusSquared = explosionRadius * explosionRadius;
            for (String playerName : context.getPlayers()) {
                Player player = server.getPlayer(playerName);
                if (player != null) {
                    Location playerLocation = player.getLocation();
                    if (location.distanceSquared(playerLocation) < radiusSquared) {
                        //this player is in the blast zone.
                        //chalk up a kill and a death.
//                        accumulatStats(playerName, DEATHS_KEY, 1);
                        if (!playerName.equals(placer.getName())) {
//                            //you only get a kill if you don't kill yourself
//                            accumulatStats(placer.getName(), KILLS_KEY, 1);
                        }
                        Location dest = spawnUtils.getPlayerSpawnPoint();
                        player.teleport(dest);
                        for (int i = 0; i < 10; i++) {
                            location.getWorld().playEffect(dest, Effect.SMOKE, rand.nextInt(9));
                        }
                        player.sendMessage("You were blown up by " + placer.getDisplayName());
                        placer.sendMessage("You blew up " + player.getDisplayName());
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------
    // ---- Event handlers

    private void startBombWatcher() {
        if (gameTaskId == 0) {
            context.debugInfo("startBombWatcher");
            gameTaskId = context.getCowNet().getServer().getScheduler().scheduleSyncRepeatingTask(context.getCowNet(), new Runnable() {
                public void run() {
                    bombWatcher();
                }
            }, GAME_FREQUENCY, GAME_FREQUENCY);
        }
    }

    private void stopBombWatcher() {
        if (gameTaskId != 0) {
            context.debugInfo("stopBombWatcher");
            context.getCowNet().getServer().getScheduler().cancelTask(gameTaskId);
            gameTaskId = 0;
        }
    }

    private void bombWatcher() {
        //debugInfo("bombWatcher");
        for (BombPlacement bomb : getBombsToExplode()) {
            bomb.doExplosion();
        }
    }
}
