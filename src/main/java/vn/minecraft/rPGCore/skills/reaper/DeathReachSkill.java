package vn.minecraft.rPGCore.skills.reaper;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DeathReachSkill implements ISkill {

    private final JavaPlugin plugin;

    public DeathReachSkill(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "reaper_deaths_reach"; }
    @Override public String getName() { return "Tử Thần Gọi Tên"; }
    @Override public String getRequiredClass() { return "reaper"; }
    @Override public Material getIcon() { return Material.CRIMSON_ROOTS; }
    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) {
        return 25.0 + (level * 2.0); // Không tốn máu, chỉ tốn Stamina
    }

    @Override
    public long getCooldown(int level) {
        double cd = 12.0 - (level * 0.4); // CD từ 11.6s xuống khoảng 8.0s
        return (long) (Math.max(6.0, cd) * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    public List<String> getDescription(Player player, int level) {
        int l = Math.max(1, level);
        int hitDmg = 120 + (l * 20);
        int dotDmg = 30 + (l * 5);

        return List.of(
                "§7Phóng một Huyết Liềm xuyên thấu về phía trước.",
                "",
                "§c[Trúng Địa Hình] §fTạo ra Huyết Vực 3x3 tồn tại 4s.",
                "§f- Kẻ địch bước vào bị §eLàm chậm 20%§f và chịu",
                "§f  §c" + dotDmg + "% ST§f mỗi giây.",
                "",
                "§4[Trúng Kẻ Địch] §fGây §c" + hitDmg + "% ST§f và chia 2 trường hợp:",
                "§a> Máu bạn trên 50%: §fLướt thẳng đến mục tiêu",
                "§f  và §eTrói chân (Root)§f chúng trong 1.0s.",
                "§c> Máu bạn dưới 50%: §fGiật mạnh dây xích, kéo",
                "§f  kẻ địch về phía bạn (Bị giảm bởi Kháng H.Ứng)."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            long expire = player.getMetadata(cdKey).get(0).asLong();
            if (System.currentTimeMillis() < expire) {
                double timeLeft = (expire - System.currentTimeMillis()) / 1000.0;
                player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
                return false;
            }
        }

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực!");
            return false;
        }

        // Trừ Stamina
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);

        // [CHUẨN HÓA V2]: Áp dụng hệ số giảm Hồi chiêu (CDR) và Notifier Action Bar
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 2.0f);

        // Bắn Huyết Liềm
        shootScythe(player, rpgPlayer, level);

        return true;
    }

    // ==========================================
    // 1. CƠ CHẾ ĐẠN XUYÊN THẤU (RAY MARCHING)
    // ==========================================
    private void shootScythe(Player shooter, RPGPlayer rpgShooter, int level) {
        Location startLoc = shooter.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();

        final double maxDistance = 20.0;
        final double speed = 1.5;

        // [CHUẨN HÓA V2]: Ngăn chặn lỗi chém nhiều lần (Double-hit) vào cùng 1 mục tiêu
        final Set<UUID> hitEntities = new HashSet<>();

        new BukkitRunnable() {
            double currentDist = 0.0;

            @Override
            public void run() {
                if (!shooter.isOnline() || shooter.isDead() || currentDist > maxDistance) {
                    this.cancel();
                    return;
                }

                // Ray Marching
                for (double d = 0; d < speed; d += 0.25) {
                    currentDist += 0.25;
                    Location currentLoc = startLoc.clone().add(direction.clone().multiply(currentDist));

                    // VFX Đạn
                    currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 2, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(org.bukkit.Color.RED, 1.2f));

                    // Check 1: Trúng địa hình -> Tự ngắt và tạo Vùng máu
                    if (currentLoc.getBlock().getType().isSolid()) {
                        createBloodZone(shooter, rpgShooter, currentLoc, level);
                        this.cancel();
                        return;
                    }

                    // Check 2: Trúng Thực thể
                    for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.75, 0.75, 0.75)) {
                        if (e instanceof LivingEntity && !e.equals(shooter) && !hitEntities.contains(e.getUniqueId()) && CombatManager.canTarget(shooter, e)) {
                            hitEntities.add(e.getUniqueId()); // Đánh dấu đã chém
                            handleEntityHit(shooter, rpgShooter, (LivingEntity) e, level);

                            // Trúng phát đầu tiên là ngắt luôn (Liềm cắm vào thịt không đi xuyên)
                            this.cancel();
                            return;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // 2. KẾT QUẢ A: TRÚNG MỤC TIÊU CỤ THỂ
    // ==========================================
    private void handleEntityHit(Player reaper, RPGPlayer rpgReaper, LivingEntity target, int level) {
        reaper.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.8f, 0.5f);
        target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, Material.REDSTONE_BLOCK.createBlockData());

        double hitMulti = 1.2 + (level * 0.2);
        double finalDmg = CombatManager.calculateDamage(reaper, rpgReaper, target, hitMulti, 0.0, false, false);

        // [CHUẨN HÓA V2]: Lệnh cấm lặp sát thương
        reaper.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
        CombatManager.applySkillDamage(reaper, target, finalDmg);
        reaper.removeMetadata("Ghost_L_Blocker", plugin);

        double maxHp = rpgReaper.getTotalMaxHealth();
        double currentHp = reaper.getHealth();
        double hpPercent = (maxHp > 0) ? (currentHp / maxHp) : 0.0;

        if (hpPercent > 0.50) {
            // [Lướt tới]
            Vector dashVec = target.getLocation().toVector().subtract(reaper.getLocation().toVector());
            if (dashVec.length() > 0.5) {
                dashVec.normalize().multiply(1.8).setY(0.4);
                reaper.setVelocity(dashVec);
            }
            reaper.getWorld().playSound(reaper.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
            reaper.sendMessage("§c§l[!] §fTử thần áp sát!");

            // Trói chân 1.0s bằng hệ thống chuẩn
            StatusEffectManager.applyAbsoluteRoot(target, 20);
            target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        } else {
            // [Kéo lại]
            reaper.sendMessage("§4§l[!] §fĐoạt hồn!");
            reaper.getWorld().playSound(reaper.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.0f, 0.8f);

            StatusEffectManager.applyPullWithResistance(target, reaper.getLocation(), 1.5);
        }
    }

    // ==========================================
    // 3. KẾT QUẢ B: TRÚNG ĐỊA HÌNH -> HUYẾT VỰC (PATTERN V2)
    // ==========================================
    private void createBloodZone(Player reaper, RPGPlayer rpgReaper, Location center, int level) {
        center.getWorld().playSound(center, Sound.ENTITY_SLIME_SQUISH, 1.0f, 0.5f);

        double dotMulti = 0.3 + (level * 0.05);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 80;

            @Override
            public void run() {
                if (ticks >= maxTicks || !reaper.isOnline()) {
                    this.cancel();
                    return;
                }

                if (ticks % 5 == 0) {
                    for (double i = 0; i < 2 * Math.PI; i += Math.PI / 8) {
                        double x = 1.5 * Math.cos(i);
                        double z = 1.5 * Math.sin(i);
                        center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0.1, z), 1, 0, 0, 0, 0, new Particle.DustOptions(org.bukkit.Color.MAROON, 1.0f));
                    }
                }

                // Xử lý DoT và Debuff
                if (ticks % 20 == 0) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, 1.5, 2.0, 1.5)) {
                        if (e instanceof LivingEntity && !e.equals(reaper) && CombatManager.canTarget(reaper, e)) {
                            LivingEntity victim = (LivingEntity) e;

                            // Làm chậm theo chuẩn V2 (Tự động tính Stats MoveSpeed)
                            StatusEffectManager.applySlow(victim, 25, 0.20, 1);

                            double finalDot = CombatManager.calculateDamage(reaper, rpgReaper, victim, dotMulti, 0.0, false, false);
                            reaper.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                            CombatManager.applySkillDamage(reaper, victim, finalDot);
                            reaper.removeMetadata("Ghost_L_Blocker", plugin);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}