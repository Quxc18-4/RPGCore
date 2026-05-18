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
import vn.minecraft.rPGCore.managers.BoredomManager;
import vn.minecraft.rPGCore.managers.CookingManager;
import vn.minecraft.rPGCore.managers.ItemManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.*;

public class ConsumableListener implements Listener {

    private final ItemManager itemManager;
    private final BoredomManager boredomManager;
    private final CookingManager cookingManager;
    // Bộ nhớ Anti-Spam
    private final Map<UUID, Long> consumeCooldowns = new HashMap<>();

    // Cập nhật Constructor
    public ConsumableListener(ItemManager itemManager, BoredomManager boredomManager, CookingManager cookingManager) {
        this.itemManager = itemManager;
        this.boredomManager = boredomManager;
        this.cookingManager = cookingManager;
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
        // DANH SÁCH THỰC PHẨM ĂN LIỀN
        List<String> instantFoods = Arrays.asList("fat_milk", "spice_honey", "veg_apple", "veg_melon", "veg_kelp_dried", "potion_digestion", "potion_anti_anorexia", "misc_lunch_bag", "starch_bread", "magic_flame_beetroot", "magic_golden_melon", "magic_star_wheat", "magic_crystal_kelp", "magic_mana_potato", "magic_chaos_glow_berry");

        if (instantFoods.contains(itemId)) {
            // KHÓA HOÀN TOÀN TÍNH NĂNG ĂN CỦA VANILLA (Tay trái lẫn tay phải)
            event.setCancelled(true);

            // KIỂM TRA ANTI-SPAM (1.5 Giây)
            long now = System.currentTimeMillis();
            if (consumeCooldowns.getOrDefault(player.getUniqueId(), 0L) > now) {
                return; // Đang trong thời gian hồi, phớt lờ click
            }

            // 1. Sữa bò
            if (itemId.equals("fat_milk")) {
                PotionEffectType[] debuffs = { PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.NAUSEA, PotionEffectType.BLINDNESS, PotionEffectType.HUNGER, PotionEffectType.WEAKNESS, PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.DARKNESS, PotionEffectType.UNLUCK, PotionEffectType.BAD_OMEN };
                List<PotionEffectType> activeDebuffs = new ArrayList<>();
                for (PotionEffectType type : debuffs) if (player.hasPotionEffect(type)) activeDebuffs.add(type);

                if (!activeDebuffs.isEmpty()) {
                    player.removePotionEffect(activeDebuffs.get(new Random().nextInt(activeDebuffs.size())));
                    player.sendMessage("§f§l[!] §aBạn đã uống Sữa. 1 Hiệu ứng xấu đã bị thanh tẩy!");
                } else player.sendMessage("§f§l[!] §aBạn đã uống Sữa. Cơ thể hoàn toàn khỏe mạnh!");

                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WANDERING_TRADER_DRINK_MILK, 1f, 1f);
                player.getInventory().addItem(new ItemStack(org.bukkit.Material.BUCKET));
                consumed = true;
            }
            // 2. Thuốc Giải Ngấy (-50%)
            else if (itemId.equals("potion_digestion")) {
                double currentPts = boredomManager.getPoints(player.getUniqueId());
                boredomManager.reduceAnorexia(player.getUniqueId(), currentPts * 0.5); // Giảm 1 nửa
                player.sendMessage("§e§l[!] §aBụng bạn nhẹ đi hẳn! Đã giảm 50% độ ngấy.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_DRINK, 1f, 1f);
                player.getInventory().addItem(new ItemStack(org.bukkit.Material.GLASS_BOTTLE));
                consumed = true;
            }
            // 3. Thuốc Trị Tuyệt Thực (Chữa 100%)
            else if (itemId.equals("potion_anti_anorexia")) {
                boredomManager.unban(player.getUniqueId());
                boredomManager.reduceAnorexia(player.getUniqueId(), 100.0);
                player.sendMessage("§e§l[!] §aChứng biếng ăn đã bị đánh bay! Bạn lại thèm ăn rồi!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_DRINK, 1f, 1f);
                player.getInventory().addItem(new ItemStack(org.bukkit.Material.BUCKET));
                consumed = true;
            }
            // 4. Túi Cơm Hộp (Gacha 3 Súp)
            else if (itemId.equals("misc_lunch_bag")) {
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage("§cTúi đồ của bạn đã đầy!"); return;
                }
                for(int i=0; i<3; i++) {
                    player.getInventory().addItem(cookingManager.generateRandomSoup(player));
                }
                player.sendMessage("§e§l[!] §aBạn đã mở Túi Cơm Hộp và nhận được 3 Bát Súp ngẫu nhiên!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
                consumed = true;
            }
            // 5. Ổ Bánh Mì (Hồi 5 Đói)
            else if (itemId.equals("starch_bread")) {
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + 8));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EAT, 1f, 1f);
                consumed = true;
            }
            // 6. Mật Ong
            else if (itemId.equals("spice_honey")) {
                rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * 0.05);
                player.sendMessage("§e§l[!] §aMật ong rừng giúp bạn phục hồi sinh lực (+5% HP)!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_HONEY_BOTTLE_DRINK, 1f, 1f);
                consumed = true;
            }
            // 7. Trái Cây
            else if (itemId.equals("veg_apple") || itemId.equals("veg_melon")) {
                rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * 0.02);
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + 4));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_BURP, 1f, 1.2f);
                consumed = true;
            }
            // 8. Rong biển
            else if (itemId.equals("veg_kelp_dried")) {
                vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(player, PotionEffectType.SPEED, 100, 0);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_BURP, 1f, 1.5f);
                consumed = true;
            }

            // --- BẮT ĐẦU 6 LOẠI MA THUẬT ĂN SỐNG ---
            else if (itemId.equals("magic_flame_beetroot")) {
                rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * 0.10);
                vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(player, PotionEffectType.FIRE_RESISTANCE, 1200, 0); // 60s
                player.setFireTicks(40); // Bốc cháy 2s cho ngầu
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
                player.sendMessage("§c§l[!] §fCơ thể bạn nóng rực! Hồi 10% HP và nhận Kháng lửa.");
                consumed = true;
            }
            else if (itemId.equals("magic_golden_melon")) {
                rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * 0.15);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_BURP, 1f, 1.2f);
                player.sendMessage("§e§l[!] §fVị ngọt ma thuật tuyệt hảo! Hồi 15% HP.");
                consumed = true;
            }
            else if (itemId.equals("magic_star_wheat")) {
                rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * 0.05);
                for (PotionEffectType type : new PotionEffectType[]{ PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.NAUSEA, PotionEffectType.BLINDNESS, PotionEffectType.HUNGER, PotionEffectType.WEAKNESS, PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.DARKNESS }) {
                    player.removePotionEffect(type);
                }
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
                player.sendMessage("§a§l[!] §fÁnh sáng vì sao thanh tẩy cơ thể bạn!");
                consumed = true;
            }
            else if (itemId.equals("magic_crystal_kelp")) {
                vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(player, PotionEffectType.WATER_BREATHING, 3600, 0); // 3 phút
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GUARDIAN_AMBIENT, 1f, 1f);
                player.sendMessage("§b§l[!] §fMa lực nước bao bọc bạn! Có thể thở dưới nước 3 phút.");
                consumed = true;
            }
            else if (itemId.equals("magic_mana_potato")) {
                rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * 0.20);
                vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(player, PotionEffectType.BLINDNESS, 100, 0); // 5s
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 1f);
                player.sendMessage("§d§l[!] §fMa năng quá lớn làm bạn mù tạm thời, nhưng hồi 20% HP!");
                consumed = true;
            }
            else if (itemId.equals("magic_chaos_glow_berry")) {
                rpgPlayer.heal(rpgPlayer.getTotalMaxHealth() * 0.10);
                vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(player, PotionEffectType.INVISIBILITY, 600, 0); // 30s = 600 ticks
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1f);
                player.sendMessage("§5§l[!] §fCơ thể bạn hòa vào hư không! Nhận Tàng Hình trong 30 giây.");
                consumed = true;
            }

            if (consumed) consumeCooldowns.put(player.getUniqueId(), now + 1500L);
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