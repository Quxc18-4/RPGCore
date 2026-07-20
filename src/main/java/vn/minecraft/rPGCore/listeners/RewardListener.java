package vn.minecraft.rPGCore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import vn.minecraft.rPGCore.RPGCore;

public class RewardListener implements Listener {
    private final RPGCore plugin;

    public RewardListener(RPGCore plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // SỰ KIỆN ĐÓNG KHO ĐỒ -> LƯU VÀO DATABASE
    // ==========================================
    @EventHandler
    public void onAdminGUIClosed(InventoryCloseEvent event) {
        String title = event.getView().getTitle();

        if (title.startsWith("§0§lEdit Kit: ")) {
            String kitName = title.replace("§0§lEdit Kit: ", "").trim();
            plugin.getRewardManager().saveCustomKit(kitName, event.getInventory());
            event.getPlayer().sendMessage("§a§l[Hệ Thống] §fĐã lưu vật phẩm cho Kit §e" + kitName + "§f!");
        }
        else if (title.startsWith("§0§lEdit Monthly: ")) {
            try {
                int day = Integer.parseInt(title.replace("§0§lEdit Monthly: ", "").trim());
                plugin.getRewardManager().saveMonthlyReward(day, event.getInventory());
                event.getPlayer().sendMessage("§a§l[Hệ Thống] §fĐã cập nhật phần thưởng cho Ngày §e" + day + "§f!");
            } catch (Exception ignored) {}
        }
    }

    // ==========================================
    // SỰ KIỆN CLICK -> QUẢN LÝ VÀ ÉP MỞ KHÓA KÉO THẢ
    // Dùng HIGHEST để đè bẹp mọi file/plugin đang khóa GUI
    // ==========================================
    @EventHandler
    public void onAdminGUIClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // 1. XỬ LÝ CHO CUSTOM KIT
        if (title.startsWith("§0§lEdit Kit: ")) {
            // [THẦN CHÚ MỞ KHÓA]: Hủy bỏ mọi lệnh cấm kéo thả từ các plugin khác
            event.setCancelled(false);
        }

        // 2. XỬ LÝ CHO MONTHLY
        else if (title.startsWith("§0§lEdit Monthly: ")) {
            int slot = event.getRawSlot();
            Player player = (Player) event.getWhoClicked();

            // Nếu click vào 2 nút Mũi Tên -> CẤM KÉO THẢ & CHUYỂN TRANG
            if (slot == 9 || slot == 17) {
                event.setCancelled(true);

                try {
                    int currentDay = Integer.parseInt(title.replace("§0§lEdit Monthly: ", "").trim());
                    plugin.getRewardManager().saveMonthlyReward(currentDay, event.getInventory());

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (slot == 9 && currentDay > 1) {
                            player.openInventory(plugin.getRewardManager().getMonthlyAdminGUI(currentDay - 1));
                        } else if (slot == 17 && currentDay < 31) {
                            player.openInventory(plugin.getRewardManager().getMonthlyAdminGUI(currentDay + 1));
                        }
                    });
                } catch (Exception ignored) {}
            }
            // Nếu click vào các ô trống còn lại -> ÉP MỞ KHÓA ĐỂ ĐẶT QUÀ
            else {
                event.setCancelled(false);
            }
        }
    }
}