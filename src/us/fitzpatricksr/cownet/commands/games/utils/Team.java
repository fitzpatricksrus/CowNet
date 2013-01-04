package us.fitzpatricksr.cownet.commands.games.utils;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;

public enum Team {
    RED,
    BLUE;

    public Team otherTeam() {
        if (this == RED) {
            return BLUE;
        } else {
            return RED;
        }
    }

    public Material getMaterial() {
        if (this == RED) {
            return Material.LAPIS_BLOCK;
        } else {
            return Material.REDSTONE;
        }
    }

    public ItemStack getWool() {
        if (this == RED) {
            return new ItemStack(Material.WOOL, 1, new Wool(DyeColor.RED).getData());
        } else {
            return new ItemStack(Material.WOOL, 1, new Wool(DyeColor.BLUE).getData());
        }
    }

    public ChatColor getChatColor() {
        if (this == RED) {
            return ChatColor.RED;
        } else {
            return ChatColor.BLUE;
        }
    }
}