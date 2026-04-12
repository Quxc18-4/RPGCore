package vn.minecraft.rPGCore.classes.impl.mage;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.List;
import java.util.Map;

public class MageClass extends RPGClass {

    // [CORRECTED]: Constructor ĐÚNG CHUẨN gọi từ ClassManager (Factory)
    public MageClass(String id, String displayName, Material icon, List<String> description,
                     double maxHealth, double maxResource, ResourceType resourceType,
                     double regenRate, boolean isEnchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, isEnchanted);

        // [CORRECTED]: Tận dụng biến protected của RPGClass cha thay vì override cứng
        this.magicPenCap = 80.0;   // Nâng giới hạn Xuyên kháng phép lên 80% (Mặc định 50%)
    }

    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, Map<String, Double> stats) {
        // [NỘI TẠI]: Trí Tuệ Vượt Trội - Khởi đầu với +25% Xuyên kháng phép
        double currentMagicPen = stats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0);
        stats.put("Tỉ lệ xuyên kháng phép", currentMagicPen + 25.0);

        // Gọi lên cha để các hệ thống Hook khác (IStatModifier) hoạt động
        super.applyPassiveBonusStats(rpgPlayer, stats);
    }

    // [CORRECTED]: Khóa hoàn toàn khả năng chém tay cận chiến của Pháp Sư
    @Override
    public boolean performMeleeAttack(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, ItemStack weapon) {
        // Có thể bắn action bar hoặc âm thanh xịt
        attacker.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§c✖ Pháp Sư không thể đánh cận chiến!"));
        return false; // Trả về false sẽ hủy đòn chém tay ở EntityDamageByEntityEvent
    }

    @Override
    public void applyPetDynamicStats(Player owner, RPGPlayer rpgOwner, org.bukkit.entity.Entity pet, Map<String, Double> petStats) {
        // Nhánh Bóng Tối (Dark) của Mage có thể triệu hồi. Buff 30% xuyên phép của chủ cho Đệ tử.
        double ownerMagicPen = rpgOwner.getTotalStatsMap().getOrDefault("Tỉ lệ xuyên kháng phép", 0.0);

        // Theo CombatManager dòng 324, nó dùng key "ArmorPen" chung cho cả vật lý/phép của pet
        petStats.put("ArmorPen", petStats.getOrDefault("ArmorPen", 0.0) + (ownerMagicPen * 0.3));
    }

    // Thêm hàm này vào trong MageClass.java
    @Override
    public boolean onSwapItem(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        // Lấy Instance của kỹ năng từ SkillManager
        vn.minecraft.rPGCore.skills.ISkill swapSkill = vn.minecraft.rPGCore.RPGCore.getInstance().getSkillManager().getSkill("mage_elemental_swap");

        if (swapSkill != null) {
            // Thực thi việc đổi nguyên tố. Nếu trả về true -> Hủy việc vứt đồ sang tay trái.
            return swapSkill.execute(player, rpgPlayer, 1);
        }
        return false;
    }
}