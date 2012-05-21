package us.fitzpatricksr.cownet;


import net.minecraft.server.MobEffect;
import net.minecraft.server.MobEffectList;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import us.fitzpatricksr.cownet.utils.CowNetThingy;


public class Eat extends CowNetThingy implements Listener {
    @Setting
    private int golden_apple = 20;
    @Setting
    private int melon = 2;
    @Setting
    private int cooked_chicken = 6;
    @Setting
    private int cookie = 1;
    @Setting
    private int mushroom_soup = 10;
    @Setting
    private int grilled_pork = 8;
    @Setting
    private int pork = 3;
    @Setting
    private int bread = 5;
    @Setting
    private int cooked_fish = 5;
    @Setting
    private int raw_fish = 2;
    @Setting
    private int raw_chicken = 3;
    @Setting
    private int rotten_flesh = 2;
    @Setting
    private int raw_beef = 3;
    @Setting
    private int cooked_beef = 8;
    @Setting
    private int apple = 4;
    @Setting
    private int cake = 4;

    @Setting
    private boolean allowPotions = false;
    @Setting
    private boolean healthRegens = false;
    @Setting
    private double healthRegenMultiplier = 2.0;
    @Setting
    private boolean hungerBarDecreases = false;
    @Setting
    private double hungerBarMultiplier = 2.0;
    @Setting
    private boolean hungerBarDoesDamage = false;
    @Setting
    private boolean allowNegatives = false;
    @Setting
    private boolean nausea = false;
    @Setting
    private int nauseaDuration = 10;

    public Eat(JavaPlugin plugin, String permissionRoot) {
        super(plugin, permissionRoot);
        if (isEnabled()) {
            reload();
        }
    }

    @Override
    protected void reload() {
        allowPotions = getConfigValue("allow-potions", allowPotions);
        healthRegens = getConfigValue("health-regens", healthRegens);
        healthRegenMultiplier = getConfigValue("Health-Regen-Multiplier", healthRegenMultiplier);
        hungerBarDecreases = getConfigValue("HungerBar-Decreases", hungerBarDecreases);
        hungerBarMultiplier = getConfigValue("HungerBar-Multiplier", hungerBarMultiplier);
        hungerBarDoesDamage = getConfigValue("HungerBar-Does-Dmg", hungerBarDoesDamage);
        allowNegatives = getConfigValue("allow-negatives", allowNegatives);
        nausea = getConfigValue("nausea", nausea);
        nauseaDuration = getConfigValue("nauseaDuration", nauseaDuration);

        golden_apple = getConfigValue("golden_apple", golden_apple);
        melon = getConfigValue("melon", melon);
        cooked_chicken = getConfigValue("cooked_chicken", cooked_chicken);
        cookie = getConfigValue("cookie", cookie);
        mushroom_soup = getConfigValue("mushroom_soup", mushroom_soup);
        grilled_pork = getConfigValue("grilled_pork", grilled_pork);
        pork = getConfigValue("pork", pork);
        bread = getConfigValue("bread", bread);
        cooked_fish = getConfigValue("cooked_fish", cooked_fish);
        raw_fish = getConfigValue("raw_fish", raw_fish);
        raw_chicken = getConfigValue("raw_chicken", raw_chicken);
        rotten_flesh = getConfigValue("rotten_flesh", rotten_flesh);
        raw_beef = getConfigValue("raw_beef", raw_beef);
        cooked_beef = getConfigValue("cooked_beef", cooked_beef);
        apple = getConfigValue("apple", apple);
        cake = getConfigValue("cake", cake);
    }

    @Override
    protected String getHelpString(CommandSender sender) {
        return "usage: /Eat help";
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            switch (event.getRegainReason()) {
                //When a player regains health from regenerating due to Peaceful mode (difficulty=0)
                case REGEN:
                    break;
                //When a player regains health from regenerating due to their hunger being satisfied
                case SATIATED:
                    if (!healthRegens) {
                        // disable regaining health because you've had enough to eat
                        event.setCancelled(true);
                    } else {
                        int regenhp = (int) Math.min(20, event.getAmount() * healthRegenMultiplier);
                        event.setAmount(regenhp);
                    }
                    break;
                //When a player regains health from eating consumables
                case EATING:
                    break;
                //When an ender dragon regains health from an ender crystal
                case ENDER_CRYSTAL:
                    break;
                //When a player is healed by a potion or spell
                case MAGIC:
                    //When a player is healed over time by a potion or spell
                case MAGIC_REGEN:
                    if (!allowPotions) {
                        event.setCancelled(true);
                    }
                    break;
                //Any other reason not covered by the reasons above
                case CUSTOM:
                    break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (hungerBarDecreases) {
            int hb = event.getFoodLevel();
            int newHb = (int) Math.min(20, 1 * hungerBarMultiplier);
            event.setFoodLevel(hb - newHb);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.STARVATION && !hungerBarDoesDamage) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)
            return;

        if (!event.hasItem() || !event.hasBlock() && action == Action.RIGHT_CLICK_BLOCK)
            return;

        if (action == Action.RIGHT_CLICK_BLOCK && event.hasBlock() && event.getClickedBlock().getTypeId() == 92)
            onPlayerRightClickCake(player, event);

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK && event.hasItem())
            onPlayerRightClick(player, event);

    }

    private void onPlayerRightClick(Player player, PlayerInteractEvent event) {
        Material type = event.getItem().getType();
        int health = grabFoodHealth(type);
        if (!allowNegatives && health < 0)
            health *= -1;

        if (health == 0 || !eatingAllowed(player, health))
            return;

        setHealth(player, health);
        if (type == Material.MUSHROOM_SOUP) {
            PlayerInventory inventory = player.getInventory();
            ItemStack bowl = new ItemStack(Material.BOWL, 1);
            inventory.addItem(bowl);
        }
        if (type == Material.PORK
                || type == Material.COOKED_FISH
                || type == Material.RAW_BEEF
                || type == Material.RAW_CHICKEN
                || type == Material.ROTTEN_FLESH)
            nauseatePlayer(player);

        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        player.getInventory().removeItem(new ItemStack(type, 1));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!hungerBarDecreases) {
            Player player = event.getPlayer();
            player.setFoodLevel(20);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!hungerBarDecreases) {
            Player player = event.getPlayer();
            player.setFoodLevel(20);
            if (player.getFoodLevel() > 20) {
                player.setFoodLevel(20);
            }
        }
    }

    private void setHealth(Player player, int hp) {
        int newHp = Math.min(20, player.getHealth() + hp);
        player.setHealth(newHp);
    }

    private boolean eatingAllowed(Player player, int value) {
        if (value > 0 && player.getHealth() >= 20)
            return false;

        return value >= 0 || player.getHealth() + value > 0;
    }

    private void onPlayerRightClickCake(Player player, PlayerInteractEvent event) {
        if (event.hasItem() && event.getItem().getTypeId() == 92)
            return;

        int health = getCakeHealth();
        if (health > 0 && !eatingAllowed(player, health)) {
            eatCake(event.getClickedBlock());
            setHealth(player, health);
        }
    }

    private void eatCake(Block block) {
        byte eaten = block.getData();
        if (eaten == 5) {
            block.setTypeId(0);
        } else {
            block.setData((byte) (eaten + 1));
        }
    }

    private void nauseatePlayer(Player player) {
        if (nausea) {
            CraftPlayer cp = (CraftPlayer) player;
            cp.getHandle().addEffect(new MobEffect(MobEffectList.CONFUSION.id, nauseaDuration * 20, 3));
        }

    }

    public int grabFoodHealth(Material item) {
        int itemId = item.getId();
        int health = 0;
        if (itemId == 0)
            return health;

        if (itemId == 322)
            health = golden_apple;

        if (itemId == 360)
            health = melon;

        if (itemId == 366)
            health = cooked_chicken;

        if (itemId == 357)
            health = cookie;

        if (itemId == 282)
            health = mushroom_soup;

        if (itemId == 320)
            health = grilled_pork;

        if (itemId == 319)
            health = pork;

        if (itemId == 297)
            health = bread;

        if (itemId == 350)
            health = cooked_fish;

        if (itemId == 349)
            health = raw_fish;

        if (itemId == 365)
            health = raw_chicken;

        if (itemId == 367)
            health = rotten_flesh;

        if (itemId == 363)
            health = raw_beef;

        if (itemId == 364)
            health = cooked_beef;

        if (itemId == 260)
            health = apple;

        return health;
    }

    public int getCakeHealth() {
        return cake;
    }
}
