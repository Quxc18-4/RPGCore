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
import org.bukkit.Location;

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
        // [ĐÃ SỬA]: Thủ thế là bản năng, ép nó thành cấp 1 kể cả khi chưa cộng điểm
        int skillLevel = Math.max(1, rpgPlayer.getSkillData().getSkillLevel("warrior_parry"));

        ISkill skill = RPGCore.getInstance().getSkillManager().getSkill("warrior_parry");
        if (skill != null) {
            skill.execute(player, rpgPlayer, skillLevel);
        }

        // LUÔN LUÔN trả về TRUE để chặn vứt vũ khí sang tay trái!
        return true;
    }

    // ==========================================
    // NHẬN SÁT THƯƠNG -> CHECK PARRY -> TĂNG NỘ -> VFX/SFX HOÀNH TRÁNG
    // ==========================================
    @Override
    public double onTakeDamage(Player victim, RPGPlayer rpgVictim, LivingEntity attacker, double incomingDamage) {
        double finalDamage = incomingDamage;

        if (victim.hasMetadata("Warrior_Parry_Window")) {
            victim.removeMetadata("Warrior_Parry_Window", RPGCore.getInstance());
            finalDamage *= 0.5; // Giảm 50% DMG

            Location loc = victim.getLocation().add(0, 1.2, 0); // Vị trí ngang ngực để spawn hiệu ứng

            // ==========================================
            // SFX: DÀN ÂM THANH "KENG KENG" BINH KHÍ VA CHẠM (image_0.png)
            // ==========================================
            // Tiếng Block cơ bản: Đã sửa lại đúng tên Enum cho Spigot 1.21
            victim.getWorld().playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.5f, 0.8f);

            // Dàn KENGS KENGS 1: Tiếng Chuông vang dội
            victim.getWorld().playSound(loc, Sound.BLOCK_BELL_USE, 1.2f, 2.0f); // Pitch cao tạo tiếng vang metallic

            // Dàn KENGS KENGS 2: Tiếng Sắt đập vào Cửa Sắt
            victim.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.5f); // Pitch cao tạo cảm giác va chạm mạnh

            // Dàn KENGS KENGS 3: Tiếng Tảng Đá đập vào Đe Sắt (Tạo tiếng 'Thud' nặng nề của va chạm)
            victim.getWorld().playSound(loc, Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f); // Pitch thấp

            // ==========================================
            // VFX: PARTICLE "HỎA TINH BÙNG NỔ" (image_0.png)
            // ==========================================
            // 1. Tia lửa bùng nổ radiating outwards (Particle.FIREWORK_SPARKS)
            victim.getWorld().spawnParticle(Particle.FIREWORK, loc, 30, 0.4, 0.4, 0.4, 0.1);

            // 2. Tia lửa va chạm (Particle.DAMAGE_INDICATOR)
            victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc, 15, 0.2, 0.2, 0.2, 0.2);

            // 3. Hiệu ứng Flash năng lượng xanh (Dùng Particle.CRIT_MAGIC y như trong ảnh, bám theo Color check cũ của 1.21 nếu có)
            victim.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 25, 0.6, 0.6, 0.6, 0.2);

            // 4. Bùng nổ nhỏ (Particle.EXPLOSION)
            victim.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);

            // Gửi Action Bar thông báo cho sướng
            victim.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent("§a§l[PARRY THÀNH CÔNG!] §c-50% CD"));

            // [NỘ KHÍ]: Hồi 15 Nộ khi Parry thành công
            rpgVictim.setCurrentResource(rpgVictim.getCurrentResource() + 15);

            // [NỘ KHÍ]: Bị đánh trúng khi đang Parry vẫn hồi 1 Nộ
            rpgVictim.setCurrentResource(rpgVictim.getCurrentResource() + 1);

            // [COOLDOWN]: Giảm 50% thời gian hồi chiêu hiện tại
            if (victim.hasMetadata("Warrior_Parry_CD_Time")) {
                long currentCdEnd = victim.getMetadata("Warrior_Parry_CD_Time").get(0).asLong();
                long now = System.currentTimeMillis();
                long remaining = currentCdEnd - now;
                if (remaining > 0) {
                    long newCdEnd = now + (remaining / 2);
                    victim.setMetadata("Warrior_Parry_CD_Time", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), newCdEnd));
                }
            }
        } else {
            // [NỘ KHÍ]: Bị đánh bình thường không hồi Nộ
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
            // [YÊU CẦU MỚI]: Chém trúng địch hồi 3 Nộ
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + 3);

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