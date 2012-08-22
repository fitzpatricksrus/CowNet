package us.fitzpatricksr.cownet.commands;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import us.fitzpatricksr.cownet.CowNetThingy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NoSwearing extends CowNetThingy implements Listener {
	private Random rand = new Random();
	private String[] bannedPhrases;
	private Consequence[] consequences;

	@Override
	protected void onEnable() throws Exception {
		String trigger = getTrigger();
		String fileName = getConfigValue("bannedPhrases", "badwords.txt");
		bannedPhrases = loadBadWords(getPlugin(), fileName, trigger);
		consequences = new Consequence[] {
				new SetOnFire(),
				new CreepThemOut(),
				new Launch(),
				new Lightning()
		};
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerChat(PlayerChatEvent event) {
		Player player = event.getPlayer();
		if (hasPermissions(player, "allowed")) {
			return;
		}

		if (scanForBadWords(event.getPlayer(), event.getMessage())) {
			event.setCancelled(true);
		}
	}

	public boolean scanForBadWords(Player player, String textToScan) {
		String text = textToScan.toLowerCase();
		for (String phrase : bannedPhrases) {
			if (text.matches(phrase)) {
				String word = phrase.replace("(", "").replace(")", "").replace(".*", "");
				consequences[rand.nextInt(consequences.length)].handleBadWord(player, word);
				return true;
			} else {
				//logger.info("Didn't see word '"+bannedPhrase+"'");
			}
		}
		return false;
	}

	// interface for all consequences
	public static interface Consequence {
		public void handleBadWord(Player player, String word);
	}

	private String[] loadBadWords(JavaPlugin plugin, String fileName, String trigger) {
		List<String> words = new LinkedList<String>();
		try {
			InputStream inStream = plugin.getResource(fileName);
			BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
			String line = in.readLine();
			while (line != null) {
				words.add(line.toLowerCase());
				line = in.readLine();
			}
			return words.toArray(new String[words.size()]);
		} catch (IOException e) {
			e.printStackTrace();
			debugInfo(trigger + " could not load bad word list.");
			return new String[0];
		}
	}

	private class SetOnFire implements Consequence {
		private int fireTicks;

		public SetOnFire() {
			fireTicks = getConfigValue("fireTicks", 5) * 20; // 5 seconds
		}

		public void handleBadWord(Player player, String word) {
			player.setFireTicks(fireTicks);
			player.sendMessage("You've been very naughty!  Saying words like " + word + "!");
		}
	}

	private class Lightning implements Consequence {
		private int damage;

		public Lightning() {
			damage = getConfigValue("lightningDamage", 5);
		}

		public void handleBadWord(Player player, String word) {
			player.getWorld().strikeLightningEffect(player.getLocation());
			player.damage(damage, player);
			player.sendMessage("I call down lightning for saying words like " + word + "!");
		}
	}

	public class Launch implements Consequence {
		private int launchVelocity;

		public Launch() {
			launchVelocity = getConfigValue("launchVelocity", 5);
		}

		public void handleBadWord(Player player, String word) {
			player.setVelocity(new Vector(0, launchVelocity, 0));
			player.sendMessage("You say " + word + " and I fling you!");
		}
	}

	// spawn random mobs in front of them for a while
	private static EntityType[] creatureTypes = new EntityType[] {
			EntityType.SKELETON,
			EntityType.CREEPER,
			EntityType.SPIDER,
			EntityType.ZOMBIE,
	};

	public class CreepThemOut implements NoSwearing.Consequence, Runnable {
		private Random rand = new Random();
		private ConcurrentMap<Player, Integer> remainingCreepers = new ConcurrentHashMap<Player, Integer>();
		private int mobsToSpawn;

		public CreepThemOut() {
			mobsToSpawn = getConfigValue("mobsToSpawn", 5); // 5 mobs will be spawned
			//we schedule a task that runs every second to spawn ALL mobs for all players that cycle
			getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), this, 0L, 20L);
		}

		public void handleBadWord(Player player, String word) {
			player.sendMessage("You're creepin me out using worlds like " + word + "!");
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
				loc.add(new Vector(0, 1, 0));
				if (loc.getBlock().isEmpty()) {
					int remaining = remainingCreepers.get(p);
					if (remaining == 1) {
						remainingCreepers.remove(p);
					} else {
						remainingCreepers.put(p, remaining - 1);
					}
					//then spawn a mob at loc
					EntityType type = creatureTypes[rand.nextInt(creatureTypes.length)];
					World world = p.getWorld();
					world.spawnEntity(loc, type);
				}
			}
		}
	}
}
