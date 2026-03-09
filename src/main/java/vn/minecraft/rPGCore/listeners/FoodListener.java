package vn.minecraft.rPGCore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FoodListener implements Listener {

    public static final Map<UUID, Long> fullnessTimers = new HashMap<>();
    public static final Map<UUID, Integer> boredomLevels = new HashMap<>(); // Lưu % Độ ngán
    private final NamespacedKey COOKED_TIME_KEY;

    public FoodListener(JavaPlugin plugin) {
        this.COOKED_TIME_KEY = new NamespacedKey(plugin, "cooked_time");
    }

    @EventHandler
    public void onInteractEat(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item == null || !item.hasItemMeta()) return;

            ItemMeta meta = item.getItemMeta();

            // CHỈ QUÉT ĐỒ ĂN CÓ DẤU TIMESTAMP CỦA RPGCORE
            if (!meta.getPersistentDataContainer().has(COOKED_TIME_KEY, PersistentDataType.LONG)) return;

            event.setCancelled(true); // Ngăn ăn kiểu Vanilla
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            String name = ChatColor.stripColor(meta.getDisplayName());

            // 1. TÍNH TOÁN ĐỘ TƯƠI MỚI (FRESHNESS)
            long cookedTime = meta.getPersistentDataContainer().get(COOKED_TIME_KEY, PersistentDataType.LONG);
            long ageInMinutes = (System.currentTimeMillis() - cookedTime) / (60 * 1000);

            if (ageInMinutes > 30) {
                // ĐÃ THIU HỎNG
                player.sendMessage("§cỌe... Món này đã bốc mùi thiu do để quá lâu!");
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 300, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 0));
                consumeItem(player, item);
                return;
            }

            // 2. KIỂM TRA ĐỘ NGÁN (BOREDOM)
            int currentBoredom = boredomLevels.getOrDefault(uuid, 0);
            boolean isSpicyOrFatty = name.contains("Đậm Vị") || name.contains("Béo Ngậy");

            if (!isSpicyOrFatty) {
                if (currentBoredom >= 50 && Math.random() < 0.3) {
                    player.sendMessage("§eBạn cảm thấy quá ngán món này và không thể nuốt trôi...");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 0.5f);
                    return; // Không mất item, chỉ không ăn được
                }
                boredomLevels.put(uuid, Math.min(100, currentBoredom + 15)); // Tăng 15% độ ngán
            } else {
                boredomLevels.put(uuid, 0); // Reset độ ngán nếu ăn đồ ngon/có gia vị
                player.sendMessage("§aHương vị đậm đà làm bạn lấy lại cảm giác thèm ăn!");
            }

            // 3. XỬ LÝ TÁC DỤNG CỦA TỪNG MÓN ĂN
            int durationMinutes = 5; // Mặc định 5 phút No Căng

            if (name.contains("Thịt Hầm Thượng Hạng")) {
                durationMinutes = 20;
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60 * 20, 0));
            } else if (name.contains("Súp Thịt Nguyên Tảng")) {
                durationMinutes = 15;
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 60 * 20, 0)); // Nặng bụng 3 phút
            } else if (name.contains("Salad Súp Thanh Lọc")) {
                durationMinutes = 2; // Cực nhanh đói
                player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType())); // Giải độc
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30 * 20, 1));
            } else if (name.contains("Kì Lạ")) {
                durationMinutes = 0;
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
                player.sendMessage("§cBạn bị ngộ độc thực phẩm!");
            }

            // 4. ÁP DỤNG HỆ SỐ TƯƠI MỚI
            if (ageInMinutes <= 5) {
                player.sendMessage("§6Bạn vừa húp một ngụm súp nóng hổi! (+1 Phút No căng)");
                durationMinutes += 1;
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
            } else if (ageInMinutes >= 20) {
                player.sendMessage("§eMón súp đã nguội lạnh... Mất 50% hiệu quả.");
                durationMinutes /= 2;
            }

            // Gắn thời gian "No Căng"
            if (durationMinutes > 0) {
                fullnessTimers.put(uuid, System.currentTimeMillis() + (durationMinutes * 60 * 1000L));
                player.setFoodLevel(20);
                player.sendMessage("§aBạn cảm thấy no căng! (" + durationMinutes + " phút)");
            }

            consumeItem(player, item);
        }
    }

    private void consumeItem(Player player, ItemStack item) {
        item.setAmount(item.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);
        if (item.getType() == Material.MUSHROOM_STEW || item.getType() == Material.RABBIT_STEW || item.getType() == Material.BEETROOT_SOUP) {
            player.getInventory().addItem(new ItemStack(Material.BOWL));
        }
    }

    // 5. NGĂN TỤT THỨC ĂN NẾU CÓ BUFF "NO CĂNG"
    @EventHandler
    public void onHungerDeplete(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();

        if (fullnessTimers.containsKey(uuid)) {
            if (System.currentTimeMillis() < fullnessTimers.get(uuid)) {
                event.setCancelled(true);
                player.setFoodLevel(20);
            } else {
                fullnessTimers.remove(uuid);
                player.sendMessage("§eBụng bạn bắt đầu sôi lên... Bạn đã hết trạng thái No Căng.");
            }
        }
    }
}