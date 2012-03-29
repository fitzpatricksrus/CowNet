package us.fitzpatricksr.cownet.utils;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class CowSerializable implements ConfigurationSerializable {
    public CowSerializable(Map<String, Object> map) {
        Class<? extends CowSerializable> c = this.getClass();
        for (Field f : c.getFields()) {
            String key = f.getName();
            Object value = map.get(key);
            if (value != null) {
                try {
                    f.set(this, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Map<String, Object> serialize() {
        HashMap<String, Object> result = new HashMap<String, Object>();
        Class<? extends CowSerializable> c = this.getClass();
        for (Field f : c.getFields()) {
            String key = f.getName();
            try {
                Object value = f.get(this);
                result.put(key, value);
            } catch (IllegalAccessException e) {
            }
        }
        return result;
    }
}
