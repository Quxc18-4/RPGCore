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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;

public class SummonerClass extends RPGClass {

    // ==========================================
    // CONSTRUCTOR & TASK QUÉT RÁC 8 GIÂY
    // ==========================================
    public SummonerClass(String id, String displayName, Material icon, java.util.List<String> description, double maxHealth, double maxResource, ResourceType resourceType, double regenRate, boolean enchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, enchanted);

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (reservedPets.isEmpty()) return;
                vn.minecraft.rPGCore.player.PlayerManager pm = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager();

                for (Map.Entry<java.util.UUID, PetInfo> entry : reservedPets.entrySet()) {
                    Entity pet = org.bukkit.Bukkit.getEntity(entry.getKey());

                    if (pet == null || !pet.isValid() || pet.isDead()) {
                        RPGPlayer owner = pm.getRPGPlayer(entry.getValue().ownerId);
                        if (owner != null) {
                            unregisterPet(entry.getKey(), owner);
                        } else {
                            reservedPets.remove(entry.getKey());
                        }
                    }
                }
            }
        }.runTaskTimer(vn.minecraft.rPGCore.RPGCore.getInstance(), 160L, 160L);
    }

    private final Map<java.util.UUID, PetInfo> reservedPets = new java.util.concurrent.ConcurrentHashMap<>();

    public static class PetInfo {
        public final java.util.UUID ownerId;
        public final double manaReserved;
        public final String petName;

        public PetInfo(java.util.UUID ownerId, double manaReserved, String petName) {
            this.ownerId = ownerId;
            this.manaReserved = manaReserved;
            this.petName = petName;
        }
    }

    public void registerPet(java.util.UUID petId, double manaReserved, RPGPlayer owner, String petName) {
        reservedPets.put(petId, new PetInfo(owner.getBukkitPlayer().getUniqueId(), manaReserved, petName));
        owner.addReservedResource(manaReserved);
    }

    public void unregisterPet(java.util.UUID petId, RPGPlayer owner) {
        PetInfo info = reservedPets.remove(petId);
        if (info != null) {
            owner.freeReservedResource(info.manaReserved);
            owner.setCurrentResource(owner.getCurrentResource() + info.manaReserved);
            if (owner.getBukkitPlayer().isOnline()) {
                owner.getBukkitPlayer().sendMessage("§b§l[!] §f" + info.petName + " đã biến mất. Hoàn trả §b" + (int)info.manaReserved + " Mana§f!");
            }
        }
    }

    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, Map<String, Double> currentStats) {
        double totalCdr = currentStats.getOrDefault("Giảm hồi chiêu", 0.0) + 8.0;

        if (totalCdr > 40.0) {
            double bonusMana = (totalCdr - 40.0) * 5.0;
            currentStats.put("Năng lượng tối đa", currentStats.getOrDefault("Năng lượng tối đa", 0.0) + bonusMana);
        }

        currentStats.put("Giảm hồi chiêu", totalCdr);

        double finalMaxMana = this.getMaxResource() + (rpgPlayer.getAttributeData().getAttribute("INT") * 1.0) + currentStats.getOrDefault("Năng lượng tối đa", 0.0);
        currentStats.put("Sát thương Triệu hồi", Math.floor(finalMaxMana / 100.0) * 5.0);

        super.applyPassiveBonusStats(rpgPlayer, currentStats);
    }

    @Override
    public boolean onLeftClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        if (player.hasMetadata("Summoner_Whip_Cooldown")) return false; // Trả về false nếu đang Cooldown

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

        // Lấy danh sách đệ tử
        List<org.bukkit.entity.Mob> myPets = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(20, 10, 20)) {
            if (e.hasMetadata("Summoner_Pet")) {
                List<MetadataValue> metaList = e.getMetadata("Summoner_Pet");
                if (metaList != null && !metaList.isEmpty() && metaList.get(0).asString().equals(player.getUniqueId().toString())) {
                    if (e instanceof org.bukkit.entity.Mob) myPets.add((org.bukkit.entity.Mob) e);
                }
            }
        }

        List<LivingEntity> hitTargets = new ArrayList<>();
        for (Entity e : impactLoc.getWorld().getNearbyEntities(impactLoc, 2.5, 2.0, 2.5)) {
            if (e.hasMetadata("Summoner_Pet")) continue;
            if (e instanceof LivingEntity && !e.equals(player) && !(e instanceof ArmorStand)) {
                hitTargets.add((LivingEntity) e);
            }
        }

        boolean isMagic = rpgPlayer.getStats().maxMagicalDamage > 0;
        List<LivingEntity> markedTargets = new ArrayList<>();
        boolean hitSomething = false;

        if (!hitTargets.isEmpty()) {
            hitSomething = true;
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
        // [ĐÃ FIX BUG BẰNG LỚP BẢO VỆ]
        // ==========================================
        @SuppressWarnings("unchecked")
        LinkedList<LivingEntity> targetStack = new LinkedList<>();
        if (player.hasMetadata("Summoner_Target_Stack")) {
            List<MetadataValue> targetMeta = player.getMetadata("Summoner_Target_Stack");
            if (targetMeta != null && !targetMeta.isEmpty()) {
                Object val = targetMeta.get(0).value();
                if (val instanceof LinkedList) {
                    targetStack = (LinkedList<LivingEntity>) val;
                }
            }
        }

        // [WORLD CHECK] Loại bỏ lỗi khác chiều không gian
        targetStack.removeIf(e -> !e.isValid() || e.isDead() || !e.getWorld().equals(player.getWorld()) || e.getLocation().distance(player.getLocation()) > 35);
        for (LivingEntity target : markedTargets) {
            targetStack.remove(target);
            targetStack.addFirst(target);
        }

        while (targetStack.size() > 8) {
            targetStack.removeLast();
        }

        player.setMetadata("Summoner_Target_Stack", new FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), targetStack));

        if (!myPets.isEmpty()) {
            if (!targetStack.isEmpty()) {
                for (int i = 0; i < myPets.size(); i++) {
                    org.bukkit.entity.Mob pet = myPets.get(i);
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
                for (org.bukkit.entity.Mob pet : myPets) {
                    pet.setTarget(null);
                    if (pet instanceof org.bukkit.entity.Wolf) {
                        ((org.bukkit.entity.Wolf) pet).setAngry(false);
                    }
                    try {
                        if (MythicBukkit.inst().getAPIHelper().isMythicMob(pet)) {
                            MythicBukkit.inst().getAPIHelper().reduceThreat(pet, pet.getTarget(), 99999);
                        }
                    } catch (Exception ex) {}
                }
            }
        }

        return hitSomething;
    }

    @Override
    public boolean performMeleeAttack(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, ItemStack weapon) {
        this.executeLeftClick(attacker, rpgAttacker, weapon);
        return false;
    }

    @Override
    public boolean onRightClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        if (player.isSneaking()) {
            Entity targetPet = null;

            org.bukkit.util.RayTraceResult result = player.getWorld().rayTraceEntities(
                    player.getEyeLocation(), player.getEyeLocation().getDirection(), 10.0,
                    entity -> entity.hasMetadata("Summoner_Pet") && reservedPets.containsKey(entity.getUniqueId())
            );

            if (result != null && result.getHitEntity() != null) {
                targetPet = result.getHitEntity();
            } else {
                for (Map.Entry<java.util.UUID, PetInfo> entry : reservedPets.entrySet()) {
                    if (entry.getValue().ownerId.equals(player.getUniqueId())) {
                        targetPet = org.bukkit.Bukkit.getEntity(entry.getKey());
                        if (targetPet == null) {
                            unregisterPet(entry.getKey(), rpgPlayer);
                            return true;
                        }
                        break;
                    }
                }
            }

            if (targetPet != null && reservedPets.containsKey(targetPet.getUniqueId())) {
                PetInfo info = reservedPets.get(targetPet.getUniqueId());
                if (info.ownerId.equals(player.getUniqueId())) {
                    targetPet.remove();
                    unregisterPet(targetPet.getUniqueId(), rpgPlayer);
                    player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);

                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public double onMeleeHit(Player attacker, RPGPlayer rpgPlayer, LivingEntity target, double baseMultiplier) {
        // 1. Mặc định Summoner bị giảm sát thương cận chiến chỉ còn 45%
        double finalMultiplier = baseMultiplier * 0.45;

        // 2. Đọc Buff sát thương từ Quả Cầu Cộng Hưởng
        if (attacker.hasMetadata("Resonance_Stats")) {
            Object val = attacker.getMetadata("Resonance_Stats").get(0).value();
            if (val instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Double> buffs = (java.util.Map<String, Double>) val;

                // [Hệ Attacker]: Tăng % Sát thương gây ra
                // Ví dụ: Đang là 0.45, cộng thêm 0.10 từ buff sẽ thành 0.55
                finalMultiplier += buffs.getOrDefault("DamageMultiplier", 0.0);
            }
        }

        return finalMultiplier;
    }

    @Override
    public double onTakeDamage(Player victim, RPGPlayer rpgPlayer, LivingEntity attacker, double initialDamage) {
        if (attacker != null && !attacker.equals(victim) && !(attacker instanceof org.bukkit.entity.ArmorStand) && !attacker.hasMetadata("Summoner_Pet")) {

            @SuppressWarnings("unchecked")
            LinkedList<LivingEntity> targetStack = new LinkedList<>();

            // ==========================================
            // [FIX BUG BẰNG LỚP BẢO VỆ]
            // ==========================================
            if (victim.hasMetadata("Summoner_Target_Stack")) {
                List<org.bukkit.metadata.MetadataValue> targetMeta = victim.getMetadata("Summoner_Target_Stack");
                if (targetMeta != null && !targetMeta.isEmpty()) {
                    Object val = targetMeta.get(0).value();
                    if (val instanceof LinkedList) {
                        targetStack = (LinkedList<LivingEntity>) val;
                    }
                }
            }

            // [WORLD CHECK] Chặn lỗi khác chiều không gian khi đo khoảng cách
            targetStack.removeIf(e -> !e.isValid() || e.isDead() || !e.getWorld().equals(victim.getWorld()) || e.getLocation().distance(victim.getLocation()) > 35);
            targetStack.remove(attacker);
            targetStack.addFirst(attacker);

            while (targetStack.size() > 8) {
                targetStack.removeLast();
            }
            victim.setMetadata("Summoner_Target_Stack", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), targetStack));

            List<org.bukkit.entity.Mob> myPets = new ArrayList<>();
            for (org.bukkit.entity.Entity e : victim.getNearbyEntities(20, 10, 20)) {
                if (e.hasMetadata("Summoner_Pet")) {
                    List<org.bukkit.metadata.MetadataValue> metaList = e.getMetadata("Summoner_Pet");

                    if (metaList != null && !metaList.isEmpty()) {
                        if (metaList.get(0).asString().equals(victim.getUniqueId().toString())) {
                            if (e instanceof org.bukkit.entity.Mob) {
                                myPets.add((org.bukkit.entity.Mob) e);
                            }
                        }
                    }
                }
            }

            if (!myPets.isEmpty() && !targetStack.isEmpty()) {
                for (int i = 0; i < myPets.size(); i++) {
                    org.bukkit.entity.Mob pet = myPets.get(i);
                    LivingEntity targetToAttack = targetStack.get(i % targetStack.size());

                    if (pet instanceof org.bukkit.entity.Wolf) {
                        ((org.bukkit.entity.Wolf) pet).setAngry(true);
                    }
                    pet.setTarget(targetToAttack);

                    try {
                        if (io.lumine.mythic.bukkit.MythicBukkit.inst().getAPIHelper().isMythicMob(pet)) {
                            io.lumine.mythic.bukkit.MythicBukkit.inst().getAPIHelper().addThreat(pet, targetToAttack, 99999);
                            io.lumine.mythic.bukkit.MythicBukkit.inst().getAPIHelper().taunt(pet, targetToAttack);
                        }
                    } catch (Exception ex) { }
                }
            }
        }

        return initialDamage;
    }

    public void clearAllPets(Player player) {
        java.util.UUID ownerId = player.getUniqueId();
        RPGPlayer rpgOwner = vn.minecraft.rPGCore.RPGCore.getInstance().getPlayerManager().getRPGPlayer(ownerId);
        int count = 0;

        for (Map.Entry<java.util.UUID, PetInfo> entry : reservedPets.entrySet()) {
            if (entry.getValue().ownerId.equals(ownerId)) {
                Entity pet = org.bukkit.Bukkit.getEntity(entry.getKey());
                if (pet != null && pet.isValid()) {
                    pet.remove();
                }

                if (rpgOwner != null) {
                    rpgOwner.freeReservedResource(entry.getValue().manaReserved);
                    rpgOwner.setCurrentResource(rpgOwner.getCurrentResource() + entry.getValue().manaReserved);
                }

                reservedPets.remove(entry.getKey());
                count++;
            }
        }

        if (count > 0 && player.isOnline()) {
            player.sendMessage("§b§l[!] §fHệ thống đã tự động thu hồi §e" + count + " Linh Thú§f!");
        }
    }

    @Override
    public void onProjectileHit(Player shooter, RPGPlayer rpgPlayer, org.bukkit.entity.Projectile projectile, org.bukkit.event.entity.ProjectileHitEvent event) { }

    // ==========================================
    // HOOK 1: HIỆU ỨNG TRÚNG ĐÍCH TỪ CỘNG HƯỞNG (RESONANCE)
    // ==========================================
    @Override
    public void onAttackLanded(Player attacker, LivingEntity target) {
        // Kiểm tra xem Triệu hồi sư có đang cầm bùa lợi Cộng hưởng không
        if (attacker.hasMetadata("Resonance_Stats")) {
            Object val = attacker.getMetadata("Resonance_Stats").get(0).value();
            if (val instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Double> buffs = (java.util.Map<String, Double>) val;

                // [Hệ Ánh Sáng]: Hồi máu chuẩn trên mỗi đòn đánh trúng
                double heal = buffs.getOrDefault("HealOnHit", 0.0);
                if (heal > 0) {
                    double maxHp = attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    attacker.setHealth(Math.min(maxHp, attacker.getHealth() + heal));
                    attacker.getWorld().spawnParticle(org.bukkit.Particle.HEART, attacker.getLocation().add(0, 2, 0), 1, 0.3, 0.3, 0.3, 0);
                }

                // [Hệ Lửa]: Xác suất thiêu đốt mục tiêu
                double burnChance = buffs.getOrDefault("BurnChance", 0.0) * 100.0;
                if (burnChance > 0 && (Math.random() * 100) <= burnChance) {
                    target.setFireTicks(60); // Đốt 3 giây
                }

                // [Hệ Băng]: Xác suất làm chậm mục tiêu
                double slowChance = buffs.getOrDefault("SlowChance", 0.0) * 100.0;
                if (slowChance > 0 && (Math.random() * 100) <= slowChance) {
                    vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(target, org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 1);
                }
            }
        }
    }

    // ==========================================
    // HOOK 2: CỘNG XUYÊN GIÁP / XUYÊN PHÉP TỪ CỘNG HƯỞNG
    // ==========================================
    @Override
    public void applyDynamicCombatStats(Player attacker, LivingEntity target, java.util.Map<String, Double> combatStats) {
        if (attacker.hasMetadata("Resonance_Stats")) {
            Object val = attacker.getMetadata("Resonance_Stats").get(0).value();
            if (val instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Double> buffs = (java.util.Map<String, Double>) val;

                // [Hệ Tinh Linh / Pháp Sư]: Đọc chỉ số xuyên thấu
                double extraArmorPen = buffs.getOrDefault("ArmorPen", 0.0) * 100.0;
                double extraMagicPen = buffs.getOrDefault("MagicPen", 0.0) * 100.0;

                if (extraArmorPen > 0) {
                    combatStats.put("Tỉ lệ xuyên giáp", combatStats.getOrDefault("Tỉ lệ xuyên giáp", 0.0) + extraArmorPen);
                }
                if (extraMagicPen > 0) {
                    combatStats.put("Tỉ lệ xuyên kháng phép", combatStats.getOrDefault("Tỉ lệ xuyên kháng phép", 0.0) + extraMagicPen);
                }
            }
        }
    }

    // ==========================================
    // HOOK 4: TRUYỀN CHỈ SỐ CỘNG HƯỞNG CHO ĐỆ TỬ
    // ==========================================
    @Override
    public void applyPetDynamicStats(Player owner, RPGPlayer rpgOwner, Entity pet, Map<String, Double> petStats) {
        if (owner.hasMetadata("Resonance_Stats")) {
            Object val = owner.getMetadata("Resonance_Stats").get(0).value();
            if (val instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Double> buffs = (Map<String, Double>) val;

                // Chuyển đổi buff Cộng hưởng thành chỉ số của Đệ tử
                petStats.put("DamageMultiplier", buffs.getOrDefault("DamageMultiplier", 0.0));
                petStats.put("ArmorPen", buffs.getOrDefault("ArmorPen", 0.0) * 100.0);
                petStats.put("Lifesteal", buffs.getOrDefault("Lifesteal", 0.0) * 100.0);
                petStats.put("HealOnHit", buffs.getOrDefault("HealOnHit", 0.0));
                petStats.put("BurnChance", buffs.getOrDefault("BurnChance", 0.0) * 100.0);
                petStats.put("SlowChance", buffs.getOrDefault("SlowChance", 0.0) * 100.0);
            }
        }
    }
}