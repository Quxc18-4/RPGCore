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

public class TripleShotSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public TripleShotSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "archer_triple_shot";
    }

    @Override
    public String getName() {
        return "Tam Tiễn"; // Tên kỹ năng theo yêu cầu
    }

    @Override
    public String getRequiredClass() {
        return "archer";
    }

    @Override
    public Material getIcon() {
        return Material.SPECTRAL_ARROW; // Dùng icon mũi tên vàng cho khác biệt với Nhị Tiễn
    }

    @Override
    public int getMaxLevel() {
        return 10;
    }

    @Override
    public double getManaCost(int level) {
        double cost = 45.0; // Base 45
        if (level >= 3) cost += 5.0;
        if (level >= 6) cost += 5.0;
        if (level >= 9) cost += 10.0;
        return cost;
    }

    @Override
    public long getCooldown(int level) {
        double cd = 8.8; // Base 8.8s
        if (level >= 3) cd -= 0.4;
        if (level >= 6) cd -= 0.4;
        if (level >= 9) cd -= 0.4;
        return (long) (cd * 1000);
    }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        lore.add("§7Bắn liên tiếp 3 đợt, mỗi đợt 3 mũi tên."); // 3 đợt x 3 mũi = 9 mũi

        // Tính toán hiển thị: Base 35% dmg, 20% pen
        double dmg = 35 + (displayLevel - 1) * 5;
        double pen = 20 + (displayLevel - 1) * 1;
        if (displayLevel >= 10) pen += 1; // [ĐÃ SỬA]: Thưởng cấp 10 (+1%) để tròn 30%

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

        player.sendMessage("§a§l[!] TAM TIỄN!");

        // THỰC THI 3 ĐỢT BẮN
        shootTripleArrows(player, level); // Đợt 1 ngay lập tức

        new BukkitRunnable() {
            @Override
            public void run() { shootTripleArrows(player, level); }
        }.runTaskLater(plugin, 5L); // Đợt 2 sau 5 ticks

        new BukkitRunnable() {
            @Override
            public void run() { shootTripleArrows(player, level); }
        }.runTaskLater(plugin, 10L); // Đợt 3 sau 10 ticks

        return true;
    }

    private void shootTripleArrows(Player player, int skillLevel) {
        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection();

        Vector right = new Vector(-dir.getZ(), 0, dir.getX());

        if (right.lengthSquared() > 0) {
            right.normalize();
        } else {
            right = new Vector(1, 0, 0);
        }

        double offsetDistance = 0.3;

        Location rightLoc = eyeLoc.clone().add(right.clone().multiply(offsetDistance));
        Location leftLoc = eyeLoc.clone().add(right.clone().multiply(-offsetDistance));
        Location centerLoc = eyeLoc.clone(); // Mũi tên ở chính giữa

        // Bắn 3 mũi tên: Trái, Giữa, Phải
        fireSkillArrow(player, leftLoc, dir.clone(), skillLevel);
        fireSkillArrow(player, centerLoc, dir.clone(), skillLevel);
        fireSkillArrow(player, rightLoc, dir.clone(), skillLevel);

        player.playSound(eyeLoc, org.bukkit.Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.1f);
    }

    private void fireSkillArrow(Player player, Location spawnLoc, Vector velocity, int skillLevel) {
        // Tốc độ 3.5f giống Nhị Tiễn để đảm bảo độ mượt và lực
        Arrow arrow = player.getWorld().spawnArrow(spawnLoc, velocity, 3.5f, 0f);
        arrow.setShooter(player);

        double dmgMultiplier = 0.35 + (skillLevel - 1) * 0.05; // Base 35%, tăng 5%
        double ignoreChance = 0.20 + (skillLevel - 1) * 0.01; // Base 20%, tăng 1%

        if (skillLevel >= 10) ignoreChance += 0.01; // [ĐÃ SỬA]: Thưởng cấp 10 (+1%) để tròn 30%

        // Giới hạn max level 10: Dmg max = 80%, Pen max = 30%
        dmgMultiplier = Math.min(0.80, dmgMultiplier);
        ignoreChance = Math.min(0.30, ignoreChance);

        // Chuyển % sang Metadata (Nhờ cơ chế Nhân tỷ lệ mới ở CombatManager, không dùng random chance nữa)
        arrow.setMetadata("Is_Custom_Skill", new FixedMetadataValue(plugin, true));
        arrow.setMetadata("Skill_DmgMulti", new FixedMetadataValue(plugin, dmgMultiplier));
        // Gán 30.0 thay vì 0.30 để khớp với hệ thống mới
        arrow.setMetadata("Skill_ArmorPen", new FixedMetadataValue(plugin, ignoreChance * 100.0));

        // [TÍCH HỢP XUYÊN THẤU]: Bơm thẳng mác Xuyên thấu vào đạn kỹ năng
        if (player.hasMetadata("Archer_Pierce_Buff")) {
            arrow.setMetadata("Is_Piercing_Arrow", new FixedMetadataValue(plugin, true));
            arrow.setMetadata("Pierce_Hit_Count", new FixedMetadataValue(plugin, 0));
            arrow.setMetadata("Pierced_Entities", new FixedMetadataValue(plugin, new java.util.ArrayList<java.util.UUID>()));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isOnGround()) {
                    arrow.remove();
                    this.cancel();
                    return;
                }
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
        return Collections.singletonMap("archer_double_shot", 8); // Cần Nhị Tiễn lv 8
    }
}