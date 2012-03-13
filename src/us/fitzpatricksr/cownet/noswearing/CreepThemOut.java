package us.fitzpatricksr.cownet.noswearing;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import us.fitzpatricksr.cownet.NoSwearing;

public class CreepThemOut implements NoSwearing.Consequence, Runnable {
	// spawn random mobs in front of them for a while
    private static EntityType[] creatureTypes = new EntityType[] {
        EntityType.SKELETON,
        EntityType.CREEPER,
        EntityType.SPIDER,
        EntityType.ZOMBIE,
    };

	private Random rand = new Random();
	private ConcurrentMap<Player, Integer> remainingCreepers = new ConcurrentHashMap<Player, Integer>();
	private int mobsToSpawn;

	public CreepThemOut(JavaPlugin plugin, String trigger) {
		FileConfiguration config = plugin.getConfig();
        mobsToSpawn = config.getInt(trigger+".mobsToSpawn", 5); // 5 mobs will be spawned
        //we schedule a task that runs every second to spawn ALL mobs for all players that cycle
		plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, this, 0L, 20L);
	}
	public void handleBadWord(Player player, String word) {
		player.sendMessage("You're creepin me out using worlds like "+word+"!");
		remainingCreepers.putIfAbsent(player, mobsToSpawn);
	}
    public void run() {
        Set<Player> players = remainingCreepers.keySet();
        for (Player p : players) {
        	Location loc = p.getLocation();
        	Vector v = loc.getDirection();
        	loc.add(v);
        	loc.add(v);
        	loc.add(v);
        	loc.add(new Vector(0,1,0));
        	if (loc.getBlock().isEmpty()) {
	        	int remaining = remainingCreepers.get(p);
	        	if (remaining == 1) {
	        		remainingCreepers.remove(p);
	        	} else {
	        		remainingCreepers.put(p,  remaining - 1);
	        	}
	        	//then spawn a mob at loc
	        	EntityType type = creatureTypes[rand.nextInt(creatureTypes.length)];
				World world = p.getWorld();
				world.spawnCreature(loc, type);
        	}
        }
    }
}