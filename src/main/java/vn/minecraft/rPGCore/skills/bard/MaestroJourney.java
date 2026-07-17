package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class MaestroJourney implements ISkill {

    private final String SKILL_ID = "bard_maestro_journey";

    @Override public String getId() { return SKILL_ID; }
    @Override public String getName() { return "Hành Trình Nhạc Sư"; }
    @Override public String getRequiredClass() { return "bard"; }
    @Override public Material getIcon() { return Material.MUSIC_DISC_5; }

    @Override public int getMaxLevel() { return 1; }
    @Override public int getUpgradeCost(int currentLevel) { return 0; }
    @Override public double getManaCost(int level) { return 0.0; }
    @Override public long getCooldown(int level) { return 0L; }
    @Override public boolean isPassiveSkill() { return true; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public List<String> getDescription(int level) {
        return Arrays.asList(
                "§e[Nội Tại - Lan Tỏa Âm Nhạc]",
                "§f- Mở khóa Tốc độ di chuyển tối đa lên §e600%§f.",
                "§f- Mỗi §b10%§f tốc độ di chuyển vượt mức 200% sẽ chuyển hóa thành:",
                "§f  §8• §a+1% §fGiảm Hồi Chiêu (Tối đa 40%).",
                "§f  §8• §d+2% §fThời gian duy trì Giai Điệu (Tối đa 80%).",
                "",
                "§d[Nội Tại - Chơi Nhạc - Cường Hóa đòn đánh thường]",
                "§f- §eÂm Cao: §fĐòn đánh thường gây thêm sát thương lan.",
                "§f- §bÂm Vang: §fSố tia đạn đòn đánh thường tăng 1.",
                "§f- §6Âm Trầm: §fĐòn đánh thường làm chậm 15%.",
                "§f- §cÂm Sắc: §f+10% Sát thương đòn đánh thường.",
                "",
                "§d[Nội Tại - Lưu Trữ Âm Sắc - Sinh ra 1 Âm mỗi 12s]",
                "§f- §eÂm Cao: §f+15% Sát thương | Hiệu quả đặc biệt cho Kỹ năng.",
                "§f- §bÂm Vang: §f+50% Phạm vi ảnh hưởng của Giai điệu.",
                "§f- §6Âm Trầm: §f+30% Thời gian duy trì Giai điệu.",
                "§f- §cÂm Sắc: §f+10% Xuyên Kháng Phép cho Kỹ năng.",
                "",
                "§d[Nội Tại - Diễn Tấu Giai Điệu - Kích nổ khi đủ 3 Âm]",
                "§f- §aDũng Cảm: §f+20% Sát thương cho đồng minh (Duy trì: 8s).",
                "§f- §aCan Trường: §fGiảm 12% Sát thương nhận vào (Duy trì: 6s).",
                "§f- §aSự Sống: §fHồi ngay lập tức 8% Máu tối đa cho đồng minh.",
                "§f- §aU Ám: §fLàm chậm 30% & Giảm 10% ST địch gây ra (Duy trì: 5s).",
                "§f- §aBùng Nổ: §fHất văng mọi kẻ địch ra xa 3 Block.",
                "§f- §aDồn Dập: §f+15% Tốc Đánh & +30% Tốc Chạy (Duy trì: 6s).",
                "§f- §aHút Hồn: §fĐồng minh đánh trúng hồi 0.5% Máu/hit (Duy trì: 8s).",
                "§f- §aẢo Ảnh: §fTàng hình & Xóa thù hận quái vật (Duy trì: 2s)."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        return true;
    }
}