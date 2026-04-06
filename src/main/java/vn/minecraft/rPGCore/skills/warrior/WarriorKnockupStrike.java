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
        // Yêu cầu kỹ năng "Địa Chấn" đạt cấp độ 3
        reqs.put("warrior_earthshatter", 3);
        return reqs;
    }

    @Override public boolean isPassiveSkill() { return false; }

    // Viết rõ để SkillUIManager hiển thị "Tiêu hao: 65 Rage"
    @Override public double getManaCost(int level) { return 65.0; }

    @Override public long getCooldown(int level) {
        // 18s, mỗi cấp giảm 0.4s
        return (long) ((18.0 - (level - 1) * 0.4) * 1000);
    }

    @Override
    public List<String> getDescription(int level) {
        // Chỉ mô tả hiệu ứng, các chỉ số Mana/CD/Level đã có UI lo
        int normalDmg = 40 + (level - 1) * 10;
        int awakenDmg = 220 + (level - 1) * 25;

        return Arrays.asList(
                "§7Kỹ thuật kiếm thuật cơ bản nhưng được",
                "§7tôi luyện đến mức phá vỡ giới hạn.",
                "",
                "§8[Cường Hóa Đòn Đánh Tiếp Theo]:",
                "§f- Chém mạnh tới trước, gây §e" + normalDmg + "% ST Vật lý§f.",
                "§f- §6Đẩy lùi§f mục tiêu.",
                "",
                "§c[Thức Tỉnh Kỹ Năng]",
                "§f- Sát thương tăng vọt lên §c" + awakenDmg + "% ST Vật lý§f.",
                "§f- §cChém ngược cực mạnh, hất tung mục tiêu lên trời.",
                "§f- Gây §8Choáng (2s)§f diện rộng 3x3 khi mục tiêu chạm đất."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            if (System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) {
                player.sendMessage("§cKỹ năng đang hồi chiêu!");
                return false;
            }
        }

        double cost = getManaCost(level);
        // Kiểm tra tài nguyên Nộ (Rage) giống WarriorEarthshatter
        boolean isEmpowered = rpgPlayer.getCurrentResource() >= cost;

        // Dữ liệu truyền xuống Listener: "level_isEmpowered"
        String buffValue = level + "_" + isEmpowered;

        if (isEmpowered) {
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
            applyWeaponEnhancement(player, "Warrior_Knockup_Buff", buffValue, 100L,
                    "§c§l[THỨC TỈNH] §fCường hóa: Đánh Bật",
                    "§8§o(Đòn đánh bật đã hủy bỏ...)");
        } else {
            // Không đủ Nộ vẫn cho dùng nhưng chỉ là đòn thường
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