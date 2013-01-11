package us.fitzpatricksr.cownet.commands.games.utils;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;

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

    public ChatColor getChatColor() {
        if (this == RED) {
            return ChatColor.RED;
        } else {
            return ChatColor.BLUE;
        }
    }

    public DyeColor getDyeColor() {
        if (this == RED) {
            return DyeColor.RED;
        } else {
            return DyeColor.BLUE;
        }
    }
}