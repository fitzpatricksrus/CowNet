package us.fitzpatricksr.cownet.commands.games.utils.inventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Stack;

/**
 * A predefined set of items that can be given to a player.
 */
public class PlayerKit {
/*
PlayerPickupItemEvent
PlayerDropItemEvent
CraftItemEvent
InventoryClickEvent
 */

    private ItemStack[] kitItems;   //this is a raw inventory.  I think ordering is important
    private Player player;
    private Stack<ItemStack[]> packing;

    public PlayerKit(Player player, ItemStack... items) {
        this.player = player;
        this.kitItems = items;
        packing = new Stack<ItemStack[]>();
    }

    public void packInventory() {
        Inventory playerInventory = player.getInventory();
        packing.push(playerInventory.getContents().clone());
        playerInventory.clear();
    }

    public void unpackInventory() {
        Inventory playerInventory = player.getInventory();
        playerInventory.setContents(packing.pop());
    }

    /* Give the player everything in the kit */
    public void refreshKit() {
        Inventory playerInventory = player.getInventory();
        playerInventory.setContents(kitItems.clone());
    }

    /* Remove anything not in the kit from inventory */
    public void enforceKit() {

    }

    public boolean contains(Material stuff) {
        return false;
    }
}
