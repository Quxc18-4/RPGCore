package vn.minecraft.rPGCore.managers;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class CurrencyManager {

    private final JavaPlugin plugin;
    public final NamespacedKey CURRENCY_KEY;

    public CurrencyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.CURRENCY_KEY = new NamespacedKey(plugin, "rpg_currency_type");
    }

    // ==========================================
    // HÀM TẠO VẬT PHẨM TIỀN TỆ (CÓ LORE TÙY CHỈNH)
    // ==========================================
    public ItemStack getCurrencyItem(String type, int amount) {
        Material mat;
        String name;
        String desc;

        if (type.equals("gold")) {
            mat = Material.GOLD_NUGGET;
            name = "§eXu Vàng";
            desc = "§7Loại tiền có mệnh giá cao nhất.";
        } else if (type.equals("iron")) {
            mat = Material.IRON_NUGGET;
            name = "§fXu Bạc";
            desc = "§7Loại tiền tương đối quý giá.";
        } else {
            mat = Material.COPPER_NUGGET; // Dùng Raw Copper để tránh lỗi không tồn tại
            name = "§6Xu Đồng";
            desc = "§7Loại tiền cơ bản dùng để giao dịch.";
        }

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add("§f[§a+§f] §fPhân loại: §eTiền tệ.");
        lore.add("§f[§a+§f] §fMô tả: " + desc);
        meta.setLore(lore);

        // Gắn NBT Tag chống làm giả
        meta.getPersistentDataContainer().set(CURRENCY_KEY, PersistentDataType.STRING, type);
        item.setItemMeta(meta);
        return item;
    }

    // Lấy giá trị của một ItemStack tiền tệ
    public long getCurrencyValue(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return 0;
        String type = item.getItemMeta().getPersistentDataContainer().get(CURRENCY_KEY, PersistentDataType.STRING);
        if (type == null) return 0;

        long multiplier = type.equals("gold") ? 4096 : (type.equals("iron") ? 64 : 1);
        return item.getAmount() * multiplier;
    }

    // Chuyển đổi con số ảo thành chuỗi hiển thị đẹp mắt
    public String formatCurrency(long amount) {
        if (amount <= 0) return "0 Đồng";

        long gold = amount / 4096;
        long iron = (amount % 4096) / 64;
        long copper = amount % 64;

        StringBuilder sb = new StringBuilder();
        if (gold > 0) sb.append("§e").append(gold).append(" Vàng ");
        if (iron > 0) sb.append("§f").append(iron).append(" Bạc ");
        if (copper > 0) sb.append("§6").append(copper).append(" Đồng");

        return sb.toString().trim();
    }

    // ==========================================
    // HÀM TỰ ĐỘNG GỘP TIỀN LẺ TRONG TÚI ĐỒ
    // (64 Đồng -> 1 Bạc, 64 Bạc -> 1 Vàng)
    // ==========================================
    public void compressCurrency(org.bukkit.entity.Player player) {
        org.bukkit.inventory.Inventory inv = player.getInventory();
        long totalValue = 0;

        // Quét toàn bộ túi đồ, gom tổng giá trị và xóa item tiền lẻ
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            long val = getCurrencyValue(item);
            if (val > 0) {
                totalValue += val;
                inv.setItem(i, null);
            }
        }

        if (totalValue == 0) return;

        // Trả lại túi đồ với cấu trúc tối ưu nhất
        long gold = totalValue / 4096;
        long iron = (totalValue % 4096) / 64;
        long copper = totalValue % 64;

        giveCurrencySafe(player, "gold", gold);
        giveCurrencySafe(player, "iron", iron);
        giveCurrencySafe(player, "copper", copper);
    }

    // ==========================================
    // HÀM NẠP TOÀN BỘ TIỀN VÀO TÀI KHOẢN (DEPOSIT ALL)
    // ==========================================
    public void depositAll(org.bukkit.entity.Player player, vn.minecraft.rPGCore.player.RPGPlayer rpgPlayer) {
        org.bukkit.inventory.Inventory inv = player.getInventory();
        long totalValue = 0;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            long val = getCurrencyValue(item);
            if (val > 0) {
                totalValue += val;
                inv.setItem(i, null);
            }
        }

        if (totalValue > 0) {
            rpgPlayer.addBalance(totalValue);
            player.sendMessage("§a[Ngân Hàng] §fĐã nạp thành công " + formatCurrency(totalValue) + " vào tài khoản!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        } else {
            player.sendMessage("§cBạn không có đồng Xu nào trong túi đồ!");
        }
    }

    // Hàm an toàn để add Item số lượng lớn (tránh lỗi stack > 64)
    public void giveCurrencySafe(org.bukkit.entity.Player player, String type, long amount) {
        while (amount > 0) {
            int toGive = (int) Math.min(64, amount);
            java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(getCurrencyItem(type, toGive));
            // Nếu túi đầy, rớt ra đất
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            amount -= toGive;
        }
    }

    // ==========================================
    // HÀM DỊCH NGƯỢC GIÁ TRỊ THÀNH VẬT PHẨM (DÙNG CHO ADMIN GUI)
    // ==========================================
    public ItemStack createCurrencyStackFromValue(long value) {
        if (value <= 0) return null;

        // Vì Admin chỉ có 1 ô để đặt giá (tối đa 64 item), ta có thể dịch ngược lại chính xác đồng tiền họ đã đặt
        if (value % 4096 == 0 && (value / 4096) <= 64) {
            return getCurrencyItem("gold", (int) (value / 4096));
        }
        else if (value % 64 == 0 && (value / 64) <= 64) {
            return getCurrencyItem("iron", (int) (value / 64));
        }
        else if (value <= 64) {
            return getCurrencyItem("copper", (int) value);
        }

        return null; // Trả về null nếu không xác định được
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ GIAO DỊCH BẰNG TIỀN MẶT
    // ==========================================

    // 1. Lấy tổng số tiền vật lý đang có trong túi đồ
    public long getTotalPhysicalCurrency(org.bukkit.entity.Player player) {
        long total = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            total += getCurrencyValue(item);
        }
        return total;
    }

    // 2. Trả tiền vật lý trực tiếp vào túi (Tự chia ra Vàng/Bạc/Đồng)
    public void givePhysicalCurrency(org.bukkit.entity.Player player, long amount) {
        if (amount <= 0) return;
        long gold = amount / 4096;
        long iron = (amount % 4096) / 64;
        long copper = amount % 64;

        if (gold > 0) giveCurrencySafe(player, "gold", gold);
        if (iron > 0) giveCurrencySafe(player, "iron", iron);
        if (copper > 0) giveCurrencySafe(player, "copper", copper);
    }

    // 3. Thanh toán tiền mặt (Tự động tính toán và thối lại tiền thừa)
    public boolean takePhysicalCurrency(org.bukkit.entity.Player player, long amount) {
        long total = getTotalPhysicalCurrency(player);
        if (total < amount) return false; // Không đủ tiền mặt

        // Xóa sạch toàn bộ tiền cũ trong túi
        org.bukkit.inventory.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (getCurrencyValue(inv.getItem(i)) > 0) {
                inv.setItem(i, null);
            }
        }

        // Tính tiền thừa và trả lại vào túi
        long remaining = total - amount;
        givePhysicalCurrency(player, remaining);

        return true; // Thanh toán thành công
    }
}