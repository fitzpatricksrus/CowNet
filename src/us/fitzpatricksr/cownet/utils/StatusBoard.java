package us.fitzpatricksr.cownet.utils;

public class StatusBoard {
    private String[] formats;
    private String[] statusLines;
    private String[] chatLines;

    public StatusBoard(int chats, String... formats) {
        this.formats = formats;
        statusLines = new String[formats.length];
        chatLines = new String[chats];
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
}
