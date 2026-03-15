package vn.minecraft.rPGCore.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.DurabilityManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.ui.ArrowCraftingUI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArcherListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerManager playerManager;
    private final ArrowCraftingUI arrowCraftingUI;
    private final Map<UUID, Long> nextShootTime = new HashMap<>();

    public ArcherListener(JavaPlugin plugin, PlayerManager playerManager, ArrowCraftingUI arrowCraftingUI) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.arrowCraftingUI = arrowCraftingUI;
    }

    // =========================================
    // MỞ GIAO DIỆN CHẾ TẠO MŨI TÊN
    // =========================================
    @EventHandler
    public void onFletchingTableClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.FLETCHING_TABLE) return;

        Player player = event.getPlayer();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());

        if (rpgPlayer != null && rpgPlayer.getCurrentClass().getId().equalsIgnoreCase("archer")) {
            event.setCancelled(true);
            arrowCraftingUI.openCraftingGUI(player, 0);
        }
    }

    // =========================================
    // 1. CƠ CHẾ CLICK TRÁI BẮN (Tay Trái + Tay Phải)
    // =========================================
    @EventHandler
    public void onLeftClickShoot(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Material type = item.getType();

        if (type != Material.BOW && type != Material.CROSSBOW) return;

        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        if (!rpgPlayer.getCurrentClass().getId().equalsIgnoreCase("archer")) {
            player.sendMessage("§c§l(!) §cChỉ có class Cung Thủ (Archer) mới có thể sử dụng vũ khí này!");
            return;
        }

        long currentTime = System.currentTimeMillis();
        long allowedTime = nextShootTime.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime < allowedTime) return;

        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        String customArrowType = null;

        if (offHandItem.getType() != Material.AIR && offHandItem.hasItemMeta() && offHandItem.getItemMeta().hasLore()) {
            List<String> lore = offHandItem.getItemMeta().getLore();
            String lastLine = lore.get(lore.size() - 1);

            if (lastLine.startsWith("§8arrow_")) {
                if (rpgPlayer.getSkillData().getSkillLevel("archer_arrow_crafting") < 1) {
                    player.sendMessage("§c§l(!) §cBạn chưa học [Chế Tác Tiễn] nên không thể sử dụng loại mũi tên này!");
                    return;
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
        } else if (type == Material.CROSSBOW) {
            nextShootTime.put(player.getUniqueId(), currentTime + crossbowCooldown);
            fireArrow(player, "CROSSBOW", customArrowType);
        }
    }

    private void fireArrow(Player player, String weaponType, String customType) {
        Arrow arrow = player.launchProjectile(Arrow.class);

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
                            loc.getWorld().spawnParticle(org.bukkit.Particle.FLAME, loc, 5, 0.1, 0.1, 0.1, 0.02);
                            org.bukkit.Particle.DustOptions orange = new org.bukkit.Particle.DustOptions(org.bukkit.Color.ORANGE, 1.2f);
                            loc.getWorld().spawnParticle(org.bukkit.Particle.DUST, loc, 4, 0.1, 0.1, 0.1, orange);
                            break;
                        case "arrow_poison":
                            org.bukkit.Particle.DustOptions purple = new org.bukkit.Particle.DustOptions(org.bukkit.Color.PURPLE, 1.5f);
                            loc.getWorld().spawnParticle(org.bukkit.Particle.DUST, loc, 6, 0.15, 0.15, 0.15, purple);
                            loc.getWorld().spawnParticle(org.bukkit.Particle.WITCH, loc, 3, 0.1, 0.1, 0.1, 0);
                            break;
                        case "arrow_explosive":
                            loc.getWorld().spawnParticle(org.bukkit.Particle.CAMPFIRE_COSY_SMOKE, loc, 6, 0.1, 0.1, 0.1, 0.05);
                            break;
                        case "arrow_lifesteal":
                            org.bukkit.Particle.DustOptions red = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(200, 0, 0), 1.5f);
                            loc.getWorld().spawnParticle(org.bukkit.Particle.DUST, loc, 6, 0.1, 0.1, 0.1, red);
                            break;
                        case "arrow_lightning":
                            loc.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK, loc, 5, 0.1, 0.1, 0.1, 0.05);
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

    @EventHandler
    public void onVanillaShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) event.setCancelled(true);
    }

    // =========================================
    // 2. KHÓA SÁT THƯƠNG VẬT LÝ GỐC CỦA MŨI TÊN
    // =========================================
    @EventHandler
    public void onVanillaArrowDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getDamager();
            if (arrow.hasMetadata("WeaponType") || arrow.hasMetadata("Is_Custom_Skill") || arrow.hasMetadata("CustomArrowType")) {
                event.setCancelled(true); // Không cho phép Minecraft tính sát thương Vanilla
            }
        }
    }

    // =========================================
    // 3. TÍNH TOÁN SÁT THƯƠNG & HIỆU ỨNG TẠI ĐIỂM CHẠM
    // =========================================
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        Arrow arrow = (Arrow) event.getEntity();

        if (arrow.hasMetadata("HeavyBlow_Level")) return; // Trọng Tiễn đã có Event xử lý riêng

        boolean isCustomSkill = arrow.hasMetadata("Is_Custom_Skill");
        boolean isNormalHit = arrow.hasMetadata("WeaponType");
        boolean isCustomArrow = arrow.hasMetadata("CustomArrowType");

        if (!isCustomSkill && !isNormalHit && !isCustomArrow) return;

        Player shooter = arrow.getShooter() instanceof Player ? (Player) arrow.getShooter() : null;
        if (shooter == null) return;

        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(shooter.getUniqueId());
        if (rpgPlayer == null) return;

        Location hitLoc = event.getHitBlock() != null ? event.getHitBlock().getLocation() :
                (event.getHitEntity() != null ? event.getHitEntity().getLocation() : arrow.getLocation());

        // ===================================
        // 3.1: GÂY SÁT THƯƠNG NẾU TRÚNG QUÁI
        // ===================================
        if (event.getHitEntity() instanceof LivingEntity && event.getHitEntity() != shooter) {
            LivingEntity target = (LivingEntity) event.getHitEntity();
            ItemStack weapon = shooter.getInventory().getItemInMainHand();

            EquipmentListener eqListener = new EquipmentListener(playerManager);
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
                        if (extraArmorPen <= 1.0) extraArmorPen *= 100.0; // Chuẩn hóa Xuyên Giáp
                    }
                    durabilityLoss = 3;
                }

                if (isCustomArrow) {
                    String cType = arrow.getMetadata("CustomArrowType").get(0).asString();
                    durabilityLoss = 2;

                    switch (cType) {
                        case "arrow_ice":
                            StatusEffectManager.applyEffect(target, PotionEffectType.SLOWNESS, 40, 10);
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

                // Quadruple Shot Effects
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

                // TÍNH TOÁN SÁT THƯƠNG -> ÉP TRỪ MÁU
                double finalDamage = CombatManager.calculateDamage(shooter, rpgPlayer, target, dmgMulti, extraArmorPen, 0.0, 0.0, false, isCrossbow);
                CombatManager.applyLifeSteal(shooter, rpgPlayer, finalDamage, extraLifeSteal, false);
                DurabilityManager.damageItem(shooter, rpgPlayer, weapon, durabilityLoss);

                // DÙNG HÀM XUYÊN I-FRAME
                CombatManager.applySkillDamage(shooter, target, finalDamage);
            }
        }

        // ===================================
        // 3.2: XỬ LÝ AOE MŨI TÊN ĐẶC BIỆT
        // ===================================
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
                        org.bukkit.Particle.DustOptions rootDust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(101, 67, 33), 0.9f);
                        org.bukkit.Particle.DustOptions groundVineDust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(34, 139, 34), 0.8f);
                        org.bukkit.Particle.DustOptions bindDust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(50, 205, 50), 0.7f);

                        for (double[] v : vines) {
                            org.bukkit.Particle.DustOptions currentDust = (v[5] == 0.0) ? rootDust : groundVineDust;
                            for (double t = 0; t <= 1; t += 0.1) {
                                exactHitLoc.getWorld().spawnParticle(org.bukkit.Particle.DUST, exactHitLoc.clone().add(v[0] + (v[2] - v[0]) * t, 4 * v[4] * t * (1 - t), v[1] + (v[3] - v[1]) * t), 1, currentDust);
                            }
                        }

                        for (org.bukkit.entity.Entity e : exactHitLoc.getWorld().getNearbyEntities(exactHitLoc, 4.0, 2.0, 4.0)) {
                            if (e instanceof LivingEntity && e != shooter) {
                                LivingEntity target = (LivingEntity) e;
                                StatusEffectManager.applyEffect(target, PotionEffectType.SLOWNESS, 40, 10);
                                Location entLoc = target.getLocation();
                                double spinSpeed = ticks * 0.4, entRadius = 0.55;
                                for (double y = 0; y <= 0.8; y += 0.2) {
                                    entLoc.getWorld().spawnParticle(org.bukkit.Particle.DUST, entLoc.clone().add(entRadius * Math.cos(spinSpeed + (y * Math.PI * 2)), y, entRadius * Math.sin(spinSpeed + (y * Math.PI * 2))), 1, bindDust);
                                    entLoc.getWorld().spawnParticle(org.bukkit.Particle.DUST, entLoc.clone().add(entRadius * Math.cos(spinSpeed + (y * Math.PI * 2) + Math.PI), y, entRadius * Math.sin(spinSpeed + (y * Math.PI * 2) + Math.PI)), 1, bindDust);
                                }
                            }
                        }
                        if (ticks % 5 == 0) exactHitLoc.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, exactHitLoc.clone().add((Math.random()*8)-4, Math.random()*2, (Math.random()*8)-4), 2);
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
                    @Override
                    public void run() {
                        if (ticks >= 40) { this.cancel(); return; }
                        Location center = exactHitLoc.clone().add(0, 1.5, 0);
                        org.bukkit.Particle.DustOptions blackDust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.BLACK, 1.5f);
                        for (int i = 0; i < 20; i++) {
                            double phi = Math.acos(2 * Math.random() - 1), theta = 2 * Math.PI * Math.random();
                            center.getWorld().spawnParticle(org.bukkit.Particle.DUST, center.clone().add(0.4 * Math.sin(phi) * Math.cos(theta), 0.4 * Math.sin(phi) * Math.sin(theta), 0.4 * Math.cos(phi)), 1, blackDust);
                        }

                        org.bukkit.Particle.DustOptions purpleDust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(138, 43, 226), 1.2f);
                        double z1 = 1.2 * Math.sin(ticks * 0.4);
                        center.getWorld().spawnParticle(org.bukkit.Particle.DUST, center.clone().add(1.2 * Math.cos(ticks * 0.4), z1 * Math.sin(Math.toRadians(40)), z1 * Math.cos(Math.toRadians(40))), 1, purpleDust);
                        double z2 = 1.8 * Math.sin(-ticks * 0.3);
                        center.getWorld().spawnParticle(org.bukkit.Particle.DUST, center.clone().add(1.8 * Math.cos(-ticks * 0.3), z2 * Math.sin(Math.toRadians(-40)), z2 * Math.cos(Math.toRadians(-40))), 1, purpleDust);

                        if (ticks % 10 == 0) center.getWorld().playSound(center, org.bukkit.Sound.BLOCK_BEACON_AMBIENT, 2.0f, 0.5f);

                        for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, 5.0, 5.0, 5.0)) {
                            if (e instanceof LivingEntity && e != shooter) {
                                e.setVelocity(e.getVelocity().add(StatusEffectManager.calculateResistedVelocity((LivingEntity)e, center.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.15))));
                            }
                        }
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }

        // CHẠM LÀ XÓA MŨI TÊN (KHÔNG CHO NẢY NGƯỢC HAY GẮM VÀO NGƯỜI)
        arrow.remove();
    }
}