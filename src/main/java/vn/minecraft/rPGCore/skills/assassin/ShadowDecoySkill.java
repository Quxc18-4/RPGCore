package vn.minecraft.rPGCore.skills.assassin;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class ShadowDecoySkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ShadowDecoySkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "assassin_shadow_decoy"; }
    @Override public String getName() { return "Thế Thân Bóng Tối"; }
    @Override public String getRequiredClass() { return "assassin"; }
    @Override public Material getIcon() { return Material.ARMOR_STAND; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return (currentLevel == 4 || currentLevel == 9) ? 2 : 1; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> req = new HashMap<>();
        req.put("assassin_smoke_bomb", 5);
        req.put("assassin_shadow_step",6);
        return req;
    }

    @Override
    public int getRequiredPlayerLevel() {
        return 18; // Yêu cầu Level 18
    }

    @Override
    public double getManaCost(int level) {
        double cost = 40.0;
        if (level >= 5) cost += 10.0;
        if (level >= 10) cost += 15.0;
        return cost;
    }

    @Override
    public long getCooldown(int level) {
        double cd = 14.0;
        if (level >= 5) cd -= 1.5;
        if (level >= 10) cd -= 2.0;
        return (long) (cd * 1000);
    }

    private double getDamageMultiplier(int level) {
        return 1.00 + ((level - 1) * 0.20); // Cấp 10 = 280%
    }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);
        int dmg = (int) Math.round(getDamageMultiplier(displayLevel) * 100);

        lore.add("§7Hóa thân thành làn khói lướt lùi về sau,");
        lore.add("§7tiến vào trạng thái §fTàng Hình §7trong §a3 giây§7,");
        lore.add("§7đồng thời phóng ra một phân thân bóng tối");
        lore.add("§7gây §c" + dmg + "% §7ST trên đường bay.");
        lore.add("");
        lore.add("§8[Nội tại - Mồi Nhử & Bạo Vũ]:");
        lore.add("§7Phân thân sẽ tự động khiêu khích quái vật.");
        lore.add("§7Khi bị chạm vào (hoặc sau 5s), phân thân vỡ nát,");
        lore.add("§7bắn §c12 Mũi Tên §7tản ra khắp các hướng.");
        lore.add("§7Sát thương: §c" + dmg + "% §7(Mỗi mũi tên)");
        lore.add("");
        lore.add("§8[Nội tại - Xuyên thấu]:");
        lore.add("§7Xuyên qua mọi kẻ địch trên đường bay.");
        lore.add("");
        lore.add("§8[Nội tại - Suy Giảm]:");
        lore.add("§7Tương tự Ám Khí Liên Hoàn, nếu nhiều hit trúng");
        lore.add("§7cùng 1 mục tiêu sẽ bị giảm §c8% §7ST mỗi hit.");

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực (Stamina)!");
            return false;
        }

        // 2. KIỂM TRA HỒI CHIÊU (Áp dụng ý tưởng của bạn)
        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);

        long baseCd = getCooldown(level); // Lấy thời gian gốc
        long finalCd = getFinalCooldown(rpgPlayer, baseCd); // Đưa qua máy ép CDR

        if (currentTime - lastUsed < finalCd) {
            double timeLeft = (finalCd - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        // 3. TRỪ NĂNG LƯỢNG VÀ LƯU COOLDOWN
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        startCooldownNotifier(player, finalCd);

        // ==========================================
        // [PHÁ ẨN THÂN]: LỘ DIỆN KHI TUNG CHIÊU TẤN CÔNG
        // ==========================================
        ISkill stealthSkill = RPGCore.getInstance().getSkillManager().getSkill("assassin_smoke_bomb");
        if (stealthSkill instanceof SmokeBombSkill) {
            ((SmokeBombSkill) stealthSkill).breakStealth(player);
        }

        int fbLevel = 0;
        if (player.hasMetadata("Assassin_FatalBlow_Level")) {
            fbLevel = player.getMetadata("Assassin_FatalBlow_Level").get(0).asInt();
            player.removeMetadata("Assassin_FatalBlow_Level", plugin);
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.1);
            player.sendMessage("§c§l[!] §fThế Thân Bóng Tối được Cường hóa!");
        }

        Location startLoc = player.getLocation();
        Vector forwardDir = startLoc.getDirection().setY(0).normalize();

        // Hiệu ứng khói nổ tung
        player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, startLoc.add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.05);

        // Player lùi về sau
        Vector backwardDir = forwardDir.clone().multiply(-1.5).setY(0.4);
        player.setVelocity(backwardDir);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);

        // ==========================================
        // [ĐÃ SỬA]: Áp dụng Tàng hình 3 giây từ hệ thống chuẩn
        // ==========================================
        if (stealthSkill instanceof SmokeBombSkill) {
            ((SmokeBombSkill) stealthSkill).applyStealth(player, 3);
        }

        // Tạo Tàng ảnh
        Location decoyLoc = player.getLocation().clone();
        spawnAndHandleDecoy(player, rpgPlayer, level, decoyLoc, fbLevel, forwardDir);

        player.sendMessage("§8§l[!] THẾ THÂN BÓNG TỐI!");

        if (rpgPlayer.getCurrentClass() instanceof vn.minecraft.rPGCore.classes.impl.AssassinClass) {
            ((vn.minecraft.rPGCore.classes.impl.AssassinClass) rpgPlayer.getCurrentClass()).addSkillStack(player);
        }

        return true;
    }

    private void spawnAndHandleDecoy(final Player player, final RPGPlayer rpgPlayer, final int level, Location loc, final int fbLevel, Vector direction) {
        final ArmorStand decoy = loc.getWorld().spawn(loc, ArmorStand.class);
        decoy.setBasePlate(false);
        decoy.setArms(true);
        decoy.setGravity(false);
        decoy.setCustomName("§8§lTàng Ảnh");
        decoy.setCustomNameVisible(true);

        ItemStack blackChest = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) blackChest.getItemMeta();
        meta.setColor(Color.BLACK);
        blackChest.setItemMeta(meta);

        ItemStack blackLegs = new ItemStack(Material.LEATHER_LEGGINGS);
        blackLegs.setItemMeta(meta.clone());

        ItemStack blackBoots = new ItemStack(Material.LEATHER_BOOTS);
        blackBoots.setItemMeta(meta.clone());

        decoy.getEquipment().setChestplate(blackChest);
        decoy.getEquipment().setLeggings(blackLegs);
        decoy.getEquipment().setBoots(blackBoots);
        decoy.getEquipment().setHelmet(new ItemStack(Material.WITHER_SKELETON_SKULL));

        loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1, 0), 30, 0.5, 1.0, 0.5, 0.05);

        Map<UUID, Integer> hitCounts = new HashMap<>(); // Dùng chung hitCounts cho cả Decoy bay và Nổ
        double dmgMulti = getDamageMultiplier(level);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxLifespan = 100;
            Location currentLoc = decoy.getLocation();
            double distanceTravelled = 0;
            final Set<UUID> hitTargets = new HashSet<>();

            @Override
            public void run() {
                if (!player.isOnline() || !decoy.isValid()) {
                    decoy.remove();
                    this.cancel();
                    return;
                }

                // Giai đoạn 1: Bay tới trước
                if (distanceTravelled < 10) {
                    currentLoc.add(direction.clone().multiply(0.5));
                    decoy.teleport(currentLoc);
                    distanceTravelled += 0.5;

                    for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.0, 1.5, 1.0)) {
                        if (e instanceof LivingEntity && CombatManager.canTarget(player, e) && !hitTargets.contains(e.getUniqueId())) {
                            LivingEntity target = (LivingEntity) e;
                            hitTargets.add(target.getUniqueId());

                            int timesHit = hitCounts.getOrDefault(target.getUniqueId(), 0);
                            double reduction = Math.min(0.80, timesHit * 0.08);
                            double currentDmgMulti = dmgMulti * (1.0 - reduction);
                            hitCounts.put(target.getUniqueId(), timesHit + 1);

                            double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, currentDmgMulti, 0.0, 0.0, 0.0, false, false);
                            // [ĐÃ SỬA]: Bọc cờ khóa Combo Ảo
                            player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                            CombatManager.applySkillDamage(player, target, finalDmg);
                            player.removeMetadata("Ghost_L_Blocker", plugin);

                            target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
                        }
                    }
                }

                // Giai đoạn 2: Đứng im khiêu khích
                if (ticks % 10 == 0) {
                    for (Entity e : decoy.getNearbyEntities(10.0, 5.0, 10.0)) {
                        if (e instanceof Mob) ((Mob) e).setTarget(decoy);
                    }
                }

                decoy.getWorld().spawnParticle(Particle.SMOKE, decoy.getLocation().add(0, 1, 0), 2, 0.3, 0.5, 0.3, 0);

                boolean shouldExplode = (ticks >= maxLifespan);
                if (!shouldExplode) {
                    for (Entity e : decoy.getNearbyEntities(1.5, 1.5, 1.5)) {
                        if (e instanceof Mob) { shouldExplode = true; break; }
                    }
                }

                if (shouldExplode) {
                    explodeDecoy(player, rpgPlayer, level, decoy.getLocation(), fbLevel, hitCounts);
                    decoy.remove();
                    this.cancel();
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void explodeDecoy(Player player, RPGPlayer rpgPlayer, int level, Location loc, int fbLevel, Map<UUID, Integer> hitCounts) {
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1, 0), 1);
        loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1, 0), 50, 0.5, 1.0, 0.5, 0.1);

        double baseMulti = getDamageMultiplier(level);
        if (fbLevel > 0) baseMulti += 2.00 + ((fbLevel - 1) * 0.15);

        for (int i = 0; i < 12; i++) {
            double angle = i * (Math.PI / 6);
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            Vector arrowDir = new Vector(x, 0, z).normalize();
            launchArrow(player, rpgPlayer, baseMulti, loc.clone().add(0, 1.0, 0), arrowDir, hitCounts);
        }
    }

    private void launchArrow(final Player player, final RPGPlayer rpgPlayer, final double dmgMulti, Location origin, Vector direction, Map<UUID, Integer> hitCounts) {
        new BukkitRunnable() {
            Location currentLoc = origin.clone();
            int distanceTravelled = 0;
            final Set<UUID> hitTargets = new HashSet<>();

            @Override
            public void run() {
                if (distanceTravelled >= 20 || !player.isOnline()) {
                    this.cancel(); return;
                }

                currentLoc.add(direction.clone().multiply(0.5));
                currentLoc.getWorld().spawnParticle(Particle.CRIT, currentLoc, 1, 0, 0, 0, 0.05);

                if (currentLoc.getBlock().getType().isSolid()) {
                    this.cancel(); return;
                }

                for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 0.8, 0.8, 0.8)) {
                    if (e instanceof LivingEntity && CombatManager.canTarget(player, e) && !hitTargets.contains(e.getUniqueId())) {
                        LivingEntity target = (LivingEntity) e;
                        hitTargets.add(target.getUniqueId());

                        int timesHit = hitCounts.getOrDefault(target.getUniqueId(), 0);
                        double reduction = Math.min(0.80, timesHit * 0.08);
                        double currentDmgMulti = dmgMulti * (1.0 - reduction);
                        hitCounts.put(target.getUniqueId(), timesHit + 1);

                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, currentDmgMulti, 0.0, 0.0, 0.0, false, false);
                        // [ĐÃ SỬA]: Bọc cờ khóa Combo Ảo
                        player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                        CombatManager.applySkillDamage(player, target, finalDmg);
                        player.removeMetadata("Ghost_L_Blocker", plugin);

                        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.5f);
                    }
                }
                distanceTravelled++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}