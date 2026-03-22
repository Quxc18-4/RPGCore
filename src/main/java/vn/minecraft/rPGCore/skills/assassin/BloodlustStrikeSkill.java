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
public class BloodlustStrikeSkill extends AbstractEnhancementSkill {

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public BloodlustStrikeSkill(JavaPlugin plugin) {
        super(plugin);
    }

    @Override public String getId() { return "assassin_bloodlust_strike"; }
    @Override public String getName() { return "Đòn Khát Máu"; }
    @Override public String getRequiredClass() { return "assassin"; }
    @Override public Material getIcon() { return Material.REDSTONE; }

    @Override public int getMaxLevel() { return 6; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> req = new HashMap<>();
        req.put("assassin_double_slash", 10);
        req.put("assassin_poison_dart",5);
        return req;
    }

    @Override
    public int getRequiredPlayerLevel() {
        return 18; // Yêu cầu Level 18
    }

    @Override
    public double getManaCost(int level) {
        return 40.0 + ((level - 1) * 5.0);
    }

    @Override
    public long getCooldown(int level) {
        double cd = 25.0 - ((level - 1) * 1.2);
        return (long) (cd * 1000);
    }

    private double getLifestealBonus(int level) {
        return 75.0 + ((level - 1) * 10.0);
    }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        int lifesteal = (int) getLifestealBonus(displayLevel);

        lore.add("§7Tế máu vào vũ khí, cường hóa cực độ");
        lore.add("§7bản năng sinh tồn trong §e6 giây§7 tiếp theo.");
        lore.add("");
        lore.add("§8[Nội tại - Khát máu]:");
        lore.add("§7§nĐòn đánh thường§7 tiếp theo sẽ chắc chắn");
        lore.add("§7hồi phục bằng §a" + lifesteal + "% §7Sát thương gây ra.");
        lore.add("");
        lore.add("§c*Lưu ý: Kích hoạt sẽ ghi đè các Cường hóa khác.");

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực (Stamina)!");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);

        long baseCd = getCooldown(level); // Lấy thời gian gốc
        long finalCd = getFinalCooldown(rpgPlayer, baseCd); // Đưa qua máy ép CDR

        if (currentTime - lastUsed < finalCd) {
            double timeLeft = (finalCd - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        startCooldownNotifier(player, finalCd);

        // ==========================================
        // [REFACTOR]: Sử dụng Abstract Class
        // ==========================================
        String castMsg = "§c§l[!] ĐÒN KHÁT MÁU!";
        String expireMsg = "§8§o(Cơn khát máu đã nguội dần...)";

        applyWeaponEnhancement(player, "Assassin_Bloodlust_Value", getLifestealBonus(level), 6 * 20L, castMsg, expireMsg);

        // Âm thanh và hiệu ứng giữ nguyên
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.8f);
        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.1);

        return true;
    }
}