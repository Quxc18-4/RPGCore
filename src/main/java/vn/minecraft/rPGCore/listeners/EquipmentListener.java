package vn.minecraft.rPGCore.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class EquipmentListener implements Listener {

    private final PlayerManager playerManager;

    public EquipmentListener(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    // 1. CHẶN MẶC BẰNG CHUỘT PHẢI KHI CẦM TRÊN TAY
    @EventHandler
    public void onRightClickEquip(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !isArmor(item.getType())) return;

        Player player = event.getPlayer();
        if (!canPlayerUse(player, item)) {
            event.setCancelled(true);
            player.updateInventory(); // Chống lỗi hiển thị ảo của client Minecraft
        } else {
            // Nếu được phép mặc, delay 1 tick để quét lại chỉ số
            org.bukkit.Bukkit.getScheduler().runTaskLater(RPGCore.getInstance(), () -> {
                RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
                if (rpgPlayer != null) rpgPlayer.updateEquipmentStats();
            }, 1L);
        }
    }

    // 2. CHẶN KÉO THẢ VÀ SHIFT-CLICK TRONG INVENTORY
    @EventHandler
    public void onEquipItem(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Chặn Shift-Click (Mặc nhanh)
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && isArmor(clickedItem.getType())) {
                if (!canPlayerUse(player, clickedItem)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Chặn Kéo Thả trực tiếp vào ô Giáp
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            ItemStack cursorItem = event.getCursor();
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                if (!canPlayerUse(player, cursorItem)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Nếu thay đổi trang bị thành công -> Cập nhật chỉ số
        org.bukkit.Bukkit.getScheduler().runTaskLater(RPGCore.getInstance(), () -> {
            RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
            if (rpgPlayer != null) rpgPlayer.updateEquipmentStats();
        }, 1L);
    }

    // Khi cuộn chuột đổi vũ khí -> Cập nhật chỉ số
    @EventHandler
    public void onWeaponSwap(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        org.bukkit.Bukkit.getScheduler().runTaskLater(RPGCore.getInstance(), () -> {
            RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
            if (rpgPlayer != null) rpgPlayer.updateEquipmentStats();
        }, 1L);
    }

    // ==============================================
    // HÀM KIỂM TRA TỔNG HỢP: CLASS & LEVEL LOCK
    // ==============================================
    public boolean canPlayerUse(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return true;

        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        String pClassId = rpgPlayer != null ? rpgPlayer.getCurrentClass().getId() : "none";
        String pClassName = rpgPlayer != null ? ChatColor.stripColor(rpgPlayer.getCurrentClass().getDisplayName()) : "None";
        int pLevel = player.getLevel();

        boolean classMatch = false;
        boolean hasClassReq = false; // Đánh dấu xem đồ này có yêu cầu class không
        int reqLevel = 0;

        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line).trim();

            // Quét yêu cầu Class
            if (clean.startsWith("▶ Thuộc class:")) {
                hasClassReq = true;
                String reqClass = clean.replace("▶ Thuộc class:", "").trim();
                if (reqClass.equalsIgnoreCase("Tất cả") || reqClass.equalsIgnoreCase(pClassId) || reqClass.equalsIgnoreCase(pClassName)) {
                    classMatch = true;
                }
            }

            // Quét yêu cầu Level
            if (clean.startsWith("▶ Yêu cầu cấp độ:")) {
                try { reqLevel = Integer.parseInt(clean.replace("▶ Yêu cầu cấp độ:", "").trim()); }
                catch (Exception ignored) {}
            }
        }

        if (hasClassReq && !classMatch) {
            sendWarning(player, "§c§l[!] §fBạn không thể sử dụng trang bị của Chức Nghiệp khác!");
            return false;
        }

        if (pLevel < reqLevel) {
            sendWarning(player, "§c§l[!] §fTrình độ của bạn chưa đủ! (Yêu cầu cấp " + reqLevel + ")");
            return false;
        }

        return true;
    }

    // ==============================================
    // [MỚI]: HÀM GỬI CẢNH BÁO CHỐNG SPAM (2 TICK)
    // ==============================================
    private void sendWarning(Player player, String message) {
        if (!player.hasMetadata("Eq_Warn_Cooldown")) {
            player.sendMessage(message);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);

            // Khóa mõm tin nhắn trong 2 Tick (Khoảng 100ms)
            player.setMetadata("Eq_Warn_Cooldown", new FixedMetadataValue(RPGCore.getInstance(), true));
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.hasMetadata("Eq_Warn_Cooldown")) {
                        player.removeMetadata("Eq_Warn_Cooldown", RPGCore.getInstance());
                    }
                }
            }.runTaskLater(RPGCore.getInstance(), 2L);
        }
    }

    private boolean isArmor(Material m) {
        String n = m.name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS") || m == Material.SHIELD;
    }
}