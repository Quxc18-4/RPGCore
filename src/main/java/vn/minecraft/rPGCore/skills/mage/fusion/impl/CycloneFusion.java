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
    @Override public String getReactionName() { return "§a§lBão Tố Hư Không"; }
    @Override public double getDamageMultiplier() { return 1.30; }

    @Override
    public List<String> getLore(double baseDmgPct) {
        double totalDmg = baseDmgPct * getDamageMultiplier();

        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Bão Tố Hư Không]",
                "§f- Bắn ra một Quả Cầu Cuồng Phong khổng lồ về phía trước.",
                "§f- Lõi bão liên tục §aCuốn Phăng§f mọi kẻ địch trong",
                "§f  bán kính 2 mét đi theo cùng với nó.",
                "§f- Phát nổ ở cự ly trung bình (15m) hoặc khi va chạm,",
                "§f  gây §c" + (int)totalDmg + "% §fSát thương kỹ năng diện rộng."
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
            // [CÂN BẰNG]: Giới hạn tầm bay đúng 15 blocks
            final int maxTicks = 15;
            // [VFX]: Bán kính Helix xoắn ốc tỏa rộng 1.5 block
            final double helixRadius = 1.5;

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

                // Lõi Gió
                currentLoc.getWorld().spawnParticle(Particle.CLOUD, currentLoc, 6, 0.5, 0.5, 0.5, 0.05);
                currentLoc.getWorld().spawnParticle(Particle.SPIT, currentLoc, 5, 0.4, 0.4, 0.4, 0.05);

                double angle = ticks * 0.6;

                Vector offset1 = xAxis.clone().multiply(Math.cos(angle) * helixRadius)
                        .add(yAxis.clone().multiply(Math.sin(angle) * helixRadius));
                Location p1 = currentLoc.clone().add(offset1);

                Vector offset2 = xAxis.clone().multiply(Math.cos(angle + Math.PI) * helixRadius)
                        .add(yAxis.clone().multiply(Math.sin(angle + Math.PI) * helixRadius));
                Location p2 = currentLoc.clone().add(offset2);

                currentLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, p1, 1, 0, 0, 0, 0);
                currentLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, p2, 1, 0, 0, 0, 0);

                // [GAMEPLAY]: Bán kính hút chính xác r = 2.0 (Tổng diện tích 4 block)
                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 2.0, 2.0, 2.0)) {
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
    // VỤ NỔ KẾT LIỄU (VFX ĐẠI TU LẦN 3: SÓNG XUNG KÍCH SIÊU THANH)
    // ==========================================
    private void explode(Player caster, RPGPlayer rpgPlayer, Location loc, double totalDmgMulti) {
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 2.0f);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.2f);

        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 15, 2.0, 2.0, 2.0, 0.1);

        // [VFX JUICE]: Sóng áp suất bằng POOF và CLOUD tốc độ cao, dứt khoát không dây dưa
        Particle.DustOptions windDust = new Particle.DustOptions(org.bukkit.Color.WHITE, 1.5f);
        for (double t = 0; t <= Math.PI * 2; t += 0.3) {
            double x = 3.5 * Math.cos(t);
            double z = 3.5 * Math.sin(t);
            Location edge = loc.clone().add(x, 0.2, z);

            // Hạt POOF tan ngay lập tức
            loc.getWorld().spawnParticle(Particle.POOF, edge, 2, 0.1, 0.1, 0.1, 0.05);
            // Hạt CLOUD với tốc độ (0.2) tạo cảm giác bị thổi văng ra ngoài tâm
            loc.getWorld().spawnParticle(Particle.CLOUD, edge, 1, 0.1, 0.1, 0.1, 0.2);
            loc.getWorld().spawnParticle(Particle.DUST, edge, 1, 0, 0, 0, 0, windDust, true);
        }

        // Sát thương nổ (Bán kính 3.5 block)
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3.5, 3.5, 3.5)) {
            if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                LivingEntity target = (LivingEntity) entity;

                double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, totalDmgMulti, 0, true, false);
                CombatManager.applySkillDamage(caster, target, hitDmg);
            }
        }
    }
}