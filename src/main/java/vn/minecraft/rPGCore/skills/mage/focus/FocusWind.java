package vn.minecraft.rPGCore.skills.mage.focus;

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
import java.util.List;

public class FocusWind implements IFocusAttack {

    @Override
    public void castFocus(Player player, RPGPlayer rpgPlayer, ItemStack weapon, int focusLevel) {
        RPGCore plugin = RPGCore.getInstance();
        player.removeMetadata("Mage_Focus_Level", plugin);
        player.removeMetadata("Active_Weapon_Enhancement", plugin);

        double totalFocusMulti = 2.0 + ((focusLevel - 1) * 0.15);

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

        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                ticks++;
                if (ticks > 40) {
                    spawnFocusWindTornado(currentLoc, player, rpgPlayer, totalFocusMulti);
                    this.cancel();
                    return;
                }
                currentLoc.add(direction);

                if (finalWindCore != null && finalWindCore.isValid()) finalWindCore.setVelocity(direction);

                if (currentLoc.getBlock().getType().isSolid()) {
                    spawnFocusWindTornado(currentLoc, player, rpgPlayer, totalFocusMulti);
                    this.cancel();
                    return;
                }

                for (org.bukkit.entity.Entity ent : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.0, 1.0, 1.0)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(player, ent)) {
                        spawnFocusWindTornado(currentLoc, player, rpgPlayer, totalFocusMulti);
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
    }

    private void spawnFocusWindTornado(Location loc, Player shooter, RPGPlayer rpgShooter, double totalFocusMulti) {
        Location center = loc.clone().add(0, 1, 0);
        center.getWorld().playSound(center, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.5f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 80) { this.cancel(); return; }

                Particle.DustOptions windCoreColor = new Particle.DustOptions(org.bukkit.Color.fromRGB(173, 216, 230), 2.0f);
                center.getWorld().spawnParticle(Particle.DUST, center, 5, 0.5, 0.5, 0.5, 0, windCoreColor);
                center.getWorld().spawnParticle(Particle.WHITE_ASH, center, 5, 0.5, 0.5, 0.5, 0);

                double radius = 4.0; // Bán kính giảm còn 4
                double angle1 = ticks * 0.3;
                double angle2 = angle1 + Math.PI;

                Location p1 = center.clone().add(Math.cos(angle1) * radius, Math.sin(ticks * 0.1), Math.sin(angle1) * radius);
                Location p2 = center.clone().add(Math.cos(angle2) * radius, Math.sin(ticks * 0.1), Math.sin(angle2) * radius);

                center.getWorld().spawnParticle(Particle.CLOUD, p1, 1, 0, 0, 0, 0.05);
                center.getWorld().spawnParticle(Particle.CLOUD, p2, 1, 0, 0, 0, 0.05);

                boolean isDamageTick = (ticks % 20 == 0); // 1 giây 1 lần
                List<LivingEntity> allTargets = new ArrayList<>();

                for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, 4.0, 4.0, 4.0)) {
                    if (ent instanceof LivingEntity && CombatManager.canTarget(shooter, ent)) {
                        allTargets.add((LivingEntity) ent);
                        Vector pull = center.toVector().subtract(ent.getLocation().toVector());
                        if (pull.lengthSquared() > 0.0001 && allTargets.size() <= 10) {
                            ent.setVelocity(ent.getVelocity().add(pull.normalize().multiply(0.15)));
                        }
                    }
                }

                if (isDamageTick) {
                    // Cơ chế QUÁ TẢI (OVERLOAD)
                    if (allTargets.size() > 10 && Math.random() < 0.25) { // 25% nổ mỗi giây nếu quá tải
                        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
                        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
                        for (LivingEntity ent : allTargets) {
                            Vector push = ent.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.5).setY(0.5);
                            ent.setVelocity(push);
                        }
                        this.cancel();
                        return;
                    }

                    if (!allTargets.isEmpty()) {
                        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);

                        int count = Math.min(allTargets.size(), 10);
                        // [ĐÃ SỬA] DMG ban đầu 25%, mỗi quái giảm 2%, tối thiểu 10%
                        double dpsPct = 0.25 - ((count - 1) * 0.02);
                        dpsPct = Math.max(dpsPct, 0.10);
                        double dmgPerTarget = totalFocusMulti * dpsPct;

                        for (int i = 0; i < count; i++) {
                            LivingEntity target = allTargets.get(i);
                            ReactionEngine.ReactionResult reaction = ReactionEngine.processHit(shooter, target, MageElement.WIND);
                            double finalDmgMulti = dmgPerTarget * reaction.damageMultiplier;

                            shooter.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                            double finalDmg = CombatManager.calculateDamage(shooter, rpgShooter, target, finalDmgMulti, 0.0, true, false);
                            CombatManager.applySkillDamage(shooter, target, finalDmg);
                            shooter.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                            if (reaction.isReacted) ReactionEngine.triggerReactionImmunity(target);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}