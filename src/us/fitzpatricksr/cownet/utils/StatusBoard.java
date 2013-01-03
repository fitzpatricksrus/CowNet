package us.fitzpatricksr.cownet.utils;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.CowNetThingy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class StatusBoard {
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

    public interface StatusSource {
        public CowNetThingy getCowNet();

        public String getGameName();

        public boolean isGaming();

        public Player getPlayer(String playerName);

        public Team getPlayerTeam(String playerName);

        public Set<String> getPlayersOnTeam(Team team);

        public int getScore(String playerName);

        public Collection<String> getPlayers();

        public void debugInfo(String message);
    }

    private static final int numChatLines = 6;
    private StatusSource context;
    private HashMap<String, String> playerMessages;
    private String[] blueChatLines;
    private String[] redChatLines;
    private String redTeam;
    private String blueTeam;
    private int gameStatusTaskId;

    /*
                "> %s:   Team: %-4s   Score: %d",
                "> Red  Team: %s",
                "> Blue Team: %s");
            status.format(1, StringUtils.flatten(getPlayersOnTeam(Team.RED)));
            status.format(2, StringUtils.flatten(getPlayersOnTeam(Team.BLUE)));
     */

    @CowNetThingy.Setting
    private int statusUpdateFrequency = 60;

    public StatusBoard(StatusSource context) {
        this.context = context;
    }

    public void enable() {
        playerMessages = new HashMap<String, String>();
        redChatLines = new String[numChatLines];
        blueChatLines = new String[numChatLines];
        for (int i = 0; i < redChatLines.length; i++) {
            redChatLines[i] = "";
            blueChatLines[i] = "";
        }
        redTeam = "";
        blueTeam = "";
        gameStatusTaskId = 0;
        startStatusTask();
    }

    public void disable() {
        stopStatusTask();
    }

    public void broadcastToAllPlayers(String message) {
        // Game: gameName   Team: team name   Score: ##
        // Red:
        // Blue:
        // chat
        // personal message

        chatLines
        updateForAll();
    }

    public void sendMessage(String playerName, String message) {
        message(playerName, message);
        updateFor(playerName);
    }

    public void broadcastChat(String playerName, String message) {
        broadcastToAllPlayers(playerName + ": " + message);
    }

    public void updateFor(String playerName) {
        Team team = context.getPlayerTeam(playerName);
        Player player = context.getPlayer(playerName);
        String gameName = context.getGameName();

        String line1 = "> %s:   Team: %-4s   Score: %d".format(
                context.isGaming() ? "Playing " + gameName : "Gathering " + gameName,
                team,
                context.getScore(playerName));
        player.sendMessage(line1);
        player.sendMessage(redTeam);
        player.sendMessage(blueTeam);
        player.sendMessage(">-------" + getPlayerMessage(playerName) + "-------------------------------------------".substring(40));
        if (Team.RED == context.getPlayerTeam(playerName)) {
            for (String line : redChatLines) {
                player.sendMessage(line);
            }
        } else {
            for (String line : blueChatLines) {
                player.sendMessage(line);
            }
        }
    }

    public void updateForAll() {
        redTeam = "> Red  Team: " + StringUtils.flatten(context.getPlayersOnTeam(Team.RED));
        blueTeam = ">Blue  Team: " + StringUtils.flatten(context.getPlayersOnTeam(Team.BLUE));
        for (String playerName : context.getPlayers()) {
            updateFor(playerName);
        }
    }

    private void message(String playerName, String msg) {
        playerMessages.put(playerName, msg);
    }

    private String getPlayerMessage(String playerName) {
        String s = playerMessages.get(playerName);
        return (s != null) ? s : "";
    }

    private void stopStatusTask() {
        if (gameStatusTaskId != 0) {
            context.debugInfo("stopTimerTask");
            context.getCowNet().getPlugin().getServer().getScheduler().cancelTask(gameStatusTaskId);
            gameStatusTaskId = 0;
        }
    }

    private void startStatusTask() {
        stopStatusTask();
        JavaPlugin plugin = context.getCowNet().getPlugin();
        gameStatusTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            public void run() {
                updateForAll();
            }
        }, 0, statusUpdateFrequency);
    }
}
