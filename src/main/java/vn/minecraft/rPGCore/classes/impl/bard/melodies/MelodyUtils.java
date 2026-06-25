package vn.minecraft.rPGCore.classes.impl.bard.melodies;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.impl.bard.BardClass;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class MelodyUtils {

    /**
     * Hàm này được gọi ở CUỐI mỗi hàm cast() của 8 Giai điệu.
     * Xử lý Lõi 1 (Afterglow), Lõi 6 (Dissonance), và Lõi 9 (Eternal Melody).
     */
    public static void processGlobalPerks(Player bard, RPGPlayer rpgBard, Location center, double radius, double durationTicks) {
        int allyCount = 0;
        boolean hasAfterglow = rpgBard.hasSetBonus("BARD_CORE_1", 1);
        boolean hasDissonance = rpgBard.hasSetBonus("BARD_CORE_6", 1);

        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (e instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) e;
                boolean isAlly = target.equals(bard) || (target instanceof Player && !CombatManager.canTarget(bard, target));

                if (isAlly) {
                    allyCount++;
                    // LÕI 1: DƯ ÂM NĂNG LƯỢNG (Hồi 50 Mana cho mọi đồng minh trong vùng)
                    if (hasAfterglow && target instanceof Player) {
                        RPGPlayer rpgTarget = RPGCore.getInstance().getPlayerManager().getRPGPlayer(target.getUniqueId());
                        if (rpgTarget != null) {
                            rpgTarget.setCurrentResource(rpgTarget.getCurrentResource() + 50.0);
                        }
                    }
                } else {
                    // LÕI 6: NGHỊCH ÂM (Gắn debuff -15% Giáp/Kháng Phép cho quái vật)
                    if (hasDissonance) {
                        long expireTime = System.currentTimeMillis() + (long) (durationTicks * 50);
                        target.setMetadata(BardClass.META_DISSONANCE_DEBUFF, new FixedMetadataValue(RPGCore.getInstance(), expireTime));
                    }
                }
            }
        }

        // LÕI 9: GIAI ĐIỆU VĨNH CỬU (Trúng 4 đồng minh -> Bật cờ giảm CD cho Kỹ năng tung ra)
        if (allyCount >= 4 && rpgBard.hasSetBonus("BARD_CORE_9", 1)) {
            bard.setMetadata("BARD_ETERNAL_MELODY_TRIGGERED", new FixedMetadataValue(RPGCore.getInstance(), true));
            bard.sendMessage("§d§l[Giai Điệu Vĩnh Cửu] §fCộng hưởng hoàn hảo! Giảm 50% hồi chiêu.");
        }
    }
}