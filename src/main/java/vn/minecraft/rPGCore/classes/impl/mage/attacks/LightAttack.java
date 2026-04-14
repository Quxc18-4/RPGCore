package vn.minecraft.rPGCore.classes.impl.mage.attacks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.classes.impl.mage.ReactionEngine;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.DurabilityManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LightAttack implements IMageBasicAttack {

    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        RPGCore plugin = RPGCore.getInstance();

        // ==========================================
        // [CƠ CHẾ CƯỜNG HÓA]: CẦU ÁNH SÁNG (TURRET)
        // ==========================================
        if (player.hasMetadata("Mage_Focus_Level")) {
            int focusLevel = player.getMetadata("Mage_Focus_Level").get(0).asInt();
            player.removeMetadata("Mage_Focus_Level", plugin);
            player.removeMetadata("Active_Weapon_Enhancement", plugin);

            // Tính tổng hệ số sát thương (VD: Lv10 = 335% -> 3.35)
            double totalFocusMulti = 2.0 + ((focusLevel - 1) * 0.15);

            // Chia nhỏ sát thương: Mỗi tia laser bằng 20% sát thương gốc
            double damagePerShot = 0.20;
            int maxShots = (int) (totalFocusMulti / damagePerShot); // Lv10 = 3.35 / 0.2 = ~16 phát bắn

            Location spawnLoc = player.getEyeLocation().add(0, 4, 0); // Lơ lửng trên đầu 4 blocks
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.5f);

            new BukkitRunnable() {
                int shotsFired = 0;
                @Override
                public void run() {
                    if (shotsFired >= maxShots || !player.isOnline()) {
                        spawnLoc.getWorld().spawnParticle(Particle.FIREWORK, spawnLoc, 15, 0.5, 0.5, 0.5, 0.1);
                        spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                        this.cancel();
                        return;
                    }

                    // [GLITTERING VISUAL]: Quả cầu ánh sáng lấp lánh (Totem + Dust + Spark)
                    Particle.DustOptions orbColor = new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.5f);
                    spawnLoc.getWorld().spawnParticle(Particle.DUST, spawnLoc, 5, 0.3, 0.3, 0.3, orbColor);
                    spawnLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, spawnLoc, 3, 0.4, 0.4, 0.4, 0);
                    spawnLoc.getWorld().spawnParticle(Particle.FIREWORK, spawnLoc, 1, 0.2, 0.2, 0.2, 0.05);

                    // Tìm mục tiêu gần nhất bên dưới (Bán kính 8 blocks)
                    LivingEntity target = null;
                    double closestDist = Double.MAX_VALUE;
                    for (org.bukkit.entity.Entity ent : spawnLoc.getWorld().getNearbyEntities(spawnLoc, 8, 8, 8)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            double dist = ent.getLocation().distanceSquared(spawnLoc);
                            if (dist < closestDist) {
                                closestDist = dist;
                                target = (LivingEntity) ent;
                            }
                        }
                    }

                    if (target != null) {
                        // Bắn Laser xuống
                        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 2.0f);
                        Location tLoc = target.getLocation().add(0, 1, 0);
                        Vector laserDir = tLoc.toVector().subtract(spawnLoc.toVector());
                        double dist = laserDir.length();
                        Vector step = laserDir.normalize().multiply(0.5);
                        Location ray = spawnLoc.clone();

                        for (double d = 0; d < dist; d += 0.5) {
                            // [GLITTERING LASER]: Tia laser lấp lánh chớp tắt cực nhanh
                            ray.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, ray, 1, 0, 0, 0, 0);
                            ray.getWorld().spawnParticle(Particle.FIREWORK, ray, 1, 0.02, 0.02, 0.02, 0);
                            ray.add(step);
                        }

                        // Gây sát thương và Nổ Phản Ứng
                        ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(player, target, MageElement.LIGHT);
                        double finalDmgMulti = damagePerShot * reaction.damageMultiplier;

                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, finalDmgMulti, 0.0, true, false);
                        CombatManager.applySkillDamage(player, target, finalDmg);
                        player.removeMetadata("Ghost_L_Blocker", plugin);

                        if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);

                        target.getWorld().spawnParticle(Particle.FIREWORK, target.getLocation().add(0, 1, 0), 5, 0.1, 0.1, 0.1, 0.05);
                        shotsFired++;
                    }
                }
            }.runTaskTimer(plugin, 10L, 5L); // 10 tick khởi động, sau đó bắn mỗi 5 tick (4 phát/giây)

            DurabilityManager.damageItem(player, rpgPlayer, weapon, 3); // Trừ 3 độ bền cho chiêu tụ lực
            return;
        }

        // ==========================================
        // ĐÒN ĐÁNH THƯỜNG (BASIC ATTACK)
        // ==========================================
        Location startLoc = player.getEyeLocation();
        player.getWorld().playSound(startLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 2.0f);
        Vector dir = startLoc.getDirection().normalize().multiply(0.5);
        Location currentLoc = startLoc.clone();

        int pierceCount = 0;
        boolean firstHit = true;
        boolean hasHit = false;
        Set<UUID> hitSet = new HashSet<>();

        Particle.DustOptions fastRayColor = new Particle.DustOptions(org.bukkit.Color.YELLOW, 0.8f);

        for (int i = 0; i < 68; i++) {
            currentLoc.add(dir);

            if (i > 6) {
                // [GLITTERING ATTACK]: Mix DUST tĩnh và Tia lửa điện
                currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 1, fastRayColor);
                if (i % 2 == 0) currentLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, currentLoc, 1, 0, 0, 0, 0);
            }

            if (currentLoc.getBlock().getType().isSolid()) {
                currentLoc.getWorld().spawnParticle(Particle.FIREWORK, currentLoc, 5, 0.05, 0.05, 0.05, 0.05);
                currentLoc.getWorld().playSound(currentLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 1.5f);
                break;
            }

            for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.5, 0.5, 0.5)) {
                if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent) && !hitSet.contains(ent.getUniqueId())) {
                    LivingEntity hitTarget = (LivingEntity) ent;
                    hitSet.add(hitTarget.getUniqueId());
                    pierceCount++;
                    hasHit = true;

                    hitTarget.getWorld().playSound(hitTarget.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1.2f);
                    double damage = CombatManager.calculateDamage(player, rpgPlayer, hitTarget, 0.6, 0.0, true, false);

                    if (firstHit) {
                        hitTarget.setMetadata("Mage_Exposure_Debuff", new FixedMetadataValue(plugin, true));
                        firstHit = false;
                    }

                    ReactionEngine.applyMarkOnly(hitTarget, MageElement.LIGHT);
                    player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                    CombatManager.applySkillDamage(player, hitTarget, damage);
                    player.removeMetadata("Ghost_L_Blocker", plugin);

                    if (pierceCount >= 3) break;
                }
            }
            if (pierceCount >= 3) break;
        }
        if (hasHit) DurabilityManager.damageItem(player, rpgPlayer, weapon, 1);
    }
}