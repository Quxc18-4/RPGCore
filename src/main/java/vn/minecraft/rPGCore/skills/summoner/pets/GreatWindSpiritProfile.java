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
        // [ĐÃ CẬP NHẬT]: Đọc số lượng thú hiến tế và buff Máu truyền vào
        double petCount = extraArgs.length > 0 ? extraArgs[0] : 1;
        double finalHpBonus = extraArgs.length > 1 ? extraArgs[1] : 0;

        // [TĂNG KÍCH THƯỚC (SCALE)]: Giống y chang AlphaWolf
        double scale = 1.5 + (petCount * 0.15);
        if (pet.getAttribute(Attribute.SCALE) != null) {
            pet.getAttribute(Attribute.SCALE).setBaseValue(scale);
        }

        // [KẾ THỪA MÁU]
        double baseHp = 1500 + (level * 200) + (finalHpBonus * 0.8);
        if (pet.getAttribute(Attribute.MAX_HEALTH) != null) {
            pet.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseHp);
            pet.setHealth(baseHp);
        }

        final int attackSpeed = Math.max(5, 15 - (level - 1));

        int durationSecs = (level == 10) ? 105 : 60 + ((level - 1) * 5);
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

                ticksLived++;
                if (ticksLived >= maxLifespanTicks) {
                    pet.getWorld().spawnParticle(Particle.CLOUD, pet.getLocation(), 50, 1.0, 1.0, 1.0, 0.2);
                    pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_BREEZE_DEATH, 1.5f, 0.8f);
                    pet.remove();
                    this.cancel();
                    return;
                }

                LivingEntity target = null;
                if (pet instanceof Mob) {
                    target = ((Mob) pet).getTarget();
                }

                Player petOwner = null;
                if (pet.hasMetadata("Summoner_Pet")) {
                    try {
                        petOwner = Bukkit.getPlayer(UUID.fromString(pet.getMetadata("Summoner_Pet").get(0).asString()));
                    } catch (Exception ignored) {}
                }

                Vector currentV = pet.getVelocity();
                double newY = currentV.getY();

                // 1. LƠ LỬNG NHẤP NHÔ
                if (pet.isOnGround()) {
                    newY = 1.8;
                } else if (newY < -0.05) {
                    newY = -0.05;
                }

                // 2. AI THẢ DIỀU
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

                // 3. ĐẠI LIÊN XẢ ĐẠN
                if (target != null && !pet.isOnGround()) {
                    if (shootCooldown <= 0) {
                        org.bukkit.Location startLoc = pet.getLocation().add(0, pet.getHeight() / 2, 0);
                        org.bukkit.Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);

                        if (canSee(startLoc, targetLoc)) {
                            Vector dir = targetLoc.toVector().subtract(startLoc.toVector()).normalize();

                            long lastTornado = pet.hasMetadata("Tornado_CD") ? pet.getMetadata("Tornado_CD").get(0).asLong() : 0;
                            boolean spawnTornado = false;

                            if (System.currentTimeMillis() - lastTornado >= 8000) {
                                if (Math.random() <= 0.20) {
                                    spawnTornado = true;
                                }
                            }

                            if (spawnTornado) {
                                org.bukkit.entity.WindCharge wc = pet.launchProjectile(org.bukkit.entity.WindCharge.class, dir.multiply(1.8));
                                wc.setMetadata("Pet_Projectile", new FixedMetadataValue(RPGCore.getInstance(), true));
                                wc.setMetadata("Tornado_Seed", new FixedMetadataValue(RPGCore.getInstance(), true));
                                pet.setMetadata("Tornado_CD", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis()));

                                pet.getWorld().spawnParticle(Particle.SONIC_BOOM, startLoc, 1);
                                pet.getWorld().playSound(startLoc, Sound.ENTITY_BREEZE_WIND_BURST, 2.0f, 0.5f);
                            } else {
                                pet.getWorld().playSound(startLoc, Sound.ENTITY_BREEZE_SHOOT, 0.6f, 1.5f);
                                double dist = startLoc.distance(targetLoc);
                                for (double d = 0; d < dist; d += 0.5) {
                                    org.bukkit.Location pLoc = startLoc.clone().add(dir.clone().multiply(d));
                                    pet.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, pLoc, 1, 0, 0, 0, 0);
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

    @EventHandler
    public void onVanillaBreezeShoot(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.WindCharge) {
            if (event.getEntity().getShooter() instanceof LivingEntity) {
                LivingEntity shooter = (LivingEntity) event.getEntity().getShooter();
                if (shooter.hasMetadata("Pet_Type") && shooter.getMetadata("Pet_Type").get(0).asString().equals("GreatWindSpirit")) {
                    if (!event.getEntity().hasMetadata("Pet_Projectile")) {
                        event.setCancelled(true);
                        event.getEntity().remove();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onTornadoSeedHit(ProjectileHitEvent event) {
        if (event.getEntity().hasMetadata("Tornado_Seed")) {
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

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks >= 40) {
                        hitLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 1);
                        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);

                        for (org.bukkit.entity.Entity e : hitLoc.getWorld().getNearbyEntities(hitLoc, 6.0, 6.0, 6.0)) {
                            if (e instanceof LivingEntity && !e.hasMetadata("Summoner_Pet") && !(e instanceof Player)) {
                                LivingEntity target = (LivingEntity) e;
                                Vector push = target.getLocation().toVector().subtract(hitLoc.toVector()).normalize().multiply(1.8).setY(0.6);
                                target.setVelocity(push);

                                if (fOwner != null && fRpgOwner != null) {
                                    double finalDmg = CombatManager.calculatePetDamage(fOwner, fRpgOwner, fPet, target, 0) * 2.0;
                                    fOwner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                                    CombatManager.applySkillDamage(fOwner, target, finalDmg);
                                    fOwner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                                }
                            }
                        }
                        this.cancel();
                        return;
                    }

                    double radius = 3.0 - (ticks * 0.05);
                    for (int i = 0; i < 3; i++) {
                        double angle = (ticks * 0.4) + (i * (2 * Math.PI / 3));
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        hitLoc.getWorld().spawnParticle(Particle.GUST, hitLoc.clone().add(x, (ticks * 0.1) % 4, z), 1, 0, 0, 0, 0);
                    }
                    hitLoc.getWorld().playSound(hitLoc, Sound.ITEM_ELYTRA_FLYING, 0.2f, 1.5f);

                    if (ticks % 10 == 0) {
                        for (org.bukkit.entity.Entity e : hitLoc.getWorld().getNearbyEntities(hitLoc, 6.0, 6.0, 6.0)) {
                            if (e instanceof LivingEntity && !e.hasMetadata("Summoner_Pet") && !(e instanceof Player)) {
                                LivingEntity target = (LivingEntity) e;
                                Vector pull = hitLoc.toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.3);
                                target.setVelocity(pull);

                                if (fOwner != null && fRpgOwner != null) {
                                    double tickDmg = CombatManager.calculatePetDamage(fOwner, fRpgOwner, fPet, target, 0) * 0.3;
                                    fOwner.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(RPGCore.getInstance(), true));
                                    CombatManager.applySkillDamage(fOwner, target, tickDmg);
                                    fOwner.removeMetadata("Ghost_L_Blocker", RPGCore.getInstance());
                                }
                            }
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

            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();
                org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) event.getDamager();
                LivingEntity pet = (LivingEntity) proj.getShooter();

                if (target instanceof Player) return;

                if (pet != null && pet.hasMetadata("Summoner_Pet") && !proj.hasMetadata("Tornado_Seed")) {
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