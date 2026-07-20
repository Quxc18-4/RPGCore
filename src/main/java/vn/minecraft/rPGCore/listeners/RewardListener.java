package vn.minecraft.rPGCore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
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
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        org.bukkit.inventory.InventoryHolder holder = event.getInventory().getHolder();

        // CHỈ KIỂM TRA NẾU TÚI ĐỒ LÀ CUSTOM GUI CỦA CHÚNG TA
        if (holder instanceof vn.minecraft.rPGCore.ui.CustomGUI) {
            vn.minecraft.rPGCore.ui.CustomGUI gui = (vn.minecraft.rPGCore.ui.CustomGUI) holder;

            // Bắt sự kiện cho GUI Điểm Danh của Người chơi
            if (gui.getGuiId().equals("MONTHLY_USER_GUI")) {
                event.setCancelled(true); // Khóa kéo thả

                Player player = (Player) event.getWhoClicked();
                ItemStack clicked = event.getCurrentItem();

                if (clicked != null && clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
                    // Trích xuất "Ngày" từ Tên của Kính (Ví dụ: "§a§lNgày 14 (Nhấn để nhận)")
                    try {
                        String cleanName = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                        String number = cleanName.replaceAll("[^0-9]", ""); // Lọc lấy số
                        int day = Integer.parseInt(number);

                        plugin.getRewardManager().claimMonthlyReward(player, day);
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}