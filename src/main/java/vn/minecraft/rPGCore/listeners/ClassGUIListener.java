package vn.minecraft.rPGCore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.ClassManager;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.managers.ItemManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.ui.BossBarManager;
import vn.minecraft.rPGCore.ui.ClassConfirmGUI;
import vn.minecraft.rPGCore.ui.ClassGUI;

public class ClassGUIListener implements Listener {

    private final PlayerManager playerManager;
    private final BossBarManager bossBarManager;
    private final ClassManager classManager;

    public ClassGUIListener(PlayerManager playerManager, BossBarManager bossBarManager, ClassManager classManager) {
        this.playerManager = playerManager;
        this.bossBarManager = bossBarManager;
        this.classManager = classManager;
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ClassGUI.GUI_TITLE)) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || !(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem.getType() == Material.BLACK_STAINED_GLASS_PANE || clickedItem.getType() == Material.AIR) return;
        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;

        String clickedName = clickedItem.getItemMeta().getDisplayName();

        RPGClass selectedClass = null;
        for (RPGClass rpgClass : classManager.getClasses().values()) {
            String translatedName = ChatColor.translateAlternateColorCodes('&', rpgClass.getDisplayName());
            if (translatedName.equals(clickedName)) {
                selectedClass = rpgClass;
                break;
            }
        }

        if (selectedClass == null) return;

        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        String currentClassId = (rpgPlayer.getCurrentClass() != null)
                ? rpgPlayer.getCurrentClass().getId()
                : "commoner";

        // Không cho chọn lại chính Commoner nếu đang có hệ phái
        if (selectedClass.getId().equalsIgnoreCase(currentClassId)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§eBạn đang thuộc hệ phái này rồi!");
            return;
        }

        // TRƯỜNG HỢP 1: Nhân vật chưa chọn Class (Commoner) -> Chọn miễn phí lần đầu và khóa cố định
        if (currentClassId.equalsIgnoreCase("commoner")) {
            rpgPlayer.changeClass(selectedClass);
            bossBarManager.updateBars(player, rpgPlayer);
            playerManager.savePlayer(player.getUniqueId(), true, false);

            String display = ChatColor.translateAlternateColorCodes('&', selectedClass.getDisplayName());
            player.sendMessage("§aBạn đã gia nhập hệ phái: " + display + " §7(Hệ phái của nhân vật này đã được cố định)");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        // Kiểm tra Cuộn Giấy Chuyển Hệ Phái trong túi đồ
        RPGCore core = RPGCore.getInstance();
        ItemManager itemManager = (core != null) ? core.getItemManager() : null;
        ItemStack scrollTemplate = (itemManager != null) ? itemManager.buildItem("class_change_scroll", 1) : null;
        boolean hasScroll = (scrollTemplate != null && itemManager.countItem(player, scrollTemplate) > 0);

        // TRƯỜNG HỢP 2: Đã có Class nhưng KHÔNG mang theo class_change_scroll -> Chặn
        if (!hasScroll) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§c§l[!] §cHệ phái của nhân vật này đã cố định! Hãy dùng lệnh §e/slot §cđể chuyển sang nhân vật khác, hoặc mang theo §5[Cuộn Giấy Chuyển Hệ Phái] §ctrong túi đồ để đổi phái.");
            return;
        }

        // TRƯỜNG HỢP 3: Đã có Class VÀ có class_change_scroll -> Mở SubMenu Cảnh báo xác nhận
        ClassConfirmGUI.open(player, core, rpgPlayer, selectedClass);
    }
}