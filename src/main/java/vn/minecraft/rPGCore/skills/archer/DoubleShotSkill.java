package vn.minecraft.rPGCore.skills.archer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class DoubleShotSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public DoubleShotSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "archer_double_shot";
    }

    @Override
    public String getName() {
        return "Nhị Tiễn";
    }

    @Override
    public String getRequiredClass() {
        return "archer";
    }

    @Override
    public Material getIcon() {
        return Material.ARROW;
    }

    @Override
    public int getMaxLevel() {
        return 10;
    }

    @Override
    public double getManaCost(int level) {
        double cost = 25.0;
        if (level >= 3) cost += 5.0;
        if (level >= 6) cost += 5.0;
        if (level >= 9) cost += 10.0;
        return cost;
    }

    @Override
    public long getCooldown(int level) {
        double cd = 4.2;
        if (level >= 3) cd -= 0.4;
        if (level >= 6) cd -= 0.4;
        if (level >= 9) cd -= 0.4;
        return (long) (cd * 1000);
    }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        lore.add("§7Bắn liên tiếp 2 đợt 2 mũi tên về phía trước.");

        // Tính toán hiển thị cho chuẩn xác
        double dmg = 65 + (displayLevel - 1) * 5;
        double pen = 20 + (displayLevel - 1) * 1;
        if (displayLevel >= 10) pen += 1; // Thưởng cấp 10

        lore.add("§7Sát thương: §c" + (int) dmg + "% §7mỗi mũi");
        lore.add("§7Tỉ lệ xuyên giáp: §e" + (int) pen + "%");

        if (level < 9) {
            lore.add("");
            lore.add("§8§o(Kỹ năng đột phá ở cấp 3, 6, 9)");
        }
        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Mana!");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cd = getCooldown(level);

        if (currentTime - lastUsed < cd) {
            double timeLeft = (cd - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        player.sendMessage("§a§l[!] NHỊ TIỄN!");

        shootParallelArrows(player, level);

        new BukkitRunnable() {
            @Override
            public void run() {
                shootParallelArrows(player, level);
            }
        }.runTaskLater(plugin, 5L);

        return true;
    }

    private void shootParallelArrows(Player player, int skillLevel) {
        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection();

        Vector right = new Vector(-dir.getZ(), 0, dir.getX());

        if (right.lengthSquared() > 0) {
            right.normalize();
        } else {
            right = new Vector(1, 0, 0);
        }

        double offsetDistance = 0.25;

        Location rightLoc = eyeLoc.clone().add(right.clone().multiply(offsetDistance));
        Location leftLoc = eyeLoc.clone().add(right.clone().multiply(-offsetDistance));

        // Truyền thẳng dir (không setDirection thủ công nữa)
        fireSkillArrow(player, rightLoc, dir.clone(), skillLevel);
        fireSkillArrow(player, leftLoc, dir.clone(), skillLevel);

        player.playSound(eyeLoc, org.bukkit.Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.2f);
    }

    private void fireSkillArrow(Player player, Location spawnLoc, Vector velocity, int skillLevel) {
        // GIẢI QUYẾT LỖI VISUAL:
        // Hàm spawnArrow sẽ tự động căn góc Pitch/Yaw chính xác 100% theo velocity trước khi xuất hiện
        Arrow arrow = player.getWorld().spawnArrow(spawnLoc, velocity, 3.5f, 0f);
        arrow.setShooter(player);

        double dmgMultiplier = 0.65 + (skillLevel - 1) * 0.05;
        double ignoreChance = 0.20 + (skillLevel - 1) * 0.01;

        if (skillLevel >= 10) ignoreChance += 0.01;

        dmgMultiplier = Math.min(1.10, dmgMultiplier);
        ignoreChance = Math.min(0.30, ignoreChance);

        arrow.setMetadata("Is_Custom_Skill", new FixedMetadataValue(plugin, true));
        arrow.setMetadata("Skill_DmgMulti", new FixedMetadataValue(plugin, dmgMultiplier));
        arrow.setMetadata("Skill_ArmorPen", new FixedMetadataValue(plugin, ignoreChance));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isOnGround()) {
                    arrow.remove();
                    this.cancel();
                    return;
                }
                // GIẢI QUYẾT LỖI CONSOLE: Đổi sang Particle.CRIT không cần class Data đi kèm
                arrow.getWorld().spawnParticle(Particle.CRIT, arrow.getLocation(), 1, 0.0, 0.0, 0.0, 0.0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public int getUpgradeCost(int currentLevel) {
        return 1; // Mặc định luôn tốn 1 SP
    }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return Collections.emptyMap(); // Không yêu cầu gì
    }
}