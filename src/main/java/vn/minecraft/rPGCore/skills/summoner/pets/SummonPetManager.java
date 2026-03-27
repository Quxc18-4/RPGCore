package vn.minecraft.rPGCore.skills.summoner.pets;

import java.util.HashMap;
import java.util.Map;

public class SummonPetManager {
    private static final Map<String, IPetProfile> profiles = new HashMap<>();

    public static void register(IPetProfile profile) {
        profiles.put(profile.getId(), profile);
    }

    public static IPetProfile getProfile(String id) {
        return profiles.get(id);
    }

    // ==========================================
    // THÊM HÀM NÀY VÀO: SummonPetManager.java
    // ==========================================
    public static org.bukkit.entity.LivingEntity getPetTarget(org.bukkit.entity.Player owner, org.bukkit.entity.LivingEntity pet, double radius) {
        org.bukkit.entity.LivingEntity bestTarget = null;
        double closestDist = radius;

        // Quét các thực thể xung quanh vị trí của Pet
        for (org.bukkit.entity.Entity e : pet.getNearbyEntities(radius, radius / 2, radius)) {
            if (e instanceof org.bukkit.entity.LivingEntity && !e.isDead() && e.isValid()) {
                org.bukkit.entity.LivingEntity livingTarget = (org.bukkit.entity.LivingEntity) e;

                // Tích hợp bộ lọc lõi: Xác định Địch / Ta / Guild / Chủ nhân
                if (vn.minecraft.rPGCore.managers.CombatManager.canTarget(owner, livingTarget)) {
                    double dist = pet.getLocation().distance(livingTarget.getLocation());

                    // Ưu tiên mục tiêu gần nhất VÀ đảm bảo không bị khuất sau bức tường (Line of Sight)
                    if (dist < closestDist && pet.hasLineOfSight(livingTarget)) {
                        closestDist = dist;
                        bestTarget = livingTarget;
                    }
                }
            }
        }
        return bestTarget; // Trả về null nếu không tìm thấy kẻ địch nào hợp lệ
    }
}