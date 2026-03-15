package vn.minecraft.rPGCore.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BankManager {

    private final CurrencyManager currencyManager;

    public BankManager(CurrencyManager currencyManager) {
        this.currencyManager = currencyManager;
    }

    // ==========================================
    // GIAO DIỆN CHÍNH (NẠP / RÚT / THÔNG TIN)
    // ==========================================
    public Inventory openBankMainUI(Player player, long currentBalance) {
        Inventory inv = Bukkit.createInventory(null, 27, "§0§lNGÂN HÀNG TRUNG ƯƠNG");

        // Kính nền
        ItemStack glass = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // Nút Nạp Tiền
        inv.setItem(11, createGuiItem(Material.EMERALD, "§a§lNẠP TẤT CẢ XU",
                "§7Quét toàn bộ túi đồ của bạn", "§7và nạp hết Xu vào tài khoản ảo.", "", "§eClick để Nạp ngay!"));

        // Nút Thông Tin
        inv.setItem(13, createGuiItem(Material.BOOK, "§e§lSỔ TÀI KHOẢN",
                "§7Chủ thẻ: §f" + player.getName(),
                "§7Số dư hiện tại: ", "§b" + currencyManager.formatCurrency(currentBalance)));

        // Nút Rút Tiền
        inv.setItem(15, createGuiItem(Material.GOLD_INGOT, "§6§lRÚT TIỀN MẶT",
                "§7Chuyển đổi tiền trong tài khoản ảo", "§7thành các đồng Xu vật lý để giao dịch.", "", "§eClick để chọn mệnh giá!"));

        return inv;
    }

    // ==========================================
    // GIAO DIỆN RÚT TIỀN MẶT
    // ==========================================
    public Inventory openWithdrawUI(long currentBalance) {
        Inventory inv = Bukkit.createInventory(null, 27, "§0§lRÚT TIỀN MẶT");

        ItemStack glass = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        inv.setItem(11, createGuiItem(Material.GOLD_NUGGET, "§eRút 1 Xu Vàng", "§7Tốn: 4096 Đồng (Số dư ảo)", "", "§eClick Trái để Rút!"));
        inv.setItem(13, createGuiItem(Material.IRON_NUGGET, "§fRút 1 Xu Bạc", "§7Tốn: 64 Đồng (Số dư ảo)", "", "§eClick Trái để Rút!"));
        inv.setItem(15, createGuiItem(Material.RAW_COPPER, "§6Rút 1 Xu Đồng", "§7Tốn: 1 Đồng (Số dư ảo)", "", "§eClick Trái để Rút!"));

        inv.setItem(26, createGuiItem(Material.BARRIER, "§cQuay Lại", "§7Trở về Menu Ngân Hàng"));
        return inv;
    }

    private ItemStack createGuiItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) lore.add(line);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}