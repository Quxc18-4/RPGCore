package vn.minecraft.rPGCore.skills.warrior;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.AbstractEnhancementSkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarriorKnockupStrike extends AbstractEnhancementSkill {

    public WarriorKnockupStrike(JavaPlugin plugin) {
        super(plugin);
    }

    @Override public String getId() { return "warrior_knockup_strike"; }
    @Override public String getName() { return "§e§lĐánh Bật"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.IRON_SWORD; }

    @Override public int getMaxLevel() { return 6; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 16; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> reqs = new HashMap<>();
        reqs.put("warrior_earthshatter", 3); // Yêu cầu Dậm Đất lv 3
        return reqs;
    }

    @Override public boolean isPassiveSkill() { return false; }

    @Override public double getManaCost(int level) { return 0; } // Cost ghi là 0 vì skill này tự trừ Nộ (Rage) theo điều kiện

    @Override public long getCooldown(int level) {
        // 18s giảm 0.4s mỗi cấp
        return (long) ((18.0 - (level - 1) * 0.4) * 1000);
    }

    @Override
    public List<String> getDescription(int level) {
        int normalDmg = 40 + (level - 1) * 10;
        int awakenDmg = 220 + (level - 1) * 25;

        // UI Manager đã tự vẽ Level, Cost và CD, nên ở đây chỉ tập trung vào cốt truyện và hiệu ứng
        return Arrays.asList(
                "§7Kỹ thuật chém ngược cơ bản nhưng được",
                "§7tôi luyện đến mức hoàn mỹ.",
                "",
                "§8[Cường Hóa Đòn Đánh Tiếp Theo]:",
                "§f- Chém mạnh tới trước, gây §e" + normalDmg + "% ST Vật lý§f.",
                "§f- §6Đẩy lùi§f mục tiêu văng ra xa.",
                "",
                "§c[Thức Tỉnh Kỹ Năng]",
                "§f- Sát thương tăng vọt lên §c" + awakenDmg + "% ST Vật lý§f.",
                "§f- §cKéo cao lưỡi kiếm, hất văng mục tiêu lên trời.",
                "§f- Gây §8Choáng (2s)§f diện rộng 3x3 khi mục tiêu chạm đất."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            List<org.bukkit.metadata.MetadataValue> metaList = player.getMetadata(cdKey);
            if (!metaList.isEmpty()) {
                long cdEnd = metaList.get(0).asLong();
                if (System.currentTimeMillis() < cdEnd) {
                    player.sendMessage("§cKỹ năng đang hồi chiêu!");
                    return false;
                }
            } else { player.removeMetadata(cdKey, plugin); }
        }

        // Kiểm tra Nộ tại thời điểm BẤM CHIÊU
        boolean isEmpowered = rpgPlayer.getCurrentResource() >= 40.0;
        String buffValue = level + "_" + isEmpowered; // Đóng gói data để Listener đọc (VD: "6_true")

        if (isEmpowered) {
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - 40.0);

            // Gọi hàm của AbstractEnhancementSkill để lưu Buff vào Pool trong 5 giây (100 ticks)
            applyWeaponEnhancement(player, "Warrior_Knockup_Buff", buffValue, 100L,
                    "§c§l[THỨC TỈNH] §fCường hóa: Đánh Bật",
                    "§8§o(Đòn đánh bật đã hủy bỏ...)");
        } else {
            applyWeaponEnhancement(player, "Warrior_Knockup_Buff", buffValue, 100L,
                    "§e§l[!] §fCường hóa: Đẩy Lùi",
                    "§8§o(Đòn đẩy lùi đã hủy bỏ...)");
        }

        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        return true;
    }
}