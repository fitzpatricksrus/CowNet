package us.fitzpatricksr.cownet;

import org.bukkit.command.Command;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class StarveCommand extends CowNetThingy {
    private static final int MAX_RADIUS = 100;
    private static final int MAX_DAMAGE = 100;
    private static final int DEFAULT_RADIUS = 5;
    private static final int DEFAULT_DAMAGE = 20;
    private int standardRadius;
    private int standardDamage;

    public StarveCommand(JavaPlugin plugin, String permissionRoot, String trigger) {
        super(plugin, permissionRoot, trigger);
        if (isEnabled()) {
            reload();
        }
    }

    protected void reload() {
        this.standardRadius = getConfigInt("radius", DEFAULT_RADIUS);
        this.standardDamage = getConfigInt("damage", DEFAULT_DAMAGE);
        logInfo("radius=" + standardRadius + ", damage=" + standardDamage);
    }

    protected boolean onCommand(Player player, Command cmd, String[] args) {
        if (!hasPermissions(player)) {
            player.sendMessage("Sorry, you don't have permission");
            return false;
        }

        if (args.length > 2) {
            player.sendMessage("usage: /" + cmd.getName() + " [standardRadius:" + standardRadius + "] [standardDamage:" + standardDamage + "]");
            return false;
        }
        int range = standardRadius;
        if (args.length >= 1) {
            try {
                range = Integer.parseInt(args[0]);
            } catch (Exception e) {
                player.sendMessage("usage: standardRadius must be a number between 1 and " + MAX_RADIUS);
                return false;
            }
            if (range < 1 || range > MAX_RADIUS) {
                player.sendMessage("usage: standardRadius must be a number between 1 and " + MAX_RADIUS);
                return false;
            }
        }
        int damage = standardDamage;
        if (args.length >= 2) {
            try {
                damage = Integer.parseInt(args[1]);
            } catch (Exception e) {
                player.sendMessage("usage: standardDamage must be a number between 1 and " + MAX_DAMAGE);
                return false;
            }
            if (damage < 1 || damage > MAX_RADIUS) {
                player.sendMessage("usage: standardDamage must be a number between 1 and " + MAX_DAMAGE);
                return false;
            }
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

