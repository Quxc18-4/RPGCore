package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RhythmOfWind implements ISkill {

    private static boolean isModifierRegistered = false;

    // ==========================================
    // KHỞI TẠO VÀ ĐĂNG KÝ CHỈ SỐ NỘI TẠI VÀO CORE
    // ==========================================
    public RhythmOfWind() {
        if (!isModifierRegistered) {
            CombatManager.registerStatModifier(new CombatManager.IStatModifier() {
                @Override
                public void applyBonusStats(Player player, Map<String, Double> stats) {
                    RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
                    if (rpgPlayer != null) {
                        // Lấy cấp độ Kỹ năng hiện tại của người chơi
                        int skillLevel = rpgPlayer.getSkillData().getSkillLevel(getId());

                        if (skillLevel > 0) {
                            // Cộng dồn Tốc độ di chuyển (10% mỗi cấp)
                            double currentSpeed = stats.getOrDefault("Tốc độ di chuyển", 0.0);
                            stats.put("Tốc độ di chuyển", currentSpeed + (skillLevel * 10.0));

                            // Cộng dồn Kháng hiệu ứng (2% mỗi cấp)
                            double currentResist = stats.getOrDefault("Kháng hiệu ứng", 0.0);
                            stats.put("Kháng hiệu ứng", currentResist + (skillLevel * 2.0));
                        }
                    }
                }
            });
            isModifierRegistered = true;
        }
    }

    @Override
    public String getId() { return "bard_rhythm_of_wind"; }

    @Override
    public String getName() { return "Nhịp Điệu Của Gió"; }

    @Override
    public String getRequiredClass() { return "bard"; }

    @Override
    public Material getIcon() { return Material.FEATHER; }

    @Override
    public int getMaxLevel() { return 5; }

    @Override
    public int getUpgradeCost(int currentLevel) {
        return 1; // Luôn tốn 1 SP theo chỉ đạo
    }

    @Override
    public int getRequiredPlayerLevel() { return 15; }

    @Override
    public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) { return 0.0; } // Nội tại không tốn Mana

    @Override
    public long getCooldown(int level) { return 0L; } // Nội tại không có hồi chiêu

    @Override
    public boolean isPassiveSkill() { return true; } // Đánh dấu đây là Nội Tại[cite: 7]

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLevel = Math.max(1, level);

        int speedBonus = displayLevel * 10;
        int resistBonus = displayLevel * 2;

        return Arrays.asList(
                "§7Lắng nghe hơi thở của tự nhiên,",
                "§7biến cơ thể trở nên nhẹ nhàng như gió.",
                "",
                "§e[Nội Tại - Nhịp Điệu Của Gió]",
                "§f- Nhận vĩnh viễn §b+" + speedBonus + "% Tốc độ di chuyển§f.",
                "§f- Nhận vĩnh viễn §a+" + resistBonus + "% Kháng hiệu ứng§f khống chế.",
                "",
                "§8(Kỹ năng bị động, tự động áp dụng khi nâng cấp)"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        // Trả về false vì Kỹ năng bị động (Passive) không thể được kích hoạt bằng cách bấm Combo[cite: 8]
        return false;
    }
}