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
    public static final Map<UUID, Integer> boredomLevels = new HashMap<>();
    private final NamespacedKey COOKED_TIME_KEY;
    private final NamespacedKey MAX_FRESHNESS_KEY;

    public FoodListener(JavaPlugin plugin) {
        this.COOKED_TIME_KEY = new NamespacedKey(plugin, "cooked_time");
        this.MAX_FRESHNESS_KEY = new NamespacedKey(plugin, "max_freshness");
    }

    @EventHandler
    public void onInteractEat(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item == null || !item.hasItemMeta()) return;

            ItemMeta meta = item.getItemMeta();

            // CHỈ XỬ LÝ NẾU ĐÂY LÀ ĐỒ ĂN CÓ DẤU THỜI GIAN CỦA RPG
            if (!meta.getPersistentDataContainer().has(COOKED_TIME_KEY, PersistentDataType.LONG)) return;

            event.setCancelled(true);
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            String name = ChatColor.stripColor(meta.getDisplayName());

            // 1. TÍNH TOÁN ĐỘ TƯƠI MỚI DỰA TRÊN THÔNG SỐ ẨN
            long cookedTime = meta.getPersistentDataContainer().get(COOKED_TIME_KEY, PersistentDataType.LONG);
            int maxFreshness = meta.getPersistentDataContainer().getOrDefault(MAX_FRESHNESS_KEY, PersistentDataType.INTEGER, 30);
            long ageInMinutes = (System.currentTimeMillis() - cookedTime) / 60000;

            if (ageInMinutes > maxFreshness) {
                player.sendMessage("§cỌe... Món này đã thiu thối do để quá lâu (" + ageInMinutes + "/" + maxFreshness + " phút)!");
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 300, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 0));
                consumeItem(player, item);
                return;
            }

            // 2. CHỐNG SPAM CLICK & XỬ LÝ ĐỘ NGÁN (BOREDOM)
            int currentBoredom = boredomLevels.getOrDefault(uuid, 0);
            boolean isSpicyOrFatty = name.contains("Đậm Vị") || name.contains("Béo Ngậy");

            if (!isSpicyOrFatty) {
                if (currentBoredom >= 100) {
                    player.sendMessage("§c✖ Bạn đã quá ngán đồ ăn nhạt nhẽo! Cổ họng bạn từ chối nuốt thêm!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 0.5f);
                    return; // KHÓA HOÀN TOÀN, CÓ SPAM CŨNG KHÔNG ĂN ĐƯỢC
                }
                else if (currentBoredom >= 50) {
                    // Tỉ lệ từ chối tăng dần theo độ ngán (50% -> 99%)
                    if (Math.random() * 100 < currentBoredom) {
                        player.sendMessage("§eBạn cảm thấy buồn nôn khi ngửi thấy món này...");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 0.5f);
                        // Càng cố spam click, độ ngán càng tăng nhanh để chặn lại
                        boredomLevels.put(uuid, Math.min(100, currentBoredom + 10));
                        return;
                    }
                }
                boredomLevels.put(uuid, Math.min(100, currentBoredom + 25)); // Ăn thành công, tăng 25% ngán
            } else {
                boredomLevels.put(uuid, 0); // Ăn đồ có gia vị sẽ chữa được ngán
                player.sendMessage("§aHương vị đậm đà làm bạn lấy lại cảm giác thèm ăn!");
            }

            // 3. XỬ LÝ TÁC DỤNG CỦA MÓN ĂN
            int durationMinutes = 5;

            if (name.contains("Thịt Hầm Thượng Hạng")) {
                durationMinutes = 20;
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60 * 20, 0));
            } else if (name.contains("Súp Thịt Nguyên Tảng")) {
                durationMinutes = 15;
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 60 * 20, 0));
            } else if (name.contains("Salad Súp Thanh Lọc")) {
                durationMinutes = 2;
                player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30 * 20, 1));
            } else if (name.contains("Kì Lạ")) {
                durationMinutes = 0;
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
                player.sendMessage("§cBạn bị ngộ độc thực phẩm!");
            }

            // 4. ÁP DỤNG TRẠNG THÁI NÓNG/NGUỘI TỰ ĐỘNG
            if (ageInMinutes <= maxFreshness * 0.15) { // 15% thời gian đầu tiên (Ví dụ: 9 phút đầu của món 60p)
                player.sendMessage("§6Bạn vừa húp một ngụm súp nóng hổi! (+2 Phút No căng)");
                durationMinutes += 2;
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
            } else if (ageInMinutes >= maxFreshness * 0.75) { // 25% thời gian cuối (Sắp hỏng)
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