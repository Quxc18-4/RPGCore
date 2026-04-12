package vn.minecraft.rPGCore.skills.warrior;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarriorSunderArmor implements ISkill {

    private final JavaPlugin plugin;

    public WarriorSunderArmor(JavaPlugin plugin) {
        this.plugin = plugin;

        // [CHUẨN SOLID]: Tự đăng ký hiệu ứng Phá Giáp Vật Lý vào Core
        CombatManager.registerCombatModifier(new CombatManager.ICombatModifier() {
            @Override
            public double getTargetArmorReductionPercentage(LivingEntity target) {
                // Kiểm tra xem mục tiêu có bị vỡ giáp không và thời gian vỡ giáp còn không
                if (target.hasMetadata("Warrior_Sunder_Reduction") && target.hasMetadata("Warrior_Sunder_Expire")) {
                    long expireTime = target.getMetadata("Warrior_Sunder_Expire").get(0).asLong();
                    if (System.currentTimeMillis() <= expireTime) {
                        return target.getMetadata("Warrior_Sunder_Reduction").get(0).asDouble(); // Trả về số % đã bị vỡ
                    } else {
                        // Hết hạn -> Dọn rác
                        target.removeMetadata("Warrior_Sunder_Reduction", plugin);
                        target.removeMetadata("Warrior_Sunder_Expire", plugin);
                        target.removeMetadata("Warrior_Sunder_Hits", plugin); // Reset số đếm
                    }
                }
                return 0.0;
            }
        });
    }

    @Override public String getId() { return "warrior_sunder_armor"; }
    @Override public String getName() { return "§c§lToái Giáp (Phá Giáp)"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE; }

    @Override public int getMaxLevel() { return 5; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public int getRequiredPlayerLevel() { return 32; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }
    @Override public boolean isPassiveSkill() { return true; }
    @Override public double getManaCost(int level) { return 0.0; }
    @Override public long getCooldown(int level) { return 0L; }

    @Override
    public List<String> getDescription(int level) {
        int dropPerStack = getReductionPerStack(level);
        return Arrays.asList(
                "§7Kỹ năng Nội Tại.",
                "§7Mỗi nhát chém đều mài mòn dần giáp trụ của kẻ thù.",
                "",
                "§e[Nội tại: Phá Giáp]",
                "§f- Cứ mỗi §a5 đòn đánh thường§f trúng cùng 1 mục tiêu,",
                "§f  sẽ phá thêm §c" + dropPerStack + "% Giáp gốc§f của chúng.",
                "§f- Có thể cộng dồn liên tục (Tối đa giảm §c50% Giáp§f).",
                "§f- Trạng thái phá giáp kéo dài trong §68 giây§f (Đánh để làm mới)."
        );
    }

    @Override public boolean execute(Player player, RPGPlayer rpgPlayer, int level) { return false; }

    // Tính lượng % trừ đi mỗi 5 Hit dựa trên cấp độ (4/4/5/5/6)
    public int getReductionPerStack(int level) {
        if (level <= 2) return 4;
        if (level <= 4) return 5;
        return 6;
    }

    // ==========================================
    // HÀM XỬ LÝ ĐẾM HIT VÀ PHÁ GIÁP (Gọi từ WarriorClass)
    // ==========================================
    public void processAttackLanded(Player attacker, LivingEntity target, int level) {
        // 1. Đọc và Tăng Hit Count (Lưu riêng cho từng người đánh)
        String hitKey = "Warrior_Sunder_Hits_" + attacker.getUniqueId().toString();
        int currentHits = target.hasMetadata(hitKey) ? target.getMetadata(hitKey).get(0).asInt() : 0;
        currentHits++;

        // Gia hạn thời gian duy trì vỡ giáp (8 giây) mỗi khi bị đánh
        long expireTime = System.currentTimeMillis() + 8000L;
        target.setMetadata("Warrior_Sunder_Expire", new FixedMetadataValue(plugin, expireTime));

        if (currentHits >= 5) {
            // Đã đủ 5 Hit -> Reset đếm và tiến hành Phá Giáp
            target.setMetadata(hitKey, new FixedMetadataValue(plugin, 0));

            double currentReduction = target.hasMetadata("Warrior_Sunder_Reduction") ? target.getMetadata("Warrior_Sunder_Reduction").get(0).asDouble() : 0.0;
            double amountToDrop = getReductionPerStack(level) / 100.0;

            // Tính tổng trừ giáp và ép Cap ở 50%
            double newReduction = Math.min(0.50, currentReduction + amountToDrop);
            target.setMetadata("Warrior_Sunder_Reduction", new FixedMetadataValue(plugin, newReduction));

            // Chạy VFX tương ứng với độ nát
            playShatterVFX(target, newReduction);
        } else {
            // Chưa đủ 5 Hit -> Lưu lại số Hit
            target.setMetadata(hitKey, new FixedMetadataValue(plugin, currentHits));
        }
    }

    // ==========================================
    // VFX PHÂN CẤP THEO TỈ LỆ NÁT GIÁP
    // ==========================================
    private void playShatterVFX(LivingEntity target, double reduction) {
        org.bukkit.Location loc = target.getLocation().add(0, 1.0, 0);

        if (reduction >= 0.50) {
            // [MỐC MAX 50%]: Vỡ vụn hoàn toàn, giáp trụ biến thành bụi đỏ
            target.getWorld().playSound(loc, Sound.BLOCK_ANVIL_DESTROY, 1.0f, 0.8f);
            target.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
            target.getWorld().spawnParticle(Particle.BLOCK, loc, 40, 0.5, 0.5, 0.5, 0, Material.REDSTONE_BLOCK.createBlockData());
            target.getWorld().spawnParticle(Particle.CRIT, loc, 30, 0.5, 0.5, 0.5, 0.1);
        }
        else if (reduction >= 0.40) {
            // [MỐC 40%]: Giáp rạn nứt nghiêm trọng, tóe lửa
            target.getWorld().playSound(loc, Sound.ENTITY_ITEM_BREAK, 1.2f, 0.5f);
            target.getWorld().spawnParticle(Particle.LAVA, loc, 10, 0.3, 0.3, 0.3, 0);
            target.getWorld().spawnParticle(Particle.BLOCK, loc, 25, 0.4, 0.4, 0.4, 0, Material.IRON_BLOCK.createBlockData());
        }
        else if (reduction >= 0.30) {
            // [MỐC 30%]: Bắt đầu vỡ mảnh sắt lớn
            target.getWorld().playSound(loc, Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);
            target.getWorld().spawnParticle(Particle.BLOCK, loc, 15, 0.3, 0.3, 0.3, 0, Material.CHAINMAIL_CHESTPLATE.createBlockData()); // Mảnh giáp văng ra
            Particle.DustOptions red = new Particle.DustOptions(Color.RED, 1.5f);
            target.getWorld().spawnParticle(Particle.DUST, loc, 10, 0.3, 0.3, 0.3, 0, red);
        }
        else if (reduction >= 0.20) {
            // [MỐC 20%]: Mẻ giáp nhỏ, bụi xám
            target.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.8f, 1.5f);
            target.getWorld().spawnParticle(Particle.ASH, loc, 30, 0.4, 0.4, 0.4, 0);
        }
        else {
            // [MỐC < 20%]: Hiệu ứng va đập kim loại nhẹ
            target.getWorld().playSound(loc, Sound.BLOCK_COPPER_BREAK, 1.0f, 1.2f);
            target.getWorld().spawnParticle(Particle.CRIT, loc, 10, 0.2, 0.2, 0.2, 0.05);
        }
    }
}