package us.fitzpatricksr.cownet.commands.games.framework;


import org.bukkit.entity.Player;
import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.utils.Team;

import java.util.Collection;
import java.util.Set;

public class DebugGameContext implements GameContext {
    private GameContext context;
    private String tag;


    public DebugGameContext(final GameContext context, final String tag) {
        this.context = context;
        this.tag = tag;
    }

    @Override
    public CowNetThingy getCowNet() {
//                debugInfo("getCowNet");
        return context.getCowNet();
    }

    @Override
    public String getGameName() {
//                debugInfo("getGameName");
        return context.getGameName();
    }

    @Override
    public boolean isLounging() {
//                debugInfo("isLounging");
        return context.isLounging();
    }

    @Override
    public void endLounging() {
        debugInfo("endLounging");
        context.endLounging();
    }

    @Override
    public boolean isGaming() {
//                debugInfo("isGaming");
        return context.isGaming();
    }

    @Override
    public void endGame() {
        debugInfo("endGame");
        context.endGame();
    }

    @Override
    public Collection<String> getPlayers() {
//                debugInfo("getPlayers");
        return context.getPlayers();
    }

    @Override
    public void broadcastToAllPlayers(String message) {
//                debugInfo("sendMessageToAll");
        context.broadcastToAllPlayers(message);
    }

    @Override
    public void sendToPlayer(String playerName, String message) {
        context.sendToPlayer(playerName, message);
    }

    @Override
    public Player getPlayer(String playerName) {
//                debugInfo("getPlayer");
        return context.getPlayer(playerName);
    }

    @Override
    public Team getPlayerTeam(String playerName) {
//                debugInfo("getPlayerTeam");
        return context.getPlayerTeam(playerName);
    }

    @Override
    public Set<String> getPlayersOnTeam(Team team) {
        return context.getPlayersOnTeam(team);
    }

    @Override
    public int getScore(String playerName) {
//                debugInfo("getScore");
        return context.getScore(playerName);
    }

    @Override
    public void addWin(String playerName) {
//                debugInfo("addWin");
        context.addWin(playerName);
    }

    @Override
    public void addLoss(String playerName) {
//                debugInfo("addLoss");
        context.addLoss(playerName);
    }

    @Override
    public void debugInfo(String message) {
        context.debugInfo(tag + ": " + message);
    }
}