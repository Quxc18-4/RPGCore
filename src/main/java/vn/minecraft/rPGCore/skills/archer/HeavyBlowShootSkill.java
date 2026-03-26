package vn.minecraft.rPGCore.skills.archer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class HeavyBlowShootSkill implements ISkill, Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // Lưu trữ những người chơi vừa bắn Trọng Tiễn để lấy thông tin hoàn Mana
    private final Map<UUID, RPGPlayer> activeShooters = new HashMap<>();

    public HeavyBlowShootSkill(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override public String getId() { return "archer_heavy_blow"; }
    @Override public String getName() { return "Trọng Tiễn"; }
    @Override public String getRequiredClass() { return "archer"; }
    @Override public Material getIcon() { return Material.MACE; }
    @Override public int getMaxLevel() { return 3; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }

    @Override public int getRequiredPlayerLevel() { return 34; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return Collections.singletonMap("archer_triple_shot", 10);
    }

    @Override
    public double getManaCost(int level) {
        return 90.0 + (level - 1) * 15.0;
    }

    @Override
    public long getCooldown(int level) {
        double cd = 20.0 - (level - 1) * 1.5;
        return (long) (cd * 1000);
    }

    // [CẬP NHẬT]: Nerf 40% sát thương
    private double getDirectDmg(int level) { return (level == 3) ? 13.2 : (level == 2) ? 10.8 : 9.0; }
    private double getHitAoEMulti(int level) { return (level == 3) ? 3.6 : (level == 2) ? 2.8 : 2.4; }
    private double getMissAoEMulti(int level) { return (level == 3) ? 3.9 : (level == 2) ? 3.0 : 2.4; }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        lore.add("§7Nảy bổng lên không trung và tụ lực bắn ra");
        lore.add("§7một mũi tên mang sức mạnh hủy diệt.");

        lore.add("§7Sát thương (Trúng đích): §c" + (int)(getDirectDmg(displayLevel)*100) + "%");
        lore.add("§7Sát thương Nổ (Xung quanh): §6" + (int)(getHitAoEMulti(displayLevel)*100) + "%");
        lore.add("");
        lore.add("§e§lNẾU BẮN TRƯỢT:");
        lore.add("§7Sát thương Nổ: §6" + (int)(getMissAoEMulti(displayLevel)*100) + "%");
        lore.add("§7Hoàn trả: §b40% Mana §7đã tiêu hao");

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Mana!");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);

        long baseCd = getCooldown(level); // Lấy thời gian gốc
        long finalCd = getFinalCooldown(rpgPlayer, baseCd); // Đưa qua máy ép CDR

        if (currentTime - lastUsed < finalCd) {
            double timeLeft = (finalCd - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        startCooldownNotifier(player, finalCd);

        player.sendMessage("§c§l[!] TRỌNG TIỄN!");

        Location currentEyeLoc = player.getEyeLocation();
        Vector lookDir = currentEyeLoc.getDirection();
        Vector backward = new Vector(-lookDir.getX(), 0, -lookDir.getZ());

        if (backward.lengthSquared() > 0) backward.normalize();
        else backward = new Vector(-1, 0, 0);

        Vector jumpVector = backward.multiply(0.8).setY(0.9);
        player.setVelocity(jumpVector);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                Location eyeLoc = player.getEyeLocation();
                Vector dir = eyeLoc.getDirection();

                Arrow arrow = player.getWorld().spawnArrow(eyeLoc, dir, 4.5f, 0f);
                arrow.setShooter(player);

                arrow.setMetadata("Is_Custom_Skill", new FixedMetadataValue(plugin, true));
                arrow.setMetadata("HeavyBlow_Level", new FixedMetadataValue(plugin, level));

                activeShooters.put(arrow.getUniqueId(), rpgPlayer);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!arrow.isValid() || arrow.isOnGround()) {
                            this.cancel();
                            return;
                        }
                        arrow.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, arrow.getLocation(), 2, 0, 0, 0, 0);
                    }
                }.runTaskTimer(plugin, 0L, 1L);

                new BukkitRunnable() {
                    @Override
                    public void run() { activeShooters.remove(arrow.getUniqueId()); }
                }.runTaskLater(plugin, 200L);
            }
        }.runTaskLater(plugin, 15L);

        return true;
    }

    @EventHandler
    public void onHeavyArrowHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        Arrow arrow = (Arrow) event.getEntity();

        if (!activeShooters.containsKey(arrow.getUniqueId())) return;
        if (arrow.hasMetadata("HeavyBlow_Exploded")) return;

        arrow.setMetadata("HeavyBlow_Exploded", new FixedMetadataValue(plugin, true));

        RPGPlayer rpgPlayer = activeShooters.remove(arrow.getUniqueId());
        Player shooter = (Player) arrow.getShooter();
        int level = arrow.getMetadata("HeavyBlow_Level").get(0).asInt();

        arrow.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, arrow.getLocation(), 1);
        arrow.getWorld().playSound(arrow.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

        boolean isCrossbow = shooter.getInventory().getItemInMainHand().getType() == Material.CROSSBOW;

        if (event.getHitEntity() != null && event.getHitEntity() instanceof org.bukkit.entity.LivingEntity) {
            org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) event.getHitEntity();

            // [ĐÃ SỬA TỐI ƯU]: Đẩy hệ số tính sát thương vào CombatManager để tận dụng Cơ chế RPG
            double directMulti = getDirectDmg(level);
            double extraArmorPen = 40.0; // Trọng Tiễn mặc định có 40% Xuyên giáp

            double finalDirectDmg = CombatManager.calculateDamage(shooter, rpgPlayer, target, directMulti, extraArmorPen, 0.0, 0.0, false, isCrossbow);
            // [FIX LỖI COMBO ẢO] - Sát thương trực tiếp
            shooter.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
            CombatManager.applySkillDamage(shooter, target, finalDirectDmg);
            shooter.removeMetadata("Ghost_L_Blocker", plugin);

            // Gây nổ AoE cho đám xung quanh
            double aoeMulti = getHitAoEMulti(level);
            damageNearby(arrow.getLocation(), shooter, rpgPlayer, target, aoeMulti, isCrossbow);

        } else if (event.getHitBlock() != null) {

            // --- TRƯỜNG HỢP: BẮN TRƯỢT XUỐNG ĐẤT ---
            double aoeMulti = getMissAoEMulti(level);
            damageNearby(arrow.getLocation(), shooter, rpgPlayer, null, aoeMulti, isCrossbow);

            double refund = getManaCost(level) * 0.40;
            double newMana = Math.min(rpgPlayer.getTotalMaxResource(), rpgPlayer.getCurrentResource() + refund);
            rpgPlayer.setCurrentResource(newMana);
            shooter.sendMessage("§b⚡ Đã hoàn lại " + (int)refund + " Mana do bắn trượt!");
        }

        arrow.remove();
    }

    private void damageNearby(Location loc, Player shooter, RPGPlayer rpgPlayer, org.bukkit.entity.Entity ignoreEntity, double dmgMulti, boolean isCrossbow) {
        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 3.0, 3.0, 3.0)) {
            if (e instanceof org.bukkit.entity.LivingEntity && e != shooter && e != ignoreEntity) {
                org.bukkit.entity.LivingEntity target = (org.bukkit.entity.LivingEntity) e;

                // Vụ nổ AoE cũng được nhận Chí Mạng, Xuyên Giáp từ Trang Bị
                double finalDmg = CombatManager.calculateDamage(shooter, rpgPlayer, target, dmgMulti, 40.0, 0.0, 0.0, false, isCrossbow);
                // [FIX LỖI COMBO ẢO] - Sát thương trực tiếp
                shooter.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                CombatManager.applySkillDamage(shooter, target, finalDmg);
                shooter.removeMetadata("Ghost_L_Blocker", plugin);
            }
        }
    }
}