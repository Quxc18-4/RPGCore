package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
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
    @Override public Material getIcon() { return Material.NETHER_STAR; }

    @Override public int getMaxLevel() { return 10; }

    // [ĐÃ SỬA]: Luôn tốn 1 SP mỗi lần nâng cấp
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

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        // Sát thương dao động: Level 1 = 200%, Level 10 = 335%
        int totalDmg = 200 + ((displayLevel - 1) * 15);

        lore.add("§7Pháp sư tập trung tinh thần, nén lượng");
        lore.add("§7ma lực khổng lồ vào đòn tấn công kế tiếp.");
        lore.add("");
        lore.add("§fSát thương: §c" + totalDmg + "%");
        lore.add("§fThời gian duy trì tụ lực: §68.0 Giây");
        lore.add("");
        lore.add("§e[Nội tại - Phản Ứng Thuật Pháp]:");
        lore.add("§8Đòn tấn công này sẽ áp dụng §nẤn Nguyên Tố§8 lên mục tiêu");
        lore.add("§8hiện tại và KÍCH NỔ các Ấn đang tồn tại của mục tiêu.");
        lore.add("");
        lore.add("§e[Bảng Phản Ứng Thuật Pháp]:");
        lore.add("§8• §7Cùng Nguyên Tố: §bBùng Nổ Thuật Pháp");
        lore.add("  §7(Gây 115% Sát Thương)");
        lore.add("§8• §cLửa §f+ §bNước: §cBốc Hơi");
        lore.add("  §7(Gây 150% Sát Thương)");
        lore.add("§8• §eÁnh Sáng §f+ §5Bóng Tối: §dHỗn Độn");
        lore.add("  §7(150% Sát Thương + Phá 20% Giáp/Giáp phép trong 5s)");
        lore.add("§8• §cLửa §f+ §eÁnh Sáng: §eChớp Giật");
        lore.add("  §7(150% Sát Thương + Tê liệt diện rộng)");
        lore.add("§8• §cLửa §f+ §5Bóng Tối: §5Hắc Hỏa");
        lore.add("  §7(Gây DOT theo % Máu đã mất của mục tiêu trong 4s)");
        lore.add("§8• §cLửa §f+ §2Tự Nhiên: §6Thiêu Rụi");
        lore.add("  §7(Đốt cháy kẻ địch trong 7s)");
        lore.add("§8• §bNước §f+ §2Tự Nhiên: §aSinh Sôi");
        lore.add("  §7(Trói buộc diện rộng 3s + Hồi 5% HP nếu kết liễu)");

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

        // Kế thừa hệ thống Buff (Lưu lại Level của chiêu để chốc nữa truyền vào sát thương)
        applyWeaponEnhancement(
                player,
                "Mage_Focus_Level",
                level,
                160L, // 8.0 Giây (160 ticks)
                "§d§l[!] §fVũ khí đã §eTụ Lực§f, sẵn sàng Kích nổ!",
                "§8§o(Ma lực Tụ Lực đã tiêu tán...)"
        );

        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 2.0f);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

        return true;
    }
}