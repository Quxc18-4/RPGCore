package vn.minecraft.rPGCore.listeners;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vn.minecraft.rPGCore.managers.BoredomManager;
import vn.minecraft.rPGCore.managers.FoodBuffManager;
import vn.minecraft.rPGCore.managers.FoodBuffManager.StatType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FoodListener implements Listener {

    private final BoredomManager boredomManager;
    private final FoodBuffManager foodBuffManager;
    public static final Map<UUID, Long> fullnessTimers = new HashMap<>(); // Lưu thời gian No Căng

    // Keys
    private final NamespacedKey COOKED_TIME_KEY, MAX_FRESHNESS_KEY, DURATION_KEY, BOREDOM_KEY, SPICE_KEY;
    private final NamespacedKey STAT_DMG, STAT_CDR, STAT_MS, STAT_AS, STAT_PEN, STAT_REGEN, STAT_HP, FAT_DEBUFF_KEY;
    private final NamespacedKey STAT_LS, STAT_CRIT, STAT_DROP, POTION_EFFECTS_KEY;

    public FoodListener(JavaPlugin plugin, BoredomManager boredomManager, FoodBuffManager foodBuffManager) {
        this.boredomManager = boredomManager;
        this.foodBuffManager = foodBuffManager;

        this.COOKED_TIME_KEY = new NamespacedKey(plugin, "food_cooked_time");
        this.MAX_FRESHNESS_KEY = new NamespacedKey(plugin, "food_max_freshness");
        this.DURATION_KEY = new NamespacedKey(plugin, "food_duration");
        this.BOREDOM_KEY = new NamespacedKey(plugin, "food_boredom");
        this.SPICE_KEY = new NamespacedKey(plugin, "food_spice");

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
    }

    @EventHandler
    public void onInteractEat(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(COOKED_TIME_KEY, PersistentDataType.LONG)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // ==========================================
        // 1. ĐỌC CHỈ SỐ TỪ PDC
        // ==========================================
        long cookedTime = meta.getPersistentDataContainer().get(COOKED_TIME_KEY, PersistentDataType.LONG);
        int maxFreshness = meta.getPersistentDataContainer().getOrDefault(MAX_FRESHNESS_KEY, PersistentDataType.INTEGER, 30);
        int baseDuration = meta.getPersistentDataContainer().getOrDefault(DURATION_KEY, PersistentDataType.INTEGER, 5);
        int boredomChance = meta.getPersistentDataContainer().getOrDefault(BOREDOM_KEY, PersistentDataType.INTEGER, 0);
        double spicePoints = meta.getPersistentDataContainer().getOrDefault(SPICE_KEY, PersistentDataType.DOUBLE, 0.0);

        long ageInMinutes = (System.currentTimeMillis() - cookedTime) / 60000;
        double effectMultiplier = 1.0;

        // ==========================================
        // 2. KIỂM TRA LỆNH CẤM ĂN (TUYỆT THỰC) - ƯU TIÊN SỐ 1
        // ==========================================
        if (boredomManager.isBanned(uuid)) {
            if (spicePoints > 1.5) {
                // Vượt ngục thành công! (Món này đủ ngon để phá cấm thực)
                boredomManager.unban(uuid);
                double cureAmount = (spicePoints / 0.5) * 3.0; // Mỗi 0.5 spice giảm 3%
                boredomManager.reduceAnorexia(uuid, cureAmount);
                player.sendMessage("§a[Thăng hoa] Hương vị siêu đậm đà đã phá vỡ sự chán ăn của bạn! (Giảm " + cureAmount + "% Điểm Tuyệt Thực)");
            } else {
                // Đồ ăn nhạt nhẽo -> Từ chối ăn luôn, KHÔNG tính độ tươi hay trừ item
                player.sendMessage("§c✖ Cổ họng bạn từ chối nuốt! Bạn đang bị Tuyệt Thực. Hãy tìm một món siêu cay/đậm đà (Spice > 1.5) để giải ngấy!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 0.5f);
                return; // Dừng code tại đây
            }
        }
        else {
            // Vòng quay Xổ số Ngấy (Khi chưa bị cấm thực)
            if (boredomChance > 0 && (Math.random() * 100 < boredomChance)) {
                boredomManager.addStrikeAndPoints(uuid);
                player.sendMessage("§eBạn cố nhồi nhét món này vào bụng... (+5% Điểm Tuyệt Thực)");

                if (boredomManager.isBanned(uuid)) {
                    player.sendMessage("§c☠ BẠN ĐÃ BỊ TUYỆT THỰC DO ĂN QUÁ NGẤY! Cấm ăn đồ tự nấu!");
                }
            }

            // Thanh lọc điểm ngấy bằng Gia vị
            if (spicePoints > 0 && !boredomManager.isBanned(uuid)) {
                double cureAmount = (spicePoints / 0.5) * 3.0;
                boredomManager.reduceAnorexia(uuid, cureAmount);
                player.sendMessage("§aGia vị đậm đà giúp bạn tiêu hóa tốt hơn. (-" + String.format("%.1f", cureAmount) + "% Tuyệt thực)");
            }
        }

        // ==========================================
        // 3. KIỂM TRA ĐỘ TƯƠI (CHỈ CHẠY KHI ĐÃ ĐƯỢC PHÉP ĂN)
        // ==========================================
        if (ageInMinutes > maxFreshness) {
            player.sendMessage("§cỌe... Món này đã thiu thối do để quá lâu!");
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 300, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 0));
            consumeItem(player, item); // Vẫn mất item vì lỡ cắn thử
            return; // Ăn đồ thiu thì không nhận buff
        }
        else if (ageInMinutes <= maxFreshness * 0.15) {
            player.sendMessage("§6Bạn húp một ngụm súp nóng hổi! (+2 Phút No Căng)");
            baseDuration += 2;
        }
        else if (ageInMinutes >= maxFreshness * 0.75) {
            player.sendMessage("§eMón súp đã nguội lạnh... Hiệu quả giảm đi một nửa.");
            effectMultiplier = 0.5;
            baseDuration = Math.max(1, baseDuration / 2);
        }

        // ==========================================
        // 4. BƠM STATS RPG VÀO NGƯỜI CHƠI
        // ==========================================

        // [FIX BUG]: Ghi đè Buff - Xóa sạch các buff thức ăn trước đó để tránh cộng dồn vô hạn
        foodBuffManager.clearAllBuffs(uuid);

        applyStat(uuid, meta, STAT_DMG, StatType.DMG, baseDuration, effectMultiplier);
        applyStat(uuid, meta, STAT_CDR, StatType.CDR, baseDuration, effectMultiplier);
        applyStat(uuid, meta, STAT_MS, StatType.MOVE_SPEED, baseDuration, effectMultiplier);
        applyStat(uuid, meta, STAT_AS, StatType.ATK_SPEED, baseDuration, effectMultiplier);
        applyStat(uuid, meta, STAT_PEN, StatType.ARMOR_PEN, baseDuration, effectMultiplier);
        applyStat(uuid, meta, STAT_REGEN, StatType.REGEN, baseDuration, effectMultiplier);
        applyStat(uuid, meta, STAT_HP, StatType.MAX_HP, baseDuration, effectMultiplier);
        applyStat(uuid, meta, STAT_LS, StatType.LIFE_STEAL, baseDuration, effectMultiplier);
        applyStat(uuid, meta, STAT_CRIT, StatType.CRIT_RATE, baseDuration, effectMultiplier);
        applyStat(uuid, meta, STAT_DROP, StatType.DROP_RATE, baseDuration, effectMultiplier);

        // BƠM POTION EFFECTS VÀ ÉP THỜI GIAN = NO CĂNG
        String effectsStr = meta.getPersistentDataContainer().get(POTION_EFFECTS_KEY, PersistentDataType.STRING);
        if (effectsStr != null && !effectsStr.isEmpty()) {
            // Tính số Ticks = Số phút * 60 giây * 20 ticks
            int durationTicks = (int) (baseDuration * 60 * 20 * effectMultiplier);

            for (String eff : effectsStr.split(";")) {
                String[] parts = eff.split(":");
                PotionEffectType type = PotionEffectType.getByName(parts[0]);
                int amplifier = Integer.parseInt(parts[1]);

                if (type != null) {
                    player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, false, false, false));
                }
            }
        }

        // Áp dụng Debuff Quá Ngấy
        if (meta.getPersistentDataContainer().getOrDefault(FAT_DEBUFF_KEY, PersistentDataType.BYTE, (byte)0) == 1) {
            foodBuffManager.applyBuff(uuid, StatType.MOVE_SPEED, -20.0 * effectMultiplier, baseDuration);
        }

        // ==========================================
        // 5. SET THANH ĐÓI VÀ TIÊU THỤ ITEM
        // ==========================================
        fullnessTimers.put(uuid, System.currentTimeMillis() + (baseDuration * 60000L));
        player.setFoodLevel(20);
        player.setExhaustion(0f);
        player.sendMessage("§aBạn cảm thấy no căng! (" + baseDuration + " phút)");

        consumeItem(player, item);
    }

    // ==========================================
    // CHẶN VANILLA MINECRAFT LÀM TỤT THANH ĐÓI KHI ĐANG "NO CĂNG"
    // ==========================================
    @EventHandler
    public void onHungerDeplete(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();

        if (fullnessTimers.containsKey(uuid) && fullnessTimers.get(uuid) > System.currentTimeMillis()) {
            if (event.getFoodLevel() < player.getFoodLevel()) {
                event.setCancelled(true);
            }
        }
    }

    private void applyStat(UUID uuid, ItemMeta meta, NamespacedKey key, StatType type, int duration, double multiplier) {
        double val = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.DOUBLE, 0.0);
        if (val != 0) {
            foodBuffManager.applyBuff(uuid, type, val * multiplier, duration);
        }
    }

    private void consumeItem(Player player, ItemStack item) {
        item.setAmount(item.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);
        if (item.getType() == Material.MUSHROOM_STEW || item.getType() == Material.RABBIT_STEW || item.getType() == Material.BEETROOT_SOUP) {
            player.getInventory().addItem(new ItemStack(Material.BOWL));
        }

        // [MỚI]: BÁO CÁO HÀNH ĐỘNG ĂN CHO HỆ THỐNG NHIỆM VỤ
        vn.minecraft.rPGCore.jobs.core.JobQuestManager.handleAction(player, vn.minecraft.rPGCore.jobs.quests.QuestAction.CONSUME_FOOD);
        Bukkit.getPluginManager().callEvent(new vn.minecraft.rPGCore.api.events.PlayerActionTrackEvent(player, "CONSUME", 1));
    }
}