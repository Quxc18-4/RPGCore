package vn.minecraft.rPGCore.skills.reaper;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReaperBloodVitality implements ISkill {

    @Override public String getId() { return "reaper_blood_vitality"; }
    @Override public String getName() { return "§4§lHuyết Mạch Trường Sinh"; }
    @Override public String getRequiredClass() { return "reaper"; }
    @Override public Material getIcon() { return Material.GLISTERING_MELON_SLICE; }

    @Override public int getMaxLevel() { return 10; } // Tối đa nâng 10 lần (40%)
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 16; } // Có thể chỉnh lại mốc level mở khóa tùy ý
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override public boolean isPassiveSkill() { return true; } // Đánh dấu là Nội Tại
    @Override public double getManaCost(int level) { return 0.0; }
    @Override public long getCooldown(int level) { return 0L; }

    @Override
    public List<String> getDescription(int level) {
        int displayLvl = Math.max(1, level);
        // Cấp 1: 4%, Cấp 10: 40%
        double bonusRegen = displayLvl * 4.0;

        return Arrays.asList(
                "§7Cường hóa dòng chảy huyết khí bên trong cơ thể,",
                "§7đẩy nhanh tốc độ tái tạo sinh lực.",
                "",
                "§4[Nội tại: Sinh Lực Dồi Dào]",
                "§f- Tăng vĩnh viễn §a" + bonusRegen + "% Tỉ lệ hồi phục§f.",
                "§f- Chỉ số này cộng dồn trực tiếp vào chỉ số nhân vật"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // Kỹ năng nội tại không kích hoạt khi bấm, trả về false
        return false;
    }
}