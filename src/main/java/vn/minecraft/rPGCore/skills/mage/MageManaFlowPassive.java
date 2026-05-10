package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MageManaFlowPassive implements ISkill {

    @Override public String getId() { return "mage_mana_flow"; }
    @Override public String getName() { return "§b§lDòng Chảy Ma Lực"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.EXPERIENCE_BOTTLE; }

    @Override public int getMaxLevel() { return 5; } // Tối đa 5 cấp giống Archer/Warrior
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 32; } // Mở khóa sớm để fam quái
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override public boolean isPassiveSkill() { return true; }
    @Override public double getManaCost(int level) { return 0.0; }
    @Override public long getCooldown(int level) { return 0L; }

    @Override
    public List<String> getDescription(int level) {
        // Nếu level = 0, lấy giá trị của cấp 1 để hiển thị (10 và 10)
        int displayLevel = (level == 0) ? 1 : level;

        double regenBonus = displayLevel * 10.0;  // 10% mỗi cấp -> Max 50%
        double maxManaBonus = displayLevel * 10.0; // 10 Mana mỗi cấp -> Max 50

        return Arrays.asList(
                "§7Cơ thể bạn hòa làm một với dòng chảy",
                "§7ma lực của thế giới tự nhiên.",
                "",
                "§b[Nội tại - Dòng Chảy Ma Lực]",
                "§f- Tăng vĩnh viễn §b+" + (int)maxManaBonus + " Năng lượng tối đa§f.",
                "§f- Tăng vĩnh viễn §e+" + (int)regenBonus + "% Tốc độ hồi năng lượng§f.",
                "§f- Chỉ số này cộng dồn trực tiếp vào nhân vật."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        player.sendMessage("§c(!) Đây là kỹ năng nội tại, tự động cộng chỉ số!");
        return false;
    }
}