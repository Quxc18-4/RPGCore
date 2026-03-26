package vn.minecraft.rPGCore.skills.archer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AerialBombardmentSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public AerialBombardmentSkill(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "archer_aerial_bombardment"; }
    @Override public String getName() { return "Tuyệt kỹ: Không Kích"; }
    @Override public String getRequiredClass() { return "archer"; }
    @Override public Material getIcon() { return Material.FIREWORK_ROCKET; }

    // [ĐÃ SỬA]: Max Lv là 3
    @Override public int getMaxLevel() { return 3; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }

    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }
    @Override public int getRequiredPlayerLevel() { return 40; }

    @Override public double getManaCost(int level) { return 150.0 + (level * 30.0); }
    @Override public long getCooldown(int level) { return 45000L; }

    private double getCastTime(int level) {
        if (level == 3) return 1.5;
        if (level == 2) return 1.8;
        return 2.0; // Level 1
    }

    @Override
    public List<String> getDescription(int level) {
        return Arrays.asList(
                "§6§l[TUYỆT KỸ TỐI THƯỢNG]",
                "§7Bay vút lên không trung, ngưng tụ Mũi Tên Pha Lê",
                "§7và oanh tạc mục tiêu bằng đạn nổ diện rộng.",
                "",
                "§f- Thời gian vận nộ: §e" + getCastTime(level) + "s",
                "§f- Số phát bắn: §a" + level + " phát",
                "§f- Sát thương nổ (R=4): §c250% / phát"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Năng lượng!");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));

        if (currentTime - lastUsed < finalCd) {
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", (finalCd - (currentTime - lastUsed)) / 1000.0) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);
        startCooldownNotifier(player, finalCd);

        player.sendMessage("§6§l[!] TUYỆT KỸ: KHÔNG KÍCH!");
        player.setMetadata("Is_Casting", new FixedMetadataValue(plugin, true));

        // Nhảy lên trời
        player.setVelocity(new Vector(0, 1.2, 0));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.5f, 1.0f);

        int castTicks = (int) (getCastTime(level) * 20);
        int shootDelayTicks = 8; // Khoảng cách giữa các phát bắn (0.4s)
        int totalFlightTicks = castTicks + (level * shootDelayTicks); // [ĐÃ SỬA]: Tính toán bay cho đến phát đạn cuối cùng

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    player.setGravity(true);
                    player.removeMetadata("Is_Casting", plugin);
                    this.cancel();
                    return;
                }

                // Từ tick 10 trở đi: Lơ lửng và vẽ Cung Pha Lê (Elysia Visuals)
                if (ticks == 10) {
                    player.setGravity(false);
                    player.setVelocity(new Vector(0, 0, 0));
                }

                if (ticks >= 10 && ticks <= totalFlightTicks) {
                    player.setVelocity(new Vector(0, 0, 0)); // Khóa cứng vị trí trên không

                    // ==========================================
                    // VISUAL: CUNG PHA LÊ HỒNG & MŨI TÊN CHỮ V (Elysia)
                    // ==========================================
                    Location center = player.getEyeLocation();
                    Vector dir = center.getDirection().normalize();
                    Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
                    Vector up = right.clone().crossProduct(dir).normalize();

                    Particle.DustOptions pinkDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 153, 204), 1.2f);
                    Particle.DustOptions whiteDust = new Particle.DustOptions(org.bukkit.Color.WHITE, 1.2f);

                    // Vẽ 4 cánh cung lan ra xung quanh
                    for (int j = 1; j <= 15; j++) {
                        double d = j * 0.15; // Độ dài 2.25 block
                        double bend = d * 0.3; // Tạo độ cong cánh cung về phía sau

                        Vector p1 = up.clone().add(right).normalize().multiply(d).subtract(dir.clone().multiply(bend));
                        Vector p2 = up.clone().subtract(right).normalize().multiply(d).subtract(dir.clone().multiply(bend));
                        Vector p3 = up.clone().multiply(-1).add(right).normalize().multiply(d).subtract(dir.clone().multiply(bend));
                        Vector p4 = up.clone().multiply(-1).subtract(right).normalize().multiply(d).subtract(dir.clone().multiply(bend));

                        player.getWorld().spawnParticle(Particle.DUST, center.clone().add(p1), 1, pinkDust);
                        player.getWorld().spawnParticle(Particle.DUST, center.clone().add(p2), 1, pinkDust);
                        player.getWorld().spawnParticle(Particle.DUST, center.clone().add(p3), 1, pinkDust);
                        player.getWorld().spawnParticle(Particle.DUST, center.clone().add(p4), 1, pinkDust);
                    }

                    // Vẽ tâm ngắm chữ V bằng ánh sáng trắng
                    Location tip = center.clone().add(dir.clone().multiply(2.0));
                    Location leftWing = center.clone().add(dir.clone().multiply(1.4)).add(right.clone().multiply(0.5));
                    Location rightWing = center.clone().add(dir.clone().multiply(1.4)).add(right.clone().multiply(-0.5));

                    for(double t = 0; t <= 1; t += 0.2) {
                        player.getWorld().spawnParticle(Particle.DUST, tip.clone().add(leftWing.clone().subtract(tip).multiply(t)), 1, whiteDust);
                        player.getWorld().spawnParticle(Particle.DUST, tip.clone().add(rightWing.clone().subtract(tip).multiply(t)), 1, whiteDust);
                    }

                    // Âm thanh Ting Ting Ting lúc đang vận nộ
                    if (ticks < castTicks && ticks % 10 == 0) {
                        float pitch = 1.0f + (ticks / (float)castTicks);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.5f, pitch);
                    }
                }

                // ==========================================
                // BẮN BOM SAU KHI ĐÃ RẶN XONG (Vẫn lơ lửng cho tới khi bắn xong)
                // ==========================================
                if (ticks == castTicks) {
                    for (int i = 0; i < level; i++) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.isOnline() && !player.isDead()) {
                                    fireExplosiveBomb(player);
                                }
                            }
                        }.runTaskLater(plugin, i * shootDelayTicks); // Khoảng cách giữa các phát đạn
                    }
                }

                // ==========================================
                // HẾT QUÁ TRÌNH BẮN -> RỚT XUỐNG ĐẤT
                // ==========================================
                if (ticks >= totalFlightTicks) {
                    player.setGravity(true);
                    player.removeMetadata("Is_Casting", plugin);
                    this.cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void fireExplosiveBomb(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize().multiply(3.0); // Bắn cực mạnh

        Arrow arrow = player.getWorld().spawnArrow(eyeLoc, dir, 3.0f, 0f);
        arrow.setShooter(player);

        arrow.setMetadata("CustomArrowType", new FixedMetadataValue(plugin, "arrow_explosive"));
        arrow.setMetadata("Is_Custom_Skill", new FixedMetadataValue(plugin, true));
        arrow.setMetadata("Skill_DmgMulti", new FixedMetadataValue(plugin, 2.5)); // 250% Dmg
        arrow.setMetadata("Skill_ArmorPen", new FixedMetadataValue(plugin, 50.0)); // 50% Pen

        player.getWorld().playSound(eyeLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f);

        // Hiệu ứng hạt bụi pha lê bay theo mũi tên
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isOnGround()) {
                    this.cancel();
                    return;
                }
                Particle.DustOptions pinkDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 153, 204), 1.5f);
                arrow.getWorld().spawnParticle(Particle.DUST, arrow.getLocation(), 5, 0.1, 0.1, 0.1, pinkDust);
                arrow.getWorld().spawnParticle(Particle.END_ROD, arrow.getLocation(), 2, 0.1, 0.1, 0.1, 0.05);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}