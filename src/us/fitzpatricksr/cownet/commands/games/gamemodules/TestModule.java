package us.fitzpatricksr.cownet.commands.games.gamemodules;

import us.fitzpatricksr.cownet.CowNetThingy;
import us.fitzpatricksr.cownet.commands.games.framework.GameContext;
import us.fitzpatricksr.cownet.commands.games.framework.GameModule;

/**
 */
public class TestModule implements GameModule {
    @CowNetThingy.Setting
    private int loungeDuration = 29; // 30 second loung
    @CowNetThingy.Setting
    private int gameDuration = 31; // 3 minutes max game length
    private GameContext context;
    private String name;

    public TestModule(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getLoungeDuration() {
        return loungeDuration;
    }

    @Override
    public int getGameDuration() {
        return gameDuration;
    }

    @Override
    public int getMinPlayers() {
        return 1;
    }

    @Override
    public boolean isTeamGame() {
        return true;
    }

    @Override
    public void startup(GameContext context) {
        this.context = context;
        dumpContext("startup");
    }

    @Override
    public void shutdown() {
        dumpContext("shutdown");
        this.context = null;
    }

    @Override
    public void loungeStarted() {
        dumpContext("loungeStarted");
    }

    @Override
    public void playerEnteredLounge(String playerName) {
        dumpContext("playerEnteredLounge: " + playerName);
    }

    @Override
    public void playerLeftLounge(String playerName) {
        dumpContext("playerLeftLounge: " + playerName);
    }

    @Override
    public void loungeEnded() {
        dumpContext("loungeEnded");
    }

    @Override
    public void gameStarted() {
        dumpContext("gameStarted");
    }

    @Override
    public void playerEnteredGame(String playerName) {
        dumpContext("playerEnteredGame: " + playerName);
    }

    @Override
    public void playerLeftGame(String playerName) {
        dumpContext("playerLeftGame: " + playerName);
    }

    @Override
    public void gameEnded() {
        dumpContext("gameEnded");
    }

    private void dumpContext(String message) {
        context.debugInfo(message);
        context.debugInfo(" - isLounging:" + context.isLounging());
        context.debugInfo(" - isGaming:" + context.isGaming());
        for (String playerName : context.getPlayers()) {
            context.debugInfo(" - " + playerName + ":" + context.getPlayerTeam(playerName));
        }
    }
}
