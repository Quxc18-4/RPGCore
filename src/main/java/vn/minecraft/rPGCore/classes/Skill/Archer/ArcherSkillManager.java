package vn.minecraft.rPGCore.classes.Skill.Archer;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArcherSkillManager {

    private final JavaPlugin plugin;
    private final PlayerManager playerManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>(); // Lưu Cooldown kỹ năng

    public ArcherSkillManager(JavaPlugin plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
    }

    public boolean executeSkill(Player player, RPGPlayer rpgPlayer, String skillId) {
        if (!skillId.equals("archer_spread")) return false; // Hiện tại chỉ test kỹ năng này
        if (!rpgPlayer.getCurrentClass().getId().equalsIgnoreCase("archer")) return false;

        int skillLevel = rpgPlayer.getSkillLevel("archer_spread");
        if (skillLevel <= 0) return false; // Chưa học

        // 1. KIỂM TRA MANA (Giả sử tiêu tốn 30 Mana)
        double manaCost = 30.0;
        if (rpgPlayer.getCurrentResource() < manaCost) {
            player.sendMessage("§cKhông đủ Mana!");
            return false;
        }

        // 2. KIỂM TRA COOLDOWN (Giả sử CD là 5 giây = 5000ms)
        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime - lastUsed < 5000) {
            double timeLeft = (5000 - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        // 3. THỰC THI (Trừ Mana & Set CD)
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - manaCost);
        cooldowns.put(player.getUniqueId(), currentTime);

        player.sendMessage("§a§l[!] MƯA TÊN KÉP!");

        // Lượt bắn 1
        shootSpreadArrows(player, skillLevel);

        // Lượt bắn 2 (Sau 5 ticks = 0.25s)
        new BukkitRunnable() {
            @Override
            public void run() {
                shootSpreadArrows(player, skillLevel);
            }
        }.runTaskLater(plugin, 5L);

        return true;
    }

    private void shootSpreadArrows(Player player, int skillLevel) {
        Location loc = player.getEyeLocation();
        Vector dir = loc.getDirection();

        // Bắn 2 mũi tên lệch nhau góc < 30 độ (VD: +10 độ và -10 độ)
        Vector dirLeft = rotateY(dir.clone(), 10);
        Vector dirRight = rotateY(dir.clone(), -10);

        fireSkillArrow(player, dirLeft, skillLevel);
        fireSkillArrow(player, dirRight, skillLevel);

        player.playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.2f);
    }

    private void fireSkillArrow(Player player, Vector velocity, int skillLevel) {
        Arrow arrow = player.getWorld().spawn(player.getEyeLocation(), Arrow.class);
        arrow.setShooter(player);
        arrow.setVelocity(velocity.multiply(1.8)); // Bắn cực mạnh

        // Gắn Metadata chứa thông tin Level Kỹ năng để Event tính Dmg nhận diện
        arrow.setMetadata("Skill_SpreadArrow", new FixedMetadataValue(plugin, skillLevel));

        // Vẽ Particle bay theo đuôi mũi tên
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isOnGround()) {
                    arrow.remove(); // Tự biến mất khi cắm đất
                    this.cancel();
                    return;
                }
                // Hạt Particle màu xanh lấp lánh
                arrow.getWorld().spawnParticle(Particle.CRIT, arrow.getLocation(), 2, 0, 0, 0, 0);
            }
        }.runTaskTimer(plugin, 0L, 1L); // Chạy mỗi tick
    }

    // Thuật toán xoay Vector hướng nhìn (Tạo góc tù)
    private Vector rotateY(Vector vector, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z);
    }
}