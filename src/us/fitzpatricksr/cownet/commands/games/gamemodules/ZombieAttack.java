package us.fitzpatricksr.cownet.commands.games.gamemodules;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.CowNetMod;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.framework.GameContext;
import us.fitzpatricksr.cownet.commands.games.framework.GameModule;
import us.fitzpatricksr.cownet.commands.games.utils.SpawnAndLoungeUtils;

import java.util.Random;

/**
 */
public class ZombieAttack implements org.bukkit.event.Listener, GameModule {
    private Random rand = new Random();
    private GameContext context;
    private SpawnAndLoungeUtils spawnUtils;
    private int gameTaskId = 0;
    private int nextEntityCount = 0;    // how many mobs to spawn the next time around.
    private EntityType[] mobs = new EntityType[]{
            EntityType.CREEPER,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.GIANT,
            EntityType.ZOMBIE,
            EntityType.SLIME,
            EntityType.GHAST,
            EntityType.PIG_ZOMBIE,
            EntityType.ENDERMAN,
            EntityType.CAVE_SPIDER,
            EntityType.SILVERFISH,
            EntityType.BLAZE,
            EntityType.MAGMA_CUBE,
            EntityType.WITHER,
            EntityType.BAT,
            EntityType.WITCH,
            EntityType.SNOWMAN,
    };

    @CowNetThingy.Setting
    private int zombieAttackSpawnJiggle = 5;
    @CowNetThingy.Setting
    private int zombieAttackLoungeDuration = 30; // 30 second loung
    @CowNetThingy.Setting
    private int zombieAttackGameDuration = 60 * 3; // 3 minutes max game length
    @CowNetThingy.Setting
    private int zombieAttackWaveRate = 60;    // how often a player's supply is topped off

    @Override
    public String getName() {
        return "ZombieAttack";
    }

    @Override
    public int getLoungeDuration() {
        return zombieAttackLoungeDuration;
    }

    @Override
    public int getGameDuration() {
        return zombieAttackGameDuration;
    }

    @Override
    public void startup(GameContext context) {
        this.context = context;
        CowNetMod plugin = context.getCowNet().getPlugin();
        spawnUtils = new SpawnAndLoungeUtils(plugin, getName(), zombieAttackSpawnJiggle);
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
        context.broadcastToAllPlayers("A game of SnowWars is gathering.");
        for (String playerName : context.getPlayers()) {
            playerEnteredLounge(playerName);
        }
    }

    @Override
    public void playerEnteredLounge(String playerName) {
        Location lounge = spawnUtils.getPlayerLoungePoint();
        Player player = context.getPlayer(playerName);
        player.teleport(lounge);
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
        Player player = context.getPlayer(playerName);
        player.teleport(spawn);
        context.broadcastToAllPlayers(playerName + " is on the " + context.getPlayerTeam(playerName) + " team.");
    }

    @Override
    public void playerLeftGame(String playerName) {
        context.broadcastToAllPlayers(playerName + " has left the game.");
    }

    @Override
    public void gameEnded() {
        context.broadcastToAllPlayers("SnowWars has eneded.");
        stopRefillTask();
        // we should remove all the mobs
        Location spawn = spawnUtils.getPlayerSpawnPoint();
        World world = spawn.getWorld();

        // remove all the stuff we spawned...and everything else.
        for (LivingEntity ent : world.getLivingEntities()) {
            if (ent instanceof HumanEntity) {
                continue;
            }

            if (ent instanceof Animals) {
                continue;
            }

            if (ent instanceof Tameable && ((Tameable) ent).isTamed()) {
                continue; // tamed wolf
            }

            try {
                // Temporary solution to fix Golems being butchered.
                if (Class.forName("org.bukkit.entity.Golem").isAssignableFrom(ent.getClass())) {
                    continue;
                }
            } catch (ClassNotFoundException e) {
            }

            try {
                // Temporary solution until org.bukkit.entity.NPC is widely deployed.
                if (Class.forName("org.bukkit.entity.NPC").isAssignableFrom(ent.getClass())) {
                    continue;
                }
            } catch (ClassNotFoundException e) {
            }

            ent.remove();
        }
    }

    // --------------------------------------------------------------
    // ---- Event handlers

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
    }

    private boolean playerIsInGame(String playerName) {
        return context.getPlayers().contains(playerName);
    }

    // --------------------------------------------------------------
    // ---- Event handlers

    private void startRefillTask() {
        if (gameTaskId == 0) {
            context.debugInfo("startRefillTask");
            nextEntityCount = 1;
            JavaPlugin plugin = context.getCowNet().getPlugin();
            gameTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                public void run() {
                    for (String playerName : context.getPlayers()) {
                        Player player = context.getPlayer(playerName);
                        Location loc = player.getLocation();
                        loc = spawnUtils.jigglePoint(loc);
                        World world = loc.getWorld();
                        for (int i = 0; i < nextEntityCount; i++) {
                            loc.add(0, 5, 0);
                            world.spawnEntity(loc, mobs[rand.nextInt(mobs.length)]);
                        }
                    }
                    nextEntityCount++;
                }
            }, zombieAttackWaveRate, zombieAttackWaveRate);
        }
    }

    private void stopRefillTask() {
        if (gameTaskId != 0) {
            context.debugInfo("stopRefillTask");
            context.getCowNet().getPlugin().getServer().getScheduler().cancelTask(gameTaskId);
            gameTaskId = 0;
        }
    }
}
