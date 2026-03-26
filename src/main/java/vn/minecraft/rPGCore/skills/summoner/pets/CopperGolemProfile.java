package vn.minecraft.rPGCore.skills.summoner.pets;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.Map;
import java.util.UUID;

public class CopperGolemProfile implements IPetProfile, Listener {

    public CopperGolemProfile() {
        Bukkit.getPluginManager().registerEvents(this, RPGCore.getInstance());
    }

    @Override public String getId() { return "CopperGolem"; }
    @Override public boolean isMagicDamage() { return false; }
    @Override public String getFusionTargetId() { return null; }
    @Override public String getFusionTargetName() { return null; }
    @Override public double getBaseFlatDamage(int level) { return 0; }
    @Override public double getWeaponScaling(int level) { return 0; }
    @Override public String getArchetype() { return "Vanguard"; }
    @Override public String getElement() { return "Earth"; }

    public static void activateReflect(LivingEntity golem) {
        golem.setMetadata("Golem_Reflect_Active", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 3000));
        golem.getWorld().playSound(golem.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 60 || golem.isDead() || !golem.isValid()) {
                    this.cancel();
                    return;
                }

                org.bukkit.Location baseLoc = golem.getLocation();
                double radius = 1.6;
                double[] heights = {0.3, 1.4, 2.5};

                for (double h : heights) {
                    for (int i = 0; i < 3; i++) {
                        double angle = (ticks * 0.4) + (i * (2 * Math.PI / 3));
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        baseLoc.getWorld().spawnParticle(Particle.CRIT, baseLoc.clone().add(x, h, z), 1, 0, 0, 0, 0);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    @Override
    public void onSpawn(Player owner, LivingEntity pet, int level, double... extraArgs) {
        if (pet.getAttribute(Attribute.SCALE) != null) pet.getAttribute(Attribute.SCALE).setBaseValue(1.75);
        if (pet.getAttribute(Attribute.MOVEMENT_SPEED) != null) pet.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.0);
        if (pet.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) pet.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        pet.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, 255, false, false));
        pet.setCollidable(false);

        RPGPlayer rpgOwner = RPGCore.getInstance().getPlayerManager().getRPGPlayer(owner.getUniqueId());
        double weaponDmgBonus = 0;
        if (rpgOwner != null) {
            Map<String, Double> stats = rpgOwner.getTotalStatsMap();
            double min = stats.getOrDefault("Sát thương phép thuật (Nhỏ nhất)", stats.getOrDefault("Sát thương vật lý (Nhỏ nhất)", 5.0));
            double max = stats.getOrDefault("Sát thương phép thuật (Lớn nhất)", stats.getOrDefault("Sát thương vật lý (Lớn nhất)", 10.0));
            weaponDmgBonus = ((min + max) / 2.0) * 15.0;
        }

        double maxHp = 2000 + (level * 300) + weaponDmgBonus;
        if (pet.getAttribute(Attribute.MAX_HEALTH) != null) {
            pet.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHp);
            pet.setHealth(maxHp);
        }

        new BukkitRunnable() {
            int timer = 0;
            @Override
            public void run() {
                if (pet.isDead() || !pet.isValid()) { this.cancel(); return; }

                if (timer % 20 == 0) addRage(pet, 1);

                if (timer % 40 == 0) {
                    pet.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, pet.getLocation().add(0, 2.5, 0), 3, 1, 0.5, 1, 0);

                    int maxTaunt = 3 + (int) (level * 0.5);
                    int count = 0;

                    for (org.bukkit.entity.Entity e : pet.getNearbyEntities(12, 5, 12)) {
                        if (e instanceof Mob && e != owner && !e.hasMetadata("Summoner_Pet")) {
                            ((Mob) e).setTarget(pet);
                            e.setMetadata("Taunted_By_Golem", new FixedMetadataValue(RPGCore.getInstance(), pet.getUniqueId().toString()));
                            count++;
                            if (count >= maxTaunt) break;
                        }
                    }
                }
                timer++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    public static void addRage(LivingEntity golem, int amount) {
        if (golem.hasMetadata("Golem_Reflect_Active")) return;

        int currentRage = golem.hasMetadata("Golem_Rage") ? golem.getMetadata("Golem_Rage").get(0).asInt() : 0;
        currentRage += amount;

        if (amount > 0) {
            golem.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, golem.getLocation().add(0, 2.5, 0), 1, 0.2, 0.2, 0.2, 0);
        }

        if (currentRage >= 50) {
            activateReflect(golem);
            golem.setMetadata("Golem_Rage", new FixedMetadataValue(RPGCore.getInstance(), 0));
        } else {
            golem.setMetadata("Golem_Rage", new FixedMetadataValue(RPGCore.getInstance(), currentRage));
        }
    }

    @EventHandler
    public void onGolemDamaged(EntityDamageEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) event.getEntity();

        if (!victim.hasMetadata("Pet_Type") || !victim.getMetadata("Pet_Type").get(0).asString().equals("CopperGolem")) return;

        // ==========================================
        // [FIX LỖI 1]: XÓA I-FRAMES (Cho phép nhận sát thương liên tục từ Mưa Tên, Liên Hoàn Tên)
        // ==========================================
        victim.setNoDamageTicks(0);

        double realIncomingDamage = event.getDamage();
        LivingEntity attacker = null;

        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
            if (edbe.getDamager() instanceof LivingEntity) {
                attacker = (LivingEntity) edbe.getDamager();
                try {
                    io.lumine.mythic.core.mobs.ActiveMob am = io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().getActiveMob(attacker.getUniqueId()).orElse(null);
                    if (am != null) {
                        realIncomingDamage = am.getDamage() / 2.0;
                    }
                } catch (Exception ignored) {}
            }
        }

        // 3.1. KHIÊN GAI (ACTIVE): GIẢM 95% DMG & PHẢN ST THEO CẤP ĐỘ
        if (victim.hasMetadata("Golem_Reflect_Active")) {
            long expire = victim.getMetadata("Golem_Reflect_Active").get(0).asLong();
            if (System.currentTimeMillis() < expire) {
                event.setDamage(realIncomingDamage * 0.05);

                if (attacker != null) {
                    // Lấy Cấp độ của Golem để tính % phản sát thương
                    int petLevel = 1;
                    if (victim.hasMetadata("Summon_Level")) {
                        petLevel = victim.getMetadata("Summon_Level").get(0).asInt();
                    }

                    // Công thức: Cấp 1 = 1.1x (110%), Cấp 10 = 2.0x (200%)
                    double reflectMultiplier = 1.0 + (petLevel * 0.1);
                    double reflectDmg = realIncomingDamage * reflectMultiplier;

                    attacker.setNoDamageTicks(0);
                    attacker.damage(reflectDmg);

//                    if (victim.hasMetadata("Summoner_Pet")) {
//                        String ownerUUID = victim.getMetadata("Summoner_Pet").get(0).asString();
//                        Player owner = Bukkit.getPlayer(UUID.fromString(ownerUUID));
//                        if (owner != null && owner.isOnline()) {
//                            owner.sendMessage("§e[Debug] Khiên Golem (Lv." + petLevel + ") vừa phản §c" + String.format("%.1f", reflectDmg) + " DMG §evào " + attacker.getName() + " §7(" + (int)(reflectMultiplier * 100) + "%)");
//                        }
//                    }
                }
                victim.getWorld().playSound(victim.getLocation(), Sound.ENCHANT_THORNS_HIT, 1.0f, 0.8f);
                return;
            }
        }

        // 3.2. NỘI TẠI (PASSIVE): LUÔN TỰ ĐỘNG PHẢN LẠI 10% SÁT THƯƠNG
        event.setDamage(realIncomingDamage);
        if (attacker != null) {
            double passiveReflect = realIncomingDamage * 0.10;
            if (passiveReflect > 0) {
                attacker.setNoDamageTicks(0);
                attacker.damage(passiveReflect); // TRUE DAMAGE
            }
        }

        // 3.3. TÍCH NỘ
        int rageGain = 1;
        if (attacker != null && attacker.hasMetadata("Taunted_By_Golem")) {
            rageGain = 2;
        }
        addRage(victim, rageGain);
    }

    @Override public double onAttack(Player owner, LivingEntity pet, LivingEntity target, double baseDamage) { return 0.0; }

    @Override
    public void onDeath(Player owner, LivingEntity pet) {
        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1.0f, 0.8f);
        pet.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, pet.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0, Material.COPPER_BLOCK.createBlockData());
    }
}