package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.Arrays;
import java.util.List;

public class CycloneFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.WIND; }
    @Override public MageElement getElement2() { return MageElement.WIND; }
    @Override public String getReactionName() { return "§a§lĐạn Gió Xoáy"; }
    @Override public double getDamageMultiplier() { return 1.30; }

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();

        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Đạn Gió Xoáy]",
                "§f- Bắn ra một Quả Cầu Cuồng Phong nén về phía trước.",
                "§f- Lõi bão liên tục §aKéo Theo§f mọi kẻ địch nằm",
                "§f  trong phạm vi lõi đi theo cùng với nó.",
                "§f- Phát nổ gây §c" + (int)totalDmg + "% §fSát thương."
        );
    }

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {
        Location startLoc = caster.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();

        double totalDmgMulti = baseMultiplier * getDamageMultiplier();

        caster.getWorld().playSound(startLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.5f);
        caster.getWorld().playSound(startLoc, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.5f, 0.5f);

        launchCyclone(caster, rpgPlayer, startLoc, direction, totalDmgMulti);
    }

    private void launchCyclone(Player caster, RPGPlayer rpgPlayer, Location startLoc, Vector direction, double totalDmgMulti) {
        Vector upAxis = new Vector(0, 1, 0);
        Vector tempX = direction.clone().crossProduct(upAxis).normalize();
        if (tempX.lengthSquared() < 0.001) {
            tempX = new Vector(1, 0, 0);
        }

        final Vector xAxis = tempX;
        final Vector yAxis = direction.clone().crossProduct(xAxis).normalize();
        final Vector velocity = direction.clone().multiply(1.0);

        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            int ticks = 0;
            final int maxTicks = 15;
            final double helixRadius = 1.2;

            @Override
            public void run() {
                if (ticks >= maxTicks || !caster.isOnline()) {
                    explode(caster, rpgPlayer, currentLoc, totalDmgMulti);
                    this.cancel();
                    return;
                }

                currentLoc.add(velocity);

                if (currentLoc.getBlock().getType().isSolid()) {
                    explode(caster, rpgPlayer, currentLoc, totalDmgMulti);
                    this.cancel();
                    return;
                }

                // 1. [VFX LÕI BÃO]: Hình cầu đặc hoàn chỉnh (Offset siêu thấp)
                currentLoc.getWorld().spawnParticle(Particle.CLOUD, currentLoc, 8, 0.1, 0.1, 0.1, 0.01);
                currentLoc.getWorld().spawnParticle(Particle.SPIT, currentLoc, 5, 0.1, 0.1, 0.1, 0.01);

                // 2. [VFX ĐUÔI HELIX]: Lùi tọa độ tâm về phía sau 1.5 block
                Location trailCenter = currentLoc.clone().subtract(direction.clone().multiply(1.5));
                double angle = ticks * 0.6;

                Vector offset1 = xAxis.clone().multiply(Math.cos(angle) * helixRadius)
                        .add(yAxis.clone().multiply(Math.sin(angle) * helixRadius));
                Location p1 = trailCenter.clone().add(offset1);

                Vector offset2 = xAxis.clone().multiply(Math.cos(angle + Math.PI) * helixRadius)
                        .add(yAxis.clone().multiply(Math.sin(angle + Math.PI) * helixRadius));
                Location p2 = trailCenter.clone().add(offset2);

                currentLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, p1, 1, 0, 0, 0, 0);
                currentLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, p2, 1, 0, 0, 0, 0);

                // 3. [CÂN BẰNG]: Giảm bán kính hút xuống r = 1.0
                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.0, 1.0, 1.0)) {
                    if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                        LivingEntity target = (LivingEntity) entity;

                        Vector dragForce = velocity.clone().multiply(0.85).setY(velocity.getY() + 0.15);
                        target.setVelocity(dragForce);

                        if (ticks % 5 == 0) {
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.5f, 2.0f);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ==========================================
    // VỤ NỔ KẾT LIỄU (VFX 3D FIBONACCI SPHERE)
    // ==========================================
    private void explode(Player caster, RPGPlayer rpgPlayer, Location loc, double totalDmgMulti) {
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 2.0f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.2f);

        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 15, 2.0, 2.0, 2.0, 0.1);

        // [TOÁN HỌC 3D]: Quả cầu Fibonacci tạo mảng Particle 3D hoàn chỉnh
        Particle.DustOptions windDust = new Particle.DustOptions(org.bukkit.Color.WHITE, 1.5f);
        int samples = 50; // Tổng số hạt tạo vỏ cầu
        double phi = Math.PI * (3.0 - Math.sqrt(5.0)); // Tỷ lệ vàng
        double explosionRadius = 3.5;

        for (int i = 0; i < samples; i++) {
            double y = 1.0 - (i / (double) (samples - 1)) * 2.0; // Phân bổ trục Y từ 1 xuống -1
            double radiusAtY = Math.sqrt(1.0 - y * y); // Bán kính đường tròn tại Y
            double theta = phi * i;

            double x = Math.cos(theta) * radiusAtY;
            double z = Math.sin(theta) * radiusAtY;

            Location edge = loc.clone().add(x * explosionRadius, y * explosionRadius, z * explosionRadius);

            // Bắn hạt bay tỏa ra ngoài theo định hướng 3D
            loc.getWorld().spawnParticle(Particle.POOF, edge, 1, 0, 0, 0, 0.05);
            loc.getWorld().spawnParticle(Particle.CLOUD, edge, 1, 0, 0, 0, 0.1);
            loc.getWorld().spawnParticle(Particle.DUST, edge, 1, 0, 0, 0, 0, windDust, true);
        }

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3.5, 3.5, 3.5)) {
            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                LivingEntity target = (LivingEntity) entity;

                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, totalDmgMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, hitDmg);
            }
        }
    }
}