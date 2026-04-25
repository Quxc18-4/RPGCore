package vn.minecraft.rPGCore.skills.summoner.pets;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GreatWindSpiritProfile implements IPetProfile, Listener {

    public GreatWindSpiritProfile() {
        Bukkit.getPluginManager().registerEvents(this, RPGCore.getInstance());
    }

    @Override public String getId() { return "GreatWindSpirit"; }
    @Override public boolean isMagicDamage() { return true; }
    @Override public String getFusionTargetId() { return null; }
    @Override public String getFusionTargetName() { return null; }
    @Override public String getArchetype() { return "Swift"; }
    @Override public String getElement() { return "Wind"; }

    @Override
    public double getBaseFlatDamage(int level) {
        if (level == 10) return 150.0;
        return 25.0 + (level - 1) * 12.0;
    }

    @Override
    public double getWeaponScaling(int level) {
        return 1.5 + (level * 0.5);
    }

    @Override
    public void onSpawn(Player owner, LivingEntity pet, int level, double... extraArgs) {
        double petCount = extraArgs.length > 0 ? extraArgs[0] : 1;
        double finalHpBonus = extraArgs.length > 1 ? extraArgs[1] : 0;

        double scale = 1.5 + (petCount * 0.15);
        if (pet.getAttribute(Attribute.SCALE) != null) {
            pet.getAttribute(Attribute.SCALE).setBaseValue(scale);
        }

        double baseHp = 1500 + (level * 200) + (finalHpBonus * 0.8);
        if (pet.getAttribute(Attribute.MAX_HEALTH) != null) {
            pet.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseHp);
            pet.setHealth(baseHp);
        }

        final int attackSpeed = Math.max(5, 15 - (level - 1));

        // [FIX LỖI 1]: Đã XÓA BỎ bộ đếm maxLifespanTicks.
        // Thời gian sống giờ đây phụ thuộc hoàn toàn vào SummonFusionSkill (Giống Linh Lang)

        new BukkitRunnable() {
            int shootCooldown = 0;

            // State Machine cho Kỹ năng Sonic Beam
            boolean isCastingBeam = false;
            int castBeamTicks = 0;
            LivingEntity beamTarget = null;

            @Override
            public void run() {
                if (pet.isDead() || !pet.isValid()) {
                    this.cancel();
                    return;
                }

                Player petOwner = null;
                if (pet.hasMetadata("Summoner_Pet")) {
                    try {
                        petOwner = Bukkit.getPlayer(UUID.fromString(pet.getMetadata("Summoner_Pet").get(0).asString()));
                    } catch (Exception ignored) {}
                }

                RPGPlayer rpgOwner = null;
                if (petOwner != null) {
                    rpgOwner = RPGCore.getInstance().getPlayerManager().getRPGPlayer(petOwner.getUniqueId());
                }

                // ==========================================
                // KỸ NĂNG 2: THI TRIỂN TIA NĂNG LƯỢNG (BEAM) - Hồi chiêu 15s
                // ==========================================
                if (isCastingBeam) {
                    pet.setVelocity(new Vector(0, 0, 0)); // Bất động
                    org.bukkit.Location center = pet.getLocation().add(0, 1.5, 0);

                    pet.getWorld().spawnParticle(Particle.GUST, center, 2, 1.5, 1.5, 1.5, 0.1);
                    pet.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 1, 1.5, 0.5, 1.5, 0);

                    castBeamTicks++;

                    if (castBeamTicks >= 40) { // Sau 2 giây
                        isCastingBeam = false;
                        shootCooldown = attackSpeed;

                        if (beamTarget != null && beamTarget.isValid() && beamTarget.getWorld().equals(pet.getWorld())) {
                            pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 3.0f, 1.0f);
                            Vector dir = beamTarget.getLocation().add(0, 1, 0).toVector().subtract(center.toVector()).normalize();

                            double dist = center.distance(beamTarget.getLocation());
                            for (double d = 0; d < dist; d += 1.0) {
                                org.bukkit.Location pLoc = center.clone().add(dir.clone().multiply(d));
                                pet.getWorld().spawnParticle(Particle.SONIC_BOOM, pLoc, 1, 0, 0, 0, 0);
                            }

                            org.bukkit.Location impact = beamTarget.getLocation();
                            pet.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1);

                            for (org.bukkit.entity.Entity e : impact.getWorld().getNearbyEntities(impact, 3.0, 3.0, 3.0)) {
                                if (e instanceof LivingEntity && e != petOwner && !e.hasMetadata("Summoner_Pet")) {
                                    if (petOwner != null && rpgOwner != null) {
                                        double finalDmg = CombatManager.calculatePetDamage(petOwner, rpgOwner, pet, (LivingEntity)e, 0) * 3.0; // 300% Sát thương
                                        petOwner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                                        CombatManager.applySkillDamage(petOwner, (LivingEntity)e, finalDmg);
                                        petOwner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                                    }
                                }
                            }
                        }
                    }
                    return; // Trong lúc rặn chiêu thì không chạy logic bên dưới
                }

                // ==========================================
                // AI BẢO VỆ CHỦ NHÂN
                // ==========================================
                LivingEntity target = null;
                if (petOwner != null) {
                    target = vn.minecraft.rPGCore.skills.summoner.pets.SummonPetManager.getSmartTarget(petOwner);
                }

                if (pet instanceof Mob) {
                    ((Mob) pet).setTarget(target); // Ép xoay mặt
                }

                // ==========================================
                // LƠ LỬNG VÀ THẢ DIỀU
                // ==========================================
                Vector currentV = pet.getVelocity();
                double newY = currentV.getY();

                if (pet.isOnGround()) {
                    newY = 1.0;
                } else if (newY < -0.05) {
                    newY = -0.05;
                }

                Vector desiredXZ = new Vector(0, 0, 0);
                boolean isMoving = false;

                if (petOwner != null && petOwner.getWorld().equals(pet.getWorld())) {
                    double distToOwner = pet.getLocation().distance(petOwner.getLocation());
                    if (distToOwner > 25.0) {
                        pet.teleport(petOwner.getLocation());
                    } else if (distToOwner > 6.0) {
                        desiredXZ = petOwner.getLocation().toVector().subtract(pet.getLocation().toVector()).setY(0).normalize().multiply(0.25);
                        isMoving = true;
                    }
                }

                if (!isMoving && target != null && target.getWorld().equals(pet.getWorld())) {
                    double distToTarget = pet.getLocation().distance(target.getLocation());
                    if (distToTarget < 6.0) {
                        desiredXZ = pet.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0).normalize().multiply(0.2);
                        isMoving = true;
                    } else if (distToTarget > 18.0 && distToTarget < 25.0) {
                        desiredXZ = target.getLocation().toVector().subtract(pet.getLocation().toVector()).setY(0).normalize().multiply(0.2);
                        isMoving = true;
                    }
                }

                if (isMoving) {
                    pet.setVelocity(new Vector(desiredXZ.getX(), newY, desiredXZ.getZ()));
                } else {
                    pet.setVelocity(new Vector(currentV.getX() * 0.7, newY, currentV.getZ() * 0.7));
                }

                // ==========================================
                // KIỂM TRA VÀ XẢ ĐẠN
                // ==========================================
                if (target != null && !pet.isOnGround()) {
                    if (shootCooldown <= 0) {
                        org.bukkit.Location startLoc = pet.getLocation().add(0, pet.getHeight() / 2, 0);
                        org.bukkit.Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);

                        if (canSee(startLoc, targetLoc)) {

                            // 1. CHECK SKILL TIA BEAM (15s Cooldown)
                            long lastBeam = pet.hasMetadata("Beam_CD") ? pet.getMetadata("Beam_CD").get(0).asLong() : 0;
                            if (System.currentTimeMillis() - lastBeam >= 15000) {
                                double currentHp = pet.getHealth();
                                double maxHp = pet.getAttribute(Attribute.MAX_HEALTH).getValue();

                                if (currentHp > maxHp * 0.15) {
                                    pet.setHealth(currentHp - (maxHp * 0.10));
                                    pet.setMetadata("Beam_CD", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis()));

                                    isCastingBeam = true;
                                    castBeamTicks = 0;
                                    beamTarget = target;

                                    pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.5f, 0.5f);
                                    return;
                                }
                            }

                            // 2. CHECK SKILL LỖ ĐEN (6s Cooldown, 20% Cơ hội)
                            long lastBlackHole = pet.hasMetadata("BlackHole_CD") ? pet.getMetadata("BlackHole_CD").get(0).asLong() : 0;
                            boolean spawnBlackHole = false;

                            if (System.currentTimeMillis() - lastBlackHole >= 6000) {
                                if (Math.random() <= 0.20) {
                                    spawnBlackHole = true;
                                }
                            }

                            Vector dir = targetLoc.toVector().subtract(startLoc.toVector()).normalize();

                            if (spawnBlackHole) {
                                // [FIX LỖI 2]: Cấp thẻ Bypass để đạn không bị Event của Vanilla xóa
                                pet.setMetadata("Bypass_Vanilla_Check", new FixedMetadataValue(RPGCore.getInstance(), true));

                                org.bukkit.entity.WindCharge wc = pet.launchProjectile(org.bukkit.entity.WindCharge.class, dir.multiply(1.8));
                                wc.setMetadata("Pet_Projectile", new FixedMetadataValue(RPGCore.getInstance(), true));
                                wc.setMetadata("BlackHole_Seed", new FixedMetadataValue(RPGCore.getInstance(), true));
                                pet.setMetadata("BlackHole_CD", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis()));

                                pet.removeMetadata("Bypass_Vanilla_Check", RPGCore.getInstance()); // Rút thẻ ngay sau khi bắn

                                pet.getWorld().spawnParticle(Particle.SQUID_INK, startLoc, 10, 0.2, 0.2, 0.2, 0.05);
                                pet.getWorld().playSound(startLoc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.8f);
                            } else {
                                // 3. ĐÁNH THƯỜNG 3 TIA
                                List<LivingEntity> multiTargets = new ArrayList<>();
                                multiTargets.add(target);

                                for (org.bukkit.entity.Entity e : pet.getNearbyEntities(15, 15, 15)) {
                                    if (multiTargets.size() >= 3) break;
                                    if (e instanceof LivingEntity && e != petOwner && !e.hasMetadata("Summoner_Pet") && e != target) {
                                        if (canSee(startLoc, e.getLocation())) {
                                            multiTargets.add((LivingEntity) e);
                                        }
                                    }
                                }

                                pet.getWorld().playSound(startLoc, Sound.ENTITY_BREEZE_SHOOT, 0.6f, 1.5f);

                                for (LivingEntity t : multiTargets) {
                                    Vector tDir = t.getLocation().add(0, target.getHeight() / 2, 0).toVector().subtract(startLoc.toVector()).normalize();
                                    double dist = startLoc.distance(t.getLocation());

                                    for (double d = 0; d < dist; d += 0.5) {
                                        org.bukkit.Location pLoc = startLoc.clone().add(tDir.clone().multiply(d));
                                        pet.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, pLoc, 1, 0, 0, 0, 0);
                                    }

                                    if (petOwner != null && rpgOwner != null) {
                                        double finalDmg = CombatManager.calculatePetDamage(petOwner, rpgOwner, pet, t, 0) * 0.7; // 70% Sát thương
                                        petOwner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                                        CombatManager.applySkillDamage(petOwner, t, finalDmg);
                                        petOwner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                                    }
                                }
                            }
                            shootCooldown = attackSpeed;
                        } else {
                            shootCooldown = 10;
                        }
                    } else {
                        shootCooldown--;
                    }
                }
            }

            private boolean canSee(org.bukkit.Location start, org.bukkit.Location end) {
                Vector direction = end.toVector().subtract(start.toVector());
                double distance = direction.length();
                if (distance <= 1) return true;
                org.bukkit.util.RayTraceResult result = start.getWorld().rayTraceBlocks(start, direction.normalize(), distance, org.bukkit.FluidCollisionMode.NEVER, true);
                return result == null;
            }

        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    @Override public double onAttack(Player owner, LivingEntity pet, LivingEntity target, double baseDamage) { return 1.0; }
    @Override public void onDeath(Player owner, LivingEntity pet) { }

    @EventHandler
    public void onPetDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity pet = (LivingEntity) event.getEntity();
            if (pet.hasMetadata("Pet_Type") && pet.getMetadata("Pet_Type").get(0).asString().equals("GreatWindSpirit")) {
                if (pet.getHealth() - event.getFinalDamage() <= 0) {
                    event.setCancelled(true);
                    pet.getWorld().spawnParticle(Particle.CLOUD, pet.getLocation(), 50, 1.0, 1.0, 1.0, 0.2);
                    pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_BREEZE_DEATH, 1.5f, 0.8f);
                    pet.remove();
                }
            }
        }
    }

    // ==========================================
    // KIỂM DUYỆT ĐẠN RÁC VANILLA
    // ==========================================
    @EventHandler
    public void onVanillaBreezeShoot(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.WindCharge) {
            if (event.getEntity().getShooter() instanceof LivingEntity) {
                LivingEntity shooter = (LivingEntity) event.getEntity().getShooter();
                if (shooter.hasMetadata("Pet_Type") && shooter.getMetadata("Pet_Type").get(0).asString().equals("GreatWindSpirit")) {

                    // Nếu Tinh Linh Gió đang CÓ thẻ Bypass -> Cho phép đạn sinh ra
                    if (shooter.hasMetadata("Bypass_Vanilla_Check")) {
                        return;
                    }

                    // Nếu không có thẻ Bypass HOẶC không có mác Pet_Projectile -> HỦY!
                    if (!event.getEntity().hasMetadata("Pet_Projectile")) {
                        event.setCancelled(true);
                        event.getEntity().remove();
                    }
                }
            }
        }
    }

    // ==========================================
    // LOGIC KỸ NĂNG 1: QUẢ CẦU KHÍ NÉN (Thay cho Lỗ Đen)
    // ==========================================
    @EventHandler
    public void onBlackHoleSeedHit(ProjectileHitEvent event) {
        if (event.getEntity().hasMetadata("BlackHole_Seed")) {
            event.setCancelled(true);
            org.bukkit.entity.Projectile proj = event.getEntity();
            org.bukkit.Location hitLoc = proj.getLocation();

            LivingEntity pet = null;
            if (proj.getShooter() instanceof LivingEntity) {
                pet = (LivingEntity) proj.getShooter();
            }
            proj.remove();

            Player owner = null;
            RPGPlayer rpgOwner = null;
            if (pet != null && pet.hasMetadata("Summoner_Pet")) {
                String ownerId = pet.getMetadata("Summoner_Pet").get(0).asString();
                owner = Bukkit.getPlayer(UUID.fromString(ownerId));
                if (owner != null) {
                    rpgOwner = RPGCore.getInstance().getPlayerManager().getRPGPlayer(owner.getUniqueId());
                }
            }

            final Player fOwner = owner;
            final RPGPlayer fRpgOwner = rpgOwner;
            final LivingEntity fPet = pet;

            // 1. Gây 140% Sát thương ngay khi chạm đích
            hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
            for (org.bukkit.entity.Entity e : hitLoc.getWorld().getNearbyEntities(hitLoc, 6.0, 6.0, 6.0)) {
                if (e instanceof LivingEntity && !e.hasMetadata("Summoner_Pet") && !(e instanceof Player)) {
                    if (fOwner != null && fRpgOwner != null) {
                        double finalDmg = CombatManager.calculatePetDamage(fOwner, fRpgOwner, fPet, (LivingEntity)e, 0) * 1.4;
                        fOwner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                        CombatManager.applySkillDamage(fOwner, (LivingEntity)e, finalDmg);
                        fOwner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                    }
                }
            }

            // 2. Tạo Vòng lặp Hút mục tiêu trong 1.5s (30 ticks) và Nổ tung
            new BukkitRunnable() {
                int ticks = 0;
                // Nâng tâm lên 1.0 để Quả cầu khí nổi trên mặt đất
                org.bukkit.Location center = hitLoc.clone().add(0, 1.0, 0);

                @Override
                public void run() {
                    // KẾT THÚC: Nổ tung và hất văng (120% Sát thương)
                    if (ticks >= 30) {
                        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
                        center.getWorld().spawnParticle(Particle.GUST_EMITTER_LARGE, center, 1); // Thêm chấn động gió
                        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);

                        for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, 6.0, 6.0, 6.0)) {
                            if (e instanceof LivingEntity && !e.hasMetadata("Summoner_Pet") && !(e instanceof Player)) {
                                LivingEntity target = (LivingEntity) e;
                                Vector push = target.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.8).setY(0.6);
                                target.setVelocity(push);

                                if (fOwner != null && fRpgOwner != null) {
                                    double finalDmg = CombatManager.calculatePetDamage(fOwner, fRpgOwner, fPet, target, 0) * 1.2;
                                    fOwner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                                    CombatManager.applySkillDamage(fOwner, target, finalDmg);
                                    fOwner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                                }
                            }
                        }
                        this.cancel();
                        return;
                    }

                    // ==========================================
                    // VISUAL: QUẢ CẦU KHÍ NÉN (Thay vì Lỗ Đen)
                    // ==========================================
                    // 1. Lõi cầu: Màu Trắng đục (Đại diện cho áp suất không khí cao)
                    Particle.DustOptions whiteDust = new Particle.DustOptions(org.bukkit.Color.WHITE, 1.5f);
                    for (int i = 0; i < 20; i++) {
                        double phi = Math.acos(2 * Math.random() - 1), theta = 2 * Math.PI * Math.random();
                        center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0.4 * Math.sin(phi) * Math.cos(theta), 0.4 * Math.sin(phi) * Math.sin(theta), 0.4 * Math.cos(phi)), 1, whiteDust);
                    }

                    // 2. Hai vòng đai xoáy lốc: Màu Xanh lơ (Light Blue / Cyan)
                    Particle.DustOptions cyanDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(173, 216, 230), 1.2f);
                    double z1 = 1.2 * Math.sin(ticks * 0.4);
                    center.getWorld().spawnParticle(Particle.DUST, center.clone().add(1.2 * Math.cos(ticks * 0.4), z1 * Math.sin(Math.toRadians(40)), z1 * Math.cos(Math.toRadians(40))), 1, cyanDust);
                    double z2 = 1.8 * Math.sin(-ticks * 0.3);
                    center.getWorld().spawnParticle(Particle.DUST, center.clone().add(1.8 * Math.cos(-ticks * 0.3), z2 * Math.sin(Math.toRadians(-40)), z2 * Math.cos(Math.toRadians(-40))), 1, cyanDust);

                    // 3. Âm thanh: Đổi tiếng Beacon thành tiếng gió rít xé gió
                    if (ticks % 10 == 0) {
                        center.getWorld().playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.0f, 1.5f);
                    }

                    // ==========================================
                    // LỰC HÚT: Có tính toán Kháng hiệu ứng
                    // ==========================================
                    for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, 6.0, 6.0, 6.0)) {
                        if (e instanceof LivingEntity && !e.hasMetadata("Summoner_Pet") && !(e instanceof Player)) {
                            LivingEntity target = (LivingEntity) e;
                            Vector pull = center.toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.2);

                            // Gọi Manager để lấy vận tốc sau khi đã trừ đi % Kháng lực kéo
                            target.setVelocity(target.getVelocity().add(vn.minecraft.rPGCore.managers.StatusEffectManager.calculateResistedVelocity(target, pull)));
                        }
                    }
                    ticks++;
                }
            }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
        }
    }

    @EventHandler
    public void onWindChargeDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager().hasMetadata("Pet_Projectile")) {
            event.setCancelled(true);
        }
    }
}