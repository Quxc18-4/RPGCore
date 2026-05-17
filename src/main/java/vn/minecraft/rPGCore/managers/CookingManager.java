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

            // KHÓA SỞ HỮU VẠC (POT OWNERSHIP)
            UUID potOwner = null;
            int maxIngredients = 9;

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

                        // XÁC ĐỊNH CHỦ SỞ HỮU VÀ GIỚI HẠN VẠC (Khi có món đồ đầu tiên rơi vào)
                        if (potOwner == null && item.getThrower() != null) {
                            potOwner = item.getThrower();
                            Player p = Bukkit.getPlayer(potOwner);
                            if (p != null) {
                                vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(p);
                                if (data != null && data.hasUnlockedSkill("CHEF", "CHEF_ASCENDED_TASTE")) {
                                    maxIngredients = 12; // Mở rộng vạc lên 12 cho Master Chef
                                }
                            }
                        }

                        // CHẶN KẺ LẠ NÉM ĐỒ VÀO VẠC
                        if (potOwner != null && item.getThrower() != null && !potOwner.equals(item.getThrower())) {
                            item.setVelocity(new org.bukkit.util.Vector((Math.random() - 0.5) * 0.5, 0.5, (Math.random() - 0.5) * 0.5));
                            continue; // Bật đồ ra ngoài
                        }

                        String ingredientId = itemManager.getItemId(item.getItemStack());
                        if (ingredientId != null) {
                            if (currentTotal < maxIngredients) {
                                int amt = item.getItemStack().getAmount();
                                if (currentTotal + amt <= maxIngredients) {
                                    ingredients.add(item);
                                    item.setPickupDelay(9999);
                                    currentTotal += amt;
                                } else {
                                    int needed = maxIngredients - currentTotal;
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
                                    currentTotal = maxIngredients;
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
                    hologram.setText("§e♨ Đang nấu...\n§f" + secondsLeft + "s §8| §a" + currentTotal + "§8/§a" + maxIngredients + " món");
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
                    finishCooking(cauldronBlock, ingredients, potOwner); // Truyền chủ sở hữu sang
                    hologram.remove();
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void finishCooking(Block cauldronBlock, List<Item> ingredients, UUID potOwner) {
        Location loc = cauldronBlock.getLocation();
        cookingPots.remove(loc);
        Player cook = potOwner != null ? Bukkit.getPlayer(potOwner) : null;

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

        FoodItemData resultData = cookingEngine.evaluate(recipeData, cook);
        NutritionStats stats = new NutritionStats(recipeData);

        // ==========================================
        // NỘI TẠI ĐẦU BẾP, TÍNH EXP & GỌI HOOK NHIỆM VỤ
        // ==========================================
        int finalScoops = 3; // Luôn luôn là 3 theo ý bạn
        if (cook != null) {
            vn.minecraft.rPGCore.jobs.core.PlayerJobData data = vn.minecraft.rPGCore.jobs.core.JobManager.getPlayerData(cook);
            if (data != null && data.getJobId().equals("CHEF")) {
                int chefLv = data.getLevel("CHEF");

                // Nội tại cơ bản: Rút thăm x2 Thời gian (Độc lập với Kỹ năng tối thượng)
                if (Math.random() * 100 <= (chefLv / 50.0) * 25.0) resultData.durationMinutes *= 2;
                if (Math.random() * 100 <= (chefLv / 50.0) * 50.0) resultData.freshnessMinutes *= 2;

                // [ĐÃ XÓA LOGIC 6 MUỖNG TẠI ĐÂY]

                // THUẬT TOÁN TÍNH TOÁN EXP THEO ĐỘ HIẾM
                double earnedExp = 15.0; // EXP Cơ bản
                if (!resultData.isRuined) {
                    for (ItemManager.ItemData itemData : recipeData) {
                        switch (itemData.rarity) {
                            case COMMON: earnedExp += 2.0; break;
                            case UNCOMMON: earnedExp += 5.0; break;
                            case RARE: earnedExp += 10.0; break;
                            case EPIC: earnedExp += 20.0; break;
                            case MYTHIC: earnedExp += 40.0; break;
                            case LEGENDARY: earnedExp += 80.0; break;
                        }
                    }
                } else {
                    earnedExp = 10.0; // Phạt vì nấu hỏng
                }

                // Cộng EXP cho người chơi
                vn.minecraft.rPGCore.jobs.core.JobManager.addExp(cook, earnedExp);

                // GỌI BÁO CÁO NHIỆM VỤ (Quest Hooks)
                vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(cook, vn.minecraft.rPGCore.jobs.quests.QuestAction.COOK_ANY);
                if (resultData.isRuined) vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(cook, vn.minecraft.rPGCore.jobs.quests.QuestAction.COOK_RUINED);
                if (recipeData.size() == 1) vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(cook, vn.minecraft.rPGCore.jobs.quests.QuestAction.COOK_SINGLE);
                if (recipeData.size() >= 9) vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(cook, vn.minecraft.rPGCore.jobs.quests.QuestAction.COOK_FULL);
            }
        }

        // ==========================================
        // RƠI TINH TÚY ẨM THỰC
        // ==========================================
        double score = stats.getCorePoints();
        int essenceCount = 0;
        if (!resultData.isRuined) {
            if (score < 3 && Math.random() < 0.10) essenceCount = 1;
            else if (score >= 3 && score < 6 && Math.random() < 0.40) essenceCount = 1;
            else if (score >= 6 && score <= 10) essenceCount = (Math.random() < 0.20) ? 2 : 1;
            else if (score > 10) essenceCount = 2;
        }
        if (essenceCount > 0) {
            loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 1.0, 0.5), itemManager.buildItem("misc_culinary_essence", essenceCount));
        }

        ItemStack resultItem = buildFoodItem(resultData, stats);

        Color pColor = Color.fromRGB(200, 150, 50);
        if (resultData.isRuined) pColor = Color.fromRGB(80, 100, 80);
        else if (stats.meat + stats.fish >= 1.0) pColor = Color.fromRGB(200, 50, 0);
        else if (stats.veg >= 1.0) pColor = Color.fromRGB(50, 200, 50);

        cauldronBlock.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0.5, 0.8, 0.5), 15, 0.2, 0.2, 0.2, 0.1);

        activePots.put(loc, new FinishedPot(resultItem, finalScoops, pColor));
    }

    private ItemStack buildFoodItem(FoodItemData data, NutritionStats stats) {
        ItemStack item = new ItemStack(data.material);
        ItemMeta meta = item.getItemMeta();
        NamespacedKey SPICE_KEY = new NamespacedKey(plugin, "food_spice");
        meta.getPersistentDataContainer().set(SPICE_KEY, PersistentDataType.DOUBLE, stats.getTotalSpice());
        meta.setDisplayName(data.name);

        List<String> lore = new ArrayList<>();
        long cookedTime = System.currentTimeMillis();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm - dd/MM");
        String timeStr = sdf.format(new java.util.Date(cookedTime));

        lore.add("§7Nấu lúc: §e" + timeStr);
        // Nhận diện Bột bảo quản vĩnh viễn
        if (data.freshnessMinutes >= 999999) {
            lore.add("§7Thời gian bảo quản: §d§lVô hạn");
        } else {
            lore.add("§7Thời gian bảo quản: §a" + data.freshnessMinutes + " Phút");
            lore.add("§8§o(Sẽ giảm chất lượng hoặc ôi thiu nếu để nguội)");
        }
        lore.add("");
        lore.add("§f§lThông tin chung:");
        lore.add("§7 - No Căng: §a" + data.durationMinutes + " Phút");
        lore.add("§7 - Tỉ lệ Ngấy: §e" + data.boredomPoints + "%");
        lore.add("");
        lore.add("§f§lBảng Dinh Dưỡng:");
        if (stats.meat > 0) lore.add("§7 - Thịt: §c" + String.format("%.2f", stats.meat));
        if (stats.fish > 0) lore.add("§7 - Cá: §b" + String.format("%.2f", stats.fish));
        if (stats.veg > 0) lore.add("§7 - Rau Củ: §a" + String.format("%.2f", stats.veg));
        if (stats.starch > 0) lore.add("§7 - Tinh Bột: §e" + String.format("%.2f", stats.starch));
        if (stats.fat > 0) lore.add("§7 - Chất Béo: §e" + String.format("%.2f", stats.fat));
        if (stats.getTotalSpice() > 0) lore.add("§7 - Gia Vị: §f" + String.format("%.2f", stats.getTotalSpice()));

        if (data.bonusDmg != 0 || data.cdr != 0 || data.moveSpeed != 0 || data.atkSpeed != 0 || data.armorPen != 0 || data.regenRate != 0 || data.maxHpBonus != 0) {
            lore.add("");
            lore.add("§f§lHiệu Ứng Cơ Thể:");
            if (data.bonusDmg != 0) lore.add("§7 - Sát thương: §c" + (data.bonusDmg > 0 ? "+" : "") + String.format("%.1f", data.bonusDmg) + "%");
            if (data.cdr != 0) lore.add("§7 - Tỉ lệ Hồi chiêu: §9" + (data.cdr > 0 ? "+" : "") + String.format("%.1f", data.cdr) + "%");
            if (data.moveSpeed != 0) lore.add("§7 - Tốc chạy: §f" + (data.moveSpeed > 0 ? "+" : "") + String.format("%.1f", data.moveSpeed) + "%");
            if (data.atkSpeed != 0) lore.add("§7 - Tốc đánh: §e" + (data.atkSpeed > 0 ? "+" : "") + String.format("%.1f", data.atkSpeed) + "%");
            if (data.armorPen != 0) lore.add("§7 - Tỉ lệ Xuyên giáp: §4" + (data.armorPen > 0 ? "+" : "") + String.format("%.1f", data.armorPen) + "%");
            if (data.regenRate != 0) lore.add("§7 - Tỉ lệ Hồi máu: §a" + (data.regenRate > 0 ? "+" : "") + String.format("%.1f", data.regenRate) + "%");
            if (data.maxHpBonus != 0) lore.add("§7 - Máu tối đa: §c" + (data.maxHpBonus > 0 ? "+" : "") + String.format("%.1f", data.maxHpBonus));
            if (data.lifeSteal != 0) lore.add("§7 - Tỉ lệ Hút máu: §c" + (data.lifeSteal > 0 ? "+" : "") + String.format("%.1f", data.lifeSteal) + "%");
            if (data.critRate != 0) lore.add("§7 - Tỉ lệ Chí mạng: §e" + (data.critRate > 0 ? "+" : "") + String.format("%.1f", data.critRate) + "%");
            if (data.dropRate != 0) lore.add("§7 - Tỉ lệ Rớt đồ: §b" + (data.dropRate > 0 ? "+" : "") + String.format("%.1f", data.dropRate) + "%");
        }

        if (data.hasFatDebuff) lore.add("§7 - Quá Ngấy: §c-20% Tốc chạy");

        if (!data.flavorTags.isEmpty()) {
            lore.add("");
            lore.addAll(data.flavorTags);
        }

        meta.getPersistentDataContainer().set(COOKED_TIME_KEY, PersistentDataType.LONG, cookedTime);
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

        // LƯU CỜ GIẤU ĐỘC VÀO VẬT PHẨM
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "food_hidden_poison"), PersistentDataType.BYTE, (byte) (data.isHiddenPoison ? 1 : 0));

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

            // [FIX BUG]: Kiểm tra túi đồ đầy
            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage("§cTúi đồ của bạn đã đầy, không thể múc súp!");
                player.playSound(loc, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

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
                // [FIX BUG]: Cập nhật mực nước của Vạc tương ứng trực tiếp với 3 lần múc (Level 1,2,3)
                cauldronData.setLevel(pot.scoopsLeft);
                block.setBlockData(cauldronData);
            }
        }
    }
    // ==========================================
    // CẬP NHẬT LORE KHI CẦM LÊN TAY (LAZY UPDATE)
    // ==========================================
    @EventHandler
    public void onHoldFood(org.bukkit.event.player.PlayerItemHeldEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(COOKED_TIME_KEY, PersistentDataType.LONG)) return;

        long cookedTime = meta.getPersistentDataContainer().get(COOKED_TIME_KEY, PersistentDataType.LONG);
        int maxFreshness = meta.getPersistentDataContainer().getOrDefault(MAX_FRESHNESS_KEY, PersistentDataType.INTEGER, 30);
        long ageInMinutes = (System.currentTimeMillis() - cookedTime) / 60000;

        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return;

        // Bỏ qua update trạng thái nếu là đồ bảo quản vĩnh viễn (Bột bảo quản)
        if (maxFreshness >= 999999) return;

        String newStatus = "§7Tình trạng: §cThiu thối (Độc)";

        // KIỂM TRA: MÓN NÀY CÓ BỊ SÁT THỦ ĐẦU BẾP GIẤU ĐỘC KHÔNG?
        boolean isHidden = meta.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "food_hidden_poison"), PersistentDataType.BYTE, (byte)0) == 1;

        if (ageInMinutes <= maxFreshness * 0.15) newStatus = "§7Tình trạng: §6Nóng hổi (Ngon nhất)";
        else if (ageInMinutes < maxFreshness * 0.75) newStatus = "§7Tình trạng: §aĐang ấm";
            // Nếu bị giấu độc, khi quá hạn nó sẽ hiển thị "Đã nguội lạnh" thay vì "Thiu thối (Độc)" !!
        else if (ageInMinutes <= maxFreshness || isHidden) newStatus = "§7Tình trạng: §eĐã nguội lạnh";

        // Ghi đè vào dòng đầu tiên có chữ "Tình trạng"
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains("Tình trạng:")) {
                if (!lore.get(i).equals(newStatus)) {
                    lore.set(i, newStatus);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                break;
            }
        }
    }
}