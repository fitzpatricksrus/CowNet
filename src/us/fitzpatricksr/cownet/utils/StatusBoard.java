package us.fitzpatricksr.cownet.utils;

import java.util.HashMap;

public class StatusBoard {
    private String[] formats;
    private String[] statusLines;
    private String[] chatLines;
    private HashMap<String, String> playerMessages;

    public StatusBoard(int chats, String... formats) {
        this.formats = formats;
        statusLines = new String[formats.length];
        for (int i = 0; i < statusLines.length; i++) statusLines[i] = "";
        chatLines = new String[chats];
        for (int i = 0; i < chatLines.length; i++) chatLines[i] = "";
        playerMessages = new HashMap<String, String>();
    }

    public void format(int ndx, Object... args) {
        statusLines[ndx] = String.format(formats[ndx], args);
    }

    public void chat(String msg) {
        for (int i = 1; i < chatLines.length; i++) {
            chatLines[i - 1] = chatLines[i];
        }
        chatLines[chatLines.length - 1] = msg;
    }

    public void message(String playerName, String msg) {
        playerMessages.put(playerName, msg);
    }

    public String getPlayerMessage(String playerName) {
        String s = playerMessages.get(playerName);
        return (s != null) ? s : "";
    }

    public String[] getStatusLines() {
        String[] result = new String[statusLines.length + chatLines.length];
        for (int i = 0; i < statusLines.length; i++) {
            result[i] = statusLines[i];
        }
        for (int i = 0; i < chatLines.length; i++) {
            result[i + statusLines.length] = chatLines[i];
        }
        return result;
    }

    public String[] getStatusLines(String playerName) {
        String[] result = new String[statusLines.length + chatLines.length + 1];
        for (int i = 0; i < statusLines.length; i++) {
            result[i] = statusLines[i];
        }
        for (int i = 0; i < chatLines.length; i++) {
            result[i + statusLines.length] = chatLines[i];
        }
        result[result.length - 1] = getPlayerMessage(playerName);
        return result;
    }
}
