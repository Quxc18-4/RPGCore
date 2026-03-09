package vn.minecraft.rPGCore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FoodListener implements Listener {

    // Bộ nhớ lưu trữ thời gian "No Căng" của từng người chơi (Tính bằng milliseconds)
    public static final Map<UUID, Long> fullnessTimers = new HashMap<>();

    // 1. NGƯỜI CHƠI CLICK CHUỘT PHẢI ĐỂ ĂN
    @EventHandler
    public void onInteractEat(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return;

            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            Player player = event.getPlayer();
            boolean consumed = false;

            // Xử lý các món ăn đã được định nghĩa trong CookingManager
            if (name.equals("Món Ăn Kì Lạ")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 1)); // Chóng mặt 10s
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 1));    // Nhanh đói 10s
                player.sendMessage("§cỌe... Mùi vị thật kinh khủng!");
                consumed = true;
            }
            else if (name.equals("Bò Hầm Nấm")) {
                // Thêm 15 phút không bị đói (15 * 60 * 1000 = 900,000 ms)
                fullnessTimers.put(player.getUniqueId(), System.currentTimeMillis() + 900000L);
                player.setFoodLevel(20); // Hồi đầy cây đói ngay lập tức

                // Tăng sát thương (Strength I) trong 15 phút
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 900 * 20, 0));

                player.sendMessage("§aBạn cảm thấy no căng! (Không bị đói & Tăng sát thương trong 15 phút)");
                consumed = true;
            }
            else if (name.equals("Táo Phết Đường")) {
                // Thêm 5 phút không bị đói (300,000 ms)
                fullnessTimers.put(player.getUniqueId(), System.currentTimeMillis() + 300000L);
                player.setFoodLevel(20);

                // Tăng tốc chạy (Speed I) trong 5 phút
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300 * 20, 0));

                player.sendMessage("§aNgọt quá! (Không bị đói & Tăng tốc độ trong 5 phút)");
                consumed = true;
            }

            // Nếu đã ăn thành công thì trừ Item đi
            if (consumed) {
                event.setCancelled(true);
                item.setAmount(item.getAmount() - 1);

                // Nếu ăn súp thì trả lại cái Bát rỗng (Bowl)
                if (item.getType() == Material.MUSHROOM_STEW || item.getType() == Material.RABBIT_STEW) {
                    player.getInventory().addItem(new ItemStack(Material.BOWL));
                }

                // Phát âm thanh Ợ
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
            }
        }
    }

    // 2. NGĂN CHẶN TỤT THỨC ĂN NẾU ĐANG CÓ BUFF "NO CĂNG"
    @EventHandler
    public void onHungerDeplete(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();

        if (fullnessTimers.containsKey(uuid)) {
            // Nếu thời gian hiện tại vẫn còn nhỏ hơn thời gian hết hạn buff
            if (System.currentTimeMillis() < fullnessTimers.get(uuid)) {
                event.setCancelled(true); // Ngăn tụt đùi gà
                player.setFoodLevel(20);
            } else {
                // Đã hết thời gian Buff -> Xóa khỏi danh sách để bắt đầu tụt đói lại bình thường
                fullnessTimers.remove(uuid);
                player.sendMessage("§eBụng bạn bắt đầu sôi lên... Bạn đã hết trạng thái No Căng.");
            }
        }
    }
}