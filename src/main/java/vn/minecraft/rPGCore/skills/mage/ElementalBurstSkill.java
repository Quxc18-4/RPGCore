package vn.minecraft.rPGCore.skills.mage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class ElementalBurstSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public ElementalBurstSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "mage_elemental_burst"; }
    @Override public String getName() { return "Bạo Phát Nguyên Tố"; }
    @Override public String getRequiredClass() { return "mage"; }
    @Override public Material getIcon() { return Material.BLAZE_POWDER; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 15; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override
    public double getManaCost(int level) {
        // Tốn nhiều Mana hơn Sphere (Lv10 = 130 Mana)
        return 80.0 + (level * 5.0);
    }

    @Override
    public long getCooldown(int level) {
        // Hồi chiêu: 14s ở Lv1 -> 11s ở Lv10
        double cd = 14.0 - (level * 0.3);
        return (long) (cd * 1000);
    }

    // Sát thương chuẩn (Lv1: 240%, Lv10: 330%)
    private double getBaseDamageMulti(int level) {
        return 2.4 + ((level - 1) * 0.1);
    }

    @Override
    public List<String> getDescription(int level) {
        return getDescription(null, level);
    }

    @Override
    public List<String> getDescription(Player player, int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);
        MageElement element = player != null ? ElementalSwapSkill.getActiveElement(player) : MageElement.FIRE;

        int baseDmgPct = (int) (getBaseDamageMulti(displayLevel) * 100);

        lore.add("§7Giải phóng toàn bộ ma lực dư thừa, tạo");
        lore.add("§7ra một vụ Bạo phát xung quanh bản thân.");
        lore.add("§7Kỹ năng tối ưu để phá vỡ vòng vây.");
        lore.add("");

        switch (element) {
            case FIRE:
                lore.add("§c[Thi Triển: Liệt Hỏa Trùng m]");
                lore.add("§f- Triệu hồi 9 cột lửa phun trào hình quạt.");
                lore.add("§f- Cột đầu trúng mục tiêu gây §c" + baseDmgPct + "% Sát Thương§f,");
                lore.add("§f  các cột sau chạm cùng mục tiêu chỉ gây §c15% Sát Thương§f.");
                lore.add("§f- Hất văng và có 50% tỷ lệ §6Thiêu Đốt§f.");
                break;
            case WATER:
                lore.add("§b[Thi Triển: Sóng Dữ]");
                lore.add("§f- Đẩy một bức tường nước dài 4m về phía trước.");
                lore.add("§f- Cuốn trôi, đẩy lùi mọi kẻ địch và làm chậm 30%.");
                lore.add("§f- Gây §c" + (int)(baseDmgPct * 1.2) + "% Sát Thương§f.");
                break;
            case WIND:
                // Tính toán ép kiểu Int và làm tròn để hiện lên Lore thật đẹp
                int speedBonus = (int) Math.round(15.0 + ((displayLevel - 1) * (20.0 / 9.0)));
                lore.add("§a[Thi Triển: Phong Nhẫn Trảm]");
                lore.add("§f- Bắn 8 lưỡi đao gió xuyên thấu 360 độ.");
                lore.add("§f- Gây §c" + baseDmgPct + "% ST§f cho mục tiêu.");
                lore.add("§f- Tăng §a" + speedBonus + "% Tốc độ di chuyển§f trong 3s.");
                break;
            case NATURE:
                lore.add("§2[Thi Triển: Bão Lá Cuồng Si]");
                lore.add("§f- Tạo ra cơn bão lá di chuyển thẳng về phía trước.");
                lore.add("§f- Khóa khả năng bật nhảy của kẻ địch bên trong.");
                lore.add("§f- Cú nổ đầu gây §c" + (int)(baseDmgPct * 0.8) + "% Sát Thương§f, sau đó");
                lore.add("§f  gây §c" + (int)(baseDmgPct * 0.2) + "% Sát Thương§f liên tục mỗi giây.");
                break;
            case LIGHT:
                lore.add("§e[Thi Triển: Quang Phổ Chói Lòa]");
                lore.add("§f- Bùng nổ ánh sáng diện rộng tại bản thân.");
                lore.add("§f- Gây §c" + (int)(baseDmgPct * 0.7) + "% Sát Thương§f.");
                lore.add("§f- Làm mù và choáng hoàn toàn kẻ địch trong §a" + String.format("%.1f", 1.5 + (displayLevel * 0.1)) + "s§f.");
                break;
            case DARK:
                lore.add("§5[Thi Triển: Khe Nứt Diệt Vong]");
                lore.add("§f- Mở khe nứt quét tia laser hình quạt về phía trước.");
                lore.add("§f- Gây §c" + (int)(baseDmgPct * 1.2) + "% Sát Thương§f, kèm hiệu ứng §8Mù Lòa§f");
                lore.add("§f  và §cPhân Rã §f trong 4s.");
                break;
        }
        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;
        if (!handleCostAndCooldown(player, rpgPlayer, level)) return false;
        applyGlobalCooldown(player, rpgPlayer, level);

        MageElement element = ElementalSwapSkill.getActiveElement(player);
        double baseMulti = getBaseDamageMulti(level);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1f);

        switch (element) {
            case FIRE: triggerFireBurst(player, rpgPlayer, baseMulti); break;
            case WATER: triggerWaterBurst(player, rpgPlayer, baseMulti); break;
            case WIND: triggerWindBurst(player, rpgPlayer, level, baseMulti); break;
            case NATURE: triggerNatureBurst(player, rpgPlayer, baseMulti); break;
            case LIGHT: triggerLightBurst(player, rpgPlayer, level, baseMulti); break;
            case DARK: triggerDarkBurst(player, rpgPlayer, baseMulti); break;
        }

        return true;
    }

    // ==========================================
    // 1. LỬA: LIỆT HỎA TRÙNG ÂM (3 ROW x 3 COL)
    // ==========================================
    private void triggerFireBurst(Player player, RPGPlayer rpgPlayer, double baseMulti) {
        Location startLoc = player.getLocation();
        Vector forward = startLoc.getDirection().setY(0).normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        // HashSet lưu quái đã dính đòn để áp dụng 15% Falloff Damage
        Set<UUID> hitTargets = new HashSet<>();

        new BukkitRunnable() {
            int row = 0;
            @Override
            public void run() {
                if (row >= 3 || !player.isOnline()) { this.cancel(); return; }

                double distance = 2.0 + (row * 2.0); // Cột nổ cách xa 2, 4, 6 blocks
                double spread = 1.5 + (row * 1.0);   // Tỏa ra hai bên rộng dần hình quạt

                // 3 Cột (Trái, Giữa, Phải)
                List<Location> pillarLocs = Arrays.asList(
                        startLoc.clone().add(forward.clone().multiply(distance)).add(right.clone().multiply(-spread)),
                        startLoc.clone().add(forward.clone().multiply(distance)),
                        startLoc.clone().add(forward.clone().multiply(distance)).add(right.clone().multiply(spread))
                );

                for (Location loc : pillarLocs) {
                    loc.getWorld().spawnParticle(Particle.FLAME, loc, 40, 0.5, 2.0, 0.5, 0.1, null, true);
                    loc.getWorld().spawnParticle(Particle.LAVA, loc, 10, 0.5, 2.0, 0.5, 0.0, null, true);
                    loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);

                    for (org.bukkit.entity.Entity ent : loc.getWorld().getNearbyEntities(loc, 1.5, 3.0, 1.5)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            LivingEntity target = (LivingEntity) ent;

                            double multi = hitTargets.contains(target.getUniqueId()) ? (baseMulti * 0.15) : baseMulti;
                            ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.FIRE);

                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, multi * reaction.damageMultiplier, 0, true, false);
                            CombatManager.applySkillDamage(player, target, finalDmg);

                            // Hất văng thẳng lên
                            target.setVelocity(new Vector(0, 0.8, 0));
                            if (random.nextBoolean()) target.setFireTicks(100); // 50% Thiêu đốt

                            if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
                            hitTargets.add(target.getUniqueId());
                        }
                    }
                }
                row++;
            }
        }.runTaskTimer(plugin, 0L, 4L); // Cách nhau 4 ticks mỗi hàng nổ
    }

    // ==========================================
    // 2. NƯỚC: SÓNG THẦN XÔ NGÃ
    // ==========================================
    private void triggerWaterBurst(Player player, RPGPlayer rpgPlayer, double baseMulti) {
        Location startLoc = player.getLocation();
        Vector forward = startLoc.getDirection().setY(0).normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        Set<UUID> hitTargets = new HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 15 || !player.isOnline()) { this.cancel(); return; } // Tiến 15 block

                Location center = startLoc.clone().add(forward.clone().multiply(ticks * 0.8)); // Tốc độ sóng 0.8
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_SPLASH, 0.5f, 0.8f);

                // Vẽ sóng ngang 4 block
                for (double i = -2.0; i <= 2.0; i += 0.5) {
                    Location pLoc = center.clone().add(right.clone().multiply(i));
                    pLoc.getWorld().spawnParticle(Particle.SPLASH, pLoc, 5, 0.2, 0.5, 0.2, 0.1, null, true);
                    pLoc.getWorld().spawnParticle(Particle.BUBBLE_POP, pLoc, 2, 0.2, 0.5, 0.2, 0.0, null, true);
                }

                // Hitbox sóng ngang
                for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 2.5, 1.5, 2.5)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                        LivingEntity target = (LivingEntity) ent;

                        // Đẩy lùi mạnh theo hướng sóng (Chạy mỗi tick để liên tục xô ngã)
                        target.setVelocity(forward.clone().multiply(0.8).setY(0.3));

                        if (!hitTargets.contains(target.getUniqueId())) {
                            ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.WATER);
                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, baseMulti * 1.2 * reaction.damageMultiplier, 0, true, false);
                            CombatManager.applySkillDamage(player, target, finalDmg);

                            // Làm chậm 30% trong 3s
                            StatusEffectManager.applyEffect(target, PotionEffectType.SLOWNESS, 60, 1);
                            if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);

                            hitTargets.add(target.getUniqueId());
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // 3. GIÓ: PHONG NHẪN TRẢM (NOVA)
    // ==========================================
    private void triggerWindBurst(Player player, RPGPlayer rpgPlayer, int level, double baseMulti) {
        Location center = player.getLocation().add(0, 1, 0);
        player.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.5f);

        // [ÉP CHỈ SỐ]: Cấp 1 = 15.0, Cấp 10 = 35.0
        double speedValue = 15.0 + ((level - 1) * (20.0 / 9.0));

        // Buff Speed nội tại
        player.setMetadata("Wind_Burst_Speed", new FixedMetadataValue(plugin, speedValue));
        new BukkitRunnable() {
            @Override
            public void run() { player.removeMetadata("Wind_Burst_Speed", plugin); }
        }.runTaskLater(plugin, 60L); // Tồn tại 3s

        Set<UUID> hitTargets = new HashSet<>();
        List<Vector> blades = new ArrayList<>();

        // Tạo 8 hướng đao gió
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            blades.add(new Vector(Math.cos(angle), 0, Math.sin(angle)).normalize().multiply(1.5));
        }

        new BukkitRunnable() {
            int ticks = 0;
            List<Location> currentLocs = new ArrayList<>();

            @Override
            public void run() {
                if (ticks == 0) {
                    for (int i = 0; i < 8; i++) currentLocs.add(center.clone());
                }

                if (ticks >= 10 || !player.isOnline()) { this.cancel(); return; } // Tầm xa ~15 block

                for (int i = 0; i < 8; i++) {
                    Location loc = currentLocs.get(i);
                    loc.add(blades.get(i));

                    loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0.0, null, true);
                    loc.getWorld().spawnParticle(Particle.CLOUD, loc, 2, 0.1, 0.1, 0.1, 0.05, null, true);

                    for (org.bukkit.entity.Entity ent : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            LivingEntity target = (LivingEntity) ent;
                            if (!hitTargets.contains(target.getUniqueId())) {
                                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.WIND);
                                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, baseMulti * reaction.damageMultiplier, 0, true, false);
                                CombatManager.applySkillDamage(player, target, finalDmg);

                                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
                                hitTargets.add(target.getUniqueId());
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // 4. THIÊN NHIÊN: BÃO LÁ CUỒNG SI
    // ==========================================
    private void triggerNatureBurst(Player player, RPGPlayer rpgPlayer, double baseMulti) {
        Location startLoc = player.getLocation().add(0, 1, 0);
        Vector forward = startLoc.getDirection().setY(0).normalize().multiply(0.2); // Tiến 0.2 block/tick

        Set<UUID> firstHitTracker = new HashSet<>();
        Particle.DustOptions leafDust = new Particle.DustOptions(org.bukkit.Color.GREEN, 1.5f);

        new BukkitRunnable() {
            int ticks = 0;
            Location current = startLoc.clone();

            @Override
            public void run() {
                if (ticks >= 80 || !player.isOnline()) { this.cancel(); return; } // Tồn tại 4s

                current.add(forward);
                if (ticks % 10 == 0) current.getWorld().playSound(current, Sound.BLOCK_GRASS_BREAK, 0.5f, 0.5f);

                // Vẽ Bão lá R=3
                for (int i = 0; i < 20; i++) {
                    double x = (random.nextDouble() - 0.5) * 6;
                    double y = (random.nextDouble() - 0.5) * 4;
                    double z = (random.nextDouble() - 0.5) * 6;
                    current.getWorld().spawnParticle(Particle.DUST, current.clone().add(x, y, z), 1, 0, 0, 0, 0.0, leafDust, true);
                }

                if (ticks % 10 == 0) { // Gây ST mỗi 0.5s
                    for (org.bukkit.entity.Entity ent : current.getWorld().getNearbyEntities(current, 3.0, 3.0, 3.0)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            LivingEntity target = (LivingEntity) ent;

                            // Khóa nhảy (Jump Boost cực cao âm hoặc logic tùy server)
                            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20, 128, false, false, false));

                            double multi = firstHitTracker.contains(target.getUniqueId()) ? (baseMulti * 0.2) : (baseMulti * 0.8);
                            ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.NATURE);

                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, multi * reaction.damageMultiplier, 0, true, false);
                            CombatManager.applySkillDamage(player, target, finalDmg);

                            if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
                            firstHitTracker.add(target.getUniqueId());
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // 5. ÁNH SÁNG: QUANG PHỔ CHÓI LÒA (STUN)
    // ==========================================
    private void triggerLightBurst(Player player, RPGPlayer rpgPlayer, int level, double baseMulti) {
        Location center = player.getLocation().add(0, 1, 0);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2f, 1.5f);
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2f, 2.0f);

        Particle.DustOptions lightDust = new Particle.DustOptions(org.bukkit.Color.WHITE, 2.5f);

        // Flashbang R=5
        for (int i = 0; i < 200; i++) {
            double phi = Math.acos(1 - 2 * random.nextDouble());
            double theta = 2 * Math.PI * random.nextDouble();
            double r = random.nextDouble() * 5.0;
            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.cos(phi);
            double z = r * Math.sin(phi) * Math.sin(theta);
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, y, z), 1, 0, 0, 0, 0.0, lightDust, true);
        }

        int stunTicks = (int) ((1.5 + (level * 0.1)) * 20); // 1.5s -> 2.5s

        for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 5.0, 5.0, 5.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                LivingEntity target = (LivingEntity) ent;

                // Cứ thế mà phang thẳng hàm tiện ích vào, hàm gốc sẽ tự lo liệu nó là Quái hay Người!
                StatusEffectManager.applyAbsoluteRoot(target, stunTicks);
                StatusEffectManager.applyEffect(target, PotionEffectType.BLINDNESS, stunTicks, 1);

                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.LIGHT);
                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, baseMulti * 0.7 * reaction.damageMultiplier, 0, true, false);
                CombatManager.applySkillDamage(player, target, finalDmg);

                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
            }
        }
    }

    // ==========================================
    // 6. BÓNG TỐI: KHE NỨT DIỆT VONG (QUÉT LASER)
    // ==========================================
    private void triggerDarkBurst(Player player, RPGPlayer rpgPlayer, double baseMulti) {
        Location castLoc = player.getEyeLocation();
        // LƯU HƯỚNG GỐC: Mage có xoay chuột thì tia quét vẫn quét tại vị trí cũ
        Vector forwardBase = castLoc.getDirection().setY(0).normalize();

        Particle.DustOptions darkDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(30, 0, 50), 1.5f);
        Set<UUID> hitTargets = new HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;
            int maxTicks = 20; // Quét trong 1 giây (20 ticks)

            @Override
            public void run() {
                if (ticks > maxTicks || !player.isOnline()) { this.cancel(); return; }

                // Tính góc quét từ -45 độ sang +45 độ
                double angle = -45.0 + (90.0 * ((double) ticks / maxTicks));
                Vector sweepDir = forwardBase.clone().rotateAroundY(Math.toRadians(angle));

                castLoc.getWorld().playSound(castLoc, Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.3f, 1.5f);

                // Vẽ tia Laser dài 8 blocks
                for (double d = 0; d <= 8.0; d += 0.5) {
                    Location partLoc = castLoc.clone().add(sweepDir.clone().multiply(d));
                    castLoc.getWorld().spawnParticle(Particle.DUST, partLoc, 3, 0.2, 0.2, 0.2, 0.0, darkDust, true);
                    castLoc.getWorld().spawnParticle(Particle.SQUID_INK, partLoc, 1, 0, 0, 0, 0.0, null, true);

                    // Hitbox dọc theo tia
                    for (org.bukkit.entity.Entity ent : partLoc.getWorld().getNearbyEntities(partLoc, 1.0, 1.0, 1.0)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            LivingEntity target = (LivingEntity) ent;
                            if (!hitTargets.contains(target.getUniqueId())) {

                                // Mù + Phân rã 4s (80 ticks)
                                StatusEffectManager.applyEffect(target, PotionEffectType.BLINDNESS, 80, 1);
                                StatusEffectManager.applyEffect(target, PotionEffectType.WITHER, 80, 1);

                                ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.DARK);
                                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, baseMulti * 1.2 * reaction.damageMultiplier, 0, true, false);
                                CombatManager.applySkillDamage(player, target, finalDmg);

                                if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
                                hitTargets.add(target.getUniqueId());
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}