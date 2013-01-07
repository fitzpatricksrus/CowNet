package us.fitzpatricksr.cownet.commands.games.utils;

import org.bukkit.enchantments.Enchantment;

public class EnchantmentUtils {
    public static Enchantment getEnchantmentByCommonName(String name) {
        name = name.toLowerCase();
        if (name.toLowerCase().equalsIgnoreCase("fire_protection")) return Enchantment.PROTECTION_FIRE;
        if (name.toLowerCase().equalsIgnoreCase("blast_protection")) return Enchantment.PROTECTION_EXPLOSIONS;
        if (name.toLowerCase().equalsIgnoreCase("projectile_protection")) return Enchantment.PROTECTION_PROJECTILE;
        if (name.toLowerCase().equalsIgnoreCase("protection")) return Enchantment.PROTECTION_ENVIRONMENTAL;
        if (name.toLowerCase().equalsIgnoreCase("feather_falling")) return Enchantment.PROTECTION_FALL;
        if (name.toLowerCase().equalsIgnoreCase("respiration")) return Enchantment.OXYGEN;
        if (name.toLowerCase().equalsIgnoreCase("aqua_affinity")) return Enchantment.WATER_WORKER;
        if (name.toLowerCase().equalsIgnoreCase("sharpness")) return Enchantment.DAMAGE_ALL;
        if (name.toLowerCase().equalsIgnoreCase("smite")) return Enchantment.DAMAGE_UNDEAD;
        if (name.toLowerCase().equalsIgnoreCase("bane_of_arthropods")) return Enchantment.DAMAGE_ARTHROPODS;
        if (name.toLowerCase().equalsIgnoreCase("knockback")) return Enchantment.KNOCKBACK;
        if (name.toLowerCase().equalsIgnoreCase("fire_aspect")) return Enchantment.FIRE_ASPECT;
        if (name.toLowerCase().equalsIgnoreCase("looting")) return Enchantment.LOOT_BONUS_MOBS;
        if (name.toLowerCase().equalsIgnoreCase("power")) return Enchantment.ARROW_DAMAGE;
        if (name.toLowerCase().equalsIgnoreCase("punch")) return Enchantment.ARROW_KNOCKBACK;
        if (name.toLowerCase().equalsIgnoreCase("flame")) return Enchantment.ARROW_FIRE;
        if (name.toLowerCase().equalsIgnoreCase("infinity")) return Enchantment.ARROW_INFINITE;
        if (name.toLowerCase().equalsIgnoreCase("efficiency")) return Enchantment.DIG_SPEED;
        if (name.toLowerCase().equalsIgnoreCase("unbreaking")) return Enchantment.DURABILITY;
        if (name.toLowerCase().equalsIgnoreCase("silk_touch")) return Enchantment.SILK_TOUCH;
        if (name.toLowerCase().equalsIgnoreCase("fortune")) return Enchantment.LOOT_BONUS_BLOCKS;
        if (name.toLowerCase().equalsIgnoreCase("thorns")) return Enchantment.THORNS;
        return null;
    }
}
