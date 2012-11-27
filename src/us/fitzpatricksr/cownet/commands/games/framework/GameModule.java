package us.fitzpatricksr.cownet.commands.games.framework;

public interface GameModule {
    public String getName();

    public void startup(GameContext context);

    public void loungeStarted();

    public void playerEnteredLounge(String playerName);

    public void playerLeftLounge(String playerName);

    public void loungeEnded();

    public void gameStarted();

    public void playerEnteredGame(String playerName);

    public void playerLeftGame(String playerName);

    public void gameEnded();

    public void shutdown(GameContext context);
}
