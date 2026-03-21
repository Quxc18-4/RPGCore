package vn.minecraft.rPGCore.classes.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SummonerClass extends RPGClass {

    public SummonerClass(String id, String displayName, Material icon, java.util.List<String> description, double maxHealth, double maxResource, ResourceType resourceType, double regenRate, boolean enchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, enchanted);
    }

    public SummonerClass() {
        super("summoner", "§dTriệu Hồi Sư", Material.LEAD,
                Arrays.asList("§7Điều khiển bầy thú cưng", "§7và hỗ trợ đồng đội."),
                100.0, 100.0, ResourceType.MANA, 1.0, true);
    }

    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, Map<String, Double> currentStats) {
        double maxMana = rpgPlayer.getTotalMaxResource();
        double summonDamageBonus = Math.floor(maxMana / 100.0) * 5.0;
        currentStats.put("Sát thương Triệu hồi", summonDamageBonus);
    }

    @Override
    public void applyOffensivePassives(Map<String, Double> combatStats) { }

    // ==========================================
    // ĐÒN ĐÁNH THƯỜNG: QUẤT ROI AOE (DIỆN RỘNG)
    // ==========================================
    @Override
    public void onLeftClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        if (player.hasMetadata("Summoner_Whip_Cooldown")) return;

        double atkSpeed = rpgPlayer.getTotalStatsMap().getOrDefault("Tốc độ đánh", 0.0);
        long cooldownTicks = (long) Math.max(4, 12 * (1.0 - (atkSpeed / 100.0)));

        player.setMetadata("Summoner_Whip_Cooldown", new FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), true));
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (player.hasMetadata("Summoner_Whip_Cooldown")) {
                    player.removeMetadata("Summoner_Whip_Cooldown", vn.minecraft.rPGCore.RPGCore.getInstance());
                }
            }
        }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), cooldownTicks);

        double maxRange = 6.0;
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        player.getWorld().playSound(eyeLoc, Sound.ITEM_LEAD_BREAK, 1.0f, 1.5f);
        player.getWorld().playSound(eyeLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 2.0f);

        for (double d = 0; d <= maxRange; d += 0.5) {
            Location point = eyeLoc.clone().add(direction.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, point, 1, 0, 0, 0, 0);
        }

        // 1. Quét tia tổng hợp (Lấy cả Block và Entity) để tìm ĐIỂM VA CHẠM (Impact Point)
        RayTraceResult result = player.getWorld().rayTrace(
                eyeLoc, direction, maxRange, org.bukkit.FluidCollisionMode.NEVER, true, 1.0,
                entity -> entity instanceof LivingEntity && !entity.equals(player) && !(entity instanceof ArmorStand) && !entity.hasMetadata("Summoner_Pet")
        );

        // Xác định tâm vụ nổ AoE
        Location impactLoc;
        if (result != null && result.getHitPosition() != null) {
            impactLoc = new Location(player.getWorld(), result.getHitPosition().getX(), result.getHitPosition().getY(), result.getHitPosition().getZ());
        } else {
            impactLoc = eyeLoc.clone().add(direction.clone().multiply(maxRange)); // Nổ ở điểm xa nhất nếu đánh hụt không khí
        }

        // Hiệu ứng Nổ AoE tại tâm
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, impactLoc, 3, 0.5, 0.5, 0.5, 0);

        // 2. Lấy toàn bộ Quái vật bị trúng đòn
        List<LivingEntity> hitTargets = new ArrayList<>();

        // [MỚI]: TẠO DANH SÁCH ĐỆ TỬ CỦA MÌNH ĐỂ RA LỆNH
        List<org.bukkit.entity.Mob> myPets = new ArrayList<>();

        for (Entity e : impactLoc.getWorld().getNearbyEntities(impactLoc, 2.5, 2.0, 2.5)) {
            if (e.hasMetadata("Summoner_Pet") && e.getMetadata("Summoner_Pet").get(0).asString().equals(player.getUniqueId().toString())) {
                // Nhặt mấy con đệ của mình vào danh sách
                if (e instanceof org.bukkit.entity.Mob) myPets.add((org.bukkit.entity.Mob) e);
                continue; // Bỏ qua, không gây sát thương cho đệ
            }
            if (e instanceof LivingEntity && !e.equals(player) && !(e instanceof ArmorStand)) {
                hitTargets.add((LivingEntity) e); // Nhặt quái địch vào danh sách ăn đòn
            }
        }

        if (hitTargets.isEmpty()) return;

        // 3. Trộn ngẫu nhiên danh sách để chọn ra 3 mục tiêu nhận Dấu Ấn
        Collections.shuffle(hitTargets);
        int markedCount = 0;
        boolean isMagic = rpgPlayer.getStats().maxMagicalDamage > 0;
        String markKey = "Summoner_Mark_" + player.getUniqueId().toString();

        // 4. Duyệt qua từng mục tiêu để Gây sát thương (45%) và Gắn Ấn
        for (LivingEntity target : hitTargets) {

            player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8f, 1.0f);

            // Gắn dấu ấn cho tối đa 3 kẻ địch
            if (markedCount < 3) {
                target.setMetadata(markKey, new FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), System.currentTimeMillis() + 5000));
                player.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 2.2, 0), 5, 0.2, 0.2, 0.2, 0);
                markedCount++;

                // ==========================================
                // [MỚI]: RA LỆNH CHO TOÀN BỘ ĐỆ TỬ LAO VÀO CẮN KẺ BỊ ĐÁNH DẤU
                // ==========================================
                for (org.bukkit.entity.Mob pet : myPets) {
                    pet.setTarget(target);
                }
            }

            // Gây sát thương: Multiplier = 0.45 (45%)
            double finalDamage = CombatManager.calculateDamage(
                    player, rpgPlayer, target, 0.45, 0.0, 0.0, 0.0, isMagic, false
            );
            CombatManager.applySkillDamage(player, target, finalDamage);
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + 2);
    }

    @Override
    public void onRightClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        // Trống cho Combo
    }

    @Override
    public double onMeleeHit(Player attacker, RPGPlayer rpgPlayer, LivingEntity target, double baseMultiplier) {
        return baseMultiplier * 0.45; // Backup phòng hờ
    }

    @Override
    public double onTakeDamage(Player victim, RPGPlayer rpgPlayer, LivingEntity attacker, double initialDamage) {
        return initialDamage;
    }

    @Override
    public void onProjectileHit(Player shooter, RPGPlayer rpgPlayer, org.bukkit.entity.Projectile projectile, org.bukkit.event.entity.ProjectileHitEvent event) { }
}