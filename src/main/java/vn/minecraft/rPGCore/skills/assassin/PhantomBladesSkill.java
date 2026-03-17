package vn.minecraft.rPGCore.skills.assassin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class PhantomBladesSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PhantomBladesSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "assassin_phantom_blades"; }
    @Override public String getName() { return "Ám Khí Liên Hoàn"; }
    @Override public String getRequiredClass() { return "assassin"; }
    @Override public Material getIcon() { return Material.FLINT; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return (currentLevel == 4 || currentLevel == 9) ? 2 : 1; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return Collections.singletonMap("assassin_poison_dart", 3);
    }

    @Override
    public double getManaCost(int level) {
        return 40.0 + ((level - 1) * 4.0);
    }

    @Override
    public long getCooldown(int level) {
        double cd = 10.0 - ((level - 1) * 0.4);
        return (long) (Math.max(4.0, cd) * 1000);
    }

    private double getDamagePerBlade(int level) {
        return 0.80 + ((level - 1) * 0.15); // [Đã chỉnh lại Damage theo đúng công thức mới: 0.8 + 0.15/lv]
    }

    private int getBladeCount(int level) {
        if (level >= 10) return 7;
        if (level >= 5) return 5;
        return 3;
    }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        int dmg = (int) Math.round(getDamagePerBlade(displayLevel) * 100);
        int count = getBladeCount(displayLevel);

        lore.add("§7Xoay người phóng ra §e" + count + " Ám Khí§7 tản ra");
        lore.add("§7theo hình quạt về phía trước (Tầm xa 12m).");
        lore.add("");
        lore.add("§7Sát thương: §c" + dmg + "% §7(Mỗi phi tiêu)");
        lore.add("§8[Nội tại - Xuyên thấu]:");
        lore.add("§7Xuyên qua mọi kẻ địch trên đường bay.");
        lore.add("");
        lore.add("§8[Nội tại - Suy Giảm]:");
        lore.add("§7Mỗi Ám Khí trúng trùng lặp trên cùng 1 kẻ địch");
        lore.add("§7sẽ bị giảm §c8% §7Sát thương/lần trúng.");

        if (level < 10) {
            lore.add("");
            lore.add("§8§o(Cấp 5: Tăng số lượng phi tiêu lên 5)");
            if (level == 9) lore.add("§c§l(!) §cĐột phá Tối thượng (Cấp 10): Cần 2 SP");
        } else {
            lore.add("");
            lore.add("§6§l✦ ĐỘT PHÁ TỐI THƯỢNG ✦");
            lore.add("§e- Số lượng Ám Khí tăng lên §c7§e.");
            lore.add("§e- Kẻ địch trúng chiêu bị Gãy Xương, giảm");
            lore.add("§e  20% Tốc chạy trong 3 giây.");
        }
        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        Map<UUID, Integer> hitCounts = new HashMap<>();

        if (level <= 0) return false;

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực (Stamina)!");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long cd = getCooldown(level);
        if (currentTime - cooldowns.getOrDefault(player.getUniqueId(), 0L) < cd) {
            double timeLeft = (cd - (currentTime - cooldowns.get(player.getUniqueId()))) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        if (player.hasMetadata("Assassin_Stealth") || player.hasMetadata("Assassin_Smoke_Bomb_Active")) {
            player.removeMetadata("Assassin_Stealth", plugin);
            player.removeMetadata("Assassin_Smoke_Bomb_Active", plugin);
            player.sendMessage("§c§l[!] Bão Ám Khí bất ngờ! Hiện hình!");
            for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) p.showPlayer(plugin, player);
        }

        int fbLevel = 0;
        if (player.hasMetadata("Assassin_FatalBlow_Level")) {
            fbLevel = player.getMetadata("Assassin_FatalBlow_Level").get(0).asInt();
            player.removeMetadata("Assassin_FatalBlow_Level", plugin);
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.1);
            player.sendMessage("§c§l[!] §fÁm Khí Liên Hoàn được Cường hóa!");
        }

        player.sendMessage("§8§l[!] ÁM KHÍ LIÊN HOÀN!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.2f);

        int count = getBladeCount(level);
        double spreadAngle = 15.0;
        double startAngle = -(count - 1) * spreadAngle / 2.0;

        Location startLoc = player.getEyeLocation();
        Vector centerDir = startLoc.getDirection().normalize();

        for (int i = 0; i < count; i++) {
            double angle = startAngle + (i * spreadAngle);
            Vector bladeDir = rotateVectorY(centerDir.clone(), angle);
            launchBlade(player, rpgPlayer, level, startLoc.clone(), bladeDir, fbLevel, hitCounts);
        }

        return true;
    }

    private Vector rotateVectorY(Vector v, double degrees) {
        double rad = Math.toRadians(degrees);
        double currentX = v.getX();
        double currentZ = v.getZ();
        double cosine = Math.cos(rad);
        double sine = Math.sin(rad);
        return new Vector((cosine * currentX) - (sine * currentZ), v.getY(), (sine * currentX) + (cosine * currentZ)).normalize();
    }

    private void launchBlade(Player player, RPGPlayer rpgPlayer, int level, Location origin, Vector direction, int fbLevel, Map<UUID, Integer> hitCounts) {

        double baseDmgMulti = getDamagePerBlade(level);
        if (fbLevel > 0) {
            baseDmgMulti += 2.00 + ((fbLevel - 1) * 0.15);
        }
        final double finalDmgMulti = baseDmgMulti;

        new BukkitRunnable() {
            Location currentLoc = origin.clone();
            int distanceTravelled = 0;
            final Set<UUID> hitTargets = new HashSet<>();

            @Override
            public void run() {
                if (distanceTravelled >= 8 || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                currentLoc.add(direction.clone().multiply(1.5));

                currentLoc.getWorld().spawnParticle(Particle.CRIT, currentLoc, 1, 0, 0, 0, 0.05);
                currentLoc.getWorld().spawnParticle(Particle.SMOKE, currentLoc, 1, 0, 0, 0, 0);

                if (currentLoc.getBlock().getType().isSolid()) {
                    currentLoc.getWorld().playSound(currentLoc, Sound.BLOCK_STONE_HIT, 0.5f, 2.0f);
                    currentLoc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, currentLoc, 5, 0.1, 0.1, 0.1, currentLoc.getBlock().getBlockData());
                    this.cancel();
                    return;
                }

                for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.2, 1.2, 1.2)) {
                    if (e instanceof LivingEntity && e != player && !hitTargets.contains(e.getUniqueId())) {
                        LivingEntity target = (LivingEntity) e;
                        hitTargets.add(target.getUniqueId());

                        // Cơ chế Fall-off 8% mỗi hit
                        int timesHit = hitCounts.getOrDefault(target.getUniqueId(), 0);
                        double reduction = Math.min(0.80, timesHit * 0.08);
                        double currentDmgMulti = finalDmgMulti * (1.0 - reduction);
                        hitCounts.put(target.getUniqueId(), timesHit + 1);

                        // Đã xóa cơ chế cộng thêm Crit vô lý
                        double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, currentDmgMulti, 0.0, 0.0, 0.0, false, false);
                        CombatManager.applySkillDamage(player, target, finalDmg);

                        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);

                        if (level >= 10) {
                            vn.minecraft.rPGCore.managers.StatusEffectManager.applyEffect(target, org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1);
                        }
                    }
                }

                distanceTravelled++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}