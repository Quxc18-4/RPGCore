package vn.minecraft.rPGCore.skills.bard;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.tones.ToneManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class MaestroJourney implements ISkill {

    private final String SKILL_ID = "bard_maestro_journey";
    public static final String META_BONUS_CDR = "BARD_BONUS_CDR";
    public static final String META_BONUS_DURATION = "BARD_BONUS_DURATION";

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
                "§7Cơ chế độc quyền của hệ phái Nhạc Sư.",
                "§7Vừa di chuyển, vừa tấu lên những khúc ca.",
                "",
                "§e[Nội Tại - Lan Tỏa Âm Nhạc]",
                "§f- Mở khóa Tốc độ di chuyển tối đa lên §e600%§f.",
                "§f- Mỗi §b10%§f tốc độ di chuyển vượt mức 200% sẽ chuyển hóa thành:",
                "§f  §8• §a+1% §fGiảm Hồi Chiêu (Tối đa 40%).",
                "§f  §8• §d+2% §fThời gian duy trì Buff (Tối đa 80%).",
                "",
                "§d[Nội Tại - Lưu Trữ Âm Sắc]",
                "§f- Tự động sinh ra ngẫu nhiên 1 Âm mỗi 8s (Tối đa: 2 Âm).",
                "§f- §eÂm Cao: §f+25% Sát thương/Hồi máu/Hệ số Buff.",
                "§f- §bÂm Vang: §f+50% Bán kính/Diện tích tác dụng.",
                "§f- §6Âm Trầm: §f+30% Thời gian duy trì Giai điệu.",
                "§f- §cÂm Sắc: §f+10% Xuyên kháng phép HOẶC +10% Kháng CC.",
                "",
                "§d[Nội Tại - Diễn Tấu]",
                "§f- Kích nổ khi thi triển kỹ năng lúc đã tích ĐẦY Âm.",
                "§f- §aDũng Cảm: §f+10% Sát thương (Duy trì: 8s).",
                "§f- §aCan Trường: §fGiảm 10% Sát thương nhận vào (Duy trì: 6s).",
                "§f- §aSự Sống: §fHồi 5% Máu tối đa & Tạo Khiên 5% (Duy trì: 5s).",
                "§f- §aU Ám: §fLàm chậm 30% & Giảm 10% ST địch gây ra (Duy trì: 5s).",
                "§f- §aBùng Nổ: §fHất văng mọi kẻ địch ra xa 3 Block.",
                "§f- §aDồn Dập: §f+15% Tốc Đánh & +30% Tốc Chạy (Duy trì: 6s).",
                "§f- §aHút Hồn: §fĐồng minh đánh trúng hồi 0.5% Máu/hit (Duy trì: 8s).",
                "§f- §aẢo Ảnh: §fTàng hình & Xóa thù hận quái vật (Duy trì: 2s)."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        String activeKey = "PASSIVE_RUNNING_" + SKILL_ID;
        if (player.hasMetadata(activeKey)) return false;

        player.setMetadata(activeKey, new FixedMetadataValue(RPGCore.getInstance(), true));

        // Tự động sinh Âm ngẫu nhiên
        ToneManager.startPassiveToneGeneration(player, rpgPlayer);

        // Vòng lặp tính toán Tốc độ dư thừa
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || rpgPlayer.getCurrentClass() == null || !rpgPlayer.getCurrentClass().getId().equalsIgnoreCase("bard")) {
                    player.removeMetadata(activeKey, RPGCore.getInstance());
                    this.cancel();
                    return;
                }

                double currentMs = rpgPlayer.getTotalStatsMap().getOrDefault("Tốc độ di chuyển", 0.0);
                double excessSpeed = Math.max(0.0, currentMs - 200.0);

                int stacks = (int) Math.floor(excessSpeed / 10.0);
                double bonusCdr = Math.min(40.0, stacks * 1.0);
                double bonusDuration = Math.min(80.0, stacks * 2.0);

                player.setMetadata(META_BONUS_CDR, new FixedMetadataValue(RPGCore.getInstance(), bonusCdr));
                player.setMetadata(META_BONUS_DURATION, new FixedMetadataValue(RPGCore.getInstance(), bonusDuration));

                if (rpgPlayer.hasSetBonus("BARD_CORE_5", 1)) {
                    player.setMetadata("BARD_OVERTONE_ACTIVE", new FixedMetadataValue(RPGCore.getInstance(), true));
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 20L);

        return true;
    }
}