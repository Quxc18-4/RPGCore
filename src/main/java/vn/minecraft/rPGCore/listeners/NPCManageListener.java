package vn.minecraft.rPGCore.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.NPCManager;
import vn.minecraft.rPGCore.ui.CustomGUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NPCManageListener implements Listener {

    private final RPGCore plugin;

    // Lưu tạm ID của NPC đang chờ xóa để xử lý trong UI
    private final Map<UUID, String> pendingDeletes = new HashMap<>();

    public NPCManageListener(RPGCore plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // 1. BẮT SỰ KIỆN SHIFT + CLICK TRÁI (ĐẤM NPC)
    // ==========================================
    @EventHandler
    public void onNPCPunch(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        // Kiểm tra xem thực thể bị đấm có phải là NPC Shop của chúng ta không
        if (event.getEntity().getPersistentDataContainer().has(plugin.getNpcManager().NPC_SHOP_KEY, PersistentDataType.STRING)) {

            // Hủy sự kiện sát thương để NPC không bị nhấp nháy đỏ
            event.setCancelled(true);

            // Nếu người chơi đang Shift (Ngồi)
            if (player.isSneaking()) {
                NPCManager.ShopNPC npc = plugin.getNpcManager().getNPCByEntityUUID(event.getEntity().getUniqueId());
                if (npc == null) return;

                // --- KIỂM TRA QUYỀN SỞ HỮU ---
                if (npc.ownerId == null) {
                    // NPC của Server -> Yêu cầu quyền Admin
                    if (!player.hasPermission("rpgcore.admin")) {
                        player.sendMessage("§cBạn không có quyền thu hồi NPC của Hệ thống!");
                        return;
                    }
                } else {
                    // NPC của Lãnh thổ -> Phải là chủ hoặc có quyền Admin
                    if (!npc.ownerId.equals(player.getUniqueId()) && !player.hasPermission("rpgcore.admin")) {
                        player.sendMessage("§cChỉ có Chủ Lãnh Thổ mới được phép thu hồi NPC này!");
                        return;
                    }
                }

                // Nếu thỏa mãn quyền -> Mở GUI Xác nhận
                openConfirmGUI(player, npc);
            }
        }
    }

    // ==========================================
    // 2. TẠO MENU XÁC NHẬN VỚI CUSTOM GUI
    // ==========================================
    private void openConfirmGUI(Player player, NPCManager.ShopNPC npc) {
        // Sử dụng CustomGUI (ID: "CONFIRM_DELETE_NPC", Data: 0)
        CustomGUI customGUI = new CustomGUI("CONFIRM_DELETE_NPC", 0);
        Inventory inv = Bukkit.createInventory(customGUI, 27, "§0§lXÁC NHẬN THU HỒI NPC");

        // Set kính nền
        ItemStack glass = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // Nút Đồng ý
        inv.setItem(11, createGuiItem(Material.EMERALD_BLOCK, "§a§lĐỒNG Ý THU HỒI",
                "§7Bạn đang chuẩn bị thu hồi:",
                "§f" + npc.name, "",
                "§eClick Trái để xác nhận!"));

        // Nút Hủy Bỏ
        inv.setItem(15, createGuiItem(Material.REDSTONE_BLOCK, "§c§lHỦY BỎ",
                "§7Giữ nguyên NPC tại vị trí này.", "",
                "§eClick Trái để đóng Menu!"));

        // Lưu ID NPC vào Map để chờ xử lý
        pendingDeletes.put(player.getUniqueId(), npc.id);

        player.openInventory(inv);
    }

    // ==========================================
    // 3. XỬ LÝ CLICK TRONG MENU XÁC NHẬN
    // ==========================================
    @EventHandler
    public void onConfirmClick(InventoryClickEvent event) {
        // Kiểm tra xem Inventory hiện tại có dùng class CustomGUI của chúng ta không
        if (event.getInventory().getHolder() instanceof CustomGUI) {
            CustomGUI gui = (CustomGUI) event.getInventory().getHolder();

            // Lọc đúng Menu Xác Nhận Xóa
            if (gui.getGuiId().equals("CONFIRM_DELETE_NPC")) {
                event.setCancelled(true);
                Player player = (Player) event.getWhoClicked();

                // Nếu bấm nút ĐỒNG Ý
                if (event.getRawSlot() == 11) {
                    String npcId = pendingDeletes.remove(player.getUniqueId());
                    if (npcId != null) {
                        // [ĐÃ SỬA] Lấy chính xác NPC từ Manager thay vì để null
                        vn.minecraft.rPGCore.managers.NPCManager.ShopNPC targetNpc = plugin.getNpcManager().getNPCById(npcId);

                        // Phát tín hiệu API
                        vn.minecraft.rPGCore.api.events.NpcRemoveEvent removeEvent = new vn.minecraft.rPGCore.api.events.NpcRemoveEvent(player, targetNpc);
                        Bukkit.getPluginManager().callEvent(removeEvent);

                        // Xóa NPC khỏi hệ thống
                        plugin.getNpcManager().removeNPC(npcId);

                        player.sendMessage("§a[Hệ thống] Đã thu hồi NPC thành công!");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                        player.closeInventory();
                    }
                }
                // Nếu bấm nút HỦY BỎ
                else if (event.getRawSlot() == 15) {
                    pendingDeletes.remove(player.getUniqueId());
                    player.closeInventory();
                }
            }
        }
    }

    // Hàm hỗ trợ tạo Item hiển thị
    private ItemStack createGuiItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(line);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}