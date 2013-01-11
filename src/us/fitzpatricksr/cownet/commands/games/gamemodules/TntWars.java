package us.fitzpatricksr.cownet.commands.games.gamemodules;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.framework.BasicGameModule;
import us.fitzpatricksr.cownet.commands.games.framework.GameContext;
import us.fitzpatricksr.cownet.commands.games.utils.InventoryUtils;

import java.util.HashMap;
import java.util.LinkedList;

/**
 */
public class TntWars extends BasicGameModule {
    private static final long GAME_FREQUENCY = 4;  // 5 times a second?
    private int gameTaskId = 0;
    private HashMap<String, LinkedList<BombPlacement>> placements;

    @CowNetThingy.Setting
    private int tntWarsMaxBlockPlacements = 1;
    @CowNetThingy.Setting
    private long tntWarsExplosionDelay = 5 * 1000; // 3 seconds
    @CowNetThingy.Setting
    private int tntWarsExplosionRadius = 6;
    @CowNetThingy.Setting
    private int tntWarsExplosivePower = 0;
    @CowNetThingy.Setting
    private int tntWarsSpawnJiggle = 5;
    @CowNetThingy.Setting
    private int tntWarsRefillRate = 60;    // how often a player's supply is topped off
    @CowNetThingy.Setting
    private int tntWarsLoungeDuration = 10; // 30 second loung
    @CowNetThingy.Setting
    private int tntWarsGameDuration = 60 * 3; // 3 minutes max game length
    @CowNetThingy.Setting
    private int tntWarsMinPlayers = 2; // 3 minutes max game length
    // manual setting
    private Material explosiveBlockType = Material.TNT;

    @Override
    public String getName() {
        return "TntWars";
    }

    @Override
    public int getLoungeDuration() {
        return tntWarsLoungeDuration;
    }

    @Override
    public int getGameDuration() {
        return tntWarsGameDuration;
    }

    @Override
    public int getMinPlayers() {
        return tntWarsMinPlayers;
    }

    @Override
    public boolean isTeamGame() {
        return true;
    }

    @Override
    public void startup(GameContext context) {
        super.startup(context);
        placements = new HashMap<String, LinkedList<BombPlacement>>();
        gameTaskId = 0;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        stopBombWatcher();
        placements = null;
    }

    @Override
    public void loungeStarted() {
        for (String playerName : context.getPlayers()) {
            playerEnteredLounge(playerName);
        }
    }

    @Override
    public void playerEnteredLounge(String playerName) {
        setupPlayerInventory(playerName);
        super.playerEnteredLounge(playerName);
    }

    @Override
    public void playerLeftLounge(String playerName) {
        super.playerLeftLounge(playerName);
        removeTnt(playerName);
    }

    @Override
    public void loungeEnded() {
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
        giveTnt(playerName);
    }

    @Override
    public void playerLeftGame(String playerName) {
        super.playerLeftGame(playerName);
        removeTnt(playerName);
    }

    @Override
    public void gameEnded() {
        stopRefillTask();
        for (String player : context.getPlayers()) {
            removeTnt(player);
        }
        super.gameEnded();
    }

    private void setupPlayerInventory(String playerName) {
        Player player = context.getPlayer(playerName);
        player.getInventory().clear();
        player.getInventory().addItem(InventoryUtils.createBook(
                "TNT War Rules", "Master Blaster", new String[]{
                "Blow up players on the other team and you score a point.\n\n" +
                        "Blow up your team members and you loose a point.",
                "TNT replenishes 5 at a time every few seconds.\n\n" +
                        "Games are 3 minutes long."
        }));
    }

    // --------------------------------------------------------------
    // ---- Event handlers

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!context.isLounging()) return;
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

    // --------------------------------------------------------------
    // ---- Event handlers

    private void startRefillTask() {
        if (gameTaskId == 0) {
            context.debugInfo("startRefillTask");
            gameTaskId = context.getCowNet().getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(
                    context.getCowNet().getPlugin(), new Runnable() {
                public void run() {
                    for (String playerName : context.getPlayers()) {
                        giveTnt(playerName);
                    }
                }
            }, tntWarsRefillRate, tntWarsRefillRate);
        }
    }

    private void stopRefillTask() {
        if (gameTaskId != 0) {
            context.debugInfo("stopRefillTask");
            context.getCowNet().getPlugin().getServer().getScheduler().cancelTask(gameTaskId);
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
        if (placementList.size() < tntWarsMaxBlockPlacements) {
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
            return System.currentTimeMillis() - blockPlacedTime > tntWarsExplosionDelay;
        }

        public void doExplosion() {
            placer.sendMessage("Boom!");
            location.getWorld().createExplosion(location, tntWarsExplosivePower);
            Server server = context.getCowNet().getPlugin().getServer();
            long radiusSquared = tntWarsExplosionRadius * tntWarsExplosionRadius;
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
                            context.addWin(placer.getName());
                            context.addLoss(player.getName());
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
            gameTaskId = context.getCowNet().getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(
                    context.getCowNet().getPlugin(), new Runnable() {
                public void run() {
                    bombWatcher();
                }
            }, GAME_FREQUENCY, GAME_FREQUENCY);
        }
    }

    private void stopBombWatcher() {
        if (gameTaskId != 0) {
            context.debugInfo("stopBombWatcher");
            context.getCowNet().getPlugin().getServer().getScheduler().cancelTask(gameTaskId);
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
