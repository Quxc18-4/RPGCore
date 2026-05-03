package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.*;

public class AbyssalRiftFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.DARK; }
    @Override public MageElement getElement2() { return MageElement.DARK; }
    @Override public String getReactionName() { return "§8§lVực Thẳm Khởi Nguyên"; }

    // [BALANCING]: Cắt giảm Sát thương từ 1.30 xuống 1.20 để bù đắp cho diện tích 3 block
    @Override public double getDamageMultiplier() { return 1.20; }

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();
        int duration = 6;

        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Vực Thẳm Khởi Nguyên]",
                "§f- Giáng một luồng ma lực hắc ám càn lướt trên mặt đất,",
                "§f  tạo ra những cột năng lượng trồi lên xé toạc địa hình.",
                "§f- Kẻ địch trúng cột hắc ám chịu §c" + (int)totalDmg + "% §fSát thương.",
                "",
                "§f§b[Nội tại - Lời Nguyền Vực Thẳm]",
                "§f- Để lại một §8Rãnh Nứt Vực Sâu§f tồn tại trong §a" + duration + "s§f.",
                "§f- Kẻ địch đứng trên rãnh nứt sẽ chịu hiệu ứng xấu:",
                "§f  Giảm §e10% Sát thương §fgây ra§f và giảm §e20% tốc chạy§f."
        );
    }

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {
        Location startLoc = caster.getLocation();
        Vector direction = startLoc.getDirection().setY(0).normalize();
        double totalDmgMulti = baseMultiplier * getDamageMultiplier();

        caster.getWorld().playSound(startLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.5f);
        caster.getWorld().playSound(startLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 0.5f);

        executeAbyssalWave(caster, rpgPlayer, startLoc, direction, totalDmgMulti);
    }

    private void executeAbyssalWave(Player caster, RPGPlayer rpgPlayer, Location start, Vector direction, double dmgMulti) {
        List<Location> riftPath = new ArrayList<>();
        Set<UUID> hitTargets = new HashSet<>();

        // [3D MATH]: Tính toán Vector vuông góc để lấy điểm bên Trái và bên Phải
        Vector rightDir = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        BlockData darkDebris = Material.COBBLED_DEEPSLATE.createBlockData();

        new BukkitRunnable() {
            int step = 0;
            final int maxSteps = 15; // Rãnh dài 15 block
            Location current = start.clone();

            @Override
            public void run() {
                if (step >= maxSteps || !caster.isOnline()) {
                    if (!riftPath.isEmpty()) {
                        startRiftZoneTask(caster, riftPath);
                    }
                    this.cancel();
                    return;
                }

                current.add(direction);

                // Khởi tạo 3 điểm ngang nhau: Trái, Giữa, Phải
                Location[] rowLocs = new Location[] {
                        current.clone(), // Giữa
                        current.clone().add(rightDir), // Phải
                        current.clone().subtract(rightDir) // Trái
                };

                boolean rowHasValidBlock = false;

                // Xử lý địa hình độc lập cho từng điểm trong 3 block
                for (Location loc : rowLocs) {
                    int highestY = loc.getWorld().getHighestBlockYAt(loc);

                    // Nếu vách đá quá cao (> 2 block) so với vị trí cast ban đầu thì bỏ qua block đó
                    if (Math.abs(highestY - start.getY()) <= 2) {
                        loc.setY(highestY + 1.0);
                        riftPath.add(loc);
                        rowHasValidBlock = true;

                        // VFX Cột năng lượng cho từng block
                        loc.getWorld().spawnParticle(Particle.SQUID_INK, loc, 15, 0.3, 1.5, 0.3, 0.05);
                        loc.getWorld().spawnParticle(Particle.SOUL, loc, 5, 0.3, 1.5, 0.3, 0.1);
                        loc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, loc, 15, 0.4, 0.2, 0.4, 0.1, darkDebris);
                    }
                }

                // Nếu cả 3 block đều đụng tường cao, dừng toàn bộ sóng
                if (!rowHasValidBlock) {
                    if (!riftPath.isEmpty()) startRiftZoneTask(caster, riftPath);
                    this.cancel();
                    return;
                }

                current.getWorld().playSound(current, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 0.5f);

                // [BALANCING]: Mở rộng Hitbox bán kính lên 2.5m để bao trọn bề ngang 3 block
                for (Entity entity : current.getWorld().getNearbyEntities(current, 2.5, 2.0, 2.5)) {
                    if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                        LivingEntity target = (LivingEntity) entity;
                        if (!hitTargets.contains(target.getUniqueId())) {
                            hitTargets.add(target.getUniqueId());

                            // Đẩy qua Cốt lõi Sát thương RPGCore
                            double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, dmgMulti, 0, true, false);
                            CombatManager.applySkillDamage(caster, target, hitDmg);

                            target.setVelocity(new Vector(0, 0.4, 0)); // Cảm giác bị hất nhẹ bởi sóng đất
                        }
                    }
                }

                step++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ==========================================
    // LINGERING ZONE: QUẢN LÝ RÃNH NỨT & DEBUFF
    // ==========================================
    private void startRiftZoneTask(Player caster, List<Location> path) {
        BlockData riftDebris = Material.OBSIDIAN.createBlockData();

        new BukkitRunnable() {
            int lifeTicks = 0;
            final int maxLife = 120; // Tồn tại 6 giây

            @Override
            public void run() {
                if (lifeTicks >= maxLife || !caster.isOnline()) {
                    this.cancel();
                    return;
                }

                // 1. VFX Rãnh nứt (Giảm tỷ lệ spawn xuống 15% vì số lượng block hiện tại đã x3)
                for (Location loc : path) {
                    if (Math.random() < 0.15) {
                        loc.getWorld().spawnParticle(Particle.PORTAL, loc, 3, 0.3, 0.1, 0.3, 0.0);
                        loc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, loc, 1, 0.4, 0.1, 0.4, 0.0, riftDebris);
                    }
                }

                // 2. Logic Debuff (Mỗi 10 ticks = 0.5s)
                if (lifeTicks % 10 == 0) {
                    Set<UUID> debuffedTargets = new HashSet<>();

                    // Quét các điểm trên đường nứt (Bán kính nhỏ 1.2m vì các block đã xếp sát nhau)
                    for (Location loc : path) {
                        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1.2, 1.5, 1.2)) {
                            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                                LivingEntity target = (LivingEntity) entity;
                                UUID tid = target.getUniqueId();

                                if (!debuffedTargets.contains(tid)) {
                                    debuffedTargets.add(tid);

                                    // A. Khống chế mềm: Chạy qua cổng StatusEffectManager chuẩn RPGCore
                                    vn.minecraft.rPGCore.managers.StatusEffectManager.applySlow(target, 20, 0.20, 0);

                                    // B. Giảm 10% Sát thương: Gắn Metadata cho MageClass đọc
                                    target.setMetadata("Abyss_Weakness_Debuff", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 1000));

                                    target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 2, 0.3, 0.5, 0.3, 0.02);
                                }
                            }
                        }
                    }
                }

                lifeTicks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}