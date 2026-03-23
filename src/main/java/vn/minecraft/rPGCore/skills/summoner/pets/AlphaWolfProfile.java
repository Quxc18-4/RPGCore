package vn.minecraft.rPGCore.skills.summoner.pets;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;

public class AlphaWolfProfile implements IPetProfile {

    @Override
    public String getId() { return "AlphaSpiritWolf"; }

    @Override
    public boolean isMagicDamage() {return false; }

    @Override
    public void onSpawn(Player owner, LivingEntity pet, int level, double... extraArgs) {
        double petCount = extraArgs[0];
        double finalHpBonus = extraArgs[1];
        double finalDmgBonus = extraArgs[2];

        pet.getAttribute(Attribute.SCALE).setBaseValue(1.5 + (petCount * 0.2));

        double newMaxHp = 2500 + (level * 300) + (finalHpBonus * 0.8);
        pet.getAttribute(Attribute.MAX_HEALTH).setBaseValue(newMaxHp);
        pet.setHealth(newMaxHp);

        double newBaseDmg = 180 + (level * 30) + finalDmgBonus;
        if (pet.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            pet.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(newBaseDmg);
        }
    }

    @Override
    public double onAttack(Player owner, LivingEntity pet, LivingEntity target, double baseDamage) {
        double random = Math.random() * 100;
        double multiplier = 1.0;
        int skillType = 0; // 0: Cào, 1: Đuôi, 2: Sét

        if (random <= 10) {
            skillType = 2; multiplier = 2.0;
            owner.sendMessage("§c§l[Tinh Lang Vương] §fThi triển §eSấm Sét Tự Hủy§f!");
            pet.getWorld().strikeLightningEffect(pet.getLocation());
            pet.setHealth(Math.max(1.0, pet.getHealth() - 500));
            StatusEffectManager.applyEffect(target, org.bukkit.potion.PotionEffectType.SLOWNESS, 30, 7);
        } else if (random <= 25) {
            skillType = 1; multiplier = 1.5;
            pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);
            pet.getWorld().spawnParticle(Particle.CRIT, pet.getLocation(), 30, 1.0, 0.5, 1.0, 0.2);
            target.setVelocity(target.getVelocity().add(new org.bukkit.util.Vector(0, 0.8, 0)));
        } else {
            skillType = 0; multiplier = 1.0;
            pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
            pet.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0);
        }

        // Kích hoạt AOE
        final int finalSkillType = skillType;
        final double finalMultiplier = multiplier; // [FIX LỖI JAVA]: Chốt sổ biến multiplier thành final

        new BukkitRunnable() {
            @Override
            public void run() {
                for (org.bukkit.entity.Entity e : target.getWorld().getNearbyEntities(target.getLocation(), 4, 2, 4)) {
                    if (e instanceof LivingEntity && e != owner && e != pet && e != target && !e.hasMetadata("Summoner_Pet")) {
                        LivingEntity aoeTarget = (LivingEntity) e;
                        CombatManager.applySkillDamage(owner, aoeTarget, baseDamage * finalMultiplier);
                        if (finalSkillType == 2) StatusEffectManager.applyEffect(aoeTarget, org.bukkit.potion.PotionEffectType.SLOWNESS, 30, 7);
                        else if (finalSkillType == 1) aoeTarget.setVelocity(aoeTarget.getVelocity().add(new org.bukkit.util.Vector(0, 0.8, 0)));
                    }
                }
            }
        }.runTaskLater(RPGCore.getInstance(), 1L);

        return multiplier;
    }

    @Override
    public void onDeath(Player owner, LivingEntity pet) {
        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);
        pet.getWorld().strikeLightningEffect(pet.getLocation());
        pet.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, pet.getLocation(), 1);

        double explosionDmg = pet.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 3.0;

        for (org.bukkit.entity.Entity e : pet.getWorld().getNearbyEntities(pet.getLocation(), 5, 3, 5)) {
            if (e instanceof LivingEntity && e != owner && !e.hasMetadata("Summoner_Pet")) {
                CombatManager.applySkillDamage(owner, (LivingEntity) e, explosionDmg);
                StatusEffectManager.applyEffect((LivingEntity) e, org.bukkit.potion.PotionEffectType.SLOWNESS, 30, 7);
            }
        }
    }
}