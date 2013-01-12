package us.fitzpatricksr.cownet.commands.games.utils.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * A chest used to temporarily store a player's inventory
 */
public class InventoryLocker {
    private static HashMap<String, ItemStack[]> lockers = new HashMap<String, ItemStack[]>();

    public static ItemStack[] storeInventory(Player player) {
        Inventory playerInventory = player.getInventory();
        ItemStack[] storedInventory = playerInventory.getContents().clone();
        lockers.put(player.getName(), storedInventory);
        playerInventory.clear();
        return storedInventory;
    }

    public static ItemStack[] restoreInventory(Player player) {
        ItemStack[] contents = lockers.get(player.getName());
        if (contents != null) {
            Inventory playerInventory = player.getInventory();
            playerInventory.setContents(contents);
            lockers.remove(player.getName());
        }
        return contents;
    }
}
