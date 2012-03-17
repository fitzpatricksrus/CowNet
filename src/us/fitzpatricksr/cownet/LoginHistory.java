package us.fitzpatricksr.cownet;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

public class LoginHistory extends CowNetThingy implements Listener {
    enum Filter {
        IN,
        OUT,
        INOUT;

        public boolean shouldDisplayLine(String line) {
            return ((this == IN || this == INOUT) && line.contains("login")) ||
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
            pm.registerEvents(this, plugin);
            getLogFile(plugin);
            getChatLogFile(plugin);
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

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerLogin(PlayerLoginEvent event) {
        File logFile = getLogFile(plugin);
        if (logFile != null) {
            try {
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
                out.println(dateFormat.format(date) + " login:" + getPlayerInfoString(event.getPlayer()));
                out.close();
                out = new PrintWriter(new BufferedWriter(new FileWriter(getChatLogFile(plugin), true)));
                out.println(getChatLogString(event.getPlayer(), "<JOIN>"));
                out.close();
                setGameMode(event.getPlayer());
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent event) {
        File logFile = getLogFile(plugin);
        if (logFile != null) {
            try {
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
                out.println(dateFormat.format(date) + " quit:" + getPlayerInfoString(event.getPlayer()));
                out.close();
                out = new PrintWriter(new BufferedWriter(new FileWriter(getChatLogFile(plugin), true)));
                out.println(getChatLogString(event.getPlayer(), "<QUIT>"));
                out.close();
                saveGameMode(event.getPlayer());
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        String message = event.getMessage();

        File logFile = getChatLogFile(plugin);
        if (logFile != null) {
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
                out.println(getChatLogString(event.getPlayer(), message));
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    private String getChatLogString(Player player, String message) {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        World world = player.getWorld();
        return dateFormat.format(date) + " [" + world.getName() + "] " + player.getName() + ": " + message;
    }

    //--------------------------------------------------
    private void setGameMode(Player player) {
        GameMode oldMode = gameModeSave.get(player.getPlayerListName());
        if (oldMode != null) {
            player.setGameMode(oldMode);
            logInfo("Restoring game mode for " + player.getPlayerListName() + " " + oldMode);
        }
    }

    private void saveGameMode(Player player) {
        gameModeSave.put(player.getPlayerListName(), player.getGameMode());
    }

    //--------------------------------------------------
    private File getChatLogFile(Plugin plugin) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        File folder = plugin.getDataFolder();
        logInfo("Chatlog folder: " + folder);
        if (!folder.exists()) {
            folder.mkdir();
        }
        File result = new File(folder, "ChatLog-" + dateFormat.format(date) + ".txt");
        if (!result.exists()) {
            try {
                logInfo("Chatlog file: " + result);
                result.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                result = null;
            }
        }
        return result;
    }

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
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
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
