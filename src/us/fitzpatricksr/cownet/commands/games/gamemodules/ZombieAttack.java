package us.fitzpatricksr.cownet.commands.games.gamemodules;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.framework.BasicGameModule;
import us.fitzpatricksr.cownet.commands.games.framework.GameContext;
import us.fitzpatricksr.cownet.commands.games.utils.SpawnAndLoungeUtils;
import us.fitzpatricksr.cownet.commands.games.utils.inventory.BookUtils;

import java.util.Random;

/**
 */
public class ZombieAttack extends BasicGameModule {
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
    private int zombieAttackLoungeDuration = 10; // 30 second lounge
    @CowNetThingy.Setting
    private int zombieAttackGameDuration = 60 * 3; // 3 minutes max game length
    @CowNetThingy.Setting
    private int zombieAttackWaveRate = 15 * 20;    // how often they spawn
    @CowNetThingy.Setting
    private int zombieAttackMinPlayers = 2;

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
    public int getMinPlayers() {
        return zombieAttackMinPlayers;
    }

    @Override
    public boolean isTeamGame() {
        return false;
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
        Player player = context.getPlayer(playerName);
        Location lounge = spawnUtils.getPlayerLoungePoint();
        if (lounge != null) {
            player.teleport(lounge);
        } else {
            context.debugInfo("Could not find lounge");
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
        super.gameStarted();
        startSpawnTask();
    }

    @Override
    public void playerEnteredGame(String playerName) {
        setupPlayerInventory(playerName);
        super.playerEnteredGame(playerName);
    }

    @Override
    public void playerLeftGame(String playerName) {
        context.broadcastToAllPlayers(playerName + " has left the game.");
    }

    @Override
    public void gameEnded() {
        super.gameEnded();
        stopSpawnTask();
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
            } catch (ClassNotFoundException ignored) {
            }

            try {
                // Temporary solution until org.bukkit.entity.NPC is widely deployed.
                if (Class.forName("org.bukkit.entity.NPC").isAssignableFrom(ent.getClass())) {
                    continue;
                }
            } catch (ClassNotFoundException ignored) {
            }

            ent.remove();
        }
    }

    private void setupPlayerInventory(String playerName) {
        Player player = context.getPlayer(playerName);
        player.getInventory().clear();
        player.getInventory().addItem(BookUtils.createBook(
                "The Mobs are Coming", "Master Mob", new String[]{
                "Mobs will drop out of the sky.  Stay alive longer than other and you will score a Win."
        }));
    }

    // --------------------------------------------------------------
    // ---- Event handlers

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntityDamage(EntityDamageEvent event) {
        Entity entityDamaged = event.getEntity();
        if (!(entityDamaged instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) entityDamaged;
        LivingEntity killer = getAttacker(event);
        if (killer instanceof Player) {
            int victimHealth = victim.getHealth();
            int damage = event.getDamage();
            if (damage >= victimHealth) {
                // OK, this will finish them off.  Award a kill...or not
                Player player = (Player) killer;
                String playerName = player.getName();
                if (victim instanceof Player) {
                    //Oh my god!  They killed Kenny!
                    context.addLoss(playerName);
                    context.sendToPlayer(playerName, "You killed one of the good guys.  1 demerit.");
                } else {
                    //Good job.
                    context.addWin(playerName);
                }
            }
        }
    }

    private LivingEntity getAttacker(EntityDamageEvent event) {
        //check for damage by entity (and arrow)
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent nEvent = (EntityDamageByEntityEvent) event;
            if ((nEvent.getDamager() instanceof Arrow)) {
                //This will retrieve the arrow object
                Arrow a = (Arrow) nEvent.getDamager();
                //This will retrieve the person who shot the arrow
                return a.getShooter();
            } else {
                if (nEvent.getDamager() instanceof LivingEntity) {
                    return (LivingEntity) nEvent.getDamager();
                } else {
                    return null;
                }
            }

        }
        return null;
    }

    private void startSpawnTask() {
        if (gameTaskId == 0) {
            context.debugInfo("startSpawnTask");
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

    private void stopSpawnTask() {
        if (gameTaskId != 0) {
            context.debugInfo("stopSpawnTask");
            context.getCowNet().getPlugin().getServer().getScheduler().cancelTask(gameTaskId);
            gameTaskId = 0;
        }
    }
}
