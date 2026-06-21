package vn.minecraft.rPGCore.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class TrinketMenuGUI implements Listener {

    public static final String GUI_NAME = "§5§lBàn Giả Kim Trang Sức";

    // ==========================================
    // TỰ LẮNG NGHE SỰ KIỆN SHIFT + RIGHT CLICK BÀN RÈN
    // ==========================================
    @EventHandler
    public void onSmithingTableInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        org.bukkit.block.Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Player player = event.getPlayer();

        if (player.isSneaking() && clickedBlock.getType() == Material.SMITHING_TABLE) {
            event.setCancelled(true);
            this.openGUI(player);
            // Đổi âm thanh mở GUI thành tiếng chuông pha lê cực chill
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
        }
    }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_NAME);

        ItemStack blackGlass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack purpleGlass = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        ItemStack magentaGlass = createItem(Material.MAGENTA_STAINED_GLASS_PANE, " ");

        // 1. Phủ nền đen huyền bí
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, blackGlass);
        }

        // 2. Bo góc bằng kính màu Tím và Đỏ tươi (Tạo pattern ma thuật)
        int[] purpleSlots = {0, 8, 9, 17, 18, 26};
        for (int s : purpleSlots) inv.setItem(s, purpleGlass);

        int[] magentaSlots = {1, 7, 19, 25};
        for (int s : magentaSlots) inv.setItem(s, magentaGlass);

        // 3. Đặt một khối tinh thể ở giữa (Ô 13) làm điểm nhấn trang trí
        inv.setItem(13, createItem(Material.AMETHYST_CLUSTER, "§d§lTinh Hoa Giả Kim",
                "§7Nơi hội tụ của ma thuật và khoáng thạch.",
                "§7Hãy chọn một phương thức chế tác."));

        // 4. Nút Ghép Ngẫu Nhiên (Hiệu ứng phát sáng bằng Nether Star)
        inv.setItem(11, createItem(Material.NETHER_STAR, "§d§l[★] Ghép Ngẫu Nhiên",
                "§7Sử dụng phôi và trang sức cũ",
                "§7để ép ra một trang sức mới",
                "§7với phẩm chất và dòng dõi",
                "§7dựa trên nguyên liệu hiến tế.",
                "",
                "§a▶ Click để tiến hành!"));

        // 5. Nút Ghép Công Thức (Dùng Sách Enchant cho đẹp)
        // Đổi từ "Tính năng đang phát triển..." thành "Mở Kho lưu trữ"
        inv.setItem(15, createItem(Material.ENCHANTED_BOOK, "§6§l[?] Ghép Công Thức",
                "§7Sử dụng công thức cố định",
                "§7để chế tạo trang sức đặc biệt.",
                "",
                "§a▶ Click để mở Kho lưu trữ!"));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_NAME)) return;

        // Khóa toàn bộ GUI (bao gồm cả túi đồ người chơi khi đang mở Menu này)
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Xử lý Click
        if (slot == 11) {
            // Âm thanh chuyển cảnh ma thuật
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 2f);
            new TrinketRandomCraftGUI().openGUI(player);
        } else if (slot == 15) {
            // [ĐÃ SỬA]: Mở GUI Ghép công thức cố định
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
            new TrinketRecipeCraftGUI().openGUI(player);
        }
    }
}