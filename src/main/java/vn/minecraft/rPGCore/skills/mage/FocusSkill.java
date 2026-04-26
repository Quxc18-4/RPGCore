package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.AbstractEnhancementSkill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FocusSkill extends AbstractEnhancementSkill {

    public FocusSkill(JavaPlugin plugin) {
        super(plugin);
    }

    @Override public String getId() { return "mage_focus"; }
    @Override public String getName() { return "Tụ Lực"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.BEACON; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override
    public double getManaCost(int level) {
        return 30.0 + (level * 2.5); // Tiêu hao khá lớn
    }

    @Override
    public long getCooldown(int level) {
        double cd = 8.0 - (level * 0.2); // Hồi chiêu từ 8.0s -> 6.2s
        return (long) (Math.max(4.0, cd) * 1000);
    }

    // Hàm mặc định (Phòng hờ lỗi nếu lấy Lore mà không truyền Player)
    @Override
    public List<String> getDescription(int level) {
        return getDescription(null, level);
    }

    // ==========================================
    // LORE ĐÃ ĐƯỢC CHUẨN HÓA THEO STYLE CỦA SERVER
    // ==========================================
    @Override
    public List<String> getDescription(Player player, int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        // Công thức Scale tổng (VD: Lv1 = 200%, Lv10 = 335%)
        int totalDmg = 200 + ((displayLevel - 1) * 15);

        MageElement element = MageElement.FIRE; // Mặc định
        if (player != null) {
            element = ElementalSwapSkill.getActiveElement(player);
        }

        lore.add("§7Nén ma lực khổng lồ vào đòn tấn công");
        lore.add("§7kế tiếp, thay đổi hình thái đòn đánh");
        lore.add("§7dựa trên Nguyên tố đang sử dụng.");
        lore.add("");
        lore.add("§8▪ §fDuy trì Tụ lực: §68.0s");
        lore.add("§8▪ §fNguyên tố hiện tại: " + element.getDisplayName());
        lore.add("");

        // HIỂN THỊ CHI TIẾT DỰA VÀO NGUYÊN TỐ ĐANG CẦM TRÊN TAY
        switch (element) {
            case FIRE:
                int minFire = (int)(totalDmg * 0.30);
                lore.add("§c[Thi Triển: Đại Hỏa Cầu]");
                lore.add("§f- Bắn quả cầu lửa khổng lồ, nổ chia đều sát");
                lore.add("§f  thương cho tối đa 10 mục tiêu.");
                lore.add("§f- Sát thương: §c" + minFire + "% §f- §c" + totalDmg + "% ST Vũ khí§f.");
                lore.add("§f- Hiệu ứng: §6Thiêu Đốt§f 5s (100% tỷ lệ).");
                break;
            case WATER:
                int waterOut = (int) Math.round(totalDmg * 0.60);
                int waterReturn = (int) Math.round(totalDmg * 0.30);
                lore.add("§b[Thi Triển: Boomerang Nước]");
                lore.add("§f- Phóng đao nước bay đi và quay về, xuyên");
                lore.add("§f  thấu tối đa 10 mục tiêu.");
                lore.add("§f- Lượt đi: §c" + waterOut + "% ST Vũ khí§f.");
                lore.add("§f- Lượt về: §c" + waterReturn + "% ST Vũ khí §f(Gây §c" + waterOut + "%");
                lore.add("§f  nếu là mục tiêu trúng lần đầu).");
                break;
            case WIND:
                // [ĐÃ SỬA]: Khớp với FocusWind.java (25% mỗi giây)
                int maxWind = (int) Math.round(totalDmg * 0.25);
                int minWind = (int) Math.round(totalDmg * 0.10);
                lore.add("§a[Thi Triển: Mắt Bão]");
                lore.add("§f- Tạo tâm bão hút quái (4m) duy trì 4s.");
                lore.add("§f- Sát thương: §c" + maxWind + "%/s§f, giảm 2% mỗi quái");
                lore.add("§f  (Tối thiểu §c" + minWind + "%§f).");
                lore.add("§f- §cQuá tải: §fPhát nổ hất văng nếu > 10 quái.");
                break;
            case NATURE:
                int minNature = (int)(totalDmg * 0.30);
                lore.add("§2[Thi Triển: Bom Hạt]");
                lore.add("§f- Ném hạt giống nổ tung, §aTrói Chân 2s§f");
                lore.add("§f  và chia đều sát thương tối đa 10 kẻ địch.");
                lore.add("§f- Sát thương: §c" + minNature + "% §f- §c" + totalDmg + "% ST Vũ khí§f.");
                lore.add("§f- Kẻ địch văng ra ngoài chỉ bị làm chậm.");
                break;
            case DARK:
                int darkFirst = (int)(totalDmg * 0.90);
                int darkSub = (int)(totalDmg * 0.30);
                int darkIso = (int)(totalDmg * 1.30);
                lore.add("§5[Thi Triển: Cầu Hắc Ám]");
                lore.add("§f- Đạn hắc ám bay xuyên thấu vô hạn.");
                lore.add("§f- Mục tiêu đầu: §c" + darkFirst + "% ST§f (Tăng lên");
                lore.add("§f  §c" + darkIso + "%§f nếu đứng cô lập).");
                lore.add("§f- Kẻ địch phía sau: §c" + darkSub + "% ST§f, bị §8Mù Lòa§f");
                lore.add("§f  và xóa bỏ thù hận (Aggro).");
                break;
            case LIGHT:
                int lightTick = (int)(totalDmg * 0.20);
                lore.add("§e[Thi Triển: Tiểu Tinh Linh]");
                lore.add("§f- Triệu hồi tinh linh lơ lửng trong 4s.");
                lore.add("§f- Tự động ngắm bắn laser vào tối đa");
                lore.add("§f  3 mục tiêu cùng một lúc.");
                lore.add("§f- Sát thương: §c" + lightTick + "% ST Vũ khí §f(Mỗi tia).");
                break;
        }

        lore.add("");
        lore.add("§8§o* Tụ lực sẽ tự động kích nổ Phản ứng");
        lore.add("§8§o  Nguyên tố đang gán trên mục tiêu.");

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        String cdKey = "CD_" + getId();
        // SỬA TẠI ĐÂY: Đảm bảo metadata list không rỗng
        if (player.hasMetadata(cdKey)) {
            java.util.List<org.bukkit.metadata.MetadataValue> meta = player.getMetadata(cdKey);
            if (!meta.isEmpty() && System.currentTimeMillis() < meta.get(0).asLong()) {
                return false;
            }
        }

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Năng lượng!");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);

        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new org.bukkit.metadata.FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        // Đánh dấu người chơi đã Bật Tụ Lực (160 ticks = 8 giây)
        applyWeaponEnhancement(
                player,
                "Mage_Focus_Level",
                level,
                160L,
                "§d§l[!] §fVũ khí đã §eTụ Lực§f, sẵn sàng Kích nổ!",
                "§8§o(Ma lực Tụ Lực đã tiêu tán...)"
        );

        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 2.0f);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

        return true;
    }
}