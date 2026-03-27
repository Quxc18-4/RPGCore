package vn.minecraft.rPGCore.classes.impl;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;
import vn.minecraft.rPGCore.skills.warrior.WarriorAuraSkill;

import java.util.Map;

public class WarriorClass extends RPGClass {

    public WarriorClass(String id, String displayName, Material icon, java.util.List<String> description, double maxHealth, double maxResource, ResourceType resourceType, double regenRate, boolean enchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, enchanted);
        this.lifeStealCap = 80.0;
    }

    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, Map<String, Double> stats) {
        stats.put("Tỉ lệ hút máu", stats.getOrDefault("Tỉ lệ hút máu", 0.0) + 12.0);
        super.applyPassiveBonusStats(rpgPlayer, stats);
    }

    // ==========================================
    // GỌI KỸ NĂNG PARRY THÔNG QUA PHÍM F
    // ==========================================
    @Override
    public boolean onSwapItem(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        int skillLevel = rpgPlayer.getSkillData().getSkillLevel("warrior_parry");
        if (skillLevel > 0) {
            ISkill skill = RPGCore.getInstance().getSkillManager().getSkill("warrior_parry");
            if (skill != null) {
                return skill.execute(player, rpgPlayer, skillLevel);
            }
        }
        return false;
    }

    // ==========================================
    // NHẬN SÁT THƯƠNG -> CHECK PARRY -> TĂNG NỘ
    // ==========================================
    @Override
    public double onTakeDamage(Player victim, RPGPlayer rpgVictim, LivingEntity attacker, double incomingDamage) {
        double finalDamage = incomingDamage;

        if (victim.hasMetadata("Warrior_Parry_Window")) {
            victim.removeMetadata("Warrior_Parry_Window", RPGCore.getInstance());
            finalDamage *= 0.5;

            victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.5f, 0.8f);
            victim.getWorld().spawnParticle(Particle.FLASH, victim.getLocation().add(0, 1.2, 0), 10, 0.5, 0.5, 0.5, 0);
            victim.sendMessage("§e§l[PARRY] §fĐỡ đòn hoàn hảo!");

            rpgVictim.setCurrentResource(rpgVictim.getCurrentResource() + 25);
        } else {
            rpgVictim.setCurrentResource(rpgVictim.getCurrentResource() + 3);
        }
        return finalDamage;
    }

    // ==========================================
    // CHÉM TRÚNG -> TĂNG NỘ -> GỌI BỘ ĐẾM KIẾM KHÍ
    // ==========================================
    @Override
    public void onAttackLanded(Player attacker, LivingEntity target) {
        super.onAttackLanded(attacker, target);

        RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(attacker.getUniqueId());
        if (rpgPlayer != null) {
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + 5);

            int skillLevel = rpgPlayer.getSkillData().getSkillLevel("warrior_aura");
            if (skillLevel > 0) {
                ISkill skill = RPGCore.getInstance().getSkillManager().getSkill("warrior_aura");
                if (skill instanceof WarriorAuraSkill) {
                    ((WarriorAuraSkill) skill).addStack(attacker, rpgPlayer, target, skillLevel);
                }
            }
        }
    }
}