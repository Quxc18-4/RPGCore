package vn.minecraft.rPGCore.skills.archer;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class ArrowRainSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random rand = new Random();

    public ArrowRainSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "archer_arrow_rain"; }
    @Override public String getName() { return "Mưa Tên"; }
    @Override public String getRequiredClass() { return "archer"; }
    @Override public Material getIcon() { return Material.TIPPED_ARROW; } // Icon mũi tên đặc biệt
    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }

    // 1. YÊU CẦU CÂY KỸ NĂNG: Nhị Tiễn 10, Tam Tiễn 6
    @Override
    public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> reqs = new HashMap<>();
        reqs.put("archer_double_shot", 10);
        reqs.put("archer_triple_shot", 6);
        return reqs;
    }

    // 2. LOGIC NĂNG LƯỢNG
    @Override
    public double getManaCost(int level) {
        double cost = 90.0;
        if (level >= 3) cost += 10.0;
        if (level >= 6) cost += 10.0;
        if (level >= 10) cost += 20.0;
        return cost;
    }

    // 3. LOGIC HỒI CHIÊU
    @Override
    public long getCooldown(int level) {
        double cd = 16.8;
        if (level >= 3) cd -= 0.9;
        if (level >= 6) cd -= 0.9;
        if (level >= 10) cd -= 1.0;
        return (long) (cd * 1000);
    }

    // --- CÁC HÀM LẤY THÔNG SỐ THEO CẤP ---
    private int getWaves(int level) {
        int waves = 3;
        if (level >= 3) waves++;
        if (level >= 6) waves++;
        if (level >= 10) waves++;
        return waves; // Tối đa 6 đợt
    }

    private int getMinArrows(int level) {
        int min = 18;
        if (level >= 3) min = 22;
        if (level >= 6) min = 28;
        if (level >= 10) min = 36;
        return min; // Tối đa 18~36 mũi
    }

    private int getMaxArrows(int level) {
        return getMinArrows(level) + 10;
    }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        lore.add("§7Triệu hồi cơn mưa mũi tên tại vị trí chỉ định");
        lore.add("§7(Phạm vi tối đa 20 block, bán kính 3 block).");

        // Tính sát thương: Base 15%, mỗi cấp +2%. Thưởng thêm 3% ở lv10 để tròn 36%.
        double dmg = 15 + (displayLevel - 1) * 2;
        if (displayLevel >= 10) dmg += 3;

        lore.add("§7Số đợt thả: §a" + getWaves(displayLevel) + " đợt");
        lore.add("§7Mũi tên/đợt: §a" + getMinArrows(displayLevel) + " - " + getMaxArrows(displayLevel) + " mũi");
        lore.add("§7Sát thương: §c" + (int) dmg + "% §7mỗi mũi");

        if (level < 10) {
            lore.add("");
            lore.add("§8§o(Kỹ năng đột phá ở cấp 3, 6, 10)");
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
        if (currentTime - lastUsed < getCooldown(level)) {
            double timeLeft = (getCooldown(level) - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        // THUẬT TOÁN TÌM TÂM TRIỆU HỒI
        Location centerLoc;
        Block targetBlock = player.getTargetBlockExact(20, org.bukkit.FluidCollisionMode.NEVER);

        if (targetBlock != null) {
            // 1. TĂNG ĐỘ CAO CƠ BẢN LÊN THÊM 2 BLOCK (5.0 -> 7.0)
            centerLoc = targetBlock.getLocation().add(0.5, 7.0, 0.5);
        } else {
            // Nhìn lên trời hoặc quá xa
            centerLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(12.0));
            // TĂNG ĐỘ CAO CƠ BẢN (5.0 -> 7.0)
            centerLoc.add(0, 7.0, 0);
        }

        player.sendMessage("§a§l[!] MƯA TÊN!");

        // THỰC THI CÁC ĐỢT THẢ (WAVES)
        int totalWaves = getWaves(level);
        int minArrows = getMinArrows(level);
        int maxArrows = getMaxArrows(level);

        for (int w = 0; w < totalWaves; w++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    int arrowsThisWave = rand.nextInt(maxArrows - minArrows + 1) + minArrows;

                    for (int i = 0; i < arrowsThisWave; i++) {
                        // Tính toán độ lệch X và Z (Bán kính r=4)
                        double offsetX = (rand.nextDouble() * 8) - 4;
                        double offsetZ = (rand.nextDouble() * 8) - 4;

                        // 2. TẠO ĐỘ LỆCH CHIỀU CAO (RANDOM 0 ĐẾN 2 BLOCK)
                        // Giúp các mũi tên trong cùng 1 đợt không bị rơi xuống cùng 1 lúc
                        double offsetY = rand.nextDouble() * 2.0;

                        // Áp dụng X, Y, Z vào vị trí spawn
                        Location spawnLoc = centerLoc.clone().add(offsetX, offsetY, offsetZ);

                        // Hướng rơi: Thẳng đứng xuống dưới, cộng thêm 1 xíu độ nghiêng
                        Vector dir = new Vector(
                                (rand.nextDouble() - 0.5) * 0.15,
                                -1.0,
                                (rand.nextDouble() - 0.5) * 0.15
                        ).normalize();

                        fireSkillArrow(player, spawnLoc, dir, level);
                    }
                    // Âm thanh mỗi đợt thả
                    player.getWorld().playSound(centerLoc, org.bukkit.Sound.ENTITY_ARROW_SHOOT, 1.5f, 0.5f);
                }
            }.runTaskLater(plugin, w * 12L); // Cách nhau 12 ticks (0.6s) mỗi đợt
        }

        return true;
    }

    private void fireSkillArrow(Player player, Location spawnLoc, Vector velocity, int skillLevel) {
        // Tốc độ 1.5f để mưa rơi có cảm giác rớt từ từ diện rộng
        Arrow arrow = player.getWorld().spawnArrow(spawnLoc, velocity, 1.5f, 0f);
        arrow.setShooter(player);

        double dmgMultiplier = 0.15 + (skillLevel - 1) * 0.02;
        if (skillLevel >= 10) dmgMultiplier += 0.03; // Thưởng cấp 10 để chốt 36%

        double ignoreChance = 0.20; // Xuyên giáp mặc định 20%

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
                arrow.getWorld().spawnParticle(Particle.CRIT, arrow.getLocation(), 1, 0.0, 0.0, 0.0, 0.0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}