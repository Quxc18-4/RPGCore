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

    // ==========================================
    // ĐÃ REFACTOR: CHUẨN BOOLEAN, KHÔNG GỌI DURABILITY THỦ CÔNG NỮA
    // ==========================================
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

        List<org.bukkit.entity.Mob> myPets = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(20, 10, 20)) {
            if (e.hasMetadata("Summoner_Pet")) {
                List<MetadataValue> metaList = e.getMetadata("Summoner_Pet");
                if (!metaList.isEmpty() && metaList.get(0).asString().equals(player.getUniqueId().toString())) {
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
        boolean hitSomething = false; // Cờ kiểm tra trúng mục tiêu

        if (!hitTargets.isEmpty()) {
            hitSomething = true; // Đã trúng quái, lát nữa sẽ báo true để hệ thống trừ độ bền
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

        @SuppressWarnings("unchecked")
        LinkedList<LivingEntity> targetStack;
        if (player.hasMetadata("Summoner_Target_Stack")) {
            targetStack = (LinkedList<LivingEntity>) player.getMetadata("Summoner_Target_Stack").get(0).value();
        } else {
            targetStack = new LinkedList<>();
        }

        targetStack.removeIf(e -> !e.isValid() || e.isDead() || e.getLocation().distance(player.getLocation()) > 35);

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

        return hitSomething; // Chỉ trả về true (tốn độ bền) nếu quất trúng ít nhất 1 quái!
    }

    @Override
    public boolean performMeleeAttack(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, ItemStack weapon) {
        // [CƠ CHẾ ĐẶC BIỆT]: Gọi phễu lọc trung tâm thay vì gọi trực tiếp onLeftClick
        // Để nó tự check level vũ khí và tự trừ độ bền nếu trúng đích
        this.executeLeftClick(attacker, rpgAttacker, weapon);

        return false; // Hủy đòn chém tay vật lý gốc
    }

    // ==========================================
    // ĐÃ REFACTOR: CHUẨN BOOLEAN, KHÔNG GỌI DURABILITY THỦ CÔNG NỮA
    // ==========================================
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
                            return true; // Thu hồi đồ kẹt chunk cũng tính là 1 lần tác vụ thành công
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

                    return true; // Xóa thành công 1 đệ tử -> Báo cáo true để trừ độ bền Dây dắt
                }
            }
        }
        return false; // Không thao tác gì thì không trừ độ bền
    }

    @Override
    public double onMeleeHit(Player attacker, RPGPlayer rpgPlayer, LivingEntity target, double baseMultiplier) {
        return baseMultiplier * 0.45;
    }

    @Override
    public double onTakeDamage(Player victim, RPGPlayer rpgPlayer, LivingEntity attacker, double initialDamage) {
        if (attacker != null && !attacker.equals(victim) && !(attacker instanceof ArmorStand) && !attacker.hasMetadata("Summoner_Pet")) {

            @SuppressWarnings("unchecked")
            LinkedList<LivingEntity> targetStack;
            if (victim.hasMetadata("Summoner_Target_Stack")) {
                targetStack = (LinkedList<LivingEntity>) victim.getMetadata("Summoner_Target_Stack").get(0).value();
            } else {
                targetStack = new LinkedList<>();
            }

            targetStack.removeIf(e -> !e.isValid() || e.isDead() || e.getLocation().distance(victim.getLocation()) > 35);
            targetStack.remove(attacker);
            targetStack.addFirst(attacker);

            while (targetStack.size() > 8) {
                targetStack.removeLast();
            }
            victim.setMetadata("Summoner_Target_Stack", new FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), targetStack));

            List<org.bukkit.entity.Mob> myPets = new ArrayList<>();
            for (Entity e : victim.getNearbyEntities(20, 10, 20)) {
                if (e.hasMetadata("Summoner_Pet")) {
                    List<org.bukkit.metadata.MetadataValue> metaList = e.getMetadata("Summoner_Pet");
                    if (!metaList.isEmpty() && metaList.get(0).asString().equals(victim.getUniqueId().toString())) {
                        if (e instanceof org.bukkit.entity.Mob) myPets.add((org.bukkit.entity.Mob) e);
                    }
                }
            }

            if (!myPets.isEmpty()) {
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
            }
        }

        return initialDamage;
    }

    @Override
    public void onProjectileHit(Player shooter, RPGPlayer rpgPlayer, org.bukkit.entity.Projectile projectile, org.bukkit.event.entity.ProjectileHitEvent event) { }
}