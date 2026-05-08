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
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SteamBurstFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.FIRE; }
    @Override public MageElement getElement2() { return MageElement.WATER; }
    @Override public String getReactionName() { return "§f§lSóng Xung Kích"; }

    // [CẬP NHẬT ĐẶC QUYỀN DUNG HỢP]: Nâng Base lên 1.2x. Tổng sát thương nổ Bốc hơi sẽ là 1.8x (180%)
    @Override public double getDamageMultiplier() { return 1.20; }

    @Override
    public List<String> getLore(double baseDmgPct) {
        double baseFusionDmg = baseDmgPct * getDamageMultiplier();
        double maxReactionDmg = baseFusionDmg * 1.5; // Giả lập hiển thị DMG sau phản ứng

        return Arrays.asList(
                "§e[Kích hoạt - Hình thái Dung Hợp - Sóng Xung Kích]",
                "§f- Bắn ra một quả cầu bị nén ở áp suất cực cao.",
                "§f- Khi va chạm sẽ lơ lửng và liên tục nén lại",
                "§f  trong §a2.5s §fsau đó phát nổ cực mạnh§f.",
                "§f- Sóng xung kích hình cầu bành trướng §a12 block§f,",
                "§f  kích hoạt §c§lBỐC HƠI, gây §c" + (int)maxReactionDmg + "% §fSát thương và thổi tung kẻ địch."
        );
    }

    @Override
    public void cast(Player caster, RPGPlayer rpgPlayer, int skillLevel, double baseMultiplier) {
        Location startLoc = caster.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();
        double totalDmgMulti = baseMultiplier * getDamageMultiplier();

        caster.getWorld().playSound(startLoc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
        caster.getWorld().playSound(startLoc, Sound.BLOCK_WATER_AMBIENT, 1.5f, 2.0f);

        fireSteamBomb(caster, rpgPlayer, startLoc, direction, totalDmgMulti);
    }

    private void fireSteamBomb(Player caster, RPGPlayer rpgPlayer, Location startLoc, Vector direction, double dmgMulti) {
        new BukkitRunnable() {
            int ticks = 0;
            final int maxFlightTicks = 20;
            Location current = startLoc.clone();

            @Override
            public void run() {
                if (ticks >= maxFlightTicks || !caster.isOnline()) {
                    plantBomb(caster, rpgPlayer, current, null, dmgMulti);
                    this.cancel();
                    return;
                }

                current.add(direction.clone().multiply(1.5));

                current.getWorld().spawnParticle(Particle.FLAME, current, 2, 0.1, 0.1, 0.1, 0);
                current.getWorld().spawnParticle(Particle.SPLASH, current, 5, 0.2, 0.2, 0.2, 0);

                if (current.getBlock().getType().isSolid()) {
                    plantBomb(caster, rpgPlayer, current, null, dmgMulti);
                    this.cancel();
                    return;
                }

                for (Entity entity : current.getWorld().getNearbyEntities(current, 1.0, 1.0, 1.0)) {
                    if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                        plantBomb(caster, rpgPlayer, current, (LivingEntity) entity, dmgMulti);
                        this.cancel();
                        return;
                    }
                }

                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void plantBomb(Player caster, RPGPlayer rpgPlayer, Location hitLoc, LivingEntity hitEntity, double totalDmgMulti) {
        Location bombLoc;

        if (hitEntity != null) {
            bombLoc = hitEntity.getLocation();
        } else {
            bombLoc = new Location(hitLoc.getWorld(), hitLoc.getBlockX() + 0.5, hitLoc.getBlockY() + 1.0, hitLoc.getBlockZ() + 0.5);
        }

        bombLoc.getWorld().playSound(bombLoc, Sound.ENTITY_CREEPER_PRIMED, 1.0f, 0.5f);

        new BukkitRunnable() {
            int lifetime = 0;
            final int maxLife = 50;

            @Override
            public void run() {
                if (!caster.isOnline()) {
                    this.cancel();
                    return;
                }

                if (lifetime >= maxLife) {
                    detonateBomb(caster, rpgPlayer, bombLoc, totalDmgMulti);
                    this.cancel();
                    return;
                }

                if (lifetime % 5 == 0) {
                    bombLoc.getWorld().playSound(bombLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f + (lifetime * 0.02f));
                }

                double rx = (Math.random() - 0.5) * 1.2;
                double ry = (Math.random() - 0.5) * 1.2;
                double rz = (Math.random() - 0.5) * 1.2;
                bombLoc.getWorld().spawnParticle(Particle.FLAME, bombLoc.clone().add(0, 0.5, 0), 0, rx, ry, rz, 0.15);
                bombLoc.getWorld().spawnParticle(Particle.SPLASH, bombLoc.clone().add(0, 0.5, 0), 0, rx, ry, rz, 0.2);

                if (lifetime > 30) {
                    bombLoc.getWorld().spawnParticle(Particle.CLOUD, bombLoc.clone().add(0, 0.5, 0), 1, 0.2, 0.4, 0.2, 0.05);
                }

                lifetime++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void detonateBomb(Player caster, RPGPlayer rpgPlayer, Location center, double totalDmgMulti) {
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
        center.getWorld().playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 1.2f);

        double maxRadius = 12.0;

        new BukkitRunnable() {
            double currentRadius = 0.5;
            Set<UUID> hitTargets = new HashSet<>();

            @Override
            public void run() {
                // CHỈNH Ở ĐÂY: Tăng số này lên để sóng lan nhanh hơn!
                currentRadius += 2.0; // Gợi ý: 1.2 (mất ~0.5s để bung hết) hoặc 1.5 (mất ~0.4s)

                if (currentRadius > maxRadius) {
                    this.cancel();
                    return;
                }

                int particles = (int) (currentRadius * currentRadius * 3);
                for (int i = 0; i < particles; i++) {
                    double theta = Math.random() * 2 * Math.PI;
                    double phi = Math.acos(2 * Math.random() - 1);

                    // Tọa độ lớp vỏ ngoài
                    double x = currentRadius * Math.sin(phi) * Math.cos(theta);
                    double y = currentRadius * Math.sin(phi) * Math.sin(theta);
                    double z = currentRadius * Math.cos(phi);
                    Location edge = center.clone().add(x, y + 0.5, z);

                    // [CẢI TIẾN 2]: Vector gia tốc - Ép đám mây cuộn văng ra khỏi tâm nổ
                    Vector outward = new Vector(x, y, z).normalize();

                    // Khi count = 0, offsetX/Y/Z sẽ trở thành Vector Hướng, và speed sẽ là Lực đẩy (0.15)
                    center.getWorld().spawnParticle(Particle.CLOUD, edge, 0, outward.getX(), outward.getY(), outward.getZ(), 0.15);

                    // [CẢI TIẾN 3]: Lớp mây phụ bên trong (Dày đặc hơn, đuổi theo lớp ngoài)
                    if (currentRadius > 2.0 && Math.random() < 0.5) {
                        double innerX = (currentRadius - 1.5) * Math.sin(phi) * Math.cos(theta);
                        double innerY = (currentRadius - 1.5) * Math.sin(phi) * Math.sin(theta);
                        double innerZ = (currentRadius - 1.5) * Math.cos(phi);
                        Location innerEdge = center.clone().add(innerX, innerY + 0.5, innerZ);
                        center.getWorld().spawnParticle(Particle.CLOUD, innerEdge, 1, 0.2, 0.2, 0.2, 0);
                    }
                }

                double outerBoundSq = currentRadius * currentRadius;
                // Vành đai sát thương dày 2 block để không bỏ lót quái khi sóng quét qua
                double innerBoundSq = Math.pow(Math.max(0, currentRadius - 2.0), 2);

                for (Entity entity : center.getWorld().getNearbyEntities(center, currentRadius + 1, currentRadius + 1, currentRadius + 1)) {
                    if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                        LivingEntity target = (LivingEntity) entity;
                        UUID tid = target.getUniqueId();
                        double distSq = target.getLocation().distanceSquared(center);

                        if (distSq <= outerBoundSq && distSq >= innerBoundSq && !hitTargets.contains(tid)) {
                            hitTargets.add(tid);

                            ReactionEngine.applyMarkOnly(target, MageElement.WATER);
                            ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(caster, target, MageElement.FIRE);

                            double finalHitMulti = totalDmgMulti * reaction.damageMultiplier;
                            double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, finalHitMulti, 0, true, false);
                            CombatManager.applySkillDamage(caster, target, hitDmg);

                            Vector pushDir = target.getLocation().toVector().subtract(center.toVector());
                            if (pushDir.lengthSquared() == 0) pushDir = new Vector(Math.random() - 0.5, 0.5, Math.random() - 0.5);
                            Vector knockback = pushDir.normalize().multiply(1.5).setY(0.6);
                            target.setVelocity(knockback);
                        }
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}