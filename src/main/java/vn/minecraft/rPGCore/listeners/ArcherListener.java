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

                // 1. TÌM VỊ TRÍ MẶT ĐẤT CHUẨN XÁC
                Location exactHitLoc;
                if (event.getHitEntity() != null) {
                    exactHitLoc = event.getHitEntity().getLocation().clone();
                } else {
                    exactHitLoc = arrow.getLocation().clone();
                    while (!exactHitLoc.getBlock().getType().isSolid() && exactHitLoc.getY() > exactHitLoc.getWorld().getMinHeight()) {
                        exactHitLoc.subtract(0, 1, 0);
                    }
                    exactHitLoc.setY(exactHitLoc.getBlockY() + 1.05);
                }

                // 2. TÍNH TOÁN DÂY LEO VÀ RỄ CÂY (Thêm biến phân loại màu)
                List<double[]> vines = new java.util.ArrayList<>();
                int vineCount = 28; // Tăng thêm chút xíu cho rậm rạp

                for (int i = 0; i < vineCount; i++) {
                    double r1 = Math.sqrt(Math.random()) * 4.5;
                    double angle1 = Math.random() * Math.PI * 2;
                    double startX = r1 * Math.cos(angle1);
                    double startZ = r1 * Math.sin(angle1);

                    double r2 = Math.sqrt(Math.random()) * 4.5;
                    double angle2 = Math.random() * Math.PI * 2;
                    double endX = r2 * Math.cos(angle2);
                    double endZ = r2 * Math.sin(angle2);

                    double maxHeight = 0.2 + (Math.random() * 0.4);

                    // Thêm biến type: 0.0 là Rễ (Nâu), 1.0 là Dây leo (Xanh)
                    double type = Math.random() > 0.5 ? 1.0 : 0.0;

                    // Lưu dữ liệu: {startX, startZ, endX, endZ, maxHeight, type}
                    vines.add(new double[]{startX, startZ, endX, endZ, maxHeight, type});
                }

                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 40) { this.cancel(); return; } // Tồn tại 2s

                        Location center = exactHitLoc.clone();

                        // ĐỊNH NGHĨA 3 BẢNG MÀU MỚI
                        // Màu Nâu Đất (Saddle Brown) cho Rễ cây
                        org.bukkit.Particle.DustOptions rootDust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(101, 67, 33), 0.9f);
                        // Màu Xanh Đậm (Forest Green) cho Dây leo dưới đất
                        org.bukkit.Particle.DustOptions groundVineDust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(34, 139, 34), 0.8f);
                        // Màu Xanh Lục Bảo/Dạ Quang (Lime Green) cho Vòng trói ma thuật
                        org.bukkit.Particle.DustOptions bindDust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(50, 205, 50), 0.7f);

                        // 1. VẼ MẶT ĐẤT: ĐAN XEN RỄ CÂY VÀ DÂY LEO
                        for (double[] v : vines) {
                            // Đọc biến type ở index 5 để quyết định màu
                            org.bukkit.Particle.DustOptions currentDust = (v[5] == 0.0) ? rootDust : groundVineDust;

                            for (double t = 0; t <= 1; t += 0.1) {
                                double curX = v[0] + (v[2] - v[0]) * t;
                                double curZ = v[1] + (v[3] - v[1]) * t;
                                double curY = 4 * v[4] * t * (1 - t);

                                center.getWorld().spawnParticle(org.bukkit.Particle.DUST, center.clone().add(curX, curY, curZ), 1, currentDust);
                            }
                        }

                        // 2. TRÓI CHÂN BẰNG MA PHÁP (Màu Xanh Dạ Quang nổi bật)
                        for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, 4.0, 2.0, 4.0)) {
                            if (e instanceof LivingEntity && e != shooter) {
                                LivingEntity target = (LivingEntity) e;
                                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 10));

                                Location entLoc = target.getLocation();
                                double spinSpeed = ticks * 0.4;
                                double entRadius = 0.55;

                                for (double y = 0; y <= 0.8; y += 0.2) {
                                    double a1 = spinSpeed + (y * Math.PI * 2);
                                    double x1 = entRadius * Math.cos(a1);
                                    double z1 = entRadius * Math.sin(a1);
                                    // Dùng bindDust (Xanh sáng) cho vòng trói
                                    entLoc.getWorld().spawnParticle(org.bukkit.Particle.DUST, entLoc.clone().add(x1, y, z1), 1, bindDust);

                                    double a2 = a1 + Math.PI;
                                    double x2 = entRadius * Math.cos(a2);
                                    double z2 = entRadius * Math.sin(a2);
                                    entLoc.getWorld().spawnParticle(org.bukkit.Particle.DUST, entLoc.clone().add(x2, y, z2), 1, bindDust);
                                }
                            }
                        }

                        // Hạt mầm tinh linh rải rác bay lên từ mặt đất
                        if (ticks % 5 == 0) {
                            center.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, center.clone().add((Math.random()*8)-4, Math.random()*2, (Math.random()*8)-4), 2);
                        }

                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
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

                Location exactHitLoc = arrow.getLocation();

                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        // Vẫn tồn tại 2s (40 ticks), nhưng hàm này giờ chạy mỗi 1 tick để animation mượt hơn
                        if (ticks >= 40) { this.cancel(); return; }

                        Location center = exactHitLoc.clone().add(0, 1.5, 0);

                        // 1. LÕI LỖ ĐEN (R = 0.4) - Giảm số lượng spawn xuống 20 vì giờ chạy mỗi 1 tick
                        org.bukkit.Particle.DustOptions blackDust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.BLACK, 1.5f);
                        for (int i = 0; i < 20; i++) {
                            double u = Math.random();
                            double v = Math.random();
                            double theta = 2 * Math.PI * u;
                            double phi = Math.acos(2 * v - 1);

                            double x = 0.4 * Math.sin(phi) * Math.cos(theta);
                            double y = 0.4 * Math.sin(phi) * Math.sin(theta);
                            double z = 0.4 * Math.cos(phi);

                            center.getWorld().spawnParticle(org.bukkit.Particle.DUST, center.clone().add(x, y, z), 1, blackDust);
                        }

                        // Khởi tạo Bụi màu Tím (Purple) cho viền bồi tụ (Không trọng lực, không bị rơi)
                        org.bukkit.Particle.DustOptions purpleDust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(138, 43, 226), 1.2f);

                        // 2. VÒNG BỒI TỤ TRONG (Quỹ đạo 1 hạt bay quanh R = 1.2)
                        // Bỏ vòng lặp for, chỉ tính 1 điểm dựa vào ticks
                        double angle1 = ticks * 0.4; // Tốc độ bay
                        double x1 = 1.2 * Math.cos(angle1);
                        double z1 = 1.2 * Math.sin(angle1);
                        double y1 = z1 * Math.sin(Math.toRadians(40)); // Nghiêng 40 độ
                        double finalZ1 = z1 * Math.cos(Math.toRadians(40));

                        center.getWorld().spawnParticle(org.bukkit.Particle.DUST, center.clone().add(x1, y1, finalZ1), 1, purpleDust);

                        // 3. VÒNG BỒI TỤ NGOÀI (Quỹ đạo 1 hạt bay quanh R = 1.8)
                        double angle2 = -ticks * 0.3; // Bay ngược chiều và chậm hơn chút
                        double x2 = 1.8 * Math.cos(angle2);
                        double z2 = 1.8 * Math.sin(angle2);
                        double y2 = z2 * Math.sin(Math.toRadians(-40)); // Nghiêng -40 độ
                        double finalZ2 = z2 * Math.cos(Math.toRadians(-40));

                        center.getWorld().spawnParticle(org.bukkit.Particle.DUST, center.clone().add(x2, y2, finalZ2), 1, purpleDust);

                        // Âm thanh rung chấn
                        if (ticks % 10 == 0) {
                            center.getWorld().playSound(center, org.bukkit.Sound.BLOCK_BEACON_AMBIENT, 2.0f, 0.5f);
                        }

                        // Lực hút quái vật (Lực kéo giảm xuống 0.15 vì giờ nó hút 20 lần/giây thay vì 10 lần)
                        for (org.bukkit.entity.Entity e : center.getWorld().getNearbyEntities(center, 5.0, 5.0, 5.0)) {
                            if (e instanceof LivingEntity && e != shooter) {
                                Vector pull = center.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.15);
                                e.setVelocity(e.getVelocity().add(pull));
                            }
                        }

                        ticks++; // Tăng 1 tick
                    }
                }.runTaskTimer(plugin, 0L, 1L); // ĐỔI THÀNH 1L: Chạy mỗi tick (siêu mượt)
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