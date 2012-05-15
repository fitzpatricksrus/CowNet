package us.fitzpatricksr.cownet;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetThingy;

import java.util.Random;

public class TntSheep extends CowNetThingy {
    private Random rand = new Random();
    @Setting
    private int chanceToExplode = 25;
    @Setting
    private String allowedWorlds = "ALL";
    @Setting
    private int explosionRadius = 3; //4 = tnt
    @Setting
    private int explosionDamage = 3; //4 = tnt
    @Setting
    private boolean sheepExplode = false;
    @Setting
    private boolean cowsExplode = false;
    @Setting
    private int wreckage = 5;

    public TntSheep(JavaPlugin plugin, String permissionRoot) {
        super(plugin, permissionRoot);
        if (isEnabled()) {
            reload();
            plugin.getServer().getPluginManager().registerEvents(
                    new ExplodingSheepListener(),
                    plugin);
        }
    }

    @Override
    public void reload() {
        chanceToExplode = getConfigInt("chanceToExplode", chanceToExplode);
        explosionRadius = getConfigInt("explosionRadius", explosionRadius);
        explosionDamage = getConfigInt("explosionDamage", explosionDamage);
        allowedWorlds = getConfigString("allowedWords", allowedWorlds);
        sheepExplode = getConfigBoolean("sheepExplode", sheepExplode);
        cowsExplode = getConfigBoolean("cowsExplode", cowsExplode);
        wreckage = getConfigInt("wreckage", wreckage);
        logInfo("(chanceToExplode=" + chanceToExplode + ",explosionRadius=" + explosionRadius + ",explosionDamage=" + explosionDamage + ",wreckage=" + wreckage + ",allowedWorlds=" + allowedWorlds + ")");
    }

    @Override
    protected void updateConfiguration() {
        setConfigInt("chanceToExplode", chanceToExplode);
        setConfigInt("explosionRadius", explosionRadius);
        setConfigInt("explosionDamage", explosionDamage);
        setConfigString("allowedWords", allowedWorlds);
        setConfigBoolean("sheepExplode", sheepExplode);
        setConfigBoolean("cowsExplode", cowsExplode);
        setConfigInt("wreckage", wreckage);
    }

    @Override
    protected String getHelpString(CommandSender sender) {
        return "usage: tntsheep";
    }

    private boolean sheepShouldExplode(Entity sheep) {
        boolean y = allowedWorlds.contains(sheep.getWorld().getName());
        boolean z = allowedWorlds.contains("ALL");

        logInfo(sheep.getWorld().getName() + " in " + allowedWorlds + " = " + y);
        logInfo(sheep.getWorld().getName() + " in ALL = " + z);

        return (rand.nextInt(100) <= chanceToExplode) &&
                (allowedWorlds.contains(sheep.getWorld().getName()) ||
                        allowedWorlds.contains("ALL"));
    }

    public float getExplosionRadius() {
        return explosionRadius;
    }

    private class ExplodingSheepListener implements Listener {
        @SuppressWarnings("unused")
        @EventHandler(priority = EventPriority.HIGH)
        public void onEntityDamage(EntityDamageEvent event) {
            // Oh please be a sheep
            if ((sheepExplode && event.getEntity() instanceof org.bukkit.entity.Sheep) ||
                    (cowsExplode && event.getEntity() instanceof org.bukkit.entity.Cow)) {
                // As if any sheep are victims. They all deserve what they get coming to them
                Entity victim = event.getEntity();
                LivingEntity killer = GetKiller(event);

                // Make sure a killer is found, and make sure it isn't a wolf
                if (killer != null && !(killer instanceof Wolf)) {
                    logInfo("Killer was a " + killer.getClass().getName());
                    // We don't want this to hurt the chances of someone getting an explosion in their face
                    try {
                        Player player = (Player) killer;
                        if (hasPermissions(player, "immune")) {
                            logInfo(player.getName() + " is immune to sheep explosions");
                            return;
                        }
                    } catch (Exception e) {
                        //class cast?  What can happen here?
                        logInfo(killer.getClass().getName() + " can't kill sheep");
                        return;
                    }

                    // Time to whip out the random number generator and get the chance that this sheep is going to blow.
                    if (sheepShouldExplode(victim)) {
                        logInfo("Sheep asploded.");
                        victim.getWorld().createExplosion(victim.getLocation(), getExplosionRadius(), false);

                        // Damage the killer
                        killer.damage(explosionDamage, victim);

                        // Make sure the sheep goes away in the explosion.
                        victim.remove();
                        buildWreckage(killer, victim.getLocation());
                    }
                } else if (killer != null) {
//	                	logger.info("killer of type "+killer.getClass().getName());
                }
            }
        }

        /**
         * Pulled from http://forums.bukkit.org/threads/work-around-for-lack-of-entitydamagebyprojectileevent.31727/#post-581023
         *
         * @param event the event.
         * @return The killer of the sheep.
         */
        private LivingEntity GetKiller(EntityDamageEvent event) {
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

        private void buildWreckage(LivingEntity killer, Location loc) {
            for (int i = 0; i < wreckage; i++) {
                World world = killer.getWorld();
            }
        }
    }
}
