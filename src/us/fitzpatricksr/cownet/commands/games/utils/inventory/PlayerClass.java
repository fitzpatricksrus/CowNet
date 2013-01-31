package us.fitzpatricksr.cownet.commands.games.utils.inventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A predefined set of items that can be given to a player.
 */
public class PlayerClass {
    private static HashMap<String, ClassKit> classes;  // kit name -> kit
    private static HashMap<String, ClassKit> playerClasses; // playerName -> class

    public static void setClass(Player player, String className) {
        String playerName = player.getName();
        ClassKit oldPlayerClass = playerClasses.get(playerName);
        if (oldPlayerClass != null) {
            oldPlayerClass.removeKit(player);
        }
        ClassKit newPlayerClass = classes.get(className);
        if (newPlayerClass != null) {
            playerClasses.put(playerName, newPlayerClass);
        } else {
            playerClasses.remove(playerName);
        }
    }

    public static String getClass(Player player) {
        ClassKit playerClass = playerClasses.get(player.getName());
        return (playerClass != null) ? playerClass.getName() : "";
    }

    /* Clear the players inventory and make it match the contents of the kit */
    public static void installKit(Player player) {
        getKit(player).installKit(player);
    }

    /* Top up the players current inventory by adding kit items as needed so that
     * they have at least as much of each item as a new kit. */
    public static void refreshKit(Player player) {
        getKit(player).refreshKit(player);
    }

    /* Remove all items of types that may have come from the kit. */
    public static void removeKit(Player player) {
        getKit(player).removeKit(player);
    }

    /* Remove anything not in the kit from inventory */
    public static void enforceKit(Player player) {
        getKit(player).enforceKit(player);
    }

    private static ClassKit getKit(Player player) {
        ClassKit kit = playerClasses.get(player.getName());
        return (kit != null) ? kit : new ClassKit();
    }

    private static class ClassKit {
        private String name;
        private ItemStack[] kitItems;   //this is a raw inventory.  I think ordering is important
        private Set<Material> materialInKit;

        private ClassKit() {
            this.name = "";
            this.kitItems = new ItemStack[0];
            materialInKit = new HashSet<Material>();
        }

        private ClassKit(String name, ItemStack... items) {
            this.name = name;
            this.kitItems = items;
            materialInKit = new HashSet<Material>();
            for (ItemStack stack : items) {
                materialInKit.add(stack.getType());
            }
        }

        public String getName() {
            return name;
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

        /* Remove all items of types that may have come from the kit. */
        public void removeKit(Player player) {
            Inventory playerInventory = player.getInventory();
            for (ItemStack stack : kitItems) {
                int slot = playerInventory.first(stack.getType());
                while (slot >= 0) {
                    playerInventory.clear(slot);
                    slot = playerInventory.first(stack.getType());
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
    }

    //hey jf - so we want to add some serialize/deserialize stuff here?
}
