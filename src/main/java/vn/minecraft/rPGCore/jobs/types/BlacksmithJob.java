package vn.minecraft.rPGCore.jobs.types;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.minecraft.rPGCore.jobs.core.RPGJob;

import java.util.ArrayList;
import java.util.List;

public class BlacksmithJob extends RPGJob {

    public BlacksmithJob() {
        super("BLACKSMITH", "Thợ Rèn");
    }

    @Override
    public ItemStack getIcon(int currentLevel) {
        ItemStack icon = new ItemStack(Material.ANVIL);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§lNGHỀ THỢ RÈN");
            meta.setLore(getDescription(currentLevel));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    @Override
    public List<String> getDescription(int currentLevel) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Bậc thầy về thao tác kim loại và nhiệt độ.");
        lore.add("");
        lore.add("§fCấp độ hiện tại: §b" + currentLevel + "§8/§b" + maxLevel);
        lore.add("");
        lore.add("§a§lNỘI TẠI NGHỀ:");
        lore.add("§8▪ §fTỉ lệ thành công khi rèn: §a+" + String.format("%.1f", getForgeSuccessBonus(currentLevel)) + "%");
        lore.add("§8▪ §fTỉ lệ nhận Dòng dõi (Mốc an toàn): §6" + String.format("%.1f", (50.0 + getLineageStarBonus(currentLevel) * 100)) + "%");
        lore.add("§8▪ §fTỉ lệ hoàn trả Mạt Thợ Rèn khi xịt: §c" + String.format("%.1f", getScrapRefundChance(currentLevel)) + "%");
        lore.add("§8▪ §fXác suất giữ lại Đá khi đập: §b" + String.format("%.1f", getStoneSaveChance(currentLevel)) + "%");
        return lore;
    }

    // =========================================
    // CÁC CHỈ SỐ TĂNG TIẾN MƯỢT MÀ THEO TỪNG LEVEL
    // =========================================

    @Override
    public double getForgeSuccessBonus(int level) {
        // Tăng 0.2% mỗi cấp (Cấp 1: 0.2% -> Cấp 50: 10.0%)
        return level * 0.2;
    }

    @Override
    public double getLineageStarBonus(int level) {
        // Cộng thêm 0.6% mỗi cấp (Cấp 1: 0.6% -> Cấp 50: 30.0%)
        // Hàm này trả về số thập phân để cộng vào 0.50 (50%) gốc
        return (level * 0.6) / 100.0;
    }

    @Override
    public double getScrapRefundChance(int level) {
        // Tăng 0.6% mỗi cấp (Cấp 1: 0.6% -> Cấp 50: 30.0%) [Đã nerf từ 50% xuống 30%]
        return level * 0.6;
    }

    // CƠ CHẾ MỚI: Xác suất đập đồ mà không bị mất đá
    public double getStoneSaveChance(int level) {
        // Tăng 0.5% mỗi cấp (Cấp 1: 0.5% -> Cấp 50: 25.0%)
        return level * 0.5;
    }
}