package vn.minecraft.rPGCore.classes.impl.mage.attacks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class WindAttack implements IMageBasicAttack {
    private final Random random = new Random();

    @Override
    public void cast(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        RPGCore plugin = RPGCore.getInstance();
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize().multiply(2.0);

        player.getWorld().playSound(startLoc, Sound.ENTITY_WIND_CHARGE_THROW, 0.8f, 1.5f);

        WindCharge realWindCharge = null;
        try {
            realWindCharge = player.launchProjectile(WindCharge.class, direction);
            realWindCharge.setGravity(false);
            realWindCharge.setMetadata("Fake_Mage_Wind", new FixedMetadataValue(plugin, true));
        } catch (Exception ignored) {}
        final WindCharge finalWindCore = realWindCharge;

        // ==========================================
        // [CƠ CHẾ CƯỜNG HÓA]: MẮT BÃO CÀO XÉ (DoT & SUCK)
        // ==========================================
        if (player.hasMetadata("Mage_Focus_Level")) {
            int focusLevel = player.getMetadata("Mage_Focus_Level").get(0).asInt();
            player.removeMetadata("Mage_Focus_Level", plugin);
            player.removeMetadata("Active_Weapon_Enhancement", plugin);

            // Sát thương tổng (VD: 3.35) chia đều cho 4 giây -> Sát thương mỗi Tick
            double totalFocusMulti = 2.0 + ((focusLevel - 1) * 0.15);
            double dpsMulti = totalFocusMulti / 4.0;

            new BukkitRunnable() {
                int ticks = 0;
                Location currentLoc = startLoc.clone();

                @Override
                public void run() {
                    ticks++;
                    if (ticks > 40) {
                        spawnFocusWindTornado(currentLoc, player, rpgPlayer, dpsMulti);
                        this.cancel();
                        return;
                    }
                    currentLoc.add(direction);

                    if (finalWindCore != null && finalWindCore.isValid()) {
                        finalWindCore.setVelocity(direction);
                    }

                    if (currentLoc.getBlock().getType().isSolid()) {
                        spawnFocusWindTornado(currentLoc, player, rpgPlayer, dpsMulti);
                        this.cancel();
                        return;
                    }

                    for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.0, 1.0, 1.0)) {
                        if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                            spawnFocusWindTornado(currentLoc, player, rpgPlayer, dpsMulti);
                            this.cancel();
                            return;
                        }
                    }
                }

                @Override
                public synchronized void cancel() {
                    super.cancel();
                    if (finalWindCore != null && finalWindCore.isValid()) finalWindCore.remove();
                }
            }.runTaskTimer(plugin, 0L, 1L);

            DurabilityManager.damageItem(player, rpgPlayer, weapon, 3);
            return;
        }

        // ==========================================
        // ĐÒN ĐÁNH THƯỜNG CŨ
        // ==========================================
        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();
            boolean hasDamagedWeapon = false;
            Set<UUID> hitSet = new HashSet<>();

            @Override
            public void run() {
                ticks++;
                if (ticks > 34) { this.cancel(); return; }
                currentLoc.add(direction);

                if (finalWindCore != null && finalWindCore.isValid()) {
                    finalWindCore.setVelocity(direction);
                }

                if (ticks > 2) {
                    currentLoc.getWorld().spawnParticle(Particle.CLOUD, currentLoc, 1, 0.1, 0.1, 0.1, 0);
                }

                if (currentLoc.getBlock().getType().isSolid()) {
                    currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1f);
                    currentLoc.getWorld().spawnParticle(Particle.GUST_EMITTER_SMALL, currentLoc, 1, 0, 0, 0, 0);
                    this.cancel();
                    return;
                }

                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent) && !hitSet.contains(ent.getUniqueId())) {
                        LivingEntity target = (LivingEntity) ent;
                        this.cancel();

                        if (!hasDamagedWeapon) {
                            DurabilityManager.damageItem(player, rpgPlayer, weapon, 1);
                            hasDamagedWeapon = true;
                        }

                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1f);
                        target.getWorld().spawnParticle(Particle.GUST_EMITTER_SMALL, target.getLocation(), 1, 0, 0, 0, 0);

                        Vector push = currentLoc.getDirection().normalize().multiply(1.2).setY(0.4);
                        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (target.isValid()) target.setVelocity(push);
                        }, 1L);

                        if (random.nextDouble() <= 0.10 && !player.hasMetadata("Wind_Tornado_CD")) {
                            player.setMetadata("Wind_Tornado_CD", new FixedMetadataValue(plugin, true));
                            spawnOrbitalWindTornado(target.getLocation(), player);
                            new BukkitRunnable() {
                                @Override public void run() { player.removeMetadata("Wind_Tornado_CD", plugin); }
                            }.runTaskLater(plugin, 60L);
                        }

                        player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, 0.7, 0.0, true, false);
                        CombatManager.applySkillDamage(player, target, finalDmg);
                        ReactionEngine.applyMarkOnly(target, MageElement.WIND);
                        player.removeMetadata("Ghost_L_Blocker", plugin);
                        return;
                    }
                }
            }

            @Override
            public synchronized void cancel() {
                super.cancel();
                if (finalWindCore != null && finalWindCore.isValid()) finalWindCore.remove();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==========================================
    // HÀM MẮT BÃO CƯỜNG HÓA (4 GIÂY, DAMAGE SPLIT)
    // ==========================================
    private void spawnFocusWindTornado(Location loc, Player shooter, RPGPlayer rpgShooter, double dpsMulti) {
        Location center = loc.clone().add(0, 1, 0);
        center.getWorld().playSound(center, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.5f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 80) { this.cancel(); return; } // Tồn tại 4s (80 ticks)

                // Visual Mắt Bão Khổng Lồ
                Particle.DustOptions windCoreColor = new Particle.DustOptions(org.bukkit.Color.fromRGB(173, 216, 230), 2.0f);
                center.getWorld().spawnParticle(Particle.DUST, center, 5, 0.5, 0.5, 0.5, 0, windCoreColor);
                center.getWorld().spawnParticle(Particle.WHITE_ASH, center, 5, 0.5, 0.5, 0.5, 0);

                double radius = 3.5; // Tầm hút siêu rộng
                double angle1 = ticks * 0.3;
                double angle2 = angle1 + Math.PI;

                Location p1 = center.clone().add(Math.cos(angle1) * radius, Math.sin(ticks * 0.1), Math.sin(angle1) * radius);
                Location p2 = center.clone().add(Math.cos(angle2) * radius, Math.sin(ticks * 0.1), Math.sin(angle2) * radius);

                center.getWorld().spawnParticle(Particle.CLOUD, p1, 1, 0, 0, 0, 0.05);
                center.getWorld().spawnParticle(Particle.CLOUD, p2, 1, 0, 0, 0, 0.05);

                if (ticks % 5 == 0) {
                    center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 3, 1.5, 1.5, 1.5, 0);
                    center.getWorld().spawnParticle(Particle.GUST, center, 2, 1.0, 1.0, 1.0, 0);
                }

                // Gây sát thương mỗi giây (Mỗi 20 Ticks)
                boolean isDamageTick = (ticks % 20 == 0);
                List<LivingEntity> validTargets = new ArrayList<>();

                // Hút quái liên tục
                for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 5.0, 5.0, 5.0)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(shooter, ent)) {
                        LivingEntity target = (LivingEntity) ent;

                        Vector pull = center.toVector().subtract(target.getLocation().toVector());
                        if (pull.lengthSquared() > 0.0001) {
                            target.setVelocity(target.getVelocity().add(pull.normalize().multiply(0.15)));
                        }

                        if (isDamageTick) validTargets.add(target);
                    }
                }

                // Xử lý sát thương cào xé
                if (isDamageTick && !validTargets.isEmpty()) {
                    center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
                    double dmgPerTarget = Math.max(dpsMulti / validTargets.size(), dpsMulti * 0.30);

                    for (LivingEntity target : validTargets) {
                        ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(shooter, target, MageElement.WIND);
                        double finalDmgMulti = dmgPerTarget * reaction.damageMultiplier;

                        shooter.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                        double finalDmg = CombatManager.calculateDamage(shooter, rpgShooter, target, finalDmgMulti, 0.0, true, false);
                        CombatManager.applySkillDamage(shooter, target, finalDmg);
                        shooter.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                        if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ==========================================
    // LỐC NHỎ ĐÁNH THƯỜNG CŨ
    // ==========================================
    private void spawnOrbitalWindTornado(Location loc, Player shooter) {
        Location center = loc.clone().add(0, 1, 0);
        new BukkitRunnable() {
            int time = 0;
            @Override
            public void run() {
                if (time >= 30) { this.cancel(); return; }
                Particle.DustOptions windCoreColor = new Particle.DustOptions(org.bukkit.Color.fromRGB(173, 216, 230), 1.2f);
                center.getWorld().spawnParticle(Particle.DUST, center, 3, 0.1, 0.1, 0.1, 0, windCoreColor);
                center.getWorld().spawnParticle(Particle.WHITE_ASH, center, 2, 0.2, 0.2, 0.2, 0);

                double radius = 1.5;
                double angle1 = time * 0.5;
                double angle2 = angle1 + Math.PI;

                Location p1 = center.clone().add(Math.cos(angle1) * radius, Math.sin(time * 0.3) * 0.5, Math.sin(angle1) * radius);
                Location p2 = center.clone().add(Math.cos(angle2) * radius, Math.sin(time * 0.3) * 0.5, Math.sin(angle2) * radius);

                center.getWorld().spawnParticle(Particle.CLOUD, p1, 1, 0, 0, 0, 0.01);
                center.getWorld().spawnParticle(Particle.CLOUD, p2, 1, 0, 0, 0, 0.01);

                if (time % 5 == 0) {
                    center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 1, 0.5, 0.5, 0.5, 0);
                    center.getWorld().spawnParticle(Particle.GUST, center, 1, 0.2, 0.2, 0.2, 0);
                }

                for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 3.5, 3.5, 3.5)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(shooter, ent)) {
                        Vector pull = center.toVector().subtract(ent.getLocation().toVector());
                        if (pull.lengthSquared() > 0.0001) {
                            ent.setVelocity(ent.getVelocity().add(pull.normalize().multiply(0.12)));
                        }
                    }
                }
                time++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}