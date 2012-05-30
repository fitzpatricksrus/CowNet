package us.fitzpatricksr.cownet.utils;

public class TimeUtils {
	public static long ticksToMillis(long ticks) {
		return ticks * 1000 / 20;
	}

	public static long millisToTicks(long millis) {
		return millis * 20 / 1000;
	}
}
