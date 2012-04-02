package us.fitzpatricksr.cownet.utils;

import org.bukkit.entity.Player;

/**
 * Basic control of Zombe and CJB mods.  Unfortunately, you can't
 * turn these mods back on once they are off.
 */
public class CowZombeControl {

    private static final String NO_CJB_FLY = "�3 �9 �2 �0 �0 �1";
    private static final String NO_CJB_MAP = "�3 �9 �2 �0 �0 �3";
    private static final String NO_CJB_XRAY = "�3 �9 �2 �0 �0 �2";

    private static final String NO_ZOMBE_CHEAT = "�f �f �2 �0 �4 �8";
    private static final String NO_ZOMBE_FLY = "�f �f �1 �0 �2 �4";
    private static final String NO_ZOMBE_NOCLIP = "�f �f �4 �0 �9 �6";

    public static void setAllowMods(Player player, boolean allowMods) {
        if (allowMods) {
            setAllowFly(player, allowMods);
            setAllowMap(player, allowMods);
            setAllowXray(player, allowMods);
            setAllowNoClip(player, allowMods);
            setAllowCheat(player, allowMods);
        }
    }

    public static void setAllowFly(Player player, boolean allowFly) {
        if (!allowFly) {
            player.sendMessage(NO_CJB_FLY);
            player.sendMessage(NO_ZOMBE_FLY);
        }
    }

    public static void setAllowMap(Player player, boolean allowMap) {
        if (!allowMap) {
            player.sendMessage(NO_CJB_MAP);
        }
    }

    public static void setAllowXray(Player player, boolean allowXray) {
        if (!allowXray) {
            player.sendMessage(NO_CJB_XRAY);
        }
    }

    public static void setAllowNoClip(Player player, boolean allowNoClip) {
        if (!allowNoClip) {
            player.sendMessage(NO_ZOMBE_NOCLIP);
        }
    }

    public static void setAllowCheat(Player player, boolean allowCheat) {
        if (!allowCheat) {
            player.sendMessage(NO_ZOMBE_CHEAT);
        }
    }
}
