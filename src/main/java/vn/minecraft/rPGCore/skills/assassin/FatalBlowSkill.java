package vn.minecraft.rPGCore.skills.assassin;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.AbstractEnhancementSkill; // Thêm Import này

import java.util.*;

// [REFACTOR]: Kế thừa AbstractEnhancementSkill
public class FatalBlowSkill extends AbstractEnhancementSkill {

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public FatalBlowSkill(JavaPlugin plugin) {
        super(plugin);
    }

    @Override public String getId() { return "assassin_fatal_blow"; }
    @Override public String getName() { return "Đòn Chí Mạng"; }
    @Override public String getRequiredClass() { return "assassin"; }
    @Override public Material getIcon() { return Material.MAGMA_CREAM; }
    @Override public int getMaxLevel() { return 6; }

    @Override
    public double getManaCost(int level) {
        return 30.0 + (level * 5.0);
    }

    @Override
    public long getCooldown(int level) {
        return 12000;
    }

    @Override
    public List<String> getDescription(int level) {
        double critRate = 50.0 + ((level - 1) * 10.0);
        double critDmg = 300.0 + ((level - 1) * 20.0);
        double skillDmg = 200.0 + ((level - 1) * 15.0);

        return Arrays.asList(
                "§7Tụ lực sát khí, cường hóa đòn tấn công tiếp theo",
                "§7trong vòng §e8 giây§7.",
                "",
                "§8[Nếu dùng Đánh Thường]:",
                "§7- Tăng §a" + (int)critRate + "% §7Tỉ lệ Chí mạng.",
                "§7- Tăng §c" + (int)critDmg + "% §7Sát thương Bạo kích.",
                "",
                "§8[Nếu dùng Kỹ Năng]:",
                "§7- Kỹ năng sát thương tiếp theo được cộng",
                "§7  thêm §e" + (int)skillDmg + "% §7Sát thương gốc.",
                "",
                "§c*Lưu ý: Kích hoạt sẽ ghi đè các Cường hóa vũ",
                "§ckhí khác (VD: Đòn Khát Máu)."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;
        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực (Stamina)!");
            return false;
        }

        long now = System.currentTimeMillis();
        long cd = getCooldown(level);
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < cd) {
            double timeLeft = (cd - (now - cooldowns.get(player.getUniqueId()))) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), now);

        // ==========================================
        // [REFACTOR]: Code giờ đã tối giản chỉ còn đúng 1 dòng này!
        // ==========================================
        String castMsg = "§c§l[!] §fVũ khí của bạn tỏa ra sát khí rực lửa...";
        String expireMsg = "§8§o(Sát khí cường hóa đã nguội dần...)";

        applyWeaponEnhancement(player, "Assassin_FatalBlow_Level", level, 8 * 20L, castMsg, expireMsg);

        // Âm thanh và hiệu ứng giữ nguyên
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

        return true;
    }

    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }
}