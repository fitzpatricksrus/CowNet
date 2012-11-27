package us.fitzpatricksr.cownet.commands.games.framework;

import org.bukkit.entity.Player;
import us.fitzpatricksr.cownet.CowNetMod;

import java.util.Collection;

public interface GameContext {
    public enum Team {
        RED,
        BLUE
    }


    public CowNetMod getCowNet();

    public boolean isLounging();

    public boolean isGaming();

    public Collection<String> getPlayers();

    public void broadcastToAllPlayers(String message);

    public Player getPlayer(String playerName);

    public Team getPlayerTeam(String playerName);

    public void addWin(String playerName);

    public void addLoss(String playerName);

    public void debugInfo(String message);
}
