package vn.minecraft.rPGCore.skills.warrior;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarriorBloodbathedBlade implements ISkill {

    @Override public String getId() { return "warrior_bloodbathed_blade"; }
    @Override public String getName() { return "§4§lLưỡi Gươm Nhuộm Máu"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.REDSTONE; }

    @Override public int getMaxLevel() { return 5; } // Tối đa nâng 5 lần
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 16; } // Mở khóa từ sớm
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override public boolean isPassiveSkill() { return true; } // Đánh dấu đây là Nội Tại
    @Override public double getManaCost(int level) { return 0.0; }
    @Override public long getCooldown(int level) { return 0L; }

    @Override
    public List<String> getDescription(int level) {
        // Cấp 1: 2%, Cấp 5: 10%
        double bonusLifesteal = level * 2.0;

        return Arrays.asList(
                "§7Mỗi nhát chém đều khao khát máu tươi của kẻ thù.",
                "",
                "§4[Nội tại: Hút máu]",
                "§f- Tăng vĩnh viễn §c" + bonusLifesteal + "% Tỉ lệ hút máu§f.",
                "§f- Chỉ số này cộng dồn trực tiếp vào nhân vật",
                "§f  và có tác dụng với mọi đòn đánh/kỹ năng."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // Kỹ năng nội tại không có tác dụng khi bấm (Combo), trả về false
        return false;
    }
}