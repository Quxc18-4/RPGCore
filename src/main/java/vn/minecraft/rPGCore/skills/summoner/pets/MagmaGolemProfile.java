package vn.minecraft.rPGCore.skills.summoner.pets;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.summoner.pets.IPetProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MagmaGolemProfile implements IPetProfile, Listener {

    public MagmaGolemProfile() {
        RPGCore.getInstance().getServer().getPluginManager().registerEvents(this, RPGCore.getInstance());
    }

    @Override public String getId() { return "MagmaGolem"; }
    @Override public boolean isMagicDamage() { return false; }
    @Override public String getFusionTargetId() { return null; }
    @Override public String getFusionTargetName() { return null; }
    @Override public String getArchetype() { return "Vanguard"; }
    @Override public String getElement() { return "Fire"; }

    @Override
    public double getBaseFlatDamage(int level) {
        if (level == 10) return 400.0;
        return 80.0 + (level - 1) * 35.0;
    }

    @Override
    public double getWeaponScaling(int level) {
        return 2.0 + (level * 0.5); // Max 7.0x Scaling
    }

    @Override
    public void onSpawn(Player owner, LivingEntity pet, int level, double... extraArgs) {
        RPGPlayer rpgOwner = RPGCore.getInstance().getPlayerManager().getRPGPlayer(owner.getUniqueId());
        if (rpgOwner == null) return;

        // [TOÁN HỌC]: Đã cộng dồn lượng máu khổng lồ của các thú hiến tế (extraArgs[1])
        double finalHpBonus = extraArgs.length > 1 ? extraArgs[1] : 0;
        double baseHp = 15500 + (level * 1050) + finalHpBonus;

        if (pet.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
            pet.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(baseHp);
            pet.setHealth(baseHp);
        }

        pet.setMetadata("Summoner_Pet", new FixedMetadataValue(RPGCore.getInstance(), owner.getUniqueId().toString()));
        pet.setMetadata("Pet_Type", new FixedMetadataValue(RPGCore.getInstance(), getId()));

        pet.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 999999, 0, false, false));

        pet.getWorld().spawnParticle(Particle.EXPLOSION, pet.getLocation(), 1);
        pet.getWorld().spawnParticle(Particle.LAVA, pet.getLocation(), 50, 2.0, 2.0, 2.0, 0.1);
        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1.0f, 0.5f);

        // ==========================================
        // MAIN AI LOOP
        // ==========================================
        new BukkitRunnable() {
            private final Map<Location, Material> originalBlocks = new HashMap<>();
            private final Map<Location, Long> transformationTimestamps = new HashMap<>();

            int leapCooldown = 100;
            long lastActiveSkillTime = System.currentTimeMillis();
            boolean isLeaping = false;
            int ticks = 0;

            @Override
            public void run() {
                if (!pet.isValid() || pet.isDead() || !owner.isOnline() || rpgOwner == null) {
                    revertAllBlocks();
                    this.cancel();
                    return;
                }
                ticks++;

                if (ticks % 4 == 0) {
                    pet.getWorld().spawnParticle(Particle.FLAME, pet.getLocation().add(0, 1.0, 0), 2, 0.6, 1.0, 0.6, 0.02);
                }

                Block blockUnderOwner = owner.getLocation().getBlock().getRelative(BlockFace.DOWN);
                if (blockUnderOwner.getType() == Material.MAGMA_BLOCK && transformationTimestamps.containsKey(blockUnderOwner.getLocation())) {
                    owner.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 10, 0, false, false));
                }

                LivingEntity bestTarget = scanNearestTarget(owner, pet, 20.0);

                // Set mục tiêu cho hệ thống Vanilla hỗ trợ
                if (pet instanceof org.bukkit.entity.Mob) {
                    ((org.bukkit.entity.Mob) pet).setTarget(bestTarget);
                }

                // ==========================================
                // AI ĐI THEO CHỦ (Khi không có mục tiêu)
                // ==========================================
                if (bestTarget == null && !isLeaping) {
                    if (owner.getWorld().equals(pet.getWorld())) {
                        double dist = pet.getLocation().distance(owner.getLocation());
                        if (dist > 30.0) {
                            pet.teleport(owner.getLocation());
                        } else if (dist > 10.0) {
                            Vector dirToOwner = owner.getLocation().toVector().subtract(pet.getLocation().toVector()).normalize();
                            pet.setVelocity(pet.getVelocity().setX(dirToOwner.getX() * 0.25).setZ(dirToOwner.getZ() * 0.25));
                        }
                    }
                }

                // 2. BIẾN ĐỔI ĐỊA HÌNH (Lây lan chậm)
                if (ticks % 10 == 0) {
                    handleTerrainTransformation(1); // Chỉ lây lan 1 block mỗi 0.5s để đỡ lag
                }

                // 3. SÁT THƯƠNG TỪ MAGMA
                if (ticks % 20 == 0) {
                    handleMagmaBlockDamage();
                }

                // 4. LOGIC NHẢY ĐẬP ĐẤT (CÀN LƯỚT)
                if (isLeaping) {
                    if (pet.isOnGround()) {
                        isLeaping = false;

                        // [VFX NÂNG CẤP]: Cú đáp đất nổ tung và biến đổi ngay lập tức 5x5 vùng đất
                        pet.getWorld().spawnParticle(Particle.EXPLOSION, pet.getLocation(), 2);
                        pet.getWorld().spawnParticle(Particle.LAVA, pet.getLocation(), 40, 2.0, 0.5, 2.0, 0.1);
                        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.5f, 0.5f);
                        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

                        instantTransformTerrain(pet.getLocation(), 2); // Biến đổi ngay vùng R=2 (5x5)

                        for (org.bukkit.entity.Entity e : pet.getNearbyEntities(5.0, 4.0, 5.0)) {
                            if (e instanceof LivingEntity && CombatManager.canTarget(owner, e)) {
                                LivingEntity hitTarget = (LivingEntity) e;
                                double baseDmg = CombatManager.calculatePetDamage(owner, rpgOwner, pet, hitTarget, 0);

                                owner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                                CombatManager.applySkillDamage(owner, hitTarget, baseDmg * 1.50);
                                owner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                                hitTarget.setVelocity(new Vector(0, 0.8, 0));
                                hitTarget.setFireTicks(100);
                            }
                        }
                    }
                } else {
                    if (leapCooldown > 0) {
                        leapCooldown--;
                    } else if (bestTarget != null) {
                        double dist = pet.getLocation().distance(bestTarget.getLocation());
                        if (dist > 3.0 && dist < 15.0) {
                            isLeaping = true;
                            leapCooldown = 100;
                            pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.8f);

                            Vector dir = bestTarget.getLocation().toVector().subtract(pet.getLocation().toVector());
                            dir.normalize().multiply(Math.max(1.0, dir.length() * 0.18)).setY(0.7);
                            pet.setVelocity(dir);
                        }
                    }
                }

                // 5. KỸ NĂNG NÉM THIÊN THẠCH MAGMA
                if (!isLeaping && bestTarget != null && System.currentTimeMillis() - lastActiveSkillTime >= 12000) {
                    lastActiveSkillTime = System.currentTimeMillis();
                    Location shootLoc = pet.getLocation().add(0, pet.getHeight() * 0.8, 0);
                    Vector dir = bestTarget.getLocation().add(0, bestTarget.getHeight() / 2, 0).toVector().subtract(shootLoc.toVector()).normalize();

                    // [AI XOAY MẶT]: Ép Golem nhìn thẳng vào mục tiêu trước khi ném
                    Location lookLoc = pet.getLocation();
                    lookLoc.setDirection(dir);
                    pet.teleport(lookLoc);

                    pet.getWorld().playSound(shootLoc, Sound.ENTITY_GHAST_SHOOT, 1.5f, 0.5f);
                    shootMagmaChunk(owner, rpgOwner, pet, shootLoc, dir.multiply(1.5));
                }
            }

            // ==========================================
            // CÁC HÀM XỬ LÝ ĐỊA HÌNH CỤC BỘ
            // ==========================================
            private void handleTerrainTransformation(int blocksPerTick) {
                Location center = pet.getLocation().clone().subtract(0, 1, 0);
                for (int i = 0; i < blocksPerTick; i++) {
                    double xX = Math.random() * 8 - 4;
                    double zZ = Math.random() * 8 - 4;
                    Block b = center.clone().add(xX, 0, zZ).getBlock();

                    if (b.getType().isSolid() && b.getRelative(BlockFace.UP).getType().isAir() && b.getType() != Material.MAGMA_BLOCK) {
                        Location bLoc = b.getLocation();
                        transformationTimestamps.put(bLoc, System.currentTimeMillis());

                        if (!originalBlocks.containsKey(bLoc)) {
                            originalBlocks.put(bLoc, b.getType());
                            b.setType(Material.MAGMA_BLOCK);
                        }
                    }
                }
            }

            // Hàm tạo bãi dung nham ngay lập tức khi nhảy dậm đất
            private void instantTransformTerrain(Location loc, int radius) {
                Location center = loc.clone().subtract(0, 1, 0);
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block b = center.clone().add(x, 0, z).getBlock();
                        if (b.getType().isSolid() && b.getRelative(BlockFace.UP).getType().isAir() && b.getType() != Material.MAGMA_BLOCK) {
                            Location bLoc = b.getLocation();
                            transformationTimestamps.put(bLoc, System.currentTimeMillis());
                            if (!originalBlocks.containsKey(bLoc)) {
                                originalBlocks.put(bLoc, b.getType());
                                b.setType(Material.MAGMA_BLOCK);
                            }
                        }
                    }
                }
            }

            private void handleMagmaBlockDamage() {
                for (org.bukkit.entity.Entity e : pet.getNearbyEntities(10.0, 4.0, 10.0)) {
                    if (e instanceof LivingEntity && CombatManager.canTarget(owner, e)) {
                        LivingEntity target = (LivingEntity) e;
                        Block bUnder = target.getLocation().getBlock().getRelative(BlockFace.DOWN);

                        if (bUnder.getType() == Material.MAGMA_BLOCK && transformationTimestamps.containsKey(bUnder.getLocation())) {

                            // [ĐÃ SỬA]: Tính sát thương = 3% Máu tối đa của Golem
                            double golemMaxHp = pet.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                            double finalMagmaDmg = golemMaxHp * 0.03;

                            // Áp dụng sát thương kèm bọc Ghost L Block
                            owner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                            CombatManager.applySkillDamage(owner, target, finalMagmaDmg);
                            owner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                            target.setFireTicks(60); // Bốc cháy 3s
                        }
                    }
                }
            }

            private void revertAllBlocks() {
                for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
                    entry.getKey().getBlock().setType(entry.getValue());
                }
                originalBlocks.clear();
                transformationTimestamps.clear();
            }

        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ==========================================
    // ĐÒN ĐÁNH TAY AOE CỦA GOLEM
    // ==========================================
    @Override
    public double onAttack(Player owner, LivingEntity pet, LivingEntity target, double baseDamage) {
        Location hitLoc = target.getLocation();
        pet.getWorld().playSound(hitLoc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
        pet.getWorld().spawnParticle(Particle.LAVA, hitLoc.add(0, 1.0, 0), 3, 0.3, 0.3, 0.3, 0.05);

        RPGPlayer rpgOwner = RPGCore.getInstance().getPlayerManager().getRPGPlayer(owner.getUniqueId());

        for (org.bukkit.entity.Entity e : pet.getWorld().getNearbyEntities(hitLoc, 3.0, 3.0, 3.0)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(owner, e)) {
                LivingEntity aoeTarget = (LivingEntity) e;

                Vector kn = aoeTarget.getLocation().toVector().subtract(pet.getLocation().toVector()).normalize().multiply(0.8);
                kn.setY(1.2);
                aoeTarget.setVelocity(kn);
                aoeTarget.setFireTicks(100);

                if (!aoeTarget.equals(target) && rpgOwner != null) {
                    owner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                    CombatManager.applySkillDamage(owner, aoeTarget, baseDamage);
                    owner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                }
            }
        }
        return 1.0;
    }

    // ==========================================
    // RADAR DÒ QUÉT MỤC TIÊU
    // ==========================================
    private LivingEntity scanNearestTarget(Player owner, LivingEntity pet, double radius) {
        LivingEntity bestTarget = null;
        double closestDist = radius;
        for (org.bukkit.entity.Entity e : pet.getNearbyEntities(radius, radius / 2, radius)) {
            if (e instanceof LivingEntity && !e.isDead() && e.isValid()) {
                LivingEntity livingTarget = (LivingEntity) e;
                if (CombatManager.canTarget(owner, livingTarget)) {
                    double dist = pet.getLocation().distance(livingTarget.getLocation());
                    if (dist < closestDist && pet.hasLineOfSight(livingTarget)) {
                        closestDist = dist;
                        bestTarget = livingTarget;
                    }
                }
            }
        }
        return bestTarget;
    }

    // ==========================================
    // KỸ NĂNG NÉM MAGMA KHỔNG LỒ (ĐÃ TĂNG KÍCH CỠ)
    // ==========================================
    private void shootMagmaChunk(Player owner, RPGPlayer rpgOwner, LivingEntity pet, Location start, Vector dir) {
        new BukkitRunnable() {
            Location current = start.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 30 || !pet.isValid() || !owner.isOnline()) { this.cancel(); return; }

                current.add(dir);
                // Tạo một quả cầu lửa cực kỳ dày đặc để dễ nhìn thấy
                pet.getWorld().spawnParticle(Particle.FLAME, current, 30, 0.7, 0.7, 0.7, 0.05);
                pet.getWorld().spawnParticle(Particle.LAVA, current, 5, 0.4, 0.4, 0.4, 0);

                if (current.getBlock().getType().isSolid()) {
                    triggerMeteorExplosion(owner, rpgOwner, pet, current);
                    this.cancel(); return;
                }

                for (org.bukkit.entity.Entity e : pet.getWorld().getNearbyEntities(current, 1.5, 1.5, 1.5)) {
                    if (e instanceof LivingEntity && CombatManager.canTarget(owner, e)) {
                        triggerMeteorExplosion(owner, rpgOwner, pet, current);
                        this.cancel(); return;
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    private void triggerMeteorExplosion(Player owner, RPGPlayer rpgOwner, LivingEntity pet, Location loc) {
        pet.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        pet.getWorld().spawnParticle(Particle.LAVA, loc, 30, 2.0, 2.0, 2.0, 0.2);
        pet.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

        for (org.bukkit.entity.Entity e : pet.getWorld().getNearbyEntities(loc, 4.0, 4.0, 4.0)) {
            if (e instanceof LivingEntity && CombatManager.canTarget(owner, e)) {
                LivingEntity target = (LivingEntity) e;
                double calculatedDmg = CombatManager.calculatePetDamage(owner, rpgOwner, pet, target, 0);

                owner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                CombatManager.applySkillDamage(owner, target, calculatedDmg * 1.80);
                owner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                target.setFireTicks(100);
            }
        }
    }

    // ==========================================
    // LOGIC PHẢN SÁT THƯƠNG 10%
    // ==========================================
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity pet = (LivingEntity) event.getEntity();

        if (!pet.hasMetadata("Pet_Type") || !pet.getMetadata("Pet_Type").get(0).asString().equals(getId())) return;
        if (!pet.hasMetadata("Summoner_Pet")) return;

        if (event.getDamager() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getDamager();
            Player owner = RPGCore.getInstance().getServer().getPlayer(UUID.fromString(pet.getMetadata("Summoner_Pet").get(0).asString()));

            if (owner != null && CombatManager.canTarget(owner, attacker)) {
                double reflectedDmg = event.getDamage() * 0.10;
                owner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                CombatManager.applySkillDamage(owner, attacker, reflectedDmg);
                owner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                attacker.getWorld().spawnParticle(Particle.FLAME, attacker.getLocation().add(0, 1.0, 0), 2, 0.2, 0.2, 0.2, 0.02);
            }
        }
    }

    @Override
    public void onDeath(Player owner, LivingEntity pet) {
        pet.getWorld().spawnParticle(Particle.EXPLOSION, pet.getLocation(), 1);
        pet.getWorld().spawnParticle(Particle.LAVA, pet.getLocation(), 40, 2.0, 2.0, 2.0, 0.2);
        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1.5f, 0.5f);
    }
}