package us.fitzpatricksr.cownet.utils;

public class StringUtils {
	public static String flatten(Iterable<String> strings) {
		return flatten(strings, ",");
	}

	public static String flatten(Iterable<String> strings, String seperator) {
		StringBuilder result = new StringBuilder();
		for (String s : strings) {
			if (result.length() > 0) {
				result.append(seperator);
			}
			result.append(s);
		}
		return result.toString();
	}

	public static String[] unflatten(String s) {
		return unflatten(s, ",");
	}

	public static String[] unflatten(String s, String seperator) {
		if (s == null || s.length() == 0) return new String[0];
		return s.split(seperator);
	}

	public static String durationString(long durationInSeconds) {
		if (durationInSeconds <= 0) {
			return "just a few seconds";
		} else {
			return String.format("%02d:%02d:%02d", durationInSeconds / 3600, (durationInSeconds % 3600) / 60, (durationInSeconds % 60));
		}
	}

	public static String fitToColumnSize(String value, int columns) {
		// truncate to max size.
		value = value.substring(0, Math.min(columns, value.length()));
		// if too short, pad on the left
		StringBuilder builder = new StringBuilder(value);
		while (builder.length() < columns) {
			builder.insert(0, ' ');
		}
		return builder.toString();
	}

	public static void main(String args[]) {
		String result = fitToColumnSize("123", 5);
		result = fitToColumnSize("123456789", 5);
	}

}
