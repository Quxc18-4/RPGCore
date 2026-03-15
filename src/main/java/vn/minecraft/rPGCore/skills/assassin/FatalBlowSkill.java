package vn.minecraft.rPGCore.skills.assassin;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class FatalBlowSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public FatalBlowSkill(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "assassin_fatal_blow"; }
    @Override public String getName() { return "Đòn Chí Mạng"; }
    @Override public String getRequiredClass() { return "assassin"; }
    @Override public Material getIcon() { return Material.MAGMA_CREAM; } // Icon rực lửa
    @Override public int getMaxLevel() { return 6; } // Tối đa 6 cấp

    @Override
    public double getManaCost(int level) {
        return 30.0 + (level * 5.0); // 35/40/45/50/55/60
    }

    @Override
    public long getCooldown(int level) {
        return 12000; // Cố định 12 giây
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
                "§7  thêm §e" + (int)skillDmg + "% §7Sát thương gốc."
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

        // GẮN BUFF (Ghi đè nếu đã tồn tại)
        player.setMetadata("Assassin_FatalBlow_Level", new FixedMetadataValue(plugin, level));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        player.sendMessage("§c§l[!] §fVũ khí của bạn tỏa ra sát khí rực lửa...");

        // HẸN GIỜ XÓA BUFF NẾU KHÔNG SỬ DỤNG
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.hasMetadata("Assassin_FatalBlow_Level")) {
                    player.removeMetadata("Assassin_FatalBlow_Level", plugin);
                    player.sendMessage("§8§o(Sát khí cường hóa đã nguội dần...)");
                }
            }
        }.runTaskLater(plugin, 8 * 20L); // 8 giây (160 ticks)

        return true;
    }

    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }
}