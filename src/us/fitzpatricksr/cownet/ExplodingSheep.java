package us.fitzpatricksr.cownet;

import java.util.Random;

import org.bukkit.command.Command;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ExplodingSheep extends CowNetThingy {
    private Random rand = new Random();
    private int chanceToExplode = 25;
    private String allowedWorlds = "guest,guestworld,d74g0n,funky";
    private float explosionRadius = 3; //4 = tnt
    private int explosionDamage = 3; //4 = tnt
    private boolean sheepExplode = false;
    private boolean cowsExplode = false;

	public ExplodingSheep(JavaPlugin plugin, String permissionRoot, String trigger) {
        super(plugin, permissionRoot, trigger);
        if (isEnabled()) {
            this.chanceToExplode = getConfigInt("chanceToExplode", this.chanceToExplode);
            this.explosionRadius = getConfigInt("explosionRadius", (int)this.explosionRadius);
            this.explosionDamage = getConfigInt("explosionDamage", this.explosionDamage);
            this.allowedWorlds = getConfigString("allowedWords", this.allowedWorlds);
            this.sheepExplode = getConfigBoolean("sheepExplode", this.sheepExplode);
            this.cowsExplode = getConfigBoolean("cowsExplode", this.cowsExplode);

    		plugin.getServer().getPluginManager().registerEvent(
    				Event.Type.ENTITY_DAMAGE, 
    				new ExplodingSheepListener(), 
    				Event.Priority.High, 
    				plugin);
            logInfo("(chanceToExplode="+chanceToExplode+",explosionRadius="+explosionRadius+",explosionDamage="+explosionDamage+",allowedWorlds="+allowedWorlds+")");
        }
	}

    @Override
    protected String getHelpString(Player player) {
        return "usage: tntsheep";
    }

    @Override
    protected boolean onCommand(Player player, Command cmd, String[] args) {
        if (!hasPermissions(player)) {
            player.sendMessage("Sorry, you don't have permissions");
            return false;
        } else {
            player.sendMessage("Huh?  Srsly?  Commands for sheep?");
            return false;
		}
	}
	
	private boolean sheepShouldExplode(Entity sheep) {
		boolean result = (rand.nextInt(100) <= chanceToExplode) &&
				(allowedWorlds.contains(sheep.getWorld().getName()) ||
                    allowedWorlds.equalsIgnoreCase("ALL"));
        /*
		if (result) {
			logInfo("Sheep should explode");
		} else {
			if (!allowedWorlds.contains(sheep.getWorld().getName())) {
                logInfo("Sheep in this world don't explode");
			} else {
                logInfo("This sheep is a dud.");
			}
		} */
		return result;
	}
	
	public float getExplosionRadius() {
		return explosionRadius;
	}
	
	private class ExplodingSheepListener extends EntityListener {	    
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
                        if (((Player)killer).hasPermission("explodingsheep.immune")) return;
                    } catch (Exception e) {
                        //class cast?  What can happen here?
                    }

                    // Time to whip out the random number generator and get the chance that this sheep is going to blow.
                    if (sheepShouldExplode(victim)) {
                        victim.getWorld().createExplosion(victim.getLocation(), getExplosionRadius(), false);

                       // Damage the killer
                       killer.damage(explosionDamage, victim);

                        // Make sure the sheep goes away in the explosion.
                       victim.remove();
                    }
                } else if (killer != null){
//	                	logger.info("killer of type "+killer.getClass().getName());
                }
            }
	    }

	    /**
	     * Pulled from http://forums.bukkit.org/threads/work-around-for-lack-of-entitydamagebyprojectileevent.31727/#post-581023
	     * @param event
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
	                    return (LivingEntity)nEvent.getDamager();
	                } else {
	                    return null;
	                }
	            }
	            
	        }
	        return null;
	    }
	}
}
