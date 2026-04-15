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
    // [MỚI]: HÀM LORE ĐỘNG - THAY ĐỔI THEO NGUYÊN TỐ ĐANG CHỌN
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

        lore.add("§7Pháp sư nén lượng ma lực khổng lồ");
        lore.add("§7vào đòn tấn công kế tiếp, tạo ra hiệu");
        lore.add("§7ứng đặc biệt tùy theo Nguyên tố.");
        lore.add("");
        lore.add("§fThời gian duy trì tụ lực: §68.0 Giây");
        lore.add("§fTrạng thái hiện tại: " + element.getDisplayName());
        lore.add("");
        lore.add("§e[Hiệu ứng Tụ Lực - " + element.getDisplayName() + "§e]:");

        // HIỂN THỊ CHI TIẾT DỰA VÀO NGUYÊN TỐ ĐANG CẦM TRÊN TAY
        switch (element) {
            case FIRE:
                int minFire = (int)(totalDmg * 0.30);
                lore.add("§c🔥 Hỏa Cầu Nổ Lan:");
                lore.add("§7Bắn quả cầu lửa khổng lồ, phát nổ");
                lore.add("§7chia đều sát thương cho kẻ địch (Tối");
                lore.add("§7thiểu §c" + minFire + "%§7, Tối đa §c" + totalDmg + "%§7).");
                break;
            case WATER:
                int waterDmg = (int)(totalDmg * 0.33);
                lore.add("§b💧 Thủy Đao Boomerang:");
                lore.add("§7Phóng lưỡi đao nước bay đi và quay");
                lore.add("§7trở lại, xuyên thấu tối đa 3 mục tiêu.");
                lore.add("§7Mỗi lần chạm gây §c" + waterDmg + "% §7Sát thương.");
                break;
            case WIND:
                int minWind = (int)(totalDmg * 0.30);
                lore.add("§a🌪 Mắt Bão Cào Xé:");
                lore.add("§7Tạo tâm bão hút quái liên tục trong 4s.");
                lore.add("§7Chia đều sát thương mỗi giây (Tối");
                lore.add("§7thiểu §c" + (minWind/4) + "%§7, Tối đa §c" + (totalDmg/4) + "%§7/s).");
                break;
            case NATURE:
                int minNature = (int)(totalDmg * 0.30);
                lore.add("§2🌿 Bom Hạt Trói Chân:");
                lore.add("§7Ném hạt giống nổ tung, trói chân 2s");
                lore.add("§7và chia đều sát thương (Tối thiểu");
                lore.add("§c" + minNature + "%§7, Tối đa §c" + totalDmg + "%§7).");
                break;
            case DARK:
                lore.add("§5🌌 Cầu Hắc Ám:");
                lore.add("§7Xuyên thấu mọi mục tiêu. Kẻ đầu tiên");
                lore.add("§7nhận §c" + totalDmg + "% §7sát thương. Các mục");
                lore.add("§7tiêu phía sau bị Mù Lòa và Xóa Aggro.");
                break;
            case LIGHT:
                int lightTick = (int)(totalDmg * 0.20);
                lore.add("§e✨ Ụ Sáng Thanh Tẩy:");
                lore.add("§7Triệu hồi tinh cầu lơ lửng, liên tục bắn");
                lore.add("§7laser xuống kẻ địch gần nhất. Gây");
                lore.add("§c" + lightTick + "% §7Sát thương/tia.");
                break;
        }

        lore.add("");
        lore.add("§8➥ Đòn Tụ lực sẽ Kích Nổ Phản Ứng");
        lore.add("§8  Nguyên Tố đang có trên mục tiêu.");

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            if (System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) return false;
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