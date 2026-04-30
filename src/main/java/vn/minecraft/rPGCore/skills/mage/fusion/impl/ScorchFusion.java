package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

public class ScorchFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.FIRE; }
    @Override public MageElement getElement2() { return MageElement.NATURE; }

    @Override public String getReactionName() { return "§6§lTHIÊU RỤI"; }
    @Override public double getDamageMultiplier() { return 1.0; } // Sát thương gốc x1.0[cite: 5]

    @Override
    public void onHit(Player caster, LivingEntity target, Location hitLocation) {
        // [SÁT THƯƠNG KÉO DÀI]: Kích hoạt 7 giây đốt cháy (140 ticks)[cite: 5]
        target.setFireTicks(140);
        target.getWorld().spawnParticle(Particle.FLAME, hitLocation.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
    }
}