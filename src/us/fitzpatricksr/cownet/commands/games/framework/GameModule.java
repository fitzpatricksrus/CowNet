package us.fitzpatricksr.cownet.commands.games.framework;

/*

Player list is available when loungeStarted and gameStarted are called.  Additional players that enter or
exist the game are signaled through playerEntered and playerLeft methods.

Game methods are called in sequence.

startup - called once when the game is started.  The context may already contain players
loungeStarted


 */
public interface GameModule {
    public String getName();

    public int getLoungeDuration();

    public int getGameDuration();

    public void startup(GameContext context);

    public void loungeStarted();

    public void playerEnteredLounge(String playerName);

    public void playerLeftLounge(String playerName);

    public void loungeEnded();

    public void gameStarted();

    public void playerEnteredGame(String playerName);

    public void playerLeftGame(String playerName);

    public void gameEnded();

    public void shutdown();
}
