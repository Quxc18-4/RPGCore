package vn.minecraft.rPGCore.skills.reaper;

import org.bukkit.Color;
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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BloodSiphonSkill implements ISkill {

    private final JavaPlugin plugin;

    public BloodSiphonSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "reaper_blood_siphon"; }
    @Override public String getName() { return "Đoạt Huyết"; }
    @Override public String getRequiredClass() { return "reaper"; }
    @Override public Material getIcon() { return Material.FERMENTED_SPIDER_EYE; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) {
        return 30.0 + (level * 1.5); // Năng lượng cấu rỉa
    }

    @Override
    public long getCooldown(int level) {
        // [CÂN BẰNG]: Giảm CD để cấu rỉa liên tục (12s -> 7s)
        double cd = 12.0 - (level * 0.5);
        return (long) (Math.max(5.0, cd) * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLvl = Math.max(1, level);

        int dmgMulti = 180 + (displayLvl * 17); // 197% -> 350% ST Vũ Khí

        // Mỗi hạt hồi bao nhiêu % (Tính để hiển thị UI)
        double singleOrbHealPct = 2.0 + (displayLvl * 0.5); // 2.5% -> 7.0% Max HP / hạt

        return List.of(
                "§7Phóng một tia Huyết Hồn vô hình về",
                "§7phía trước (Tầm xa 15 block).",
                "",
                "§e[ Thi Triển - Đoạt Huyết ]",
                "§f- Nếu trúng mục tiêu, lập tức rút gọn sinh",
                "§f  lực của kẻ đó, gây §c" + dmgMulti + "% ST Vũ Khí§f.",
                "",
                "§a[ Phản Dưỡng - Hồi Phục ]",
                "§f- Mục tiêu trúng chiêu sẽ bị xé rách, giải",
                "§f  phóng §c3 Huyết Mạch§f bay ngược về phía bạn.",
                "§f- Khi Huyết Mạch chạm vào cơ thể, mỗi hạt",
                "§f  hồi phục lại §a" + String.format("%.1f", singleOrbHealPct) + "%§f Máu Tối Đa."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            if (System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) {
                double timeLeft = (player.getMetadata(cdKey).get(0).asLong() - System.currentTimeMillis()) / 1000.0;
                player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
                return false;
            }
        }

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực để xuất chiêu!");
            return false;
        }

        // ==========================================
        // 1. TÌM KIẾM MỤC TIÊU (RAYTRACE CHECK BUKKIT WORLD)
        // ==========================================
        if (player.getWorld() == null) return false; // Tránh NullWorldException

        RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                15.0, // Tầm xa
                0.5,  // Hitbox của tia
                e -> e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e) // [CHUẨN V2]: Lọc đồng minh
        );

        if (rayTrace == null || rayTrace.getHitEntity() == null) {
            player.sendMessage("§c§l[!] §fKhông có mục tiêu nào trong tầm ngắm!");
            return false; // Trượt mục tiêu -> Không mất Mana / CD
        }

        LivingEntity victim = (LivingEntity) rayTrace.getHitEntity();

        // ==========================================
        // 2. THANH TOÁN TÀI NGUYÊN & GÂY SÁT THƯƠNG
        // ==========================================
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);

        // [CHUẨN V2]: CDR & Thông báo Actionbar
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        // SFX Hit
        player.getWorld().playSound(victim.getLocation(), Sound.ENTITY_EVOKER_FANGS_ATTACK, 1.0f, 1.5f);
        player.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.5f);

        // Tính Toán Sát Thương
        double baseDmgMulti = 1.80 + (level * 0.17); // Sát thương giảm để spam chiêu
        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, victim, baseDmgMulti, 0.0, false, false);

        // [CHUẨN V2]: Áp dụng Sát thương & Chặn lặp hit
        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));

        // Vì đây là chiêu "Hút máu đặc biệt" bằng hạt, ta cắm cờ Cấm Hút Máu Tự Động của hệ thống (nếu server hỗ trợ)
        // Hoặc đơn giản coi như đòn đánh này ăn penalty kỹ năng 10% pool (không đáng kể), sức mạnh dồn vào 3 hạt bay về.
        CombatManager.applySkillDamage(player, victim, finalDmg);

        player.removeMetadata("Ghost_L_Blocker", plugin);

        // ==========================================
        // 3. KÍCH HOẠT HIỆU ỨNG TIA MÁU
        // ==========================================
        // Toán học Hồi phục: Tính số máu hồi MỖI HẠT dựa trên Max HP
        double maxHp = rpgPlayer.getTotalMaxHealth();
        double healPctPerOrb = 0.02 + (level * 0.005); // 2.5% -> 7.0% Max HP / Hạt
        double healAmountPerOrb = maxHp * healPctPerOrb;

        startBloodStreamsTask(player, rpgPlayer, victim.getLocation().add(0, 1.0, 0), healAmountPerOrb);

        player.sendMessage("§4§l[ĐOẠT HUYẾT] §fRút cạn sinh lực mục tiêu!");
        return true;
    }

    // ==========================================
    // [PATTERN 2] VẼ 3 TIA MÁU XOẮN ỐC THEO CHUẨN V2
    // ==========================================
    private void startBloodStreamsTask(Player player, RPGPlayer rpgPlayer, Location spawnLoc, double healAmountPerOrb) {
        new BukkitRunnable() {
            Location currentLoc = spawnLoc.clone();
            int ticks = 0; // Động cơ thời gian (Spatiotemporal)

            final double speed = 0.8;
            final Particle.DustOptions redDust = new Particle.DustOptions(Color.fromRGB(220, 20, 60), 1.2f);
            final Particle.DustOptions darkRedDust = new Particle.DustOptions(Color.fromRGB(139, 0, 0), 0.8f);

            @Override
            public void run() {
                // Kiểm tra An toàn & Timeout (Tránh kẹt task vĩnh viễn)
                if (!player.isOnline() || player.isDead() || ticks > 100 || !player.getWorld().equals(currentLoc.getWorld())) {
                    this.cancel();
                    return;
                }

                Location targetLoc = player.getLocation().add(0, 1.2, 0); // Đích đến: Ngực Reaper
                Vector dir = targetLoc.toVector().subtract(currentLoc.toVector());
                double dist = dir.length();

                // VA CHẠM THÀNH CÔNG
                if (dist <= 1.0) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_DRINK, 0.6f, 1.8f);
                    player.getWorld().spawnParticle(Particle.HEART, targetLoc, 5, 0.4, 0.4, 0.4, 0);

                    // Bơm Máu trực tiếp x 3 (Vì có 3 hạt)
                    rpgPlayer.heal(healAmountPerOrb * 3);

                    this.cancel();
                    return;
                }

                // Tịnh tiến vị trí trung tâm của 3 hạt
                Vector step = dir.normalize().multiply(Math.min(speed, dist));
                currentLoc.add(step);

                // TOÁN HỌC: Tính mặt phẳng vuông góc với đường bay để làm đĩa xoay
                Vector up = new Vector(0, 1, 0);
                Vector right = step.clone().crossProduct(up).normalize();

                // Chặn lỗi CrossProduct khi bắn thẳng lên trời/xuống đất
                if (right.lengthSquared() < 0.01) {
                    right = step.clone().crossProduct(new Vector(1, 0, 0)).normalize();
                }
                up = right.clone().crossProduct(step).normalize();

                // Bán kính xoắn ốc thu hẹp dần khi bay tới người
                double radius = Math.min(1.2, dist * 0.2);
                double rotationSpeed = ticks * 0.5; // Xoay dựa trên Engine Thời gian Ticks

                // Vẽ 3 hạt máu
                for (int i = 0; i < 3; i++) {
                    double angle = rotationSpeed + (i * 2 * Math.PI / 3);
                    Vector offset = right.clone().multiply(Math.cos(angle) * radius).add(up.clone().multiply(Math.sin(angle) * radius));
                    Location particleLoc = currentLoc.clone().add(offset);

                    player.getWorld().spawnParticle(Particle.DUST, particleLoc, 2, 0.02, 0.02, 0.02, 0, redDust);
                    player.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0.05, 0.05, 0.05, 0, darkRedDust);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}