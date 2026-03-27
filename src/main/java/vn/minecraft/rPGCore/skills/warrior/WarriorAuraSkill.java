package vn.minecraft.rPGCore.skills.warrior;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.AbstractHitCounterSkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarriorAuraSkill extends AbstractHitCounterSkill {

    @Override public String getId() { return "warrior_aura"; }
    @Override public String getName() { return "§c§lKiếm Khí Huyết Sát"; }
    @Override public String getRequiredClass() { return "warrior"; }
    @Override public Material getIcon() { return Material.IRON_SWORD; }

    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; } // Tốn 1 SP
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override public double getManaCost(int level) { return 0; }
    @Override public long getCooldown(int level) { return 0; }

    @Override protected int getRequiredHits() { return 4; } // Đòn thứ 4 kích hoạt
    @Override protected long getComboTimeout() { return 2500L; } // 2.5s đứt chuỗi

    @Override
    public List<String> getDescription(int level) {
        double normalDmg = (0.5 + (level * 0.05)) * 100;
        double empowerDmg = (1.4 + (level * 0.1)) * 100;

        return Arrays.asList(
                "§7Nghệ thuật chém gió thành bão của Chiến Binh.",
                "",
                "§e[Nội Tại: Chuỗi Combo]",
                "§f- Mỗi đòn đánh thường tích 1 điểm Combo.",
                "§f- Đạt §a4 điểm Combo§f, phóng ra một luồng Kiếm Khí.",
                "§f  gây §e" + (int)normalDmg + "% Sát thương vật lý§f.",
                "",
                "§c[Thức Tỉnh Kỹ Năng]",
                "§f- Nếu Nộ Khí §c>= 30§f, tự động tiêu hao để Thức Tỉnh:",
                "§f  + Kiếm Khí hóa Đỏ, bay xa hơn (7 block).",
                "§f  + Gây §c" + (int)empowerDmg + "% Sát thương chuẩn§f và Xuyên 20 Giáp.",
                "§f  + Hất tung mọi kẻ địch trên đường bay."
        );
    }

    @Override
    protected void onMaxStacksReached(Player player, RPGPlayer rpgPlayer, LivingEntity lastTarget, int skillLevel) {
        boolean isEmpowered = rpgPlayer.getCurrentResource() >= 30;

        if (isEmpowered) {
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - 30);
            player.sendMessage("§c§l[THỨC TỈNH] §fKiếm Khí Huyết Sát!");
        }

        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();

        player.getWorld().playSound(startLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, isEmpowered ? 0.5f : 1.2f);
        if (isEmpowered) player.getWorld().playSound(startLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

        int maxDist = isEmpowered ? 7 : 5;

        new BukkitRunnable() {
            int distance = 0;

            @Override
            public void run() {
                if (distance > maxDist) { this.cancel(); return; }

                Location currentLoc = startLoc.clone().add(direction.clone().multiply(distance));
                Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();

                for (double w = -1.5; w <= 1.5; w += 0.5) {
                    Location pLoc = currentLoc.clone().add(right.clone().multiply(w));

                    if (isEmpowered) {
                        Particle.DustOptions redDust = new Particle.DustOptions(org.bukkit.Color.RED, 1.5f);
                        player.getWorld().spawnParticle(Particle.DUST, pLoc, 2, 0.1, 0.1, 0.1, 0, redDust);
                        player.getWorld().spawnParticle(Particle.LAVA, pLoc, 1, 0, 0, 0, 0);
                    } else {
                        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, pLoc, 1, 0, 0, 0, 0);
                    }
                }

                for (Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.5, 1.5, 1.5)) {
                    if (e instanceof LivingEntity && !(e instanceof ArmorStand) && CombatManager.canTarget(player, e)) {
                        LivingEntity target = (LivingEntity) e;

                        if (!target.hasMetadata("Aura_Hit_" + player.getUniqueId())) {
                            target.setMetadata("Aura_Hit_" + player.getUniqueId(), new FixedMetadataValue(plugin, true));

                            // Tỷ lệ sát thương Scale theo cấp độ kỹ năng
                            double multi = isEmpowered ? (1.4 + (skillLevel * 0.1)) : (0.5 + (skillLevel * 0.05));
                            double extraPen = isEmpowered ? 20.0 : 0.0;
                            double auraDmg = CombatManager.calculateDamage(player, rpgPlayer, target, multi, extraPen, false, false);

                            player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                            CombatManager.applySkillDamage(player, target, auraDmg);
                            player.removeMetadata("Ghost_L_Blocker", plugin);

                            if (isEmpowered) target.setVelocity(new Vector(0, 0.5, 0));

                            new BukkitRunnable() {
                                @Override public void run() { target.removeMetadata("Aura_Hit_" + player.getUniqueId(), plugin); }
                            }.runTaskLater(plugin, 5L);
                        }
                    }
                }
                distance++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}