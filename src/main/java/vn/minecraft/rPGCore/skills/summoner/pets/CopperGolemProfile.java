package vn.minecraft.rPGCore.skills.summoner.pets;

import org.bukkit.*;
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

    // ==========================================
    // 1. KÍCH HOẠT KHIÊN PHẢN SÁT THƯƠNG (3 VÒNG SÁNG)
    // ==========================================
    public static void activateReflect(LivingEntity golem) {
        golem.setMetadata("Golem_Reflect_Active", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 3000));
        golem.getWorld().playSound(golem.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.5f);

        // VISUAL: 3 vòng sáng Particle.ENCHANTED_HIT bao quanh
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 60 || golem.isDead() || !golem.isValid()) {
                    this.cancel();
                    return;
                }

                Location center = golem.getLocation().add(0, 1.5, 0); // Tâm ở giữa ngực Golem
                double radius = 1.6;

                for (int i = 0; i < 3; i++) {
                    // Tạo 3 vòng đan chéo nhau, lệch góc 120 độ
                    double angle = ticks * 0.4 + (i * (2 * Math.PI / 3));
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = Math.sin(ticks * 0.15 + i) * 0.8; // Gợn sóng lên xuống

                    center.getWorld().spawnParticle(Particle.ENCHANTED_HIT, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }

    // ==========================================
    // 2. KHI VỪA ĐƯỢC TRIỆU HỒI
    // ==========================================
    @Override
    public void onSpawn(Player owner, LivingEntity pet, int level, double... extraArgs) {
        // 2.1. TĂNG KÍCH THƯỚC (Scale 1.75) VÀ BẤT ĐỘNG HOÀN TOÀN
        if (pet.getAttribute(Attribute.SCALE) != null) pet.getAttribute(Attribute.SCALE).setBaseValue(1.75);
        if (pet.getAttribute(Attribute.MOVEMENT_SPEED) != null) pet.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.0);
        pet.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, 255, false, false));

        // 2.2. MÁU ĐƯỢC GIẢM XUỐNG ĐỂ CÂN BẰNG
        RPGPlayer rpgOwner = RPGCore.getInstance().getPlayerManager().getRPGPlayer(owner.getUniqueId());
        double weaponDmgBonus = 0;
        if (rpgOwner != null) {
            Map<String, Double> stats = rpgOwner.getTotalStatsMap();
            double min = stats.getOrDefault("Sát thương phép thuật (Nhỏ nhất)", stats.getOrDefault("Sát thương vật lý (Nhỏ nhất)", 5.0));
            double max = stats.getOrDefault("Sát thương phép thuật (Lớn nhất)", stats.getOrDefault("Sát thương vật lý (Lớn nhất)", 10.0));
            weaponDmgBonus = ((min + max) / 2.0) * 15.0; // Giảm quy đổi còn 15 máu / 1 DMG
        }

        // Lv 10 = ~1500 HP cơ bản + HP từ vũ khí
        double maxHp = 500 + (level * 100) + weaponDmgBonus;
        if (pet.getAttribute(Attribute.MAX_HEALTH) != null) {
            pet.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHp);
            pet.setHealth(maxHp);
        }

        // 2.3. VÒNG LẶP AUTO-TAUNT (Gầm khiêu khích mỗi 5 giây)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pet.isDead() || !pet.isValid()) {
                    this.cancel();
                    return;
                }

                // Đánh dấu nhận 1.3x sát thương trong 5s (Do dùng sức để gầm)
                pet.setMetadata("Golem_Vulnerable", new FixedMetadataValue(RPGCore.getInstance(), System.currentTimeMillis() + 5000));

                pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.5f);
                pet.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, pet.getLocation().add(0, 2.5, 0), 5, 1, 0.5, 1, 0);

                for (org.bukkit.entity.Entity e : pet.getNearbyEntities(10, 5, 10)) {
                    if (e instanceof Mob && e != owner && !e.hasMetadata("Summoner_Pet")) {
                        ((Mob) e).setTarget(pet); // Ép quái đánh Golem
                    }
                }
            }
        }.runTaskTimer(RPGCore.getInstance(), 20L, 100L); // Trễ 1s, lặp mỗi 5s
    }

    // ==========================================
    // 3. XỬ LÝ SỰ KIỆN KHI GOLEM MẤT MÁU (VẠN NĂNG)
    // ==========================================
    @EventHandler
    public void onGolemDamaged(EntityDamageEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) event.getEntity();

        if (!victim.hasMetadata("Pet_Type") || !victim.getMetadata("Pet_Type").get(0).asString().equals("CopperGolem")) return;

        // 3.1. NẾU KHIÊN ĐANG BẬT -> GIẢM 90% DMG & PHẢN SÁT THƯƠNG (KHÔNG TÍCH NỘ)
        if (victim.hasMetadata("Golem_Reflect_Active")) {
            long expire = victim.getMetadata("Golem_Reflect_Active").get(0).asLong();
            if (System.currentTimeMillis() < expire) {
                double originalDamage = event.getDamage();
                event.setDamage(originalDamage * 0.1);

                if (event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
                    if (edbe.getDamager() instanceof LivingEntity) {
                        LivingEntity attacker = (LivingEntity) edbe.getDamager();
                        attacker.damage(originalDamage * 2.0, victim); // Phản 200%
                    }
                }
                victim.getWorld().playSound(victim.getLocation(), Sound.ENCHANT_THORNS_HIT, 1.0f, 0.8f);
                return; // Thoát ngang, không bị chịu debuff 1.3x và không tích Nộ thêm
            }
        }

        // 3.2. NẾU ĐANG BỊ SUY YẾU (DO TAUNT) -> NHẬN 130% DMG
        if (victim.hasMetadata("Golem_Vulnerable")) {
            long expire = victim.getMetadata("Golem_Vulnerable").get(0).asLong();
            if (System.currentTimeMillis() < expire) {
                event.setDamage(event.getDamage() * 1.3);
            }
        }

        // 3.3. TÍCH LŨY 1 NỘ CHO MỖI LẦN BỊ TRỪ MÁU (BAO GỒM CẢ ĐỘC/CHÁY)
        int currentRage = victim.hasMetadata("Golem_Rage") ? victim.getMetadata("Golem_Rage").get(0).asInt() : 0;
        currentRage += 1;

        // Hạt bụi đỏ báo hiệu đang sạc Nộ
        victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 2.5, 0), 1, 0.2, 0.2, 0.2, 0);

        if (currentRage >= 60) {
            activateReflect(victim);
            victim.setMetadata("Golem_Rage", new FixedMetadataValue(RPGCore.getInstance(), 0)); // Reset nộ
        } else {
            victim.setMetadata("Golem_Rage", new FixedMetadataValue(RPGCore.getInstance(), currentRage));
        }
    }

    @Override
    public double onAttack(Player owner, LivingEntity pet, LivingEntity target, double baseDamage) {
        return 0.0;
    }

    @Override
    public void onDeath(Player owner, LivingEntity pet) {
        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 1.0f, 0.8f);
        pet.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, pet.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0, Material.COPPER_BLOCK.createBlockData());
    }
}