package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.Arrays;

public class AttrGUI implements Listener {

    private final PlayerManager playerManager;
    private final String GUI_TITLE = "§8§lNÂNG CẤP CHỈ SỐ";

    public AttrGUI(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    public void openGUI(Player player) {
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        // Vẽ viền bằng kính đen
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, "§r");
        for (int i = 0; i < 27; i++) {
            if (i < 10 || i == 15 || i > 16) {
                inv.setItem(i, border);
            }
        }

        int unspent = rpgPlayer.getAttributeData().getUnspentAttrPoints();

        // Nút STR (Sức mạnh) - Kính Đỏ
        inv.setItem(10, createItem(Material.RED_STAINED_GLASS_PANE, "§c§lSỨC MẠNH (STR)",
                "§7Tăng Sát thương vật lý & Thể lực tối đa & khả năng đỡ đòn.",
                "§fĐã cộng: §c" + rpgPlayer.getAttributeData().getAttribute("STR"),
                "", "§aNhấp để nâng cấp!", "§eĐiểm tiềm năng: " + unspent));

        // Nút INT (Trí tuệ) - Kính Tím
        inv.setItem(11, createItem(Material.PURPLE_STAINED_GLASS_PANE, "§5§lTRÍ TUỆ (INT)",
                "§7Tăng Sát thương phép & Mana tối đa & Giảm hồi chiêu.",
                "§fĐã cộng: §5" + rpgPlayer.getAttributeData().getAttribute("INT"),
                "", "§aNhấp để nâng cấp!", "§eĐiểm tiềm năng: " + unspent));

        // Nút VIT (Thể lực) - Kính Xanh Lá
        inv.setItem(12, createItem(Material.GREEN_STAINED_GLASS_PANE, "§a§lTHỂ LỰC (VIT)",
                "§7Tăng Máu tối đa & Tỉ lệ hồi phục.",
                "§fĐã cộng: §a" + rpgPlayer.getAttributeData().getAttribute("VIT"),
                "", "§aNhấp để nâng cấp!", "§eĐiểm tiềm năng: " + unspent));

        // Nút AGI (Nhanh nhẹn) - Kính Xanh Dương
        inv.setItem(13, createItem(Material.BLUE_STAINED_GLASS_PANE, "§9§lNHANH NHẸN (AGI)",
                "§7Tăng tốc độ chạy & Tỉ lệ né tránh.",
                "§fĐã cộng: §9" + rpgPlayer.getAttributeData().getAttribute("AGI"),
                "", "§aNhấp để nâng cấp!", "§eĐiểm tiềm năng: " + unspent));

        // Nút LUK (May mắn) - Kính Vàng
        inv.setItem(14, createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§lMAY MẮN (LUK)",
                "§7Tăng tỉ lệ rơi đồ & Tỉ lệ chí mạng.",
                "§fĐã cộng: §e" + rpgPlayer.getAttributeData().getAttribute("LUK"),
                "", "§aNhấp để nâng cấp!", "§eĐiểm tiềm năng: " + unspent));

        // Nút Reset
        inv.setItem(16, createItem(Material.BARRIER, "§c§lRESET CHỈ SỐ",
                "§7Đặt lại toàn bộ điểm chỉ số.",
                "§cHoàn trả 80% số điểm đã cộng.",
                "", "§eNhấp để tiến hành!"));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true); // Chặn không cho người chơi lấy item ra ngoài

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());

        if (rpgPlayer == null) return;

        int slot = event.getSlot();
        boolean upgraded = false;

        if (slot == 10) upgraded = rpgPlayer.getAttributeData().upgradeAttribute("STR");
        else if (slot == 11) upgraded = rpgPlayer.getAttributeData().upgradeAttribute("INT");
        else if (slot == 12) upgraded = rpgPlayer.getAttributeData().upgradeAttribute("VIT");
        else if (slot == 13) upgraded = rpgPlayer.getAttributeData().upgradeAttribute("AGI");
        else if (slot == 14) upgraded = rpgPlayer.getAttributeData().upgradeAttribute("LUK");
        else if (slot == 16) {
            rpgPlayer.getAttributeData().resetAttributes();
            openGUI(player); // Load lại GUI sau khi reset
            return;
        }

        if (upgraded) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            openGUI(player); // Load lại GUI để cập nhật số điểm
        } else if (slot >= 10 && slot <= 14) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§cBạn không có đủ điểm tiềm năng!");
        }
    }
}