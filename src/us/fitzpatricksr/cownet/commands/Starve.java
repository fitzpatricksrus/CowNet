package us.fitzpatricksr.cownet.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import us.fitzpatricksr.cownet.CowNetThingy;

import java.util.List;

public class Starve extends CowNetThingy {
	private static final int MAX_RADIUS = 100;
	private static final int MAX_DAMAGE = 100;
	@Setting(name = "radius")
	private int standardRadius = 5;
	@Setting(name = "damage")
	private int standardDamage = 20;

	@Override
	protected String getHelpString(CommandSender player) {
		return "Usage: /starve [radius] [damage]";
	}

	//    protected boolean handleCommand(Player player, Command cmd, String[] args) {
	@CowCommand
	protected boolean doStarve(Player player) {
		return doStarve(player, "" + standardRadius, "" + standardDamage);
	}

	@CowCommand
	protected boolean doStarve(Player player, String radius) {
		return doStarve(player, radius, "" + standardDamage);
	}

	@CowCommand
	protected boolean doStarve(Player player, String radiusStr, String damageStr) {
		int range;
		try {
			range = Integer.parseInt(radiusStr);
		} catch (Exception e) {
			player.sendMessage("usage: standardRadius must be a number between 1 and " + MAX_RADIUS);
			return false;
		}
		if (range < 1 || range > MAX_RADIUS) {
			player.sendMessage("usage: standardRadius must be a number between 1 and " + MAX_RADIUS);
			return false;
		}
		int damage;
		try {
			damage = Integer.parseInt(damageStr);
		} catch (Exception e) {
			player.sendMessage("usage: standardDamage must be a number between 1 and " + MAX_DAMAGE);
			return false;
		}
		if (damage < 1 || damage > MAX_RADIUS) {
			player.sendMessage("usage: standardDamage must be a number between 1 and " + MAX_DAMAGE);
			return false;
		}
		player.sendMessage("Starving creatures within a standardRadius of: " + range);
		List<Entity> entities = player.getNearbyEntities(range, range, range);
		int deathToll = 0;
		int wounded = 0;
		for (Entity entity : entities) {
			if (entity instanceof LivingEntity) {
				LivingEntity livingEntity = (LivingEntity) entity;
				livingEntity.damage(damage, player);
				if (livingEntity.getHealth() < 1) {
					deathToll++;
				} else {
					wounded++;
				}
			}
		}
		if (deathToll == 0 && wounded == 0) {
			player.sendMessage("No dead.  No wounded.  Happy day.");
		} else {
			if (deathToll > 0) {
				player.sendMessage("" + deathToll + " creatures died.");
			}
			if (wounded > 0) {
				player.sendMessage("" + deathToll + " creatures wounded.");
			}
		}
		return true;
	}
}

