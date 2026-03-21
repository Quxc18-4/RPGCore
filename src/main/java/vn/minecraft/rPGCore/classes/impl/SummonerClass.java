package vn.minecraft.rPGCore.classes.impl;

import io.lumine.mythic.bukkit.MythicBukkit;
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
import org.bukkit.metadata.MetadataValue;
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
import java.util.LinkedList;

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
        // 1. Cộng 8% CDR cơ bản
        double totalCdr = currentStats.getOrDefault("Giảm hồi chiêu", 0.0) + 8.0;

        // 2. Tính Mana dư (Vẫn tính trên con số thực tế chưa bị Cap)
        if (totalCdr > 40.0) {
            double bonusMana = (totalCdr - 40.0) * 5.0;
            currentStats.put("Năng lượng tối đa", currentStats.getOrDefault("Năng lượng tối đa", 0.0) + bonusMana);
        }

        // Cập nhật lại CDR vào Map để lát nữa hàm cha applyOffensivePassives khóa nó lại
        currentStats.put("Giảm hồi chiêu", totalCdr);

        // 3. Tính Damage Triệu hồi (Như cũ)
        double finalMaxMana = this.getMaxResource() + (rpgPlayer.getAttributeData().getAttribute("INT") * 1.0) + currentStats.getOrDefault("Năng lượng tối đa", 0.0);
        currentStats.put("Sát thương Triệu hồi", Math.floor(finalMaxMana / 100.0) * 5.0);

        // QUAN TRỌNG: Gọi super để nó chạy hàm khóa Cap (Xuyên giáp, Crit, CDR...)
        super.applyPassiveBonusStats(rpgPlayer, currentStats);
    }

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

        Vector rightDir = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        if (rightDir.lengthSquared() < 0.001) rightDir = new Vector(1, 0, 0);

        for (double d = 1.0; d <= maxRange; d += 0.15) {
            Location basePoint = eyeLoc.clone().add(direction.clone().multiply(d));
            double curveRight = Math.sin((d / maxRange) * Math.PI) * 1.2;
            double sagDown = Math.sin((d / maxRange) * Math.PI) * 0.5;
            Location curvedPoint = basePoint.add(rightDir.clone().multiply(curveRight)).add(0, -sagDown, 0);
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, curvedPoint, 1, 0.02, 0.02, 0.02, 0);
        }

        RayTraceResult result = player.getWorld().rayTrace(
                eyeLoc, direction, maxRange, org.bukkit.FluidCollisionMode.NEVER, true, 1.5,
                entity -> entity instanceof LivingEntity && !entity.equals(player) && !(entity instanceof ArmorStand) && !entity.hasMetadata("Summoner_Pet")
        );

        Location impactLoc;
        if (result != null && result.getHitPosition() != null) {
            impactLoc = new Location(player.getWorld(), result.getHitPosition().getX(), result.getHitPosition().getY(), result.getHitPosition().getZ());
        } else {
            impactLoc = eyeLoc.clone().add(direction.clone().multiply(maxRange));
        }

        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, impactLoc, 1, 0, 0, 0, 0);
        player.getWorld().spawnParticle(Particle.ENCHANT, impactLoc, 1, 0, 0, 0, 0);
        player.getWorld().spawnParticle(Particle.CRIT, impactLoc, 25, 1.0, 1.0, 1.0, 0.2);

        player.getWorld().playSound(impactLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.5f, 2.0f);
        player.getWorld().playSound(impactLoc, Sound.ITEM_TRIDENT_HIT, 1.0f, 0.8f);

        // ==========================================
        // 1. TÌM ĐỆ TỬ CỦA NGƯỜI CHƠI (BÁN KÍNH 20 BƯỚC)
        // ==========================================
        List<org.bukkit.entity.Mob> myPets = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(20, 10, 20)) {
            if (e.hasMetadata("Summoner_Pet")) {
                List<MetadataValue> metaList = e.getMetadata("Summoner_Pet");
                if (!metaList.isEmpty() && metaList.get(0).asString().equals(player.getUniqueId().toString())) {
                    if (e instanceof org.bukkit.entity.Mob) myPets.add((org.bukkit.entity.Mob) e);
                }
            }
        }

        // ==========================================
        // 2. LỌC TÌM QUÁI MỚI BỊ ĐÁNH TRÚNG TẠI ĐIỂM NỔ
        // ==========================================
        List<LivingEntity> hitTargets = new ArrayList<>();
        for (Entity e : impactLoc.getWorld().getNearbyEntities(impactLoc, 2.5, 2.0, 2.5)) {
            if (e.hasMetadata("Summoner_Pet")) continue;
            if (e instanceof LivingEntity && !e.equals(player) && !(e instanceof ArmorStand)) {
                hitTargets.add((LivingEntity) e);
            }
        }

        boolean isMagic = rpgPlayer.getStats().maxMagicalDamage > 0;
        List<LivingEntity> markedTargets = new ArrayList<>();

        if (!hitTargets.isEmpty()) {
            Collections.shuffle(hitTargets);
            int markedCount = 0;
            String markKey = "Summoner_Mark_" + player.getUniqueId().toString();

            for (LivingEntity target : hitTargets) {
                player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8f, 1.0f);

                if (markedCount < 3) {
                    target.setMetadata(markKey, new FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), System.currentTimeMillis() + 5000));
                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 2.2, 0), 15, 0.2, 0.2, 0.2, 0);
                    markedTargets.add(target);
                    markedCount++;
                }

                double finalDamage = CombatManager.calculateDamage(player, rpgPlayer, target, 0.45, 0.0, 0.0, 0.0, isMagic, false);
                CombatManager.applySkillDamage(player, target, finalDamage);
            }
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() + 2);
        }

        // ==========================================
        // 3. XỬ LÝ STACK MỤC TIÊU (HÀNG ĐỢI THÔNG MINH)
        // ==========================================
        @SuppressWarnings("unchecked")
        LinkedList<LivingEntity> targetStack;
        if (player.hasMetadata("Summoner_Target_Stack")) {
            targetStack = (LinkedList<LivingEntity>) player.getMetadata("Summoner_Target_Stack").get(0).value();
        } else {
            targetStack = new LinkedList<>();
        }

        // A. Dọn dẹp Stack: Xóa quái đã chết hoặc chạy quá xa (VD: > 35 block)
        targetStack.removeIf(e -> !e.isValid() || e.isDead() || e.getLocation().distance(player.getLocation()) > 35);

        // B. Cập nhật Stack: Thêm quái mới bị đánh trúng vào đầu Stack (Ưu tiên nhất)
        for (LivingEntity target : markedTargets) {
            targetStack.remove(target); // Nếu đã có trong hàng chờ thì gỡ ra...
            targetStack.addFirst(target); // ...để đôn nó lên vị trí đầu tiên (vừa bị đánh)
        }

        // C. Giới hạn trí nhớ: Bầy thú chỉ nhớ tối đa 6 mục tiêu gần nhất
        while (targetStack.size() > 8) {
            targetStack.removeLast();
        }

        // Lưu Stack lại cho lần vụt roi sau
        player.setMetadata("Summoner_Target_Stack", new FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), targetStack));

        // ==========================================
        // 4. RA LỆNH CHO ĐỆ TỬ DỰA TRÊN STACK
        // ==========================================
        if (!myPets.isEmpty()) {
            if (!targetStack.isEmpty()) {
                // Trong Stack có quái -> Chia việc cắn xé
                for (int i = 0; i < myPets.size(); i++) {
                    org.bukkit.entity.Mob pet = myPets.get(i);
                    // Chia đều: Chó 1 đánh Quái 1, Chó 2 đánh Quái 2 (Lặp lại nếu chó nhiều hơn quái)
                    LivingEntity targetToAttack = targetStack.get(i % targetStack.size());

                    if (pet instanceof org.bukkit.entity.Wolf) {
                        ((org.bukkit.entity.Wolf) pet).setAngry(true);
                    }
                    pet.setTarget(targetToAttack);

                    try {
                        if (MythicBukkit.inst().getAPIHelper().isMythicMob(pet)) {
                            MythicBukkit.inst().getAPIHelper().addThreat(pet, targetToAttack, 99999);
                            MythicBukkit.inst().getAPIHelper().taunt(pet, targetToAttack);
                        }
                    } catch (Exception ex) {}
                }
            } else {
                // Stack rỗng (Quái chết sạch) -> Reset Aggro, Về chế độ phòng thủ!
                for (org.bukkit.entity.Mob pet : myPets) {
                    pet.setTarget(null); // Bỏ qua mục tiêu
                    if (pet instanceof org.bukkit.entity.Wolf) {
                        ((org.bukkit.entity.Wolf) pet).setAngry(false); // Ngưng sủa
                    }
                    try {
                        // Trả quyền điều khiển lại cho AI mặc định (đi theo chủ)
                        if (MythicBukkit.inst().getAPIHelper().isMythicMob(pet)) {
                            MythicBukkit.inst().getAPIHelper().reduceThreat(pet, pet.getTarget(), 99999);
                        }
                    } catch (Exception ex) {}
                }
            }
        }
    }

    @Override
    public void onRightClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) { }

    @Override
    public double onMeleeHit(Player attacker, RPGPlayer rpgPlayer, LivingEntity target, double baseMultiplier) {
        return baseMultiplier * 0.45;
    }

    @Override
    public double onTakeDamage(Player victim, RPGPlayer rpgPlayer, LivingEntity attacker, double initialDamage) {
        return initialDamage;
    }

    @Override
    public void onProjectileHit(Player shooter, RPGPlayer rpgPlayer, org.bukkit.entity.Projectile projectile, org.bukkit.event.entity.ProjectileHitEvent event) { }
}