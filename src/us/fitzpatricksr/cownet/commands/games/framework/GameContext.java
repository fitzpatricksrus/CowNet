package us.fitzpatricksr.cownet.commands.games.framework;

import org.bukkit.entity.Player;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.utils.StatusBoard;
import us.fitzpatricksr.cownet.commands.games.utils.Team;

import java.util.Collection;
import java.util.Set;

public interface GameContext extends StatusBoard.StatusSource {

    @Override
    public CowNetThingy getCowNet();

    @Override
    public String getGameName();

    public boolean isLounging();

    public void endLounging();

    @Override
    public boolean isGaming();

    public void endGame();

    @Override
    public Collection<String> getPlayers();

    public void broadcastToAllPlayers(String message);

    public void sendToPlayer(String playerName, String message);

    @Override
    public Player getPlayer(String playerName);

    @Override
    public Team getPlayerTeam(String playerName);

    @Override
    public Set<String> getPlayersOnTeam(Team team);

    @Override
    public int getScore(String playerName);

    public void addWin(String playerName);

    public void addLoss(String playerName);

    @Override
    public void debugInfo(String message);
}
