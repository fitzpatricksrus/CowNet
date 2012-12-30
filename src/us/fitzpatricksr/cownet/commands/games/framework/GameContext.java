package us.fitzpatricksr.cownet.commands.games.framework;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;
import us.fitzpatricksr.cownet.CowNetThingy;

import java.util.Collection;
import java.util.Set;

public interface GameContext {
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
    }

    public CowNetThingy getCowNet();

    public boolean isLounging();

    public void endLounging();

    public boolean isGaming();

    public void endGame();

    public Collection<String> getPlayers();

    public void broadcastToAllPlayers(String message);

    public Player getPlayer(String playerName);

    public Team getPlayerTeam(String playerName);

    public Set<String> getPlayersOnTeam(Team team);

    public void addWin(String playerName);

    public void addLoss(String playerName);

    public void debugInfo(String message);
}
