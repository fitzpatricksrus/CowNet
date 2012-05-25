package us.fitzpatricksr.cownet.hardcore;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import us.fitzpatricksr.cownet.utils.CowNetConfig;
import us.fitzpatricksr.cownet.utils.CowNetThingy;
import us.fitzpatricksr.cownet.utils.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@SerializableAs("PlayerState")
public class PlayerState implements ConfigurationSerializable, Comparable<PlayerState> {
	static {
		ConfigurationSerialization.registerClass(PlayerState.class);
	}

	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	@CowNetThingy.Setting
	public static long deathDuration = 60;  //default time before you are removed from game in seconds.
	@CowNetThingy.Setting
	public static double timeOutGrowth = 2.0; //the rate at which timeout increases.
	@CowNetThingy.Setting
	public static long liveTimeout = 7 * 24 * 60 * 60;  //live players keep things going for 7 days.

	public String name;         // come si chiama
	public long lastActivity;   // either time when player should be removed from allPlayers list
	public boolean isLive;      // hey?  You still there?
	public boolean wasOp;       // was this player an op outside hardcore

	public int deathCount;      // you died how many times?
	public long timeInGame;
	public long blocksPlaced;
	public long blocksBroken;
	public long mobsKilled;
	private volatile long lastEnteredWorld; //if in hardcore, then non-zero.   Used to accumulate total time.

	public int compareTo(PlayerState other) {
		return (rawScore() >= other.rawScore()) ? -1 : 1;
	}

	private double rawScore() {
		double activity = blocksPlaced + blocksBroken + mobsKilled;
		double efficiency = activity / Math.max(1, timeInGame / 1000 / 60 / 60); //blocks per hour
		double deathPenalty = Math.max(1, deathCount);
		double afterDeath = efficiency / deathPenalty;

		double secondsElapsed = (System.currentTimeMillis() - lastActivity) / 1000;
		double activityWindow = 0;
		if (isLive) {
			activityWindow = liveTimeout / (deathCount + 1);
		} else {
			activityWindow = (long) (deathDuration * Math.pow(timeOutGrowth, deathCount - 1));
		}
		double windowLeft = 1.0 - (secondsElapsed / activityWindow) * 0.5;

		return afterDeath * windowLeft;
	}

	public void dumpScoreComponents() {
		double activity = blocksPlaced + blocksBroken + mobsKilled;
		double efficiency = activity / Math.max(1, timeInGame / 1000 / 60 / 60);
		double deathPenalty = Math.max(1, deathCount);
		double afterDeath = efficiency / deathPenalty;

		double secondsElapsed = (System.currentTimeMillis() - lastActivity) / 1000;
		double activityWindow = 0;
		if (isLive) {
			activityWindow = liveTimeout / (deathCount + 1);
		} else {
			activityWindow = (long) (deathDuration * Math.pow(timeOutGrowth, deathCount - 1));
		}
		double windowLeft = 1.0 - (secondsElapsed / activityWindow) * 0.5;
		double score = afterDeath * windowLeft;

		System.out.println(name);
		System.out.println("        activity=" + activity);
		System.out.println("      efficiency=" + efficiency);
		System.out.println("    deathPenalty=" + deathPenalty);
		System.out.println("  secondsElapsed=" + secondsElapsed);
		System.out.println("  activityWindow=" + activityWindow);
		System.out.println("      windowLeft=" + windowLeft);
		System.out.println("           score=" + score);
	}

	public void setIsLive() {
		if (!isLive) {
			isLive = true;
			// start accumulating game time again
			playerEnteredHardCore();
		}
	}

	public void setIsDead() {
		playerLeftHardCore();
		isLive = false;
		deathCount++;
		noteActivity();
	}

	public void setWasOp(boolean wasOp) {
		this.wasOp = wasOp;
	}

	public void noteActivity() {
		lastActivity = System.currentTimeMillis();
	}

	public long getSecondsTillTimeout() {
		long secondsElapsed = (System.currentTimeMillis() - lastActivity) / 1000;
		if (isLive) {
			long timeRequired = liveTimeout / (deathCount + 1);
			long timeLeft = timeRequired - secondsElapsed;
			return Math.max(0, timeLeft);
		} else {
			long timeRequired = (long) (deathDuration * Math.pow(timeOutGrowth, deathCount - 1));
			long timeLeft = timeRequired - secondsElapsed;
			return Math.max(0, timeLeft);
		}
	}

	public long getSecondsInHardcore() {
		if (lastEnteredWorld == 0) {
			return timeInGame / 1000;
		} else {
			return (timeInGame + (System.currentTimeMillis() - lastEnteredWorld)) / 1000;
		}
	}

	public void playerEnteredHardCore() {
		if (isLive) {
			// only accumulate time stats unless the player is alive
			lastEnteredWorld = System.currentTimeMillis();
		} else {
			lastEnteredWorld = 0;
		}
	}

	public void playerLeftHardCore() {
		if (isLive) {
			// only accumulate time stats unless the player is alive
			timeInGame = timeInGame + (System.currentTimeMillis() - lastEnteredWorld);
		} else {
			lastEnteredWorld = 0;
		}
	}

	public void accrueBlockPlaced() {
		blocksPlaced++;
	}

	public void accrueBlockBroken() {
		blocksBroken++;
	}

	public void accrueMobKilled() {
		mobsKilled++;
	}

	public PlayerState(String name) {
		this.name = name;
		this.isLive = true;
		noteActivity();
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(name);
		builder.append("  ");
		builder.append((isLive) ? "live  " : "dead  ");
		builder.append("deathCount:");
		builder.append(deathCount);
		builder.append("  activity: ");
		builder.append(dateFormat.format(new Date(lastActivity)));
		builder.append("  timeInGame: ");
		builder.append(StringUtils.durationString(timeInGame / 1000));
		builder.append("  lastEnteredWorld: ");
		builder.append(dateFormat.format(new Date(lastEnteredWorld)));
		return builder.toString();
	}

	// --- serialize/deserialize support
	public static PlayerState deserialize(Map<String, Object> args) {
		return new PlayerState(args);
	}

	public static PlayerState valueOf(Map<String, Object> map) {
		return new PlayerState(map);
	}

	public PlayerState(Map<String, Object> map) {
		CowNetConfig.deserialize(this, map);
	}

	@Override
	public Map<String, Object> serialize() {
		return CowNetConfig.serialize(this);
	}
}
