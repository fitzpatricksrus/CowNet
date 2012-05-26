package us.fitzpatricksr.cownet.hardcore;

import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetConfig;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class is a simple text file repository for event information in HardCore.  Output only.
 */
public class HardCoreLog extends CowNetConfig {
	private static final DateFormat TIME_STAMP_FORMAT = new SimpleDateFormat("HH:mm:ss");
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private PrintWriter log;
	private String nameRoot;

	public HardCoreLog(JavaPlugin plugin, String name) throws IOException {
		super(plugin);
		nameRoot = name;
	}

	protected String getFileName() {
		return nameRoot + "-" + DATE_FORMAT.format(new Date()) + ".txt";
	}

	public void log(String message) {
		try {
			log = new PrintWriter(new BufferedWriter(new FileWriter(getConfigFile(), true)));
			String timeStamp = TIME_STAMP_FORMAT.format(new Date());
			log.println("[" + timeStamp + "] " + message);
			log.flush();
			log.close();
		} catch (Exception e) {
			//if we can't write log file, whatever
			e.printStackTrace();
		}
	}
}
