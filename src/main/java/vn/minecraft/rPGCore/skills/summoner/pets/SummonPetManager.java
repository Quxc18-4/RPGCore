package vn.minecraft.rPGCore.skills.summoner.pets;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class SummonPetManager {
    private static final Map<String, IPetProfile> profiles = new HashMap<>();

    public static void register(IPetProfile profile) {
        profiles.put(profile.getId(), profile);
    }

    public static IPetProfile getProfile(String id) {
        return profiles.get(id);
    }

    // ==========================================
    // [BƯỚC 2]: TRẠM CHỈ HUY TRUNG TÂM (AI ĐIỀU PHỐI)
    // ==========================================
    public static LivingEntity getSmartTarget(Player owner) {
        if (owner == null || !owner.isOnline()) return null;

        // ƯU TIÊN SỐ 1: LỆNH CHỈ HUY BẰNG ROI (Không giới hạn thời gian)
        if (owner.hasMetadata("Summoner_Target_Stack")) {
            Object val = owner.getMetadata("Summoner_Target_Stack").get(0).value();
            if (val instanceof LinkedList) {
                @SuppressWarnings("unchecked")
                LinkedList<LivingEntity> stack = (LinkedList<LivingEntity>) val;

                // Lấy kẻ thù bị tag đầu tiên chưa chết
                for (LivingEntity target : stack) {
                    if (target != null && !target.isDead() && target.isValid() && CombatManager.canTarget(owner, target)) {
                        return target;
                    }
                }
            }
        }

        // ƯU TIÊN SỐ 2: CẢM BIẾN PHÒNG VỆ (15 Giây)
        if (owner.hasMetadata("Summoner_Defend_Target") && owner.hasMetadata("Summoner_Defend_Expiry")) {
            long expiryTime = owner.getMetadata("Summoner_Defend_Expiry").get(0).asLong();

            // Nếu vẫn còn trong 15s
            if (System.currentTimeMillis() < expiryTime) {
                String attackerUUIDStr = owner.getMetadata("Summoner_Defend_Target").get(0).asString();

                try {
                    UUID attackerUUID = UUID.fromString(attackerUUIDStr);
                    Entity entity = Bukkit.getEntity(attackerUUID);

                    if (entity instanceof LivingEntity) {
                        LivingEntity attacker = (LivingEntity) entity;
                        // Phải còn sống và nằm trong tầm target hợp lệ
                        if (!attacker.isDead() && attacker.isValid() && CombatManager.canTarget(owner, attacker)) {
                            return attacker;
                        }
                    }
                } catch (IllegalArgumentException ignored) {}
            } else {
                // Đã hết 15s -> Xóa trí nhớ về kẻ đánh lén (Pop-out)
                owner.removeMetadata("Summoner_Defend_Target", RPGCore.getInstance());
                owner.removeMetadata("Summoner_Defend_Expiry", RPGCore.getInstance());
            }
        }

        // Không có lệnh chủ động, không ai đánh lén -> Đệ tử không đánh ai cả
        return null;
    }
}