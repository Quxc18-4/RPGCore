package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

import java.util.ArrayList;
import java.util.List;

public class DarkRiftFusion implements IFusion {

    // [HOOK VÀO COMBAT MANAGER ĐỂ GIẢM SÁT THƯƠNG GÂY RA TỪ QUÁI]
    static {
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getOutgoingDamageModifier(LivingEntity attacker, LivingEntity victim) {
                if (attacker.hasMetadata("DarkRift_Damage_Debuff")) {
                    long expire = attacker.getMetadata("DarkRift_Damage_Debuff").get(0).asLong();
                    if (System.currentTimeMillis() < expire) return -0.10; // Giảm 10% Sát thương gây ra
                }
                return 0.0;
            }
        });
    }

    @Override public MageElement getElement1() { return MageElement.DARK; }
    @Override public MageElement getElement2() { return MageElement.DARK; }
    @Override public String getReactionName() { return "§5§lHƯ KHÔNG ĐOẠN LIỆT"; }
    @Override public double getDamageMultiplier() { return 1.20; }

    @Override
    public void executeVisualAndDamage(Player caster, RPGPlayer rpgPlayer, Location hitLoc, Vector direction, double baseMultiplier) {
        Vector flatDir = direction.clone().setY(0).normalize();
        double finalDmgMulti = baseMultiplier * getDamageMultiplier();

        hitLoc.getWorld().playSound(hitLoc, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.5f);

        List<Location> riftNodes = new ArrayList<>();
        Particle.DustOptions darkDust = new Particle.DustOptions(org.bukkit.Color.PURPLE, 1.2f);

        // Nổ sát thương lần đầu
        for (int i = 0; i <= 8; i += 2) {
            Location node = hitLoc.clone().add(flatDir.clone().multiply(i));
            riftNodes.add(node);

            for (Entity entity : node.getWorld().getNearbyEntities(node, 2.5, 2.5, 2.5)) {
                if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                    LivingEntity target = (LivingEntity) entity;
                    if (!target.hasMetadata("Rift_Hit_" + caster.getUniqueId())) {
                        target.setMetadata("Rift_Hit_" + caster.getUniqueId(), new FixedMetadataValue(RPGCore.getInstance(), true));
                        double hitDmg = CombatManager.calculateDamage(caster, rpgPlayer, target, finalDmgMulti, 0, true, false);
                        CombatManager.applySkillDamage(caster, target, hitDmg);
                    }
                }
            }
        }

        // Rãnh nứt tồn tại 3s (60 ticks)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 60 || !caster.isOnline()) { this.cancel(); return; }

                for (Location node : riftNodes) {
                    node.getWorld().spawnParticle(Particle.PORTAL, node, 10, 1.0, 0.2, 1.0, 0.1);
                    node.getWorld().spawnParticle(Particle.DUST, node, 5, 1.0, 0.1, 1.0, 0, darkDust, true);

                    for (Entity entity : node.getWorld().getNearbyEntities(node, 2.5, 2.5, 2.5)) {
                        if (entity instanceof LivingEntity && !entity.equals(caster) && CombatManager.canTarget(caster, entity)) {
                            LivingEntity target = (LivingEntity) entity;
                            // 1. Slow 20%
                            StatusEffectManager.applySlow(target, 40, 0.20, 1);
                            // 2. Cấp Metadata khóa 10% sát thương (Duy trì 2s sau khi ra khỏi rãnh)
                            target.setMetadata("DarkRift_Damage_Debuff", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 2000L));
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}