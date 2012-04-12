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

    public static String durationString(long duration) {
        if (duration <= 0) {
            return "just a few seconds";
        } else {
            return String.format("%02d:%02d:%02d", duration / 3600, (duration % 3600) / 60, (duration % 60));
        }
    }

}
