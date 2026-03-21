package vn.minecraft.rPGCore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.classes.ClassManager;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.ui.BossBarManager;
import vn.minecraft.rPGCore.ui.ClassGUI;

public class ClassGUIListener implements Listener {

    private final PlayerManager playerManager;
    private final BossBarManager bossBarManager;
    private final ClassManager classManager;

    // Truyền 3 công cụ quản lý vào
    public ClassGUIListener(PlayerManager playerManager, BossBarManager bossBarManager, ClassManager classManager) {
        this.playerManager = playerManager;
        this.bossBarManager = bossBarManager;
        this.classManager = classManager;
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent event) {
        // Kiểm tra xem có đúng là người chơi đang mở GUI Chọn Class không
        if (!event.getView().getTitle().equals(ClassGUI.GUI_TITLE)) return;

        // 1. CHẶN NGAY LẬP TỨC việc nhấc item ra khỏi rương (Chặn mọi cú click)
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;
        ItemStack clickedItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();

        // Bỏ qua nếu click vào kính màu đen hoặc ô trống
        if (clickedItem.getType() == Material.BLACK_STAINED_GLASS_PANE || clickedItem.getType() == Material.AIR) return;

        // Lấy tên hiển thị của item (ví dụ: "§cChiến Binh")
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;
        String clickedName = clickedItem.getItemMeta().getDisplayName();

        // 2. TÌM CLASS TƯƠNG ỨNG VỚI TÊN ITEM
        RPGClass selectedClass = null;
        for (RPGClass rpgClass : classManager.getClasses().values()) {
            // Dịch mã màu (ví dụ &c thành §c) để so sánh cho khớp chuẩn
            String translatedName = ChatColor.translateAlternateColorCodes('&', rpgClass.getDisplayName());
            if (translatedName.equals(clickedName)) {
                selectedClass = rpgClass;
                break;
            }
        }

        // 3. THỰC HIỆN ĐỔI CLASS
        if (selectedClass != null) {
            RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
            if (rpgPlayer != null) {
                // Cập nhật dữ liệu
                rpgPlayer.changeClass(selectedClass);

                // Cập nhật UI BossBar
                bossBarManager.updateBars(player, rpgPlayer);

                // Thông báo, phát âm thanh level up và đóng GUI
                player.sendMessage(ChatColor.GREEN + "Bạn đã chuyển sang Class: " +
                        ChatColor.translateAlternateColorCodes('&', selectedClass.getDisplayName()));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.closeInventory();
            }
        }
    }
}