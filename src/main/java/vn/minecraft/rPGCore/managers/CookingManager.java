package vn.minecraft.rPGCore.managers;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.cooking.CookingEngine;
import vn.minecraft.rPGCore.cooking.FoodItemData;
import vn.minecraft.rPGCore.cooking.NutritionStats;

import java.util.*;

public class CookingManager implements Listener {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final CookingEngine cookingEngine;

    private final Map<Location, FinishedPot> activePots = new HashMap<>();
    private final Set<Location> cookingPots = new HashSet<>();

    public final NamespacedKey COOKED_TIME_KEY;
    public final NamespacedKey MAX_FRESHNESS_KEY;
    public final NamespacedKey DURATION_KEY;
    public final NamespacedKey BOREDOM_KEY;

    public final NamespacedKey STAT_DMG, STAT_CDR, STAT_MS, STAT_AS, STAT_PEN, STAT_REGEN, STAT_HP;
    public final NamespacedKey FAT_DEBUFF_KEY;
    public final NamespacedKey STAT_LS, STAT_CRIT, STAT_DROP, POTION_EFFECTS_KEY;

    public CookingManager(JavaPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.cookingEngine = new CookingEngine();

        this.COOKED_TIME_KEY = new NamespacedKey(plugin, "food_cooked_time");
        this.MAX_FRESHNESS_KEY = new NamespacedKey(plugin, "food_max_freshness");
        this.DURATION_KEY = new NamespacedKey(plugin, "food_duration");
        this.BOREDOM_KEY = new NamespacedKey(plugin, "food_boredom");

        this.STAT_DMG = new NamespacedKey(plugin, "food_stat_dmg");
        this.STAT_CDR = new NamespacedKey(plugin, "food_stat_cdr");
        this.STAT_MS = new NamespacedKey(plugin, "food_stat_ms");
        this.STAT_AS = new NamespacedKey(plugin, "food_stat_as");
        this.STAT_PEN = new NamespacedKey(plugin, "food_stat_pen");
        this.STAT_REGEN = new NamespacedKey(plugin, "food_stat_regen");
        this.STAT_HP = new NamespacedKey(plugin, "food_stat_hp");
        this.FAT_DEBUFF_KEY = new NamespacedKey(plugin, "food_fat_debuff");

        this.STAT_LS = new NamespacedKey(plugin, "food_stat_ls");
        this.STAT_CRIT = new NamespacedKey(plugin, "food_stat_crit");
        this.STAT_DROP = new NamespacedKey(plugin, "food_stat_drop");
        this.POTION_EFFECTS_KEY = new NamespacedKey(plugin, "food_potion_effects");

        startItemScanner();
    }

    private static class FinishedPot {
        ItemStack resultItem;
        int scoopsLeft;
        Color particleColor;

        public FinishedPot(ItemStack resultItem, int scoopsLeft, Color particleColor) {
            this.resultItem = resultItem;
            this.scoopsLeft = scoopsLeft;
            this.particleColor = particleColor;
        }
    }

    private void startItemScanner() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<Location, FinishedPot>> activeIt = activePots.entrySet().iterator();
                while (activeIt.hasNext()) {
                    Map.Entry<Location, FinishedPot> entry = activeIt.next();
                    if (entry.getKey().getBlock().getType() != Material.WATER_CAULDRON) activeIt.remove();
                }
                cookingPots.removeIf(loc -> loc.getBlock().getType() != Material.WATER_CAULDRON);

                Bukkit.getWorlds().forEach(world -> {
                    for (Item item : world.getEntitiesByClass(Item.class)) {
                        if (item.isDead()) continue;
                        if (itemManager.getItemId(item.getItemStack()) == null) continue;

                        Location loc = item.getLocation();
                        Block currentBlock = loc.getBlock();
                        Block blockBelow = loc.clone().subtract(0, 0.5, 0).getBlock();

                        Block cauldronBlock = null;
                        if (currentBlock.getType() == Material.WATER_CAULDRON) cauldronBlock = currentBlock;
                        else if (blockBelow.getType() == Material.WATER_CAULDRON) cauldronBlock = blockBelow;

                        if (cauldronBlock != null) {
                            Block campfireBlock = cauldronBlock.getRelative(BlockFace.DOWN);
                            if (isHeatSource(campfireBlock.getType())) {
                                if (!cookingPots.contains(cauldronBlock.getLocation()) && !activePots.containsKey(cauldronBlock.getLocation())) {
                                    startCookingProcess(cauldronBlock);
                                }
                            }
                        }
                    }
                });

                for (Location loc : activePots.keySet()) {
                    Color c = activePots.get(loc).particleColor;
                    Particle.DustOptions dust = new Particle.DustOptions(c, 1.2f);
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0.5, 0.9, 0.5), 3, 0.2, 0.0, 0.2, dust);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private boolean isHeatSource(Material mat) {
        return mat == Material.CAMPFIRE || mat == Material.SOUL_CAMPFIRE || mat == Material.FIRE || mat == Material.SOUL_FIRE || mat == Material.MAGMA_BLOCK;
    }

    private void startCookingProcess(Block cauldronBlock) {
        Location loc = cauldronBlock.getLocation();
        cookingPots.add(loc);
        cauldronBlock.getWorld().playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 1f, 1f);

        org.bukkit.entity.TextDisplay hologram = loc.getWorld().spawn(loc.clone().add(0.5, 1.2, 0.5), org.bukkit.entity.TextDisplay.class);
        hologram.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        hologram.setDefaultBackground(false);
        hologram.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));

        new BukkitRunnable() {
            int ticks = 0;
            final int MAX_TICKS = 400; // 20s
            final List<Item> ingredients = new ArrayList<>();

            @Override
            public void run() {
                if (cauldronBlock.getType() != Material.WATER_CAULDRON) {
                    cookingPots.remove(loc);
                    hologram.remove();
                    this.cancel();
                    return;
                }

                int currentTotal = 0;
                for (Item i : ingredients) currentTotal += i.getItemStack().getAmount();

                for (Item item : loc.getWorld().getEntitiesByClass(Item.class)) {
                    if (item.isDead()) continue;
                    Block iBlock = item.getLocation().getBlock();
                    Block iBlockBelow = item.getLocation().clone().subtract(0, 0.5, 0).getBlock();

                    if ((iBlock.equals(cauldronBlock) || iBlockBelow.equals(cauldronBlock)) && !ingredients.contains(item)) {
                        String ingredientId = itemManager.getItemId(item.getItemStack());
                        if (ingredientId != null) {
                            if (currentTotal < 9) {
                                int amt = item.getItemStack().getAmount();
                                if (currentTotal + amt <= 9) {
                                    ingredients.add(item);
                                    item.setPickupDelay(9999);
                                    currentTotal += amt;
                                } else {
                                    int needed = 9 - currentTotal;
                                    ItemStack originalStack = item.getItemStack();
                                    ItemStack suckedStack = originalStack.clone();
                                    suckedStack.setAmount(needed);

                                    originalStack.setAmount(originalStack.getAmount() - needed);
                                    item.setItemStack(originalStack);
                                    item.setVelocity(new org.bukkit.util.Vector((Math.random() - 0.5) * 0.5, 0.5, (Math.random() - 0.5) * 0.5));

                                    Item suckedEntity = loc.getWorld().dropItem(item.getLocation(), suckedStack);
                                    suckedEntity.setPickupDelay(9999);
                                    suckedEntity.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                                    ingredients.add(suckedEntity);
                                    currentTotal = 9;
                                }
                            } else {
                                item.setVelocity(new org.bukkit.util.Vector((Math.random() - 0.5) * 0.5, 0.5, (Math.random() - 0.5) * 0.5));
                            }
                        } else {
                            item.setVelocity(new org.bukkit.util.Vector((Math.random() - 0.5) * 0.5, 0.5, (Math.random() - 0.5) * 0.5));
                        }
                    }
                }

                if (ticks % 20 == 0) {
                    int secondsLeft = (MAX_TICKS - ticks) / 20;
                    hologram.setText("§e♨ Đang nấu...\n§f" + secondsLeft + "s §8| §a" + currentTotal + "§8/§a9 món");
                }

                double cX = loc.getX() + 0.5, cZ = loc.getZ() + 0.5, wY = loc.getY() + 0.65;
                for (int i = 0; i < ingredients.size(); i++) {
                    Item item = ingredients.get(i);
                    if (item.isDead()) continue;
                    item.teleport(new Location(loc.getWorld(), cX + Math.cos(ticks*0.1+i)*0.2, wY + Math.sin(ticks*0.3+i)*0.15, cZ + Math.sin(ticks*0.15+i)*0.2));
                }

                if (ticks % 3 == 0) {
                    loc.getWorld().spawnParticle(Particle.BUBBLE_POP, cX, wY + 0.2, cZ, 3, 0.2, 0.1, 0.2, 0.05);
                    loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, cX, wY + 0.3, cZ, 1, 0.1, 0.1, 0.1, 0.01);
                }

                if (ticks >= MAX_TICKS) {
                    finishCooking(cauldronBlock, ingredients);
                    hologram.remove();
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void finishCooking(Block cauldronBlock, List<Item> ingredients) {
        Location loc = cauldronBlock.getLocation();
        cookingPots.remove(loc);

        List<ItemManager.ItemData> recipeData = new ArrayList<>();
        for (Item item : ingredients) {
            String id = itemManager.getItemId(item.getItemStack());
            if (id != null) {
                ItemManager.ItemData data = itemManager.getItemData(id);
                if (data != null) {
                    int amount = item.getItemStack().getAmount();
                    for (int i = 0; i < amount; i++) {
                        recipeData.add(data);
                    }
                }
            }
            item.remove();
        }

        FoodItemData resultData = cookingEngine.evaluate(recipeData);
        NutritionStats stats = new NutritionStats(recipeData);

        ItemStack resultItem = buildFoodItem(resultData, stats);

        Color pColor = Color.fromRGB(200, 150, 50);
        if (resultData.isRuined) pColor = Color.fromRGB(80, 100, 80);
        else if (stats.meat + stats.fish >= 1.0) pColor = Color.fromRGB(200, 50, 0);
        else if (stats.veg >= 1.0) pColor = Color.fromRGB(50, 200, 50);

        cauldronBlock.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0.5, 0.8, 0.5), 15, 0.2, 0.2, 0.2, 0.1);

        activePots.put(loc, new FinishedPot(resultItem, 6, pColor));
    }

    private ItemStack buildFoodItem(FoodItemData data, NutritionStats stats) {
        ItemStack item = new ItemStack(data.material);
        ItemMeta meta = item.getItemMeta();
        NamespacedKey SPICE_KEY = new NamespacedKey(plugin, "food_spice");
        meta.getPersistentDataContainer().set(SPICE_KEY, PersistentDataType.DOUBLE, stats.getTotalSpice());
        meta.setDisplayName(data.name);

        List<String> lore = new ArrayList<>();
        lore.add("§8Đang xác định độ tươi...");
        lore.add("");
        lore.add("§f§lThông tin chung:");
        lore.add("§7 ⌚ No Căng: §a" + data.durationMinutes + " Phút");
        lore.add("§7 🤢 Tỉ lệ Ngấy: §e" + data.boredomPoints + "%");
        lore.add("");
        lore.add("§f§lBảng Dinh Dưỡng:");
        if (stats.meat > 0) lore.add("§7 🥩 Thịt: §c" + String.format("%.2f", stats.meat));
        if (stats.fish > 0) lore.add("§7 🐟 Cá: §b" + String.format("%.2f", stats.fish));
        if (stats.veg > 0) lore.add("§7 🥬 Rau Củ: §a" + String.format("%.2f", stats.veg));
        if (stats.starch > 0) lore.add("§7 🥖 Tinh Bột: §e" + String.format("%.2f", stats.starch));
        if (stats.fat > 0) lore.add("§7 🧈 Chất Béo: §e" + String.format("%.2f", stats.fat));
        if (stats.getTotalSpice() > 0) lore.add("§7 🧂 Gia Vị: §f" + String.format("%.2f", stats.getTotalSpice()));

        if (data.bonusDmg != 0 || data.cdr != 0 || data.moveSpeed != 0 || data.atkSpeed != 0 || data.armorPen != 0 || data.regenRate != 0 || data.maxHpBonus != 0) {
            lore.add("");
            lore.add("§f§lHiệu Ứng Cơ Thể:");
            if (data.bonusDmg != 0) lore.add("§7 🗡 Sát thương: §c" + (data.bonusDmg > 0 ? "+" : "") + String.format("%.1f", data.bonusDmg) + "%");
            if (data.cdr != 0) lore.add("§7 ⏳ Hồi chiêu: §9" + (data.cdr > 0 ? "+" : "") + String.format("%.1f", data.cdr) + "%");
            if (data.moveSpeed != 0) lore.add("§7 💨 Tốc chạy: §f" + (data.moveSpeed > 0 ? "+" : "") + String.format("%.1f", data.moveSpeed) + "%");
            if (data.atkSpeed != 0) lore.add("§7 ⚡ Tốc đánh: §e" + (data.atkSpeed > 0 ? "+" : "") + String.format("%.1f", data.atkSpeed) + "%");
            if (data.armorPen != 0) lore.add("§7 🛡 Xuyên giáp: §4" + (data.armorPen > 0 ? "+" : "") + String.format("%.1f", data.armorPen) + "%");
            if (data.regenRate != 0) lore.add("§7 💖 Hồi máu: §a" + (data.regenRate > 0 ? "+" : "") + String.format("%.1f", data.regenRate) + "%");
            if (data.maxHpBonus != 0) lore.add("§7 ❤ Máu tối đa: §c" + (data.maxHpBonus > 0 ? "+" : "") + String.format("%.1f", data.maxHpBonus));
            if (data.lifeSteal != 0) lore.add("§7 🦇 Hút máu: §c" + (data.lifeSteal > 0 ? "+" : "") + String.format("%.1f", data.lifeSteal) + "%");
            if (data.critRate != 0) lore.add("§7 🎯 Tỉ lệ Chí mạng: §e" + (data.critRate > 0 ? "+" : "") + String.format("%.1f", data.critRate) + "%");
            if (data.dropRate != 0) lore.add("§7 🎁 Tỉ lệ Rớt đồ: §b" + (data.dropRate > 0 ? "+" : "") + String.format("%.1f", data.dropRate) + "%");
        }

        if (data.hasFatDebuff) {
            lore.add("§7 ⚠ Quá Ngấy: §c-20% Tốc chạy");
        }

        if (!data.flavorTags.isEmpty()) {
            lore.add("");
            lore.addAll(data.flavorTags);
        }

        meta.getPersistentDataContainer().set(COOKED_TIME_KEY, PersistentDataType.LONG, System.currentTimeMillis());
        meta.getPersistentDataContainer().set(MAX_FRESHNESS_KEY, PersistentDataType.INTEGER, data.freshnessMinutes);
        meta.getPersistentDataContainer().set(DURATION_KEY, PersistentDataType.INTEGER, data.durationMinutes);
        meta.getPersistentDataContainer().set(BOREDOM_KEY, PersistentDataType.INTEGER, data.boredomPoints);

        meta.getPersistentDataContainer().set(STAT_DMG, PersistentDataType.DOUBLE, data.bonusDmg);
        meta.getPersistentDataContainer().set(STAT_CDR, PersistentDataType.DOUBLE, data.cdr);
        meta.getPersistentDataContainer().set(STAT_MS, PersistentDataType.DOUBLE, data.moveSpeed);
        meta.getPersistentDataContainer().set(STAT_AS, PersistentDataType.DOUBLE, data.atkSpeed);
        meta.getPersistentDataContainer().set(STAT_PEN, PersistentDataType.DOUBLE, data.armorPen);
        meta.getPersistentDataContainer().set(STAT_REGEN, PersistentDataType.DOUBLE, data.regenRate);
        meta.getPersistentDataContainer().set(STAT_HP, PersistentDataType.DOUBLE, data.maxHpBonus);
        meta.getPersistentDataContainer().set(FAT_DEBUFF_KEY, PersistentDataType.BYTE, (byte) (data.hasFatDebuff ? 1 : 0));
        meta.getPersistentDataContainer().set(STAT_LS, PersistentDataType.DOUBLE, data.lifeSteal);
        meta.getPersistentDataContainer().set(STAT_CRIT, PersistentDataType.DOUBLE, data.critRate);
        meta.getPersistentDataContainer().set(STAT_DROP, PersistentDataType.DOUBLE, data.dropRate);

        if (!data.potionEffects.isEmpty()) {
            String joinedEffects = String.join(";", data.potionEffects);
            meta.getPersistentDataContainer().set(POTION_EFFECTS_KEY, PersistentDataType.STRING, joinedEffects);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onScoopSoup(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.WATER_CAULDRON) return;

        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();

        if (handItem.getType() != Material.BOWL) return;

        Location loc = block.getLocation();
        if (activePots.containsKey(loc)) {
            event.setCancelled(true);

            FinishedPot pot = activePots.get(loc);
            handItem.setAmount(handItem.getAmount() - 1);

            player.getInventory().addItem(pot.resultItem.clone());
            player.playSound(loc, Sound.ENTITY_GENERIC_DRINK, 1f, 1f);

            pot.scoopsLeft--;

            if (pot.scoopsLeft <= 0) {
                activePots.remove(loc);
                block.setType(Material.CAULDRON);
                player.sendMessage("§eNồi súp đã cạn sạch!");
            } else {
                Levelled cauldronData = (Levelled) block.getBlockData();
                if (pot.scoopsLeft <= 4 && cauldronData.getLevel() == 3) cauldronData.setLevel(2);
                if (pot.scoopsLeft <= 2 && cauldronData.getLevel() == 2) cauldronData.setLevel(1);
                block.setBlockData(cauldronData);
            }
        }
    }
}