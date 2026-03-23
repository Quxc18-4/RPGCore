package vn.minecraft.rPGCore.skills.summoner.pets;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpiritWolfProfile implements IPetProfile {

    @Override
    public String getId() { return "SpiritWolf"; }

    @Override
    public boolean isMagicDamage() {return false; }

    @Override
    public void onSpawn(Player owner, LivingEntity pet, int level, double... extraArgs) {
        // Chỉ số của Sói thường
        double baseHp = 1000 + (level * 120);
        double baseDmg = 50 + (level * 15);

        if (pet.getAttribute(Attribute.MAX_HEALTH) != null) {
            pet.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseHp);
            pet.setHealth(baseHp);
        }
        if (pet.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            pet.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(baseDmg);
        }
    }

    @Override
    public double onAttack(Player owner, LivingEntity pet, LivingEntity target, double baseDamage) {
        // Sói thường không có kỹ năng, chỉ cắn chay -> Trả về hệ số 1.0
        return 1.0;
    }

    @Override
    public void onDeath(Player owner, LivingEntity pet) {
        // Sói thường chết bình thường, không nổ sấm sét
    }
}