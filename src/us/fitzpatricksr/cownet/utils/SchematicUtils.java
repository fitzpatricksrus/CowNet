package us.fitzpatricksr.cownet.utils;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.schematic.SchematicFormat;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.FilenameFilter;

public class SchematicUtils {
    public static boolean placeSchematic(File schematic, Location location) {
        try {
            World w = location.getWorld();
            int maxBlocks = 10000;
            EditSession session = new EditSession(new BukkitWorld(w), maxBlocks);
            CuboidClipboard clipBoard = SchematicFormat.MCEDIT.load(schematic);
            Vector v = new Vector(location.getX(), location.getY(), location.getZ());
            clipBoard.place(session, v, false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static File[] getSchematics(File folder) {
        if ((folder != null) && folder.exists() && folder.isDirectory()) {
            return folder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return s.endsWith(".schematic");
                }
            });
        } else {
            return new File[0];
        }
    }
}
