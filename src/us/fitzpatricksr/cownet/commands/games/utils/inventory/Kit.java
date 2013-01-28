package us.fitzpatricksr.cownet.commands.games.utils.inventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * A predefined set of items that can be given to a player.
 */
public class Kit {
/*
PlayerPickupItemEvent
PlayerDropItemEvent
CraftItemEvent
InventoryClickEvent
 */

    private ItemStack[] kitItems;   //this is a raw inventory.  I think ordering is important
    private Set<Material> materialInKit;

    public Kit(ItemStack... items) {
        this.kitItems = items;
        materialInKit = new HashSet<Material>();
        for (ItemStack stack : items) {
            materialInKit.add(stack.getType());
        }
    }

    /* Clear the players inventory and make it match the contents of the kit */
    public void installKit(Player player) {
        Inventory playerInventory = player.getInventory();
        playerInventory.setContents(kitItems.clone());
    }

    /* Top up the players current inventory by adding kit items as needed so that
     * they have at least as much of each item as a new kit. */
    public void refreshKit(Player player) {
        Inventory playerInventory = player.getInventory();
        for (ItemStack stack : kitItems) {
            int slot = playerInventory.first(stack.getType());
            if (slot >= 0) {
                playerInventory.setItem(slot, stack.clone());
            } else {
                playerInventory.addItem(stack.clone());
            }
        }
    }

    /* Remove anything not in the kit from inventory */
    public void enforceKit(Player player) {
        Inventory inv = player.getInventory();
        for (ItemStack items : inv) {
            if (!materialInKit.contains(items.getType())) {
                inv.remove(items);
            }
        }
    }

    public boolean contains(Material stuff) {
        return materialInKit.contains(stuff);
    }

    //hey jf - so we want to add some serialize/deserialize stuff here?
}
