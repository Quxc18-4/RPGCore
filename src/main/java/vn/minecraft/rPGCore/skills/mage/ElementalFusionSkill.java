package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;
import vn.minecraft.rPGCore.skills.mage.fusion.FusionEngine;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementalFusionSkill implements ISkill {

    private final JavaPlugin plugin;

    public ElementalFusionSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "mage_elemental_fusion"; }
    @Override public String getName() { return "Ma Pháp Dung Hợp"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.NETHER_STAR; }
    @Override public boolean isPassiveSkill() { return false; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public int getRequiredPlayerLevel() { return 20; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> reqs = new HashMap<>();
        reqs.put("mage_focus", 10);
        reqs.put("mage_elemental_burst", 5);
        reqs.put("mage_elemental_sphere", 5);
        return reqs;
    }

    @Override
    public double getManaCost(int level) {
        double cost = 90.0;
        if (level >= 3) cost += 20.0;
        if (level >= 6) cost += 20.0;
        if (level >= 9) cost += 20.0;
        return cost;
    }

    @Override public long getCooldown(int level) { return (long) ((18.0 - (level * 0.5)) * 1000L); }

    @Override public List<String> getDescription(int level) { return getDescription(null, level); }

    // ==========================================
    // LORE SẼ ĐƯỢC BỔ SUNG DẦN DẦN (THEO YÊU CẦU CỦA BẠN)
    // ==========================================
    @Override
    public List<String> getDescription(Player player, int level) {
        int lv = Math.max(1, level);
        double baseDmgPct = 330.0 + (lv * 22.0);

        MageElement nth = MageElement.FIRE;
        MageElement tnt = null; // Mặc định là null nếu chưa có TNT
        String tntName = "§8(Không có)";

        if (player != null) {
            nth = ElementalSwapSkill.getActiveElement(player);
            if (player.hasMetadata("Mage_Previous_Element")) {
                try {
                    tnt = MageElement.valueOf(player.getMetadata("Mage_Previous_Element").get(0).asString());
                    tntName = tnt.getDisplayName();
                } catch (Exception ignored) {}
            }
        }

        // Khởi tạo Lore cơ bản
        List<String> lore = new java.util.ArrayList<>(Arrays.asList(
                "§7Dung hợp 2 loại nguyên tố với nhau, sự giao thoa của",
                "§7hiện tại và quá khứ để kích hoạt phản ứng dữ dội.",
                "",
                "§8▪ §fNguyên Tố Trước: " + tntName,
                "§8▪ §fNguyên Tố Hiện Tại: " + nth.getDisplayName(),
                "",
                "§f- Sát thương gốc: §b" + (int)baseDmgPct + "% §fSát thương.",
                "",
                "§e[Kích hoạt - Hình thái Dung Hợp]"
        ));

        // Phân nhánh hiển thị (Hiện tại chỉ có Default, sẽ thêm dần)
        if (tnt == null) {
            double finalDmg = baseDmgPct * 1.0;
            lore.add("§8▪ §f§lMA LỰC THUẦN TÚY");
            lore.add("§f- Bắn một quả cầu ma lực trung tính, chạm nổ.");
            lore.add("§f- Gây §c§l" + (int)finalDmg + "% §fSát thương.");
        }

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;

        // 1. Phân tích Nguyên tố
        MageElement nth = ElementalSwapSkill.getActiveElement(player);
        MageElement tnt = null;

        if (player.hasMetadata("Mage_Previous_Element")) {
            try {
                tnt = MageElement.valueOf(player.getMetadata("Mage_Previous_Element").get(0).asString());
            } catch (Exception ignored) {}
        }

        // Trong hàm execute() của ElementalFusionSkill.java

        // [FIX BUG]: Chỉ tính Hệ số Kỹ năng truyền xuống Lõi (VD Lv 10 = 5.5)
        double multiplier = (330.0 + (level * 22.0)) / 100.0;

        // Gọi Engine định tuyến (Tìm Class Phản ứng)
        IFusion activeFusion = FusionEngine.getActiveFusion(tnt, nth);

        player.sendMessage("§d§l[!] " + activeFusion.getReactionName() + " §fKích hoạt!");

        // Giao toàn quyền thi triển cho Class đó (Truyền đúng multiplier)
        activeFusion.cast(player, rpgPlayer, level, multiplier);

        // 6. Dọn dẹp & Khóa Cooldown
        player.removeMetadata("Mage_Previous_Element", RPGCore.getInstance());
        applyGlobalCooldown(player, rpgPlayer, level);
        return true;
    }
}