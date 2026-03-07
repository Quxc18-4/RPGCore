package vn.minecraft.rPGCore.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
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

        // KIỂM TRA TAY TRÁI (Mũi tên Custom)
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

        if (type == Material.BOW) {
            nextShootTime.put(player.getUniqueId(), currentTime + 1100);
            fireArrow(player, "BOW", customArrowType);
        } else if (type == Material.CROSSBOW) {
            nextShootTime.put(player.getUniqueId(), currentTime + 1800);
            fireArrow(player, "CROSSBOW", customArrowType);
        }
    }

    private void fireArrow(Player player, String weaponType, String customType) {
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setVelocity(arrow.getVelocity().multiply(1.5));
        arrow.setMetadata("WeaponType", new FixedMetadataValue(plugin, weaponType));

        if (customType != null) {
            arrow.setMetadata("CustomArrowType", new FixedMetadataValue(plugin, customType));
        }

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() { if (arrow != null && arrow.isValid()) arrow.remove(); }
        }.runTaskLater(plugin, 40L);
    }

    // =========================================
    // 2. CÔNG THỨC SÁT THƯƠNG (HIT ENTITY)
    // =========================================
    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Arrow arrow = (Arrow) event.getDamager();
        LivingEntity target = (LivingEntity) event.getEntity();

        if (!(arrow.getShooter() instanceof Player)) return;

        boolean isCustomSkill = arrow.hasMetadata("Is_Custom_Skill");
        boolean isNormalHit = arrow.hasMetadata("WeaponType");

        if (!isCustomSkill && !isNormalHit) return;

        Player player = (Player) arrow.getShooter();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        String weaponType = "BOW";
        if (isNormalHit) {
            weaponType = arrow.getMetadata("WeaponType").get(0).asString();
        } else {
            Material heldType = player.getInventory().getItemInMainHand().getType();
            if (heldType == Material.CROSSBOW) weaponType = "CROSSBOW";
        }

        double playerDmg = rpgPlayer.getAttributeData().getAttribute("STR") * 2.0;
        double weaponDmg = (weaponType.equals("BOW")) ? 15.0 : 25.0;

        double targetArmor = 0;
        // Đã FIX LỖI 1.21.1
        AttributeInstance armorAttr = target.getAttribute(Attribute.ARMOR);
        if (armorAttr != null) targetArmor = armorAttr.getValue();

        // --- XỬ LÝ 8 MŨI TÊN ĐẶC BIỆT ---
        if (arrow.hasMetadata("CustomArrowType")) {
            String cType = arrow.getMetadata("CustomArrowType").get(0).asString();
            double dmgMulti = 1.0;

            if (cType.equals("arrow_ice")) {
                event.setCancelled(true);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 10));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!target.isDead()) target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                    }
                }.runTaskLater(plugin, 20L);
                return;
            }
            else if (cType.equals("arrow_fire")) { dmgMulti = 1.2; target.setFireTicks(100); }
            else if (cType.equals("arrow_lightning")) { dmgMulti = 1.5; target.getWorld().strikeLightningEffect(target.getLocation()); target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 10)); }
            else if (cType.equals("arrow_explosive")) { dmgMulti = 1.35; }
            else if (cType.equals("arrow_poison")) { dmgMulti = 0.8; target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0)); target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 0)); }

            double rawDamage = (playerDmg + weaponDmg - targetArmor) * dmgMulti;
            event.setDamage(Math.max(1.0, rawDamage));

            if (cType.equals("arrow_lifesteal")) {
                double healAmt = event.getFinalDamage() * 0.15;
                double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                player.setHealth(Math.min(maxHp, player.getHealth() + healAmt));
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 3, 0.5, 0.5, 0.5, 0);
            }

            removeIFrame(target);
            return;
        }

        // --- XỬ LÝ SÁT THƯƠNG KỸ NĂNG CƠ BẢN ---
        if (isCustomSkill) {
            double dmgMultiplier = arrow.getMetadata("Skill_DmgMulti").get(0).asDouble();
            double ignoreChance = arrow.getMetadata("Skill_ArmorPen").get(0).asDouble();

            if (arrow.hasMetadata("Quad_Effect")) {
                String effect = arrow.getMetadata("Quad_Effect").get(0).asString();
                if (effect.equals("LIGHTNING")) { target.getWorld().strikeLightningEffect(target.getLocation()); dmgMultiplier = 3.0; }
                else if (effect.equals("FREEZE")) { target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 10)); target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, org.bukkit.Bukkit.createBlockData(Material.ICE)); new BukkitRunnable() { @Override public void run() { if (!target.isDead()) target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1)); } }.runTaskLater(plugin, 30L); }
                else if (effect.equals("FLAME")) { target.setFireTicks(60); }
                else if (effect.equals("POISON")) { target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1)); }
            }

            if (Math.random() <= ignoreChance) { targetArmor = 0; player.sendMessage("§b§lXUYÊN GIÁP! §fMũi tên kỹ năng đã phớt lờ giáp của mục tiêu!"); }

            double rawDamage = (playerDmg + weaponDmg - targetArmor) * dmgMultiplier;
            event.setDamage(Math.max(1.0, rawDamage));
            removeIFrame(target);
            return;
        }

        // --- XỬ LÝ ĐÁNH THƯỜNG ---
        double finalDamage = 0;
        if (weaponType.equals("BOW")) { finalDamage = Math.max(1.0, playerDmg + weaponDmg - targetArmor) * 0.95; }
        else if (weaponType.equals("CROSSBOW")) {
            if (Math.random() <= 0.33) targetArmor = 0;
            finalDamage = Math.max(1.0, playerDmg + weaponDmg - targetArmor) * 1.55;
        }

        event.setDamage(finalDamage);
        removeIFrame(target);
    }

    // =========================================
    // 3. HIỆU ỨNG KHU VỰC (AOE)
    // =========================================
    @EventHandler
    public void onArrowHitTarget(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        Arrow arrow = (Arrow) event.getEntity();

        Location hitLoc = event.getHitBlock() != null ? event.getHitBlock().getLocation() :
                (event.getHitEntity() != null ? event.getHitEntity().getLocation() : arrow.getLocation());

        if (arrow.hasMetadata("CustomArrowType")) {
            String cType = arrow.getMetadata("CustomArrowType").get(0).asString();
            Player shooter = arrow.getShooter() instanceof Player ? (Player) arrow.getShooter() : null;

            if (cType.equals("arrow_nature")) {
                hitLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, hitLoc, 30, 2, 0.5, 2);
                for (org.bukkit.entity.Entity e : hitLoc.getWorld().getNearbyEntities(hitLoc, 4.0, 2.0, 4.0)) {
                    if (e instanceof LivingEntity && e != shooter) { ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 10)); }
                }
            }
            else if (cType.equals("arrow_ice") && event.getHitBlock() != null) {
                hitLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, hitLoc, 30, 1.5, 0.5, 1.5, 0.05);
                for (org.bukkit.entity.Entity e : hitLoc.getWorld().getNearbyEntities(hitLoc, 3.0, 2.0, 3.0)) {
                    if (e instanceof LivingEntity && e != shooter) { ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0)); }
                }
            }
            else if (cType.equals("arrow_explosive")) {
                hitLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 1);
                hitLoc.getWorld().playSound(hitLoc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                for (org.bukkit.entity.Entity e : hitLoc.getWorld().getNearbyEntities(hitLoc, 4.0, 4.0, 4.0)) {
                    if (e instanceof LivingEntity && e != shooter) {
                        Vector knockback = e.getLocation().toVector().subtract(hitLoc.toVector()).normalize().multiply(1.5).setY(0.5);
                        e.setVelocity(knockback);
                    }
                }
            }
            else if (cType.equals("arrow_blackhole")) {
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 40) { this.cancel(); return; }
                        hitLoc.getWorld().spawnParticle(Particle.PORTAL, hitLoc.clone().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                        hitLoc.getWorld().playSound(hitLoc, org.bukkit.Sound.BLOCK_BEACON_AMBIENT, 0.5f, 2.0f);
                        for (org.bukkit.entity.Entity e : hitLoc.getWorld().getNearbyEntities(hitLoc, 5.0, 5.0, 5.0)) {
                            if (e instanceof LivingEntity && e != shooter) {
                                Vector pull = hitLoc.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.2);
                                e.setVelocity(e.getVelocity().add(pull));
                            }
                        }
                        ticks += 2;
                    }
                }.runTaskTimer(plugin, 0L, 2L);
            }
        }

        if (arrow.hasMetadata("WeaponType") || arrow.hasMetadata("Is_Custom_Skill") || arrow.hasMetadata("CustomArrowType")) {
            new BukkitRunnable() { @Override public void run() { if (arrow != null && arrow.isValid()) arrow.remove(); } }.runTaskLater(plugin, 20L);
        }
    }

    @EventHandler
    public void onVanillaShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) event.setCancelled(true);
    }

    private void removeIFrame(LivingEntity target) {
        new BukkitRunnable() {
            @Override
            public void run() { if (target != null && !target.isDead()) target.setNoDamageTicks(0); }
        }.runTaskLater(plugin, 1L);
    }
}