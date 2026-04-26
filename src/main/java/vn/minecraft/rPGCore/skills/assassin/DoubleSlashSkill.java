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
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class DoubleSlashSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public DoubleSlashSkill(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "assassin_double_slash"; }
    @Override public String getName() { return "Đoạt Mệnh Song Đao"; }
    @Override public String getRequiredClass() { return "assassin"; }
    @Override public Material getIcon() { return Material.IRON_SWORD; }
    @Override public int getMaxLevel() { return 10; }

    @Override
    public double getManaCost(int level) {
        double cost = 25.0;
        if (level >= 3) cost += 5.0;
        if (level >= 6) cost += 5.0;
        if (level >= 9) cost += 10.0;
        return cost;
    }

    @Override
    public long getCooldown(int level) {
        double cd = 4.2;
        if (level >= 3) cd -= 0.4;
        if (level >= 6) cd -= 0.4;
        if (level >= 9) cd -= 0.4;
        return (long) (cd * 1000);
    }

    @Override
    public List<String> getDescription(int level) {
        double dmg = 145 + ((level - 1) * 10);
        return Arrays.asList(
                "§7Vung đao chém 2 nhát chéo càn quét phía trước.",
                "§7Gây sát thương: §c" + (int)dmg + "% §7mỗi nhát.",
                "",
                "§8[Nội tại - Ám sát]: ",
                "§7Nếu tấn công từ phía sau đối thủ,",
                "§7Chiêu thức được cộng thêm §e30% Tỉ lệ Chí mạng§7."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;
        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực (Stamina)!");
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
            player.sendMessage("§c§l[!] §fĐoạt Mệnh Song Đao được Cường hóa!");
        }

        final int finalFbLevel = fbLevel;

        performSlash(player, rpgPlayer, level, false, finalFbLevel);

        new BukkitRunnable() {
            @Override
            public void run() { performSlash(player, rpgPlayer, level, true, finalFbLevel); }
        }.runTaskLater(plugin, 4L);

        if (rpgPlayer.getCurrentClass() instanceof vn.minecraft.rPGCore.classes.impl.AssassinClass) {
            ((vn.minecraft.rPGCore.classes.impl.AssassinClass) rpgPlayer.getCurrentClass()).addSkillStack(player);
        }

        return true;
    }

    private void performSlash(Player player, RPGPlayer rpgPlayer, int level, boolean secondHit, int fbLevel) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, secondHit ? 1.5f : 1.0f);

        Location loc = player.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        Location spawnLoc = loc.clone().add(0, 1.2, 0).add(dir.clone().multiply(1.5));
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        Vector up = new Vector(0, 1, 0);

        spawnLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, spawnLoc, 1);

        for (double t = -1.2; t <= 1.2; t += 0.2) {
            Vector offset;
            if (!secondHit) offset = right.clone().multiply(t).add(up.clone().multiply(-t));
            else offset = right.clone().multiply(t).add(up.clone().multiply(t));

            Location particleLoc = spawnLoc.clone().add(offset);
            particleLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, particleLoc, 2, 0.05, 0.05, 0.05, 0.02);
            particleLoc.getWorld().spawnParticle(Particle.ASH, particleLoc, 3, 0.1, 0.1, 0.1, 0);
        }

        double dmgMulti = 1.45 + ((level - 1) * 0.10);
        if (fbLevel > 0) {
            dmgMulti += 2.00 + ((fbLevel - 1) * 0.15);
        }

        for (Entity e : loc.getWorld().getNearbyEntities(spawnLoc, 1.5, 1.5, 1.5)) {
            // [ĐÃ SỬA]: Tích hợp CombatManager.canTarget
            if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                LivingEntity target = (LivingEntity) e;
                Vector targetDir = target.getLocation().getDirection().setY(0).normalize();
                double dot = dir.dot(targetDir);

                if (dot > 0.5) {
                    player.setMetadata("DoubleSlash_Backstab", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 10);
                }

                double finalDmg = CombatManager.calculateDamage(player, rpgPlayer, target, dmgMulti, 0.0, 0.0, 0.0, false, false);
                if (player.hasMetadata("DoubleSlash_Backstab")) player.removeMetadata("DoubleSlash_Backstab", plugin);

                // [ĐÃ SỬA]: Bọc cờ khóa Combo Ảo
                player.setMetadata("Ghost_L_Blocker", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                CombatManager.applySkillDamage(player, target, finalDmg);
                player.removeMetadata("Ghost_L_Blocker", plugin);

                target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation().add(0, 1, 0), 10, org.bukkit.Material.REDSTONE_BLOCK.createBlockData());
            }
        }
    }

    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }
}