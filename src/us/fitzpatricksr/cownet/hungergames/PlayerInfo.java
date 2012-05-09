package us.fitzpatricksr.cownet.hungergames;

import org.bukkit.entity.Player;

public class PlayerInfo {
    private static enum PlayerState {
        TRIBUTE("a tribute in the games"),
        DEAD("an ex-tribute"),
        SPONSOR("a sponsor");

        private String printString;

        PlayerState(String printString) {
            this.printString = printString;
        }

        public String toString() {
            return printString;
        }
    }

    public static long timeBetweenGifts = 1 * 60 * 1000;

    private Player player;
    private PlayerState state;
    private long lastGiftTime = 0;

    public PlayerInfo(Player player) {
        this.player = player;
        this.state = PlayerState.SPONSOR;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean dropGiftToTribute() {
        if (System.currentTimeMillis() - lastGiftTime <= timeBetweenGifts) {
            return false;
        } else {
            lastGiftTime = System.currentTimeMillis();
            return true;
        }
    }

    public boolean isInGame() {
        return state == PlayerState.TRIBUTE;
    }

    public void setIsInGame() {
        lastGiftTime = System.currentTimeMillis();
        state = PlayerState.TRIBUTE;
    }

    public boolean isSponsor() {
        return state == PlayerState.SPONSOR;
    }

    public void setIsSponsor() {
        state = PlayerState.SPONSOR;
    }

    public boolean isOutOfGame() {
        return state == PlayerState.DEAD;
    }

    public void setIsOutOfGame() {
        lastGiftTime = System.currentTimeMillis() - lastGiftTime;
        state = PlayerState.DEAD;
    }

    public PlayerState getState() {
        return state;
    }

    public String toString() {
        return player.getDisplayName() + ": " + getState();
    }
}