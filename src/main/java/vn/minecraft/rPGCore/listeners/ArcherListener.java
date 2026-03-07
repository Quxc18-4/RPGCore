package vn.minecraft.rPGCore.listeners;

import org.bukkit.Material;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArcherListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerManager playerManager;
    private final Map<UUID, Long> nextShootTime = new HashMap<>();

    public ArcherListener(JavaPlugin plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
    }

    // =========================================
    // 1. CƠ CHẾ CLICK TRÁI BẮN CUNG/NỎ
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

        if (type == Material.BOW) {
            nextShootTime.put(player.getUniqueId(), currentTime + 1200);
            fireArrow(player, "BOW");
        } else if (type == Material.CROSSBOW) {
            nextShootTime.put(player.getUniqueId(), currentTime + 2000);
            fireArrow(player, "CROSSBOW");
        }
    }

    private void fireArrow(Player player, String weaponType) {
        Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setVelocity(arrow.getVelocity().multiply(1.5));
        arrow.setMetadata("WeaponType", new FixedMetadataValue(plugin, weaponType));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (arrow != null && arrow.isValid()) arrow.remove();
            }
        }.runTaskLater(plugin, 40L);
    }

    // =========================================
    // 2. CÔNG THỨC SÁT THƯƠNG (DAMAGE FORMULA)
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

        // Bỏ qua mũi tên lạ không thuộc hệ thống
        if (!isCustomSkill && !isNormalHit) return;

        Player player = (Player) arrow.getShooter();
        RPGPlayer rpgPlayer = playerManager.getRPGPlayer(player.getUniqueId());
        if (rpgPlayer == null) return;

        // Xác định loại vũ khí đang cầm để lấy Base Dmg
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
        AttributeInstance armorAttr = target.getAttribute(Attribute.ARMOR);
        if (armorAttr != null) targetArmor = armorAttr.getValue();

        double finalDamage = 0;

        // --- XỬ LÝ SÁT THƯƠNG KỸ NĂNG (Đọc Metadata) ---
        if (isCustomSkill) {
            double dmgMultiplier = arrow.getMetadata("Skill_DmgMulti").get(0).asDouble();
            double ignoreChance = arrow.getMetadata("Skill_ArmorPen").get(0).asDouble();

            // KIỂM TRA HIỆU ỨNG TỨ TIỄN (CẤP 6)
            if (arrow.hasMetadata("Quad_Effect")) {
                String effect = arrow.getMetadata("Quad_Effect").get(0).asString();

                if (effect.equals("LIGHTNING")) {
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    dmgMultiplier = 3.0; // Tăng sát thương mũi tên lên 300%
                }
                else if (effect.equals("FREEZE")) {
                    // Khóa di chuyển 1.5s (30 ticks) bằng Slowness cấp 10
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 30, 10));
                    // Hạt Particle Băng dày đặc thay thế cho việc đặt block vật lý (tránh lỗi kẹt map)
                    target.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, target.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, org.bukkit.Bukkit.createBlockData(org.bukkit.Material.ICE));

                    // Delay 1.5s (30 ticks) sau đó áp dụng Slowness 30% (Cấp 2) trong 2s (40 ticks)
                    new org.bukkit.scheduler.BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!target.isDead()) {
                                target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 1));
                            }
                        }
                    }.runTaskLater(plugin, 30L);
                }
                else if (effect.equals("FLAME")) {
                    target.setFireTicks(60); // Đốt cháy 3s
                    // Vẫn giữ dmgMultiplier = 60% (mặc định của lv6)
                }
                else if (effect.equals("POISON")) {
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.POISON, 60, 1)); // Độc cấp 2 trong 3s
                    // Vẫn giữ dmgMultiplier = 60% (mặc định của lv6)
                }
            }

            if (Math.random() <= ignoreChance) {
                targetArmor = 0;
                player.sendMessage("§b§lXUYÊN GIÁP! §fMũi tên kỹ năng đã phớt lờ giáp của mục tiêu!");
            }

            double rawDamage = (playerDmg + weaponDmg - targetArmor) * dmgMultiplier;
            event.setDamage(Math.max(1.0, rawDamage));
            return; // Đã là kỹ năng thì bỏ qua bước đánh thường
        }

        // --- XỬ LÝ SÁT THƯƠNG ĐÁNH THƯỜNG ---
        if (weaponType.equals("BOW")) {
            double rawDamage = playerDmg + weaponDmg - targetArmor;
            finalDamage = Math.max(1.0, rawDamage) * 0.95;
        } else if (weaponType.equals("CROSSBOW")) {
            if (Math.random() <= 0.33) {
                targetArmor = 0;
                player.sendMessage("§b§lXUYÊN GIÁP! §fMũi tên nỏ đã phớt lờ giáp của mục tiêu!");
            }
            double rawDamage = playerDmg + weaponDmg - targetArmor;
            finalDamage = Math.max(1.0, rawDamage) * 1.55;
        }

        event.setDamage(finalDamage);
    }

    // =========================================
    // 3 & 4. CHẶN GỒNG CUNG VÀ DỌN DẸP
    // =========================================
    @EventHandler
    public void onVanillaShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) event.setCancelled(true);
    }

    @EventHandler
    public void onArrowHitTarget(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            if (arrow.hasMetadata("WeaponType") || arrow.hasMetadata("Is_Custom_Skill")) {
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() { if (arrow != null && arrow.isValid()) arrow.remove(); }
                }.runTaskLater(plugin, 20L);
            }
        }
    }
}