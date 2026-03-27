package vn.minecraft.rPGCore.skills.summoner.pets;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

public class GrandFireSpiritProfile implements IPetProfile {

    @Override
    public String getId() { return "GrandFireSpirit"; }

    @Override
    public boolean isMagicDamage() { return true; }

    @Override
    public String getFusionTargetId() { return null; } // Đỉnh cao hệ Lửa, không dung hợp nữa (Trừ khi ghép Golem)

    @Override
    public String getFusionTargetName() { return null; }

    @Override
    public double getBaseFlatDamage(int level) {
        // Sát thương phẳng của Tier 2 Caster (Cao hơn Fire Spirit)
        if (level == 10) return 180.0;
        return 40.0 + (level - 1) * 15.0;
    }

    @Override
    public double getWeaponScaling(int level) {
        // Tỷ lệ scale sức mạnh ma thuật tương đương GreatWindSpirit
        return 1.5 + (level * 0.5);
    }

    @Override
    public String getArchetype() { return "Caster"; }

    @Override
    public String getElement() { return "Fire"; }

    @Override
    public void onSpawn(Player owner, LivingEntity pet, int level, double... extraArgs) {
        double petCount = extraArgs.length > 0 ? extraArgs[0] : 1;
        double finalHpBonus = extraArgs.length > 1 ? extraArgs[1] : 0;

        // Scale bự ra (Phải dùng Mob Vanilla có hỗ trợ Scale, VD: Magma Cube, Slime, Zombie...)
        double scale = 2.0 + (petCount * 0.15);
        if (pet.getAttribute(Attribute.SCALE) != null) {
            pet.getAttribute(Attribute.SCALE).setBaseValue(scale);
        }

        // Máu siêu trâu cho Đại Tinh Linh
        double baseHp = 2000 + (level * 250) + (finalHpBonus * 0.8);
        if (pet.getAttribute(Attribute.MAX_HEALTH) != null) {
            pet.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseHp);
            pet.setHealth(baseHp);
        }

        pet.setMetadata("Summoner_Pet", new FixedMetadataValue(RPGCore.getInstance(), owner.getUniqueId().toString()));
        pet.setMetadata("Pet_Type", new FixedMetadataValue(RPGCore.getInstance(), getId()));

        // Hiệu ứng triệu hồi hoành tráng
        pet.getWorld().spawnParticle(Particle.FLAME, pet.getLocation(), 100, 1.5, 2.0, 1.5, 0.1);
        pet.getWorld().spawnParticle(Particle.LAVA, pet.getLocation(), 30, 1.0, 1.0, 1.0, 0.1);
        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.8f);

        RPGPlayer rpgOwner = RPGCore.getInstance().getPlayerManager().getRPGPlayer(owner.getUniqueId());

        // ==========================================
        // NÃO BỘ: ĐẠI TINH LINH LỬA
        // ==========================================
        new BukkitRunnable() {
            int attackCooldown = 0; // Đạn 5 tia (2s / 40 ticks)
            int aoeCooldown = 60;   // Hào quang r=5 (3s / 60 ticks)
            long lastMeteorTime = 0; // Kỹ năng Thiên thạch (15s)

            @Override
            public void run() {
                if (!pet.isValid() || pet.isDead() || !owner.isOnline() || rpgOwner == null) {
                    this.cancel();
                    return;
                }

                // 1. Kìm chân (Nếu dùng Blaze làm model)
                if (!pet.isOnGround() && pet.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getType().isAir()) {
                    pet.setVelocity(pet.getVelocity().setY(-0.4));
                }

                if (Math.random() > 0.5) {
                    pet.getWorld().spawnParticle(Particle.FLAME, pet.getLocation().add(0, 1.5, 0), 2, 0.5, 0.5, 0.5, 0.05);
                }

                // ==========================================
                // KỸ NĂNG 1: HÀO QUANG LUYỆN NGỤC (Mỗi 3s, R=5)
                // ==========================================
                if (aoeCooldown > 0) {
                    aoeCooldown--;
                } else {
                    aoeCooldown = 60; // Reset 3 giây
                    pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.5f);
                    pet.getWorld().playSound(pet.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.5f, 0.5f);

                    // Vẽ 2 vòng tròn lửa lồng nhau (R=3 và R=5)
                    drawFireCircle(pet.getLocation(), 3.0);
                    drawFireCircle(pet.getLocation(), 5.0);

                    for (org.bukkit.entity.Entity e : pet.getNearbyEntities(5.0, 4.0, 5.0)) {
                        if (e instanceof LivingEntity && !e.isDead() && e.isValid()) {
                            LivingEntity hitTarget = (LivingEntity) e;
                            if (CombatManager.canTarget(owner, hitTarget)) {
                                double baseDmg = CombatManager.calculatePetDamage(owner, rpgOwner, pet, hitTarget, 0);
                                double finalAoeDmg = baseDmg * 0.50; // 50% Sát thương

                                owner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                                CombatManager.applySkillDamage(owner, hitTarget, finalAoeDmg);
                                owner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                                hitTarget.setFireTicks(80); // Cháy 4 giây
                            }
                        }
                    }
                }

                // ==========================================
                // TÌM MỤC TIÊU CHO ĐẠN & THIÊN THẠCH
                // ==========================================
                LivingEntity targetToShoot = null;
                double closestDist = 25.0;

                for (org.bukkit.entity.Entity e : pet.getNearbyEntities(25, 15, 25)) {
                    if (e instanceof LivingEntity && !e.isDead() && e.isValid()) {
                        LivingEntity livingTarget = (LivingEntity) e;
                        if (CombatManager.canTarget(owner, livingTarget)) {
                            double dist = pet.getLocation().distance(livingTarget.getLocation());
                            if (dist < closestDist && pet.hasLineOfSight(livingTarget)) {
                                closestDist = dist;
                                targetToShoot = livingTarget;
                            }
                        }
                    }
                }

                if (targetToShoot != null) {
                    Location shootLoc = pet.getLocation().add(0, pet.getHeight() * 0.8, 0);
                    Location targetCenter = targetToShoot.getLocation().add(0, targetToShoot.getHeight() / 2, 0);

                    // ==========================================
                    // KỸ NĂNG 2: THIÊN THẠCH (METEOR) - Cooldown 15s
                    // ==========================================
                    if (System.currentTimeMillis() - lastMeteorTime >= 15000) {
                        lastMeteorTime = System.currentTimeMillis();

                        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_GHAST_WARN, 1.5f, 0.8f);

                        // Khóa mục tiêu và gọi thiên thạch rớt xuống từ độ cao 15 block
                        Location meteorSpawn = targetToShoot.getLocation().add(0, 15, 0);
                        Vector dropDir = new Vector(0, -1, 0); // Rớt thẳng đứng

                        // Bắn viên đạn đặc biệt (Thiên thạch)
                        shootMeteor(owner, rpgOwner, pet, meteorSpawn, dropDir);

                        // Hãm lại không cho bắn đạn thường ngay lập tức để tránh trùng animation
                        attackCooldown = 20;
                        return;
                    }

                    // ==========================================
                    // KỸ NĂNG 3: BẮN ĐẠN 5 TIA CHÙM (Mỗi 2s)
                    // ==========================================
                    if (attackCooldown > 0) {
                        attackCooldown--;
                    } else {
                        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.5f, 1.2f);

                        Vector centerDir = targetCenter.toVector().subtract(shootLoc.toVector()).normalize().multiply(1.2);

                        // Bẻ vector thành 5 hướng (Góc: -30, -15, 0, 15, 30)
                        shootCustomFireball(owner, rpgOwner, pet, shootLoc, rotateVectorAroundY(centerDir, -30));
                        shootCustomFireball(owner, rpgOwner, pet, shootLoc, rotateVectorAroundY(centerDir, -15));
                        shootCustomFireball(owner, rpgOwner, pet, shootLoc, centerDir);
                        shootCustomFireball(owner, rpgOwner, pet, shootLoc, rotateVectorAroundY(centerDir, 15));
                        shootCustomFireball(owner, rpgOwner, pet, shootLoc, rotateVectorAroundY(centerDir, 30));

                        attackCooldown = 40;
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ==========================================
    // CÁC HÀM TOÁN HỌC & ĐẠN ĐẠO
    // ==========================================
    private void drawFireCircle(Location center, double radius) {
        for (int i = 0; i < 360; i += 15) {
            double angle = Math.toRadians(i);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(x, 0.2, z), 1, 0, 0.1, 0, 0.01);
        }
    }

    private Vector rotateVectorAroundY(Vector vector, double degrees) {
        double rad = Math.toRadians(degrees);
        double currentX = vector.getX();
        double currentZ = vector.getZ();
        double cosine = Math.cos(rad);
        double sine = Math.sin(rad);
        return new Vector((cosine * currentX - sine * currentZ), vector.getY(), (sine * currentX + cosine * currentZ));
    }

    // Đạn 5 tia (25% ST mỗi viên)
    private void shootCustomFireball(Player owner, RPGPlayer rpgOwner, LivingEntity pet, Location startLoc, Vector dir) {
        new BukkitRunnable() {
            Location current = startLoc.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 30 || !pet.isValid() || !owner.isOnline()) { this.cancel(); return; }

                current.add(dir);
                pet.getWorld().spawnParticle(Particle.FLAME, current, 4, 0.2, 0.2, 0.2, 0.02);
                if (ticks % 2 == 0) pet.getWorld().spawnParticle(Particle.SMOKE, current, 1, 0, 0, 0, 0);

                if (current.getBlock().getType().isSolid()) {
                    pet.getWorld().spawnParticle(Particle.LAVA, current, 5, 0.1, 0.1, 0.1, 0.1);
                    this.cancel();
                    return;
                }

                for (org.bukkit.entity.Entity e : pet.getWorld().getNearbyEntities(current, 0.8, 0.8, 0.8)) {
                    if (e instanceof LivingEntity && !e.isDead() && e.isValid()) {
                        LivingEntity hitTarget = (LivingEntity) e;
                        if (CombatManager.canTarget(owner, hitTarget)) {
                            // 25% Sát thương mỗi viên
                            double baseDmg = CombatManager.calculatePetDamage(owner, rpgOwner, pet, hitTarget, 0);
                            double finalShotgunDmg = baseDmg * 0.25;

                            owner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                            CombatManager.applySkillDamage(owner, hitTarget, finalShotgunDmg);
                            owner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                            hitTarget.setFireTicks(60);
                            pet.getWorld().spawnParticle(Particle.LAVA, current, 10, 0.3, 0.3, 0.3, 0.1);
                            pet.getWorld().playSound(current, Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.2f);

                            this.cancel();
                            return;
                        }
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // Thiên thạch rớt từ trên trời (160% ST, AoE R=4)
    private void shootMeteor(Player owner, RPGPlayer rpgOwner, LivingEntity pet, Location startLoc, Vector dir) {
        new BukkitRunnable() {
            Location current = startLoc.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 40 || !pet.isValid() || !owner.isOnline()) { this.cancel(); return; }

                current.add(dir.clone().multiply(1.5)); // Rớt nhanh

                // Cầu lửa khổng lồ
                pet.getWorld().spawnParticle(Particle.FLAME, current, 20, 0.8, 0.8, 0.8, 0.05);
                pet.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, current, 10, 0.5, 0.5, 0.5, 0.02);

                boolean hitGround = current.getBlock().getType().isSolid();
                boolean hitEntity = false;

                if (!hitGround) {
                    for (org.bukkit.entity.Entity e : pet.getWorld().getNearbyEntities(current, 1.5, 1.5, 1.5)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(owner, e)) {
                            hitEntity = true; break;
                        }
                    }
                }

                if (hitGround || hitEntity) {
                    // NỔ BÙM!
                    pet.getWorld().spawnParticle(Particle.EXPLOSION, current, 1);
                    pet.getWorld().spawnParticle(Particle.LAVA, current, 50, 2.0, 2.0, 2.0, 0.2);
                    pet.getWorld().playSound(current, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);

                    for (org.bukkit.entity.Entity e : pet.getWorld().getNearbyEntities(current, 4.0, 4.0, 4.0)) {
                        if (e instanceof LivingEntity && !e.isDead() && e.isValid()) {
                            LivingEntity hitTarget = (LivingEntity) e;
                            if (CombatManager.canTarget(owner, hitTarget)) {
                                // 160% Sát thương
                                double baseDmg = CombatManager.calculatePetDamage(owner, rpgOwner, pet, hitTarget, 0);
                                double finalMeteorDmg = baseDmg * 1.60;

                                owner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                                CombatManager.applySkillDamage(owner, hitTarget, finalMeteorDmg);
                                owner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());

                                hitTarget.setFireTicks(100); // Cháy 5s
                            }
                        }
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    @Override
    public double onAttack(Player owner, LivingEntity pet, LivingEntity target, double baseDamage) {
        return 1.0;
    }

    @Override
    public void onDeath(Player owner, LivingEntity pet) {
        pet.getWorld().spawnParticle(Particle.EXPLOSION, pet.getLocation(), 2);
        pet.getWorld().spawnParticle(Particle.LAVA, pet.getLocation(), 100, 1.5, 1.5, 1.5, 0.2);
        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);
    }
}