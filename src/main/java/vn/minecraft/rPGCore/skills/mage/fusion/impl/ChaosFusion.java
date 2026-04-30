package vn.minecraft.rPGCore.skills.mage.fusion.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.mage.MageElement;
import vn.minecraft.rPGCore.skills.mage.fusion.IFusion;

public class ChaosFusion implements IFusion {

    @Override public MageElement getElement1() { return MageElement.LIGHT; }
    @Override public MageElement getElement2() { return MageElement.DARK; }

    @Override public String getReactionName() { return "§d§lHỖN ĐỘN"; }
    @Override public double getDamageMultiplier() { return 1.50; }

    @Override
    public void onHit(Player caster, LivingEntity target, Location hitLocation) {
        // [DEBUFF LÕI]: Gắn cờ Phá Giáp & Phá Kháng (Hook thẳng vào CombatManager như bạn đã làm ở Engine cũ)[cite: 5]
        target.setMetadata("Chaos_Armor_Shred", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 5000L));
        target.setMetadata("Chaos_Magic_Shred", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 5000L));

        // [VFX & SFX][cite: 5]
        target.getWorld().spawnParticle(Particle.WITCH, hitLocation.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        target.getWorld().playSound(hitLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
    }
}