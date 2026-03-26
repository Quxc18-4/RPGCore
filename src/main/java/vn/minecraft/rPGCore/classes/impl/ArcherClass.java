package vn.minecraft.rPGCore.classes.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.classes.RPGClass;
import vn.minecraft.rPGCore.classes.ResourceType;
import vn.minecraft.rPGCore.listeners.EquipmentListener;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.DurabilityManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArcherClass extends RPGClass implements Listener {

    private final Map<UUID, Long> nextShootTime = new HashMap<>();

    public ArcherClass(String id, String displayName, Material icon, List<String> description,
                       double maxHealth, double maxResource, ResourceType resourceType,
                       double regenRate, boolean isEnchanted) {
        super(id, displayName, icon, description, maxHealth, maxResource, resourceType, regenRate, isEnchanted);
        this.armorPenCap = 80.0;

        RPGCore.getInstance().getServer().getPluginManager().registerEvents(this, RPGCore.getInstance());
    }

    @Override
    public void applyPassiveBonusStats(RPGPlayer rpgPlayer, Map<String, Double> stats) {
        double currentPen = stats.getOrDefault("Tỉ lệ xuyên giáp", 0.0);
        stats.put("Tỉ lệ xuyên giáp", currentPen + 25.0);
        super.applyPassiveBonusStats(rpgPlayer, stats);
    }

    // ==========================================
    // GẮN CỜ XUYÊN THẤU VÀ DANH SÁCH MỤC TIÊU ĐÃ XUYÊN
    // ==========================================
    @EventHandler
    public void onArrowLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Arrow && event.getEntity().getShooter() instanceof Player) {
            Player player = (Player) event.getEntity().getShooter();
            RPGPlayer rpgPlayer = RPGCore.getInstance().getPlayerManager().getRPGPlayer(player.getUniqueId());

            if (rpgPlayer != null && rpgPlayer.getCurrentClass() != null && rpgPlayer.getCurrentClass().getId().equals("archer")) {
                if (player.hasMetadata("Archer_Pierce_Buff")) {
                    Arrow arrow = (Arrow) event.getEntity();
                    // KHÔNG dùng setPierceLevel của Vanilla nữa
                    arrow.setMetadata("Is_Piercing_Arrow", new FixedMetadataValue(RPGCore.getInstance(), true));
                    arrow.setMetadata("Pierce_Hit_Count", new FixedMetadataValue(RPGCore.getInstance(), 0));
                    // Tạo danh sách lưu quái đã bị xuyên qua để tránh cắn dame liên tục trong lúc bay
                    arrow.setMetadata("Pierced_Entities", new FixedMetadataValue(RPGCore.getInstance(), new java.util.ArrayList<UUID>()));
                }
            }
        }
    }

    @Override
    public boolean onLeftClick(Player player, RPGPlayer rpgPlayer, ItemStack weapon) {
        Material type = weapon.getType();
        if (type != Material.BOW && type != Material.CROSSBOW) return false;

        long currentTime = System.currentTimeMillis();
        long allowedTime = nextShootTime.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime < allowedTime) return false;

        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        String customArrowType = null;

        if (offHandItem.getType() != Material.AIR && offHandItem.hasItemMeta() && offHandItem.getItemMeta().hasLore()) {
            List<String> lore = offHandItem.getItemMeta().getLore();
            String lastLine = lore.get(lore.size() - 1);

            if (lastLine.startsWith("§8arrow_")) {
                if (rpgPlayer.getSkillData().getSkillLevel("archer_arrow_crafting") < 1) {
                    player.sendMessage("§c§l(!) §cBạn chưa học [Chế Tác Tiễn] nên không thể sử dụng loại mũi tên này!");
                    return false;
                }
                customArrowType = lastLine.replace("§8", "");
                offHandItem.setAmount(offHandItem.getAmount() - 1);
            }
        }

        double totalAtkSpeedBonus = rpgPlayer.getTotalStatsMap().getOrDefault("Tốc độ đánh", 0.0);
        long bowCooldown = (long) (1300 / (1.0 + (totalAtkSpeedBonus / 100.0)));
        long crossbowCooldown = (long) (2200 / (1.0 + (totalAtkSpeedBonus / 100.0)));

        bowCooldown = Math.max(300, bowCooldown);
        crossbowCooldown = Math.max(500, crossbowCooldown);

        if (type == Material.BOW) {
            nextShootTime.put(player.getUniqueId(), currentTime + bowCooldown);
            fireArrow(player, "BOW", customArrowType);
        } else {
            nextShootTime.put(player.getUniqueId(), currentTime + crossbowCooldown);
            fireArrow(player, "CROSSBOW", customArrowType);
        }
        return false;
    }

    private void fireArrow(Player player, String weaponType, String customType) {
        Arrow arrow = player.launchProjectile(Arrow.class);
        RPGCore plugin = RPGCore.getInstance();

        if (customType != null && customType.equals("arrow_lightning")) {
            arrow.setVelocity(arrow.getVelocity().normalize().multiply(4.5));
            arrow.setGravity(false);
        } else {
            arrow.setVelocity(arrow.getVelocity().multiply(1.5));
        }

        arrow.setMetadata("WeaponType", new FixedMetadataValue(plugin, weaponType));

        if (customType != null) {
            arrow.setMetadata("CustomArrowType", new FixedMetadataValue(plugin, customType));
            if (customType.equals("arrow_fire")) arrow.setFireTicks(2000);
        }

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);

        if (customType != null) {
            Location startLoc = player.getEyeLocation();
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (arrow.isDead() || arrow.isOnGround()) { this.cancel(); return; }
                    Location loc = arrow.getLocation();
                    if (loc.distanceSquared(startLoc) < 2.25) return;

                    switch (customType) {
                        case "arrow_fire":
                            loc.getWorld().spawnParticle(Particle.FLAME, loc, 5, 0.1, 0.1, 0.1, 0.02);
                            loc.getWorld().spawnParticle(Particle.DUST, loc, 4, 0.1, 0.1, 0.1, new Particle.DustOptions(org.bukkit.Color.ORANGE, 1.2f));
                            break;
                        case "arrow_poison":
                            loc.getWorld().spawnParticle(Particle.DUST, loc, 6, 0.15, 0.15, 0.15, new Particle.DustOptions(org.bukkit.Color.PURPLE, 1.5f));
                            loc.getWorld().spawnParticle(Particle.WITCH, loc, 3, 0.1, 0.1, 0.1, 0);
                            break;
                        case "arrow_explosive":
                            loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 6, 0.1, 0.1, 0.1, 0.05);
                            break;
                        case "arrow_lifesteal":
                            loc.getWorld().spawnParticle(Particle.DUST, loc, 6, 0.1, 0.1, 0.1, new Particle.DustOptions(org.bukkit.Color.fromRGB(200, 0, 0), 1.5f));
                            break;
                        case "arrow_lightning":
                            loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 5, 0.1, 0.1, 0.1, 0.05);
                            loc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 4, 0.1, 0.1, 0.1, 0.05);
                            break;
                    }
                }
            }.runTaskTimer(plugin, 1L, 1L);
        }

        new BukkitRunnable() {
            @Override
            public void run() { if (arrow != null && arrow.isValid()) arrow.remove(); }
        }.runTaskLater(plugin, 60L);
    }

    @Override
    public void onProjectileHit(Player shooter, RPGPlayer rpgShooter, org.bukkit.entity.Projectile projectile, org.bukkit.event.entity.ProjectileHitEvent event) {
        if (!(projectile instanceof Arrow)) return;
        Arrow arrow = (Arrow) projectile;

        if (arrow.hasMetadata("HeavyBlow_Level")) return;

        boolean isCustomSkill = arrow.hasMetadata("Is_Custom_Skill");
        boolean isNormalHit = arrow.hasMetadata("WeaponType");
        boolean isCustomArrow = arrow.hasMetadata("CustomArrowType");

        if (!isCustomSkill && !isNormalHit && !isCustomArrow) return;

        RPGCore plugin = RPGCore.getInstance();

        // ==========================================
        // [FIX LỖI XUYÊN THẤU]: HACK CƠ CHẾ VẬT LÝ VANILLA
        // ==========================================
        if (arrow.hasMetadata("Is_Piercing_Arrow") && event.getHitEntity() != null) {
            java.util.List<UUID> piercedList = (java.util.List<UUID>) arrow.getMetadata("Pierced_Entities").get(0).value();

            // Nếu mũi tên vẫn đang kẹt trong Hitbox của con quái cũ -> Bỏ qua
            if (piercedList.contains(event.getHitEntity().getUniqueId())) {
                event.setCancelled(true); // Cho mũi tên bay tiếp
                return;
            }

            // Đánh dấu quái mới đã bị đâm
            piercedList.add(event.getHitEntity().getUniqueId());

            // HỦY VA CHẠM VẬT LÝ: Ép mũi tên trở thành "bóng ma", không bị dội lại (Bounce)
            event.setCancelled(true);
        }

        Location hitLoc = event.getHitBlock() != null ? event.getHitBlock().getLocation() :
                (event.getHitEntity() != null ? event.getHitEntity().getLocation() : arrow.getLocation());

        // 1. GÂY SÁT THƯƠNG NẾU TRÚNG QUÁI
        if (event.getHitEntity() instanceof LivingEntity && event.getHitEntity() != shooter) {
            LivingEntity target = (LivingEntity) event.getHitEntity();
            ItemStack weapon = shooter.getInventory().getItemInMainHand();

            EquipmentListener eqListener = new EquipmentListener(plugin.getPlayerManager());
            if (eqListener.canPlayerUse(shooter, weapon)) {

                String weaponType = "BOW";
                if (isNormalHit) {
                    weaponType = arrow.getMetadata("WeaponType").get(0).asString();
                } else if (weapon.getType() == Material.CROSSBOW) {
                    weaponType = "CROSSBOW";
                }
                boolean isCrossbow = weaponType.equals("CROSSBOW");

                double dmgMulti = 1.0;
                double extraArmorPen = 0.0;
                double extraLifeSteal = 0.0;
                int durabilityLoss = 1;

                if (isCustomSkill) {
                    dmgMulti = arrow.getMetadata("Skill_DmgMulti").get(0).asDouble();
                    if (arrow.hasMetadata("Skill_ArmorPen")) {
                        extraArmorPen = arrow.getMetadata("Skill_ArmorPen").get(0).asDouble();
                        if (extraArmorPen <= 1.0) extraArmorPen *= 100.0;
                    }
                    durabilityLoss = 3;
                }

                // ==========================================
                // NỘI TẠI NGẮM CHUẨN (SNIPER)
                // ==========================================
                int sniperLv = rpgShooter.getSkillData().getSkillLevel("archer_sniper_passive");
                if (sniperLv > 0) {
                    double dist = shooter.getLocation().distance(target.getLocation());
                    double cappedDist = Math.min(dist, 25.0);
                    double distanceBonus = cappedDist * (sniperLv * 0.01);
                    dmgMulti += distanceBonus;
                }

                // ==========================================
                // GIẢM SÁT THƯƠNG ĐẠN XUYÊN THẤU
                // ==========================================
                if (arrow.hasMetadata("Is_Piercing_Arrow")) {
                    int hitCount = arrow.getMetadata("Pierce_Hit_Count").get(0).asInt();

                    double piercePenalty = Math.max(0.30, 1.0 - (hitCount * 0.08));
                    dmgMulti *= piercePenalty;

                    arrow.setMetadata("Pierce_Hit_Count", new FixedMetadataValue(plugin, hitCount + 1));
                    target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
                }

                if (isCustomArrow) {
                    String cType = arrow.getMetadata("CustomArrowType").get(0).asString();
                    durabilityLoss = 2;

                    switch (cType) {
                        case "arrow_ice":
                            StatusEffectManager.applyEffect(target, PotionEffectType.SLOWNESS, 20, 10);
                            target.setFreezeTicks(60);
                            target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.BLOCK_GLASS_PLACE, 1f, 2f);
                            org.bukkit.block.data.BlockData iceData = org.bukkit.Bukkit.createBlockData(Material.BLUE_ICE);

                            new BukkitRunnable() {
                                int ticks = 0;
                                @Override
                                public void run() {
                                    if (target.isDead() || !target.isValid()) {
                                        this.cancel();
                                        return;
                                    }
                                    if (ticks >= 20) {
                                        StatusEffectManager.applyEffect(target, PotionEffectType.SLOWNESS, 40, 0);
                                        target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 50, 0.5, 0.8, 0.5, 0.15, iceData);
                                        target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
                                        this.cancel();
                                        return;
                                    }
                                    Location entLoc = target.getLocation();
                                    Particle.DustOptions iceDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(173, 216, 230), 1.2f);
                                    Particle.DustOptions whiteDust = new Particle.DustOptions(org.bukkit.Color.WHITE, 1.0f);
                                    double angleOffset = ticks * 0.1;
                                    for (double y = 0; y <= 2.0; y += 0.4) {
                                        for (double a = 0; a < Math.PI * 2; a += Math.PI / 4) {
                                            double x = Math.cos(a + angleOffset) * 0.7;
                                            double z = Math.sin(a + angleOffset) * 0.7;
                                            Particle.DustOptions color = (y % 0.8 == 0) ? whiteDust : iceDust;
                                            entLoc.getWorld().spawnParticle(Particle.DUST, entLoc.clone().add(x, y, z), 1, color);
                                        }
                                    }
                                    ticks++;
                                }
                            }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
                            break;
                        case "arrow_fire":
                            dmgMulti *= 1.2;
                            target.setFireTicks(100);
                            break;
                        case "arrow_lightning":
                            dmgMulti *= 1.5;
                            target.getWorld().strikeLightningEffect(target.getLocation());
                            StatusEffectManager.applyEffect(target, PotionEffectType.SLOWNESS, 30, 10);
                            break;
                        case "arrow_explosive":
                            dmgMulti *= 1.35;
                            break;
                        case "arrow_poison":
                            dmgMulti *= 0.8;
                            StatusEffectManager.applyEffect(target, PotionEffectType.WEAKNESS, 200, 0);
                            StatusEffectManager.applyEffect(target, PotionEffectType.POISON, 200, 0);
                            break;
                        case "arrow_lifesteal":
                            extraLifeSteal = 15.0;
                            break;
                    }
                }

                if (arrow.hasMetadata("Quad_Effect")) {
                    String qEffect = arrow.getMetadata("Quad_Effect").get(0).asString();
                    switch (qEffect) {
                        case "FREEZE":
                            StatusEffectManager.applyEffect(target, PotionEffectType.SLOWNESS, 40, 2);
                            target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
                            break;
                        case "LIGHTNING":
                            target.getWorld().strikeLightningEffect(target.getLocation());
                            break;
                        case "FLAME":
                            target.setFireTicks(60);
                            break;
                        case "POISON":
                            StatusEffectManager.applyEffect(target, PotionEffectType.POISON, 60, 1);
                            break;
                    }
                }

                double finalDamage = CombatManager.calculateDamage(shooter, rpgShooter, target, dmgMulti, extraArmorPen, 0.0, 0.0, false, isCrossbow);
                CombatManager.applyLifeSteal(shooter, rpgShooter, finalDamage, extraLifeSteal, false);

                if (!arrow.hasMetadata("Is_Piercing_Arrow") || arrow.getMetadata("Pierce_Hit_Count").get(0).asInt() == 1) {
                    DurabilityManager.damageItem(shooter, rpgShooter, weapon, durabilityLoss);
                }

                CombatManager.applySkillDamage(shooter, target, finalDamage);
            }
        }

        // 2. XỬ LÝ AOE MŨI TÊN ĐẶC BIỆT CHẠM ĐẤT
        if (isCustomArrow) {
            String cType = arrow.getMetadata("CustomArrowType").get(0).asString();

            if (cType.equals("arrow_nature")) {
                Location exactHitLoc = (event.getHitEntity() != null) ? event.getHitEntity().getLocation().clone() : arrow.getLocation().clone();
                if (event.getHitEntity() == null) {
                    while (!exactHitLoc.getBlock().getType().isSolid() && exactHitLoc.getY() > exactHitLoc.getWorld().getMinHeight()) {
                        exactHitLoc.subtract(0, 1, 0);
                    }
                    exactHitLoc.setY(exactHitLoc.getBlockY() + 1.05);
                }

                List<double[]> vines = new java.util.ArrayList<>();
                for (int i = 0; i < 28; i++) {
                    double r1 = Math.sqrt(Math.random()) * 4.5, angle1 = Math.random() * Math.PI * 2;
                    double r2 = Math.sqrt(Math.random()) * 4.5, angle2 = Math.random() * Math.PI * 2;
                    vines.add(new double[]{r1 * Math.cos(angle1), r1 * Math.sin(angle1), r2 * Math.cos(angle2), r2 * Math.sin(angle2), 0.2 + (Math.random() * 0.4), Math.random() > 0.5 ? 1.0 : 0.0});
                }

                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 40) { this.cancel(); return; }
                        Particle.DustOptions rootDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(101, 67, 33), 0.9f);
                        Particle.DustOptions groundVineDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(34, 139, 34), 0.8f);
                        Particle.DustOptions bindDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(50, 205, 50), 0.7f);

                        for (double[] v : vines) {
                            Particle.DustOptions currentDust = (v[5] == 0.0) ? rootDust : groundVineDust;
                            for (double t = 0; t <= 1; t += 0.1) {
                                exactHitLoc.getWorld().spawnParticle(Particle.DUST, exactHitLoc.clone().add(v[0] + (v[2] - v[0]) * t, 4 * v[4] * t * (1 - t), v[1] + (v[3] - v[1]) * t), 1, currentDust);
                            }
                        }

                        for (org.bukkit.entity.Entity e : exactHitLoc.getWorld().getNearbyEntities(exactHitLoc, 4.0, 2.0, 4.0)) {
                            if (e instanceof LivingEntity && e != shooter) {
                                LivingEntity target = (LivingEntity) e;
                                StatusEffectManager.applyEffect(target, PotionEffectType.SLOWNESS, 40, 10);
                                Location entLoc = target.getLocation();
                                double spinSpeed = ticks * 0.4, entRadius = 0.55;
                                for (double y = 0; y <= 0.8; y += 0.2) {
                                    entLoc.getWorld().spawnParticle(Particle.DUST, entLoc.clone().add(entRadius * Math.cos(spinSpeed + (y * Math.PI * 2)), y, entRadius * Math.sin(spinSpeed + (y * Math.PI * 2))), 1, bindDust);
                                    entLoc.getWorld().spawnParticle(Particle.DUST, entLoc.clone().add(entRadius * Math.cos(spinSpeed + (y * Math.PI * 2) + Math.PI), y, entRadius * Math.sin(spinSpeed + (y * Math.PI * 2) + Math.PI)), 1, bindDust);
                                }
                            }
                        }
                        if (ticks % 5 == 0) exactHitLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, exactHitLoc.clone().add((Math.random()*8)-4, Math.random()*2, (Math.random()*8)-4), 2);
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
            else if (cType.equals("arrow_ice") && event.getHitBlock() != null) {
                arrow.getLocation().getWorld().playSound(arrow.getLocation(), org.bukkit.Sound.BLOCK_SNOW_BREAK, 1.0f, 0.5f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 60) { this.cancel(); return; }
                        double rx = (Math.random() * 6) - 3, rz = (Math.random() * 6) - 3, ry = Math.random() * 2.5 + 0.5;
                        arrow.getLocation().getWorld().spawnParticle(Particle.SNOWFLAKE, arrow.getLocation().clone().add(rx, ry, rz), 2, 0.2, 0.2, 0.2, 0.01);
                        arrow.getLocation().getWorld().spawnParticle(Particle.WHITE_ASH, arrow.getLocation().clone().add(rx, 0.1, rz), 1, 0, 0, 0, 0);

                        if (ticks % 10 == 0) {
                            for (org.bukkit.entity.Entity e : arrow.getLocation().getWorld().getNearbyEntities(arrow.getLocation(), 3.0, 2.0, 3.0)) {
                                if (e instanceof LivingEntity && e != shooter) StatusEffectManager.applyEffect((LivingEntity) e, PotionEffectType.SLOWNESS, 40, 0);
                            }
                        }
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
            else if (cType.equals("arrow_explosive")) {
                hitLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 1);
                hitLoc.getWorld().playSound(hitLoc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                for (org.bukkit.entity.Entity e : hitLoc.getWorld().getNearbyEntities(hitLoc, 4.0, 4.0, 4.0)) {
                    if (e instanceof LivingEntity && e != shooter) {
                        Vector knockback = e.getLocation().toVector().subtract(hitLoc.toVector());
                        if (knockback.lengthSquared() < 0.0001) knockback = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
                        e.setVelocity(knockback.normalize().multiply(1.5).setY(0.5));
                    }
                }
            }
            else if (cType.equals("arrow_blackhole")) {
                Location exactHitLoc = arrow.getLocation();
                new BukkitRunnable() {
                    int ticks = 0;
                    Location center = exactHitLoc.clone().add(0, 1.0, 0);
                    @Override
                    public void run() {
                        if (ticks >= 30) {
                            center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
                            center.getWorld().spawnParticle(Particle.GUST_EMITTER_LARGE, center, 1);
                            center.getWorld().playSound(center, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);

                            for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, 6.0, 6.0, 6.0)) {
                                if (e instanceof LivingEntity && e != shooter) {
                                    Vector push = e.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.8).setY(0.6);
                                    e.setVelocity(push);
                                }
                            }
                            this.cancel();
                            return;
                        }

                        Particle.DustOptions whiteDust = new Particle.DustOptions(org.bukkit.Color.WHITE, 1.5f);
                        for (int i = 0; i < 20; i++) {
                            double phi = Math.acos(2 * Math.random() - 1), theta = 2 * Math.PI * Math.random();
                            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0.4 * Math.sin(phi) * Math.cos(theta), 0.4 * Math.sin(phi) * Math.sin(theta), 0.4 * Math.cos(phi)), 1, whiteDust);
                        }

                        Particle.DustOptions cyanDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(173, 216, 230), 1.2f);
                        double z1 = 1.2 * Math.sin(ticks * 0.4);
                        center.getWorld().spawnParticle(Particle.DUST, center.clone().add(1.2 * Math.cos(ticks * 0.4), z1 * Math.sin(Math.toRadians(40)), z1 * Math.cos(Math.toRadians(40))), 1, cyanDust);
                        double z2 = 1.8 * Math.sin(-ticks * 0.3);
                        center.getWorld().spawnParticle(Particle.DUST, center.clone().add(1.8 * Math.cos(-ticks * 0.3), z2 * Math.sin(Math.toRadians(-40)), z2 * Math.cos(Math.toRadians(-40))), 1, cyanDust);

                        if (ticks % 10 == 0) center.getWorld().playSound(center, org.bukkit.Sound.ITEM_ELYTRA_FLYING, 1.0f, 1.5f);

                        for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, 6.0, 6.0, 6.0)) {
                            if (e instanceof LivingEntity && e != shooter) {
                                Vector pull = center.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.2);
                                e.setVelocity(e.getVelocity().add(StatusEffectManager.calculateResistedVelocity((LivingEntity)e, pull)));
                            }
                        }
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }

        // [QUAN TRỌNG]: KHÔNG XÓA MŨI TÊN NẾU NÓ LÀ ĐẠN XUYÊN THẤU VÀ CHƯA CHẠM TƯỜNG (CHẠM ĐẤT)
        if (arrow.hasMetadata("Is_Piercing_Arrow") && event.getHitEntity() != null) {
            return;
        }

        arrow.remove();
    }

    @Override
    public boolean performMeleeAttack(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, ItemStack weapon) {
        if (!attacker.hasMetadata("Ranged_Warning_Cooldown")) {
            attacker.sendMessage("§c§l[!] §fHệ phái của bạn không thể tấn công cận chiến!");
            attacker.setMetadata("Ranged_Warning_Cooldown", new org.bukkit.metadata.FixedMetadataValue(vn.minecraft.rPGCore.RPGCore.getInstance(), true));

            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    if (attacker.hasMetadata("Ranged_Warning_Cooldown")) {
                        attacker.removeMetadata("Ranged_Warning_Cooldown", vn.minecraft.rPGCore.RPGCore.getInstance());
                    }
                }
            }.runTaskLater(vn.minecraft.rPGCore.RPGCore.getInstance(), 40L);
        }
        return false;
    }
}