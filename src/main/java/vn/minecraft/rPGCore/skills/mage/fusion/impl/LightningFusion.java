package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

public class LightningFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.FIRE; }
    @Override public MageElement getElement2() { return MageElement.LIGHT; }

    @Override public String getReactionName() { return "§e§lCHỚP GIẬT"; }
    @Override public double getDamageMultiplier() { return 1.50; } // Khuếch đại 150%

    @Override
    public void onHit(Player caster, LivingEntity target, Location hitLocation) {
        // [VFX]: Sét giật nhân tạo (Visual only để tránh cháy map)
        target.getWorld().strikeLightningEffect(hitLocation);

        // [KHỐNG CHẾ AoE]: Tê liệt diện rộng (Slowness cực mạnh trong 2s)
        for (Entity ent : target.getWorld().getNearbyEntities(hitLocation, 4.0, 4.0, 4.0)) {
            if (ent instanceof LivingEntity && CombatManager.canTarget(caster, ent)) {
                // Trói chân hoàn toàn đám đông trong 2 giây (40 ticks)
                StatusEffectManager.applySlow((LivingEntity) ent, 40, 1.0, 10);
                ent.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, ent.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }
}