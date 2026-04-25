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

import java.util.UUID;

public class WindSpiritProfile implements IPetProfile, Listener {

    public WindSpiritProfile() {
        Bukkit.getPluginManager().registerEvents(this, RPGCore.getInstance());
    }

    @Override public String getId() { return "WindSpirit"; }
    @Override public boolean isMagicDamage() { return true; }
    @Override public String getFusionTargetId() { return "GreatWindSpirit"; }
    @Override public String getFusionTargetName() { return "Đại Tinh Linh Gió"; }
    @Override public String getArchetype() { return "Swift"; }
    @Override public String getElement() { return "Wind"; }

    @Override
    public double getBaseFlatDamage(int level) {
        return 12.0 + (level - 1) * 6.0;
    }

    @Override
    public double getWeaponScaling(int level) {
        return 0.8 + (level * 0.25);
    }

    @Override
    public void onSpawn(Player owner, LivingEntity pet, int level, double... extraArgs) {
        double baseHp = 700 + (level * 80);

        if (pet.getAttribute(Attribute.MAX_HEALTH) != null) {
            pet.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseHp);
            pet.setHealth(baseHp);
        }

        final int attackSpeed = Math.max(8, 20 - (int)((level - 1) * 1.33));

        // [ĐÃ CẬP NHẬT]: Giới hạn tuổi thọ mới (40s + 5s mỗi cấp)
        // [MỐC ĐỘT PHÁ CẤP 10]: Tăng vọt lên 90s thay vì 85s
        int durationSecs = (level == 10) ? 90 : 40 + ((level - 1) * 5);
        final int maxLifespanTicks = durationSecs * 20;

        new BukkitRunnable() {
            int shootCooldown = 0;
            int ticksLived = 0;

            @Override
            public void run() {
                if (pet.isDead() || !pet.isValid()) {
                    this.cancel();
                    return;
                }

                // ==========================================
                // KIỂM TRA TUỔI THỌ (TỰ ĐỘNG BỐC HƠI)
                // ==========================================
                ticksLived++;
                if (ticksLived >= maxLifespanTicks) {
                    pet.getWorld().spawnParticle(Particle.CLOUD, pet.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                    pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_BREEZE_DEATH, 1.0f, 1.0f);
                    pet.remove(); // [BIẾN MẤT HẲN KHÔNG TÌ VẾT]
                    this.cancel();
                    return;
                }

                // 1. TÌM CHỦ NHÂN TRƯỚC
                Player petOwner = null;
                if (pet.hasMetadata("Summoner_Pet")) {
                    try {
                        petOwner = Bukkit.getPlayer(UUID.fromString(pet.getMetadata("Summoner_Pet").get(0).asString()));
                    } catch (Exception ignored) {}
                }

                // 2. TỪ CHỦ NHÂN, ĐỌC LỆNH MỤC TIÊU TỪ TRẠM CHỈ HUY
                LivingEntity target = null;
                if (petOwner != null) {
                    target = vn.minecraft.rPGCore.skills.summoner.pets.SummonPetManager.getSmartTarget(petOwner);
                }

                // 3. ÉP VANILLA MOB XOAY MẶT VỀ ĐÚNG MỤC TIÊU ĐÓ
                if (pet instanceof Mob) {
                    ((Mob) pet).setTarget(target);
                }

                if (pet.hasMetadata("Summoner_Pet")) {
                    try {
                        petOwner = Bukkit.getPlayer(UUID.fromString(pet.getMetadata("Summoner_Pet").get(0).asString()));
                    } catch (Exception ignored) {}
                }

                Vector currentV = pet.getVelocity();
                double newY = currentV.getY();

                // 1. CƠ CHẾ LƠ LỬNG (BOBBING)
                if (pet.isOnGround()) {
                    newY = 1.2;
                } else if (newY < -0.1) {
                    newY = -0.1;
                }

                // 2. AI BAY THEO CHỦ VÀ THẢ DIỀU
                Vector desiredXZ = new Vector(0, 0, 0);
                boolean isMoving = false;

                if (petOwner != null && petOwner.getWorld().equals(pet.getWorld())) {
                    double distToOwner = pet.getLocation().distance(petOwner.getLocation());
                    if (distToOwner > 20.0) {
                        pet.teleport(petOwner.getLocation());
                    } else if (distToOwner > 6.0) {
                        desiredXZ = petOwner.getLocation().toVector().subtract(pet.getLocation().toVector()).setY(0).normalize().multiply(0.25);
                        isMoving = true;
                    }
                }

                if (!isMoving && target != null && target.getWorld().equals(pet.getWorld())) {
                    double distToTarget = pet.getLocation().distance(target.getLocation());
                    if (distToTarget < 5.0) {
                        desiredXZ = pet.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0).normalize().multiply(0.2);
                        isMoving = true;
                    } else if (distToTarget > 15.0 && distToTarget < 25.0) {
                        desiredXZ = target.getLocation().toVector().subtract(pet.getLocation().toVector()).setY(0).normalize().multiply(0.2);
                        isMoving = true;
                    }
                }

                if (isMoving) {
                    pet.setVelocity(new Vector(desiredXZ.getX(), newY, desiredXZ.getZ()));
                } else {
                    pet.setVelocity(new Vector(currentV.getX() * 0.7, newY, currentV.getZ() * 0.7));
                }

                // 3. XẢ ĐẠN
                if (target != null && !pet.isOnGround()) {
                    if (shootCooldown <= 0) {
                        org.bukkit.Location startLoc = pet.getLocation().add(0, pet.getHeight() / 2, 0);
                        org.bukkit.Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);

                        if (canSee(startLoc, targetLoc)) {
                            Vector dir = targetLoc.toVector().subtract(startLoc.toVector()).normalize();

                            long lastEnhanced = pet.hasMetadata("Breeze_Enhanced_CD") ? pet.getMetadata("Breeze_Enhanced_CD").get(0).asLong() : 0;
                            boolean isEnhanced = false;

                            if (System.currentTimeMillis() - lastEnhanced >= 6000) {
                                if (Math.random() <= 0.15) {
                                    isEnhanced = true;
                                }
                            }

                            if (isEnhanced) {
                                org.bukkit.entity.WindCharge wc = pet.launchProjectile(org.bukkit.entity.WindCharge.class, dir.multiply(1.5));
                                wc.setMetadata("Pet_Projectile", new FixedMetadataValue(RPGCore.getInstance(), true));
                                wc.setMetadata("Enhanced_WindCharge", new FixedMetadataValue(RPGCore.getInstance(), true));
                                pet.setMetadata("Breeze_Enhanced_CD", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis()));

                                pet.getWorld().spawnParticle(Particle.GUST, startLoc, 3, 0.3, 0.3, 0.3, 0.05);
                                pet.getWorld().playSound(startLoc, Sound.ENTITY_BREEZE_SHOOT, 1.5f, 0.5f);
                            } else {
                                pet.getWorld().playSound(startLoc, Sound.ENTITY_BREEZE_SHOOT, 0.5f, 1.8f);
                                double dist = startLoc.distance(targetLoc);
                                for (double d = 0; d < dist; d += 0.5) {
                                    org.bukkit.Location pLoc = startLoc.clone().add(dir.clone().multiply(d));
                                    pet.getWorld().spawnParticle(Particle.ENCHANTED_HIT, pLoc, 1, 0, 0, 0, 0);
                                }

                                if (petOwner != null && petOwner.isOnline()) {
                                    RPGPlayer rpgOwner = RPGCore.getInstance().getPlayerManager().getRPGPlayer(petOwner.getUniqueId());
                                    double finalDmg = CombatManager.calculatePetDamage(petOwner, rpgOwner, pet, target, 0);

                                    petOwner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                                    CombatManager.applySkillDamage(petOwner, target, finalDmg);
                                    petOwner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
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
    @Override public void onDeath(Player owner, LivingEntity pet) { } // Xử lý thủ công bên dưới

    // ==========================================
    // KHÓA HOẠT ẢNH CHẾT VANILLA BẰNG CÁCH XÓA HẲN
    // ==========================================
    @EventHandler
    public void onPetDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity pet = (LivingEntity) event.getEntity();
            if (pet.hasMetadata("Pet_Type") && pet.getMetadata("Pet_Type").get(0).asString().equals("WindSpirit")) {
                // Nếu sát thương đủ làm chết Tinh Linh
                if (pet.getHealth() - event.getFinalDamage() <= 0) {
                    event.setCancelled(true);
                    pet.getWorld().spawnParticle(Particle.CLOUD, pet.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                    pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_BREEZE_DEATH, 1.0f, 1.0f);
                    pet.remove(); // Bốc hơi ngay lập tức!
                }
            }
        }
    }

    @EventHandler
    public void onVanillaBreezeShoot(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.WindCharge) {
            if (event.getEntity().getShooter() instanceof LivingEntity) {
                LivingEntity shooter = (LivingEntity) event.getEntity().getShooter();
                if (shooter.hasMetadata("Pet_Type") && shooter.getMetadata("Pet_Type").get(0).asString().equals("WindSpirit")) {
                    if (!event.getEntity().hasMetadata("Pet_Projectile")) {
                        event.setCancelled(true);
                        event.getEntity().remove();
                    }
                }
            }
        }
    }

    // ==========================================
    // LOGIC NỔ DIỆN RỘNG (GÂY 140% SÁT THƯƠNG)
    // ==========================================
    @EventHandler
    public void onWindChargeHit(ProjectileHitEvent event) {
        if (event.getEntity().hasMetadata("Enhanced_WindCharge")) {
            event.setCancelled(true);
            org.bukkit.entity.Projectile proj = event.getEntity();
            org.bukkit.Location hitLoc = proj.getLocation();

            // Tìm chủ nhân để tính sát thương
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

            if (event.getHitEntity() != null) {
                proj.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 1);
                proj.getWorld().playSound(hitLoc, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.0f);

                for (org.bukkit.entity.Entity e : proj.getWorld().getNearbyEntities(hitLoc, 4.0, 4.0, 4.0)) {
                    if (e instanceof LivingEntity && !e.hasMetadata("Summoner_Pet") && !(e instanceof Player)) {
                        LivingEntity target = (LivingEntity) e;
                        Vector push = e.getLocation().toVector().subtract(hitLoc.toVector()).normalize().multiply(1.5).setY(0.5);
                        e.setVelocity(push);

                        // [MỚI]: Gây 140% Sát thương trực tiếp
                        if (owner != null && rpgOwner != null) {
                            double finalDmg = CombatManager.calculatePetDamage(owner, rpgOwner, pet, target, 0) * 1.4;
                            owner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                            CombatManager.applySkillDamage(owner, target, finalDmg);
                            owner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                        }
                    }
                }
            } else if (event.getHitBlock() != null) {
                proj.getWorld().spawnParticle(Particle.CLOUD, hitLoc, 50, 2.0, 0.5, 2.0, 0.2);
                proj.getWorld().playSound(hitLoc, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.5f);

                for (org.bukkit.entity.Entity e : proj.getWorld().getNearbyEntities(hitLoc, 3.0, 3.0, 3.0)) {
                    if (e instanceof LivingEntity && !e.hasMetadata("Summoner_Pet") && !(e instanceof Player)) {
                        LivingEntity target = (LivingEntity) e;
                        e.setVelocity(new Vector(0, 1.2, 0));

                        // [MỚI]: Gây 140% Sát thương trực tiếp
                        if (owner != null && rpgOwner != null) {
                            double finalDmg = CombatManager.calculatePetDamage(owner, rpgOwner, pet, target, 0) * 1.4;
                            owner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                            CombatManager.applySkillDamage(owner, target, finalDmg);
                            owner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onWindChargeDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager().hasMetadata("Pet_Projectile")) {
            event.setCancelled(true);

            // Cản những sát thương rác còn sót lại
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();
                org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) event.getDamager();
                LivingEntity pet = (LivingEntity) proj.getShooter();

                if (target instanceof Player) return;

                if (pet != null && pet.hasMetadata("Summoner_Pet") && !proj.hasMetadata("Enhanced_WindCharge")) {
                    String ownerId = pet.getMetadata("Summoner_Pet").get(0).asString();
                    Player owner = Bukkit.getPlayer(UUID.fromString(ownerId));

                    if (owner != null) {
                        RPGPlayer rpgOwner = RPGCore.getInstance().getPlayerManager().getRPGPlayer(owner.getUniqueId());
                        double finalDmg = CombatManager.calculatePetDamage(owner, rpgOwner, pet, target, 0);

                        owner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                        CombatManager.applySkillDamage(owner, target, finalDmg);
                        owner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                    }
                }
            }
        }
    }
}