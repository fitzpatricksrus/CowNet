package us.fitzpatricksr.cownet.commands.games.framework;

public class DebugGameModule implements GameModule {
    private GameContext context;
    private GameModule module;

    public DebugGameModule(GameContext context, GameModule module) {
        this.context = context;
        this.module = module;
    }

    @Override
    public String getName() {
        context.debugInfo(getName() + ".getName()");
        try {
            return module.getName();
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown Game";
        }
    }

    @Override
    public int getLoungeDuration() {
        context.debugInfo(getName() + ".getLoungeDuration()");
        try {
            return module.getLoungeDuration();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int getGameDuration() {
        context.debugInfo(getName() + ".getGameDuration()");
        try {
            return module.getGameDuration();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int getMinPlayers() {
        context.debugInfo(getName() + ".getMinPlayers()");
        try {
            return module.getMinPlayers();
        } catch (Exception e) {
            e.printStackTrace();
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public boolean isTeamGame() {
        context.debugInfo(getName() + ".isTeamGame()");
        try {
            return module.isTeamGame();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void startup(GameContext context) {
        context.debugInfo(getName() + ".startup()");
        try {
            module.startup(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loungeStarted() {
        context.debugInfo(getName() + ".loungeStarted()");
        try {
            module.loungeStarted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void playerEnteredLounge(String playerName) {
        context.debugInfo(getName() + ".playerEnteredLounge()");
        try {
            module.playerEnteredLounge(playerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void playerLeftLounge(String playerName) {
        context.debugInfo(getName() + ".playerLeftLounge()");
        try {
            module.playerLeftLounge(playerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loungeEnded() {
        context.debugInfo(getName() + ".loungeEnded()");
        try {
            module.loungeEnded();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void gameStarted() {
        context.debugInfo(getName() + ".gameStarted()");
        try {
            module.gameStarted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void playerEnteredGame(String playerName) {
        context.debugInfo(getName() + ".playerEnteredGame()");
        try {
            module.playerEnteredGame(playerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void playerLeftGame(String playerName) {
        context.debugInfo(getName() + ".playerLeftGame()");
        try {
            module.playerLeftGame(playerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void gameEnded() {
        context.debugInfo(getName() + ".gameEnded()");
        try {
            module.gameEnded();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        context.debugInfo(getName() + ".shutdown()");
        try {
            module.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
