package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class WanderingMinstrel implements ISkill {

    private final String SKILL_ID = "bard_wandering_minstrel";

    @Override public String getId() { return SKILL_ID; }
    @Override public String getName() { return "Âm Vang Bay Bổng"; }
    @Override public String getRequiredClass() { return "bard"; }
    @Override public Material getIcon() { return Material.DIAMOND_BOOTS; }

    @Override public int getMaxLevel() { return 1; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public int getRequiredPlayerLevel() { return 22; }
    @Override public double getManaCost(int level) { return 0.0; }
    @Override public long getCooldown(int level) { return 0L; }
    @Override public boolean isPassiveSkill() { return true; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return Map.of("bard_maestro_journey", 1);
    }

    // Default Lore cho GUI khi chưa có dữ liệu Player
    @Override
    public List<String> getDescription(int level) {
        return getDescription(null, level);
    }

    @Override
    public List<String> getDescription(Player player, int level) {
        int currentDist = 0;
        int unspentCores = 0;

        if (player != null) {
            RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());
            if (rpgPlayer != null) {
                // ĐỌC THẲNG TỪ DATABASE KỸ NĂNG
                currentDist = rpgPlayer.getSkillData().getSkillLevel("BARD_DISTANCE");
                unspentCores = rpgPlayer.getSkillData().getSkillLevel("BARD_UNSPENT_CORES");
            }
        }

        return Arrays.asList(
                "§7Kỹ năng đột phá dành cho Nhạc Sư kỳ cựu.",
                "§7Sự vất vả trên hành trình được đền đáp.",
                "",
                "§e[Nội Tại - Giai Điệu Ngân Vang]",
                "§f- Tích lũy Khoảng cách di chuyển để nhận Nâng cấp.",
                "§f- Có thể chọn tối đa §e9 Nâng Cấp§f.",
                "§f- Phím tắt Mở Bảng Nâng Cấp: §b[F]§f. §8*Khi đang cầm vũ khí",
                "",
                "§a[Tiến Độ Hành Trình]",
                "§f- Quãng đường đã đi: §e" + currentDist + " §fBlocks.",
                "§f- Mốc tăng tiến: §65.000 §f/ §650.000 §f/ §6200.000 §f/ §6500.000§f",
                        "§61.000.000 §f/ §62.800.000 §f/ §64.000.000 §f/ §65.500.000§f.",
                "",
                "§d§l[!] §fĐiểm Nâng Cấp đang có: §d" + unspentCores
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        int unspent = rpgPlayer.getSkillData().getSkillLevel("BARD_UNSPENT_CORES");
        boolean isAdmin = player.hasPermission("rpgcore.admin");

        if (unspent > 0 || isAdmin) {
            player.closeInventory(); // Đóng bảng skill
            vn.minecraft.rPGCore.skills.bard.BardCoreGUI.openSelectionMenu(player, rpgPlayer);
        } else {
            player.closeInventory(); // Phải đóng bảng skill thì người chơi mới đọc được chat
            vn.minecraft.rPGCore.skills.bard.BardCoreGUI.sendPreviewPrompt(player);
        }

        return true;
    }
}