package vn.minecraft.rPGCore.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.ItemManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ConsumableListener implements Listener {

    private final ItemManager itemManager;
    // Bộ nhớ Anti-Spam
    private final Map<UUID, Long> consumeCooldowns = new HashMap<>();

    public ConsumableListener(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onConsume(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        String itemId = itemManager.getItemId(item);
        if (itemId == null) return;

        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        boolean consumed = false;

        // ==========================================
        // KHỐI LOGIC: CÁC VẬT PHẨM "ĂN LIỀN" CÓ ANTI-SPAM VÀ CHẶN VANILLA OFF-HAND
        // ==========================================
        List<String> instantFoods = java.util.Arrays.asList("fat_milk", "spice_honey", "veg_apple", "veg_melon", "veg_kelp_dried");

        if (instantFoods.contains(itemId)) {
            // KHÓA HOÀN TOÀN TÍNH NĂNG ĂN CỦA VANILLA (Tay trái lẫn tay phải)
            event.setCancelled(true);

            // KIỂM TRA ANTI-SPAM (1.5 Giây)
            long now = System.currentTimeMillis();
            if (consumeCooldowns.getOrDefault(player.getUniqueId(), 0L) > now) {
                return; // Đang trong thời gian hồi, phớt lờ click
            }

            // 1. SỮA BÒ: Xóa 1 Debuff ngẫu nhiên (Trả lại xô)
            if (itemId.equals("fat_milk")) {
                PotionEffectType[] debuffs = {
                        PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.NAUSEA,
                        PotionEffectType.BLINDNESS, PotionEffectType.HUNGER, PotionEffectType.WEAKNESS,
                        PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.DARKNESS,
                        PotionEffectType.UNLUCK, PotionEffectType.BAD_OMEN
                };

                List<PotionEffectType> activeDebuffs = new ArrayList<>();
                for (PotionEffectType type : debuffs) {
                    if (player.hasPotionEffect(type)) activeDebuffs.add(type);
                }

                if (!activeDebuffs.isEmpty()) {
                    PotionEffectType removedEffect = activeDebuffs.get(new Random().nextInt(activeDebuffs.size()));
                    player.removePotionEffect(removedEffect);
                    player.sendMessage("§f§l[!] §aBạn đã uống Sữa. Hiệu ứng xấu đã bị thanh tẩy!");
                } else {
                    player.sendMessage("§f§l[!] §aBạn đã uống Sữa. Cơ thể hoàn toàn khỏe mạnh!");
                }

                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WANDERING_TRADER_DRINK_MILK, 1f, 1f);
                player.getInventory().addItem(new ItemStack(org.bukkit.Material.BUCKET));
                consumed = true;
            }

            // 2. MẬT ONG RỪNG: Hồi 5% Máu (Biến mất hoàn toàn)
            else if (itemId.equals("spice_honey")) {
                double healAmount = rpgPlayer.getTotalMaxHealth() * 0.05;
                rpgPlayer.heal(healAmount);

                player.sendMessage("§e§l[!] §aMật ong rừng giúp bạn phục hồi sinh lực (+5% HP)!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_HONEY_BOTTLE_DRINK, 1f, 1f);
                consumed = true;
            }

            // 3. TÁO / DƯA HẤU: Hồi 2% HP và 1 Đùi gà (Biến mất)
            else if (itemId.equals("veg_apple") || itemId.equals("veg_melon")) {
                rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * 0.02);
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + 2));

                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_BURP, 1f, 1.2f);
                consumed = true;
            }

            // 4. RONG BIỂN KHÔ: Buff Tốc chạy Vanilla (Không cộng dồn)
            else if (itemId.equals("veg_kelp_dried")) {
                // [ĐÃ SỬA]: Gọi qua StatusEffectManager để tính toán Kháng hiệu ứng
                vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(player, PotionEffectType.SPEED, 100, 0);

                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_BURP, 1f, 1.5f);
                consumed = true;
            }

            // Ghi đè thời gian Anti-Spam nếu ăn thành công
            if (consumed) {
                consumeCooldowns.put(player.getUniqueId(), now + 1500L); // Khóa mồm 1.5 giây
            }
        }

        // ==========================================
        // CÁC LOGIC CŨ GIỮ NGUYÊN (Không ảnh hưởng Anti-Spam)
        // ==========================================

        // Bình Máu
        else if (itemId.equals("potion_hp_1")) {
            double healAmount = rpgPlayer.getTotalMaxHealth() * 0.10;
            rpgPlayer.heal(healAmount);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_DRINK, 1f, 1f);
            player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0);
            consumed = true;
        }

        // Tinh Thể Nguyên Tố
        else if (itemId.startsWith("crystal_")) {
            if (!rpgPlayer.getCurrentClass().getId().equalsIgnoreCase("mage")) {
                player.sendMessage("§cChỉ Pháp Sư mới có thể hấp thụ Tinh Thể Nguyên Tố!");
                return;
            }
            String elementToUnlock = itemId.replace("crystal_", "").toUpperCase();
            if (rpgPlayer.hasElement(elementToUnlock)) {
                player.sendMessage("§eBạn đã sở hữu Nguyên Tố này rồi!");
                return;
            }
            rpgPlayer.addElement(elementToUnlock);
            player.sendMessage("§a§l[!] §fBạn đã hấp thụ thành công §dNguyên Tố " + elementToUnlock + "§f!");
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            consumed = true;
        }

        // Túi Gacha
        else if (itemId.startsWith("bag_weapon_") || itemId.startsWith("bag_armor_")) {
            String type = itemId.startsWith("bag_weapon_") ? "weapon" : "armor";
            String typeName = type.equals("weapon") ? "Vũ Khí" : "Phòng Cụ";

            int tierIndex = 0;
            if (itemId.endsWith("uncommon")) tierIndex = 1;
            else if (itemId.endsWith("rare")) tierIndex = 2;
            else if (itemId.endsWith("epic")) tierIndex = 3;

            int playerLevel = player.getLevel();
            if (playerLevel <= 0) playerLevel = 1;

            player.sendMessage("§a§l[!] §fĐang mở Túi " + typeName + "...");
            ItemStack gachaItem = vn.minecraft.rPGCore.commands.EquipmentCommand.createGachaItem(
                    type, playerLevel, null, tierIndex, null, -1, -1
            );
            player.getInventory().addItem(gachaItem);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
            consumed = true;
        }

        // Trừ Item sau khi dùng thành công (Trừ trên tay nào click)
        if (consumed) {
            event.setCancelled(true);
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                // Fix lỗi hiển thị khi cầm tay trái
                EquipmentSlot hand = event.getHand();
                if (hand == EquipmentSlot.HAND) {
                    item.setAmount(item.getAmount() - 1);
                } else if (hand == EquipmentSlot.OFF_HAND) {
                    ItemStack offHandItem = player.getInventory().getItemInOffHand();
                    offHandItem.setAmount(offHandItem.getAmount() - 1);
                }
            }
        }
    }
}