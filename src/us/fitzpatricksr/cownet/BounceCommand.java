package us.fitzpatricksr.cownet;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import us.fitzpatricksr.cownet.utils.CowNetThingy;
import us.fitzpatricksr.cownet.utils.SettingsTwiddler;

import java.util.List;

public class BounceCommand extends CowNetThingy {
    private static final int MAX_RADIUS = 100;
    private static final int MAX_VELOCITY = 5;
    private static final int DEFAULT_RADIUS = 5;
    private static final int DEFAULT_VELOCITY = 1;
    @SettingsTwiddler.Setting
    private int standardRadius;
    @SettingsTwiddler.Setting
    private int standardVelocity;

    public BounceCommand(JavaPlugin plugin, String permissionRoot, String trigger) {
        super(plugin, permissionRoot, trigger);
        if (isEnabled()) {
            reload();
        }
    }

    @Override
    protected void reload() {
        this.standardRadius = getConfigInt("radius", DEFAULT_RADIUS);
        this.standardVelocity = getConfigInt("velocity", DEFAULT_VELOCITY);
        logInfo("radius=" + standardRadius + ",velocity=" + standardVelocity);
    }

    @Override
    protected String getHelpString(CommandSender sender) {
        return "usage: bounce [radius] [velocity]";
    }

    @Override
    protected boolean handleCommand(Player player, Command cmd, String[] args) {
        if (hasPermissions(player)) {
            if (args.length > 2) {
                player.sendMessage("usage: /" + cmd.getName() + " [radius:" + standardRadius + "] [velocity:" + standardVelocity + "]");
                return false;
            }
            int radius = standardRadius;
            if (args.length >= 1) {
                try {
                    radius = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    player.sendMessage("usage: radius must be a number between 1 and " + MAX_RADIUS);
                    return false;
                }
                if (radius < 1 || radius > MAX_RADIUS) {
                    player.sendMessage("usage: radius must be a number between 1 and " + MAX_RADIUS);
                    return false;
                }
            }
            int velocity = standardVelocity;
            if (args.length >= 2) {
                try {
                    velocity = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    player.sendMessage("usage: velocity must be a number between 1 and " + MAX_VELOCITY);
                    return false;
                }
                if (velocity < 1 || velocity > MAX_RADIUS) {
                    player.sendMessage("usage: velocity must be a number between 1 and " + MAX_VELOCITY);
                    return false;
                }
            }
            final Vector vector = new Vector(0, velocity, 0);
            List<Entity> entities = player.getNearbyEntities(radius, radius, radius);
            if (entities.size() == 0) {
                player.sendMessage("Nothing to bounce within a radius of " + radius);
            } else {
                player.sendMessage("Bouncing creatures " + velocity + " within a radius of " + radius);
                for (Entity entity : entities) {
                    if (entity instanceof LivingEntity) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        livingEntity.setVelocity(vector);
                    }
                }
            }
            return true;
        } else {
            player.sendMessage("You must be a player to bounce creatures.");
            return false;
        }

    }
}

