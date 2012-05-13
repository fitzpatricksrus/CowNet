package us.fitzpatricksr.cownet;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import us.fitzpatricksr.cownet.utils.CowNetThingy;

import java.util.List;

public class Bounce extends CowNetThingy {
    private static final int MAX_RADIUS = 100;
    private static final int MAX_VELOCITY = 5;
    private static final int DEFAULT_RADIUS = 5;
    private static final int DEFAULT_VELOCITY = 1;
    @Setting
    private int standardRadius;
    @Setting
    private int standardVelocity;

    private Bounce() {
    }

    public Bounce(JavaPlugin plugin, String permissionRoot) {
        super(plugin, permissionRoot);
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

    @SubCommand
    protected boolean doBounce(Player player) {
        return doBounce(player, "" + standardRadius, "" + standardVelocity);
    }

    @SubCommand
    protected boolean doBounce(Player player, String radius) {
        return doBounce(player, radius, "" + standardVelocity);
    }

    @SubCommand
    protected boolean doBounce(Player player, String radiusStr, String velocityStr) {
        int radius;
        try {
            radius = Integer.parseInt(radiusStr);
        } catch (Exception e) {
            player.sendMessage("usage: radius must be a number between 1 and " + MAX_RADIUS);
            return true;
        }
        if (radius < 1 || radius > MAX_RADIUS) {
            player.sendMessage("usage: radius must be a number between 1 and " + MAX_RADIUS);
            return true;
        }
        int velocity;
        try {
            velocity = Integer.parseInt(velocityStr);
        } catch (Exception e) {
            player.sendMessage("usage: velocity must be a number between 1 and " + MAX_VELOCITY);
            return true;
        }
        if (velocity < 1 || velocity > MAX_RADIUS) {
            player.sendMessage("usage: velocity must be a number between 1 and " + MAX_VELOCITY);
            return true;
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
    }


    public static void main(String[] args) {
        Bounce thingy = new Bounce();
        thingy.findHandlerMethod(null, "doStats", 0);
        thingy.findHandlerMethod(null, "doTp", 1);
        thingy.findHandlerMethod(null, "doSettings", 0);
    }
}
