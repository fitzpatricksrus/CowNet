package us.fitzpatricksr.cownet.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;
import us.fitzpatricksr.cownet.CowNetThingy;

import java.util.Random;

public class TntSheep extends CowNetThingy implements Listener {
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

	@Override
	protected String getHelpString(CommandSender sender) {
		return "usage: tntsheep";
	}

	private boolean sheepShouldExplode(Entity sheep) {
		boolean y = allowedWorlds.contains(sheep.getWorld().getName());
		boolean z = allowedWorlds.contains("ALL");

		logInfo(sheep.getWorld().getName() + " in " + allowedWorlds + " = " + y);
		logInfo(sheep.getWorld().getName() + " in ALL = " + z);

		return (rand.nextInt(100) <= chanceToExplode) && (allowedWorlds.contains(sheep.getWorld().getName()) || allowedWorlds.contains("ALL"));
	}

	public float getExplosionRadius() {
		return explosionRadius;
	}

	@SuppressWarnings("unused")
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		// Oh please be a sheep
		if ((sheepExplode && event.getEntity() instanceof org.bukkit.entity.Sheep) || (cowsExplode && event.getEntity() instanceof org.bukkit.entity.Cow)) {
			// As if any sheep are victims. They all deserve what they get coming to them
			Entity victim = event.getEntity();
			ProjectileSource killer = GetKiller(event);

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
					if (rand.nextBoolean()) {
						victim.getWorld().createExplosion(victim.getLocation(), getExplosionRadius(), false);

						// Damage the killer
						if (killer instanceof LivingEntity) {
							LivingEntity e = (LivingEntity) killer;
							e.damage(explosionDamage, victim);
						}

						// Make sure the sheep goes away in the explosion.
						victim.remove();
						buildWreckage(killer, victim.getLocation());
					} else {
						// no boom.  just fire.
						victim.setFireTicks(5 * 20);
					}
				}
			} else if (killer != null) {
				debugInfo("killer of type " + killer.getClass().getName());
			}
		}
	}

	/**
	 * Pulled from http://forums.bukkit.org/threads/work-around-for-lack-of-entitydamagebyprojectileevent.31727/#post-581023
	 *
	 * @param event the event.
	 * @return The killer of the sheep.
	 */
	private ProjectileSource GetKiller(EntityDamageEvent event) {
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

	private void buildWreckage(ProjectileSource killer, Location loc) {
	}
}
