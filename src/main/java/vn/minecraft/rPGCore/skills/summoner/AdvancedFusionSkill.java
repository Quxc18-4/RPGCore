package vn.minecraft.rPGCore.skills.summoner;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancedFusionSkill implements ISkill {

    @Override
    public String getId() { return "summoner_advanced_fusion"; }

    @Override
    public String getName() { return "§6§lĐột biến: Dung Hợp Lai Tạo"; }

    @Override
    public String getRequiredClass() { return "summoner"; }

    @Override
    public Material getIcon() { return Material.ENDER_PEARL; } // Biểu tượng hợp lý cho lai tạo

    @Override
    public int getMaxLevel() { return 1; } // Cấp tối đa là 1 (Mở khóa tính năng)

    @Override
    public int getUpgradeCost(int currentLevel) { return 2; } // Tốn 2 điểm kỹ năng như bạn yêu cầu

    @Override
    public int getRequiredPlayerLevel() { return 24; } // Yêu cầu cấp 24

    // [KHAI BÁO]: Đây là kỹ năng Nội Tại!
    @Override public boolean isPassiveSkill() { return true; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> req = new HashMap<>();
        req.put("summoner_fusion", 3); // Yêu cầu kỹ năng dung hợp đạt cấp 3
        return req;
    }

    @Override
    public double getManaCost(int level) { return 0; } // Nội tại không tốn mana

    @Override
    public long getCooldown(int level) { return 0; }

    @Override
    public List<String> getDescription(int level) {
        return Arrays.asList(
                "§7Đạt đến đỉnh cao của nghệ thuật Triệu hồi.",
                "§7Phá vỡ giới hạn tự nhiên, cho phép bạn",
                "§7dung hợp các tinh linh khác loài với nhau.",
                "",
                "§e[Nội Tại Đột Phá]",
                "§fMở khóa các công thức §6§lDung Hợp Lai§f bí mật,",
                "§fsử dụng kỹ năng dung hợp để thử kết hợp thú triệu hồi."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        player.sendMessage("§cĐây là Kỹ năng Nội tại (Passive), nó tự động hoạt động khi bạn sử dụng kỹ năng Dung Hợp chính!");
        return false;
    }
}