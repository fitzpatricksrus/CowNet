package us.fitzpatricksr.cownet.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * This utility handles reporting and changing simple bean fields
 */
public class SettingsTwiddler {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Setting {
    }

    @Setting
    private int intSetting = 10;
    @Setting
    private boolean booleanSetting = true;
    @Setting
    private String stringSetting = "some string";

    private String hiddenVariableNotASetting = "NotASetting";

    public static Map<String, String> getSettings(Object source) {
        HashMap<String, String> result = new HashMap<String, String>();
        Class c = source.getClass();
        while (c != null && !c.equals(Object.class)) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(Setting.class)) {
                    f.setAccessible(true);
                    try {
                        result.put(f.getName(), f.get(source).toString());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } finally {
                        f.setAccessible(false);
                    }
                }
            }
            c = c.getSuperclass();
        }
        return result;
    }

    public static boolean setSetting(Object source, String settingName, String settingValue) {
        Class c = source.getClass();
        while (c != null && !c.equals(Object.class)) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(Setting.class) && field.getName().equalsIgnoreCase(settingName)) {
                    try {
                        field.setAccessible(true);
                        if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
                            field.set(source, Boolean.valueOf(settingValue));
                            return true;
                        } else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
                            field.set(source, Integer.valueOf(settingValue));
                            return true;
                        } else if (field.getType().equals(String.class)) {
                            field.set(source, settingValue);
                            return true;
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } finally {
                        field.setAccessible(false);
                    }
                }
            }
            c = c.getSuperclass();
        }
        return false;
    }
}
