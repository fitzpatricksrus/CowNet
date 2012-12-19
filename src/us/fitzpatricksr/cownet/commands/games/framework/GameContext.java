package us.fitzpatricksr.cownet.commands.games.framework;

import org.bukkit.entity.Player;
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
