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
    // UI TIỀN TỆ ĐỒNG BỘ
    // ==========================================
    public ItemStack getCurrencyItem(String type, int amount) {
        Material mat;
        String name;
        String desc;
        String rarityColor;
        String rarityName;

        if (type.equals("gold")) {
            mat = Material.GOLD_NUGGET;
            name = "Xu Vàng";
            desc = "Loại tiền có mệnh giá cao nhất.";
            rarityColor = "§b"; // RARE
            rarityName = "Rare";
        } else if (type.equals("iron")) {
            mat = Material.IRON_NUGGET;
            name = "Xu Bạc";
            desc = "Loại tiền tương đối quý giá.";
            rarityColor = "§a"; // UNCOMMON
            rarityName = "Uncommon";
        } else {
            mat = Material.COPPER_NUGGET;
            name = "Xu Đồng";
            desc = "Loại tiền cơ bản dùng để giao dịch.";
            rarityColor = "§7"; // COMMON
            rarityName = "Common";
        }

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();

        // Tên có màu theo Rarity
        meta.setDisplayName(rarityColor + "§l" + name);

        List<String> lore = new ArrayList<>();

        // Phân loại
        lore.add("§8[ Phân loại: Tiền tệ - Tiền kim loại ]");
        lore.add("§8§m                                ");

        // Mô tả
        lore.add("§7Mô tả:");
        lore.add("§7" + desc);
        lore.add("");

        // Rarity Footer
        lore.add("§8§m                                ");
        lore.add("Độ hiếm: " + rarityColor + "[" + rarityName + "]");

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(CURRENCY_KEY, PersistentDataType.STRING, type);
        item.setItemMeta(meta);
        return item;
    }

    public long getCurrencyValue(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return 0;
        String type = item.getItemMeta().getPersistentDataContainer().get(CURRENCY_KEY, PersistentDataType.STRING);
        if (type == null) return 0;

        long multiplier = type.equals("gold") ? 4096 : (type.equals("iron") ? 64 : 1);
        return item.getAmount() * multiplier;
    }

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

    public void compressCurrency(org.bukkit.entity.Player player) {
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

        if (totalValue == 0) return;

        long gold = totalValue / 4096;
        long iron = (totalValue % 4096) / 64;
        long copper = totalValue % 64;

        giveCurrencySafe(player, "gold", gold);
        giveCurrencySafe(player, "iron", iron);
        giveCurrencySafe(player, "copper", copper);
    }

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

    public void giveCurrencySafe(org.bukkit.entity.Player player, String type, long amount) {
        while (amount > 0) {
            int toGive = (int) Math.min(64, amount);
            java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(getCurrencyItem(type, toGive));
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            amount -= toGive;
        }
    }

    public ItemStack createCurrencyStackFromValue(long value) {
        if (value <= 0) return null;

        if (value % 4096 == 0 && (value / 4096) <= 64) {
            return getCurrencyItem("gold", (int) (value / 4096));
        }
        else if (value % 64 == 0 && (value / 64) <= 64) {
            return getCurrencyItem("iron", (int) (value / 64));
        }
        else if (value <= 64) {
            return getCurrencyItem("copper", (int) value);
        }
        return null;
    }

    public long getTotalPhysicalCurrency(org.bukkit.entity.Player player) {
        long total = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            total += getCurrencyValue(item);
        }
        return total;
    }

    public void givePhysicalCurrency(org.bukkit.entity.Player player, long amount) {
        if (amount <= 0) return;
        long gold = amount / 4096;
        long iron = (amount % 4096) / 64;
        long copper = amount % 64;

        if (gold > 0) giveCurrencySafe(player, "gold", gold);
        if (iron > 0) giveCurrencySafe(player, "iron", iron);
        if (copper > 0) giveCurrencySafe(player, "copper", copper);
    }

    public boolean takePhysicalCurrency(org.bukkit.entity.Player player, long amount) {
        long total = getTotalPhysicalCurrency(player);
        if (total < amount) return false;

        org.bukkit.inventory.Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (getCurrencyValue(inv.getItem(i)) > 0) {
                inv.setItem(i, null);
            }
        }

        long remaining = total - amount;
        givePhysicalCurrency(player, remaining);

        return true;
    }
}