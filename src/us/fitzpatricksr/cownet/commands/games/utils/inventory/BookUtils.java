package us.fitzpatricksr.cownet.commands.games.utils.inventory;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import us.fitzpatricksr.cownet.commands.games.utils.Team;

public class BookUtils {
    public static ItemStack createColoredStack(Material material, Team team) {
        if (material == Material.LEATHER_CHESTPLATE ||
                material == Material.LEATHER_BOOTS ||
                material == Material.LEATHER_LEGGINGS ||
                material == Material.LEATHER_HELMET) {
            ItemStack stack = new ItemStack(material, 1);
            LeatherArmorMeta metaData = (LeatherArmorMeta) stack.getItemMeta();
            metaData.setColor(team == Team.BLUE ? Color.BLUE : Color.RED);
            stack.setItemMeta(metaData);
            return stack;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static ItemStack createBook(String title, String author, String[] pages) {
        ItemStack stack = new ItemStack(Material.WRITTEN_BOOK, 1);
        BookMeta meta = (BookMeta) stack.getItemMeta();
        meta.setTitle(title);
        meta.setAuthor(author);
        for (String page : pages) {
            meta.addPage(page);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack createBook(String title, String author, String[] pages, Enchantment... enchantments) {
        ItemStack stack = new ItemStack(Material.WRITTEN_BOOK, 1);
        BookMeta meta = (BookMeta) stack.getItemMeta();
        meta.setTitle(title);
        meta.setAuthor(author);
        for (String page : pages) {
            meta.addPage(page);
        }
        for (Enchantment enchantment : enchantments) {
            meta.addEnchant(enchantment, 1, true);
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
