package vn.minecraft.rPGCore.skills.summoner.pets;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;

public class CopperGolemProfile implements IPetProfile {

    @Override
    public String getId() { return "CopperGolem"; }

    @Override
    public boolean isMagicDamage() { return false; }

    @Override
    public String getFusionTargetId() { return null; } // Hiện tại chưa có dạng tiến hóa

    @Override
    public String getFusionTargetName() { return null; }

    @Override
    public double getBaseFlatDamage(int level) {
        // Sát thương cơ bản không cần quá cao vì là Tanker (Lv1 = 30, Lv10 = 120)
        return 30.0 + (level - 1) * 10.0;
    }

    @Override
    public double getWeaponScaling(int level) {
        // Tỉ lệ scale vũ khí (Lv1 = 1.0x, Lv10 = 2.8x)
        return 0.8 + (level * 0.2);
    }

    // ==========================================
    // HỆ PHÁI & THUỘC TÍNH (DÙNG CHO DUNG HỢP)
    // ==========================================
    @Override
    public String getArchetype() { return "Vanguard"; } // Tiên Phong: Giảm sát thương nhận vào

    @Override
    public String getElement() { return "Earth"; } // Thổ: Chuyển hóa máu thành Khiên ảo khi Dung hợp

    // ==========================================
    // KHI VỪA ĐƯỢC TRIỆU HỒI
    // ==========================================
    @Override
    public void onSpawn(Player owner, LivingEntity pet, int level, double... extraArgs) {
        // 1. THU NHỎ KÍCH THƯỚC: Ép Iron Golem to xác thành Copper Golem bé xíu (Scale 0.65)
        if (pet.getAttribute(Attribute.SCALE) != null) {
            pet.getAttribute(Attribute.SCALE).setBaseValue(0.65);
        }

        // 2. MÁU SIÊU TRÂU: Khởi điểm 3000, mỗi cấp tăng 500 (Lv10 = 7500 Máu)
        double maxHp = 3000 + (level * 500);
        if (pet.getAttribute(Attribute.MAX_HEALTH) != null) {
            pet.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHp);
            pet.setHealth(maxHp); // Hồi đầy máu ngay khi triệu hồi
        }
    }

    // ==========================================
    // KHI TẤN CÔNG (HOẶC DÙNG KỸ NĂNG)
    // ==========================================
    @Override
    public double onAttack(Player owner, LivingEntity pet, LivingEntity target, double baseDamage) {
        // 1. KỸ NĂNG: KHIÊU KHÍCH (TAUNT) - Hồi chiêu 10 giây
        if (isSkillReady(pet, "Golem_Taunt_CD", 10000)) {
            setSkillCooldown(pet, "Golem_Taunt_CD");

            // Hiệu ứng gầm thét
            pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.5f);
            pet.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, pet.getLocation().add(0, 1.5, 0), 10, 0.5, 0.5, 0.5, 0);

            owner.sendMessage("§6§l[Đồng Nhân] §fĐã sử dụng §cKhiêu Khích§f! Kẻ địch đang nhắm vào nó!");

            // Quét mục tiêu xung quanh (Bán kính 8 block)
            for (org.bukkit.entity.Entity e : pet.getNearbyEntities(8, 5, 8)) {
                if (e instanceof Mob && e != owner && !e.hasMetadata("Summoner_Pet")) {
                    Mob enemy = (Mob) e;

                    // Lõi Vanilla AI: Bắt buộc quái vật chuyển mục tiêu sang đánh Golem
                    enemy.setTarget(pet);

                    // Đánh dấu hiệu ứng lên đầu kẻ địch bị dính Taunt
                    enemy.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, enemy.getLocation().add(0, 2, 0), 3, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }

        // 2. ĐÁNH THƯỜNG: Kèm hiệu ứng hất tung nhẹ đặc trưng của Golem
        target.setVelocity(target.getVelocity().add(new org.bukkit.util.Vector(0, 0.4, 0)));
        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.2f);

        // Trả về hệ số sát thương 1.0x cho đòn đánh thường
        return 1.0;
    }

    // ==========================================
    // KHI BỊ TIÊU DIỆT
    // ==========================================
    @Override
    public void onDeath(Player owner, LivingEntity pet) {
        // Tạo ra tiếng kim loại vỡ và rớt vụn đồng (Copper)
        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1.0f, 0.8f);
        pet.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, pet.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0, Material.COPPER_BLOCK.createBlockData());
    }

    // ==========================================
    // HELPER: QUẢN LÝ HỒI CHIÊU
    // ==========================================
    private boolean isSkillReady(LivingEntity pet, String skillName, long cooldownMillis) {
        if (!pet.hasMetadata(skillName)) return true;
        long lastUsed = pet.getMetadata(skillName).get(0).asLong();
        return (System.currentTimeMillis() - lastUsed) >= cooldownMillis;
    }

    private void setSkillCooldown(LivingEntity pet, String skillName) {
        pet.setMetadata(skillName, new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis()));
    }
}