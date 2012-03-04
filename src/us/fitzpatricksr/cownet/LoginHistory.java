package us.fitzpatricksr.cownet;

import java.io.*;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import com.google.common.io.Files;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LoginHistory extends CowNetThingy {
    enum Filter {
        IN,
        OUT,
        INOUT;

        public boolean shouldDisplayLine(String line) {
            return ((this == IN  || this == INOUT) && line.contains("login")) ||
                    ((this == OUT || this == INOUT) && line.contains("quit"));
        }
    }


    private static final int DEFAULT_MAX_RESULTS = 5;
    private Plugin plugin;
    private HashMap<String, GameMode> gameModeSave = new HashMap<String, GameMode>();
    
    public LoginHistory(JavaPlugin plugin, String permissionRoot, String trigger) {
        super(plugin, permissionRoot, trigger);
        this.plugin = plugin;
        if (isEnabled()) {
            PluginManager pm = plugin.getServer().getPluginManager();
            pm.registerEvent(Event.Type.PLAYER_LOGIN, new PlayerListenerStub(), Event.Priority.Normal, plugin);
            pm.registerEvent(Event.Type.PLAYER_QUIT, new PlayerListenerStub(), Event.Priority.Normal, plugin);
            getLogFile(plugin);
        }
    }

    @Override
    protected String getHelpString(Player player) {
        return "usage: <command> [filter string]";
    }

    private class Output {
        private Player p;
        public Output(Player p) {
            this.p = p;
        }
        public void say(String s) {
            if (p != null) {
                p.sendMessage(s);
            } else {
                logInfo(s);
            }
        }
    }

    @Override
    protected boolean onCommand(Command cmd, String[] args) {
        return onCommand(null, cmd, args);
    }

    @Override
    protected boolean onCommand(Player player, Command cmd, String[] args) {
        int count = DEFAULT_MAX_RESULTS;
        Filter directionFilter = Filter.INOUT;
        Output out = new Output(player);
        
        if (args.length > 0) {
            if (args.length > 2) {
                out.say(getHelpString(player));
            } else {
                // 1 or 2 args
                if (args[0].startsWith("-")) {
                    try {
                        directionFilter = Filter.valueOf(args[0].substring(1).toUpperCase());
                    } catch (Exception e) {
                        out.say(getHelpString(player));
                        return false;
                    }
                    if (args.length == 2) {
                        try {
                            count = Integer.parseInt(args[1]);
                        } catch (Exception e) {
                            // only arg not a number OR a filter type
                            out.say(getHelpString(player));
                            return false;
                        }
                    }
                } else {
                    // first arg was not a filter so it must be the only arg and be a count
                    if (args.length != 1) {
                        out.say(getHelpString(player));
                        return false;
                    } else {
                        try {
                            count = Integer.parseInt(args[0]);
                        } catch (Exception e) {
                            // only arg not a number OR a filter type
                            out.say(getHelpString(player));
                            return false;
                        }
                    }
                }
            }
        }
        String[] results = lastNLogins(getLogFile(plugin), directionFilter, count);
        if (results.length > 0) {
            for (String line : results) {
                out.say(line);
            }
        } else {
            out.say("Nobody has logged in since the log file was created.");
        }
        return true;
    }

    public void onPlayerLogin(PlayerLoginEvent event) {
        File logFile = getLogFile(plugin);
        if (logFile != null) {
            try {
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
                out.println(dateFormat.format(date) + " login:" + getPlayerInfoString(event.getPlayer()));
                out.close();
                logInfo(dateFormat.format(date) + " login:" + event.getPlayer().getPlayerListName());
                setGameMode(event.getPlayer());
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public void onPlayerQuit(PlayerQuitEvent event) {
        File logFile = getLogFile(plugin);
        if (logFile != null) {
            try {
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
                out.println(dateFormat.format(date) + " quit:" + getPlayerInfoString(event.getPlayer()));
                out.close();
                saveGameMode(event.getPlayer());
                logInfo(dateFormat.format(date) + " quit:" + event.getPlayer().getPlayerListName());
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    //--------------------------------------------------
    private void setGameMode(Player player) {
        GameMode oldMode = gameModeSave.get(player.getPlayerListName());
        if (oldMode != null) {
            player.setGameMode(oldMode);
            logInfo("Restoring game mode for "+player.getPlayerListName()+" "+oldMode);
        }
    }
    
    private void saveGameMode(Player player) {
        gameModeSave.put(player.getPlayerListName(), player.getGameMode());
    }

    //--------------------------------------------------
    private class PlayerListenerStub extends org.bukkit.event.player.PlayerListener {
        @Override
        public void onPlayerLogin(PlayerLoginEvent event) {
            LoginHistory.this.onPlayerLogin(event);
        }
        @Override
        public void onPlayerQuit(PlayerQuitEvent event) {
            LoginHistory.this.onPlayerQuit(event);
        }
    }

    //--------------------------------------------------
    private File getLogFile(Plugin plugin) {
        File folder = plugin.getDataFolder();
        if (!folder.exists()) {
            folder.mkdir();
        }
        File result = new File(folder, "loginHistory.txt");
        if (!result.exists()) {
            try {
                result.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                result = null;
            }
        }
        return result;
    }

    private String getPlayerInfoString(Player player) {
//        return player.getPlayerListName()+" "+player.getLocation();
        return player.getPlayerListName();
    }
    
    private String[] lastNLogins(File file, Filter filter, int count) {
        LinkedList<String> results = new LinkedList<String>();
        Charset charset = Charset.forName("US-ASCII");
        try {
            BufferedReader reader = Files.newReader(file, charset);
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (filter.shouldDisplayLine(line)) {
                    if (results.size() > count) {
                        results.removeFirst();
                    }
                    results.offerLast(line);
                } else {
//                    logInfo("skipping: "+line);
                }
            }
            return results.toArray(new String[results.size()]);
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
            return new String[0];
        }
    }
}
