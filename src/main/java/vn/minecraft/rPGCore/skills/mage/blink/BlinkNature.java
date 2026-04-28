package vn.minecraft.rPGCore.skills.mage.blink;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
import vn.minecraft.rPGCore.player.RPGPlayer;

import java.util.HashSet;
import java.util.Set;

public class BlinkNature implements IBlink {
    @Override
    public void executeBlink(Player player, RPGPlayer rpgPlayer, int skillLevel) {
        Location center = player.getLocation().clone(); // Đứng yên, không lướt
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
        center.getWorld().playSound(center, Sound.BLOCK_GRASS_PLACE, 1f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;
            double radius = 6.0;
            int durationTicks = (6 + (skillLevel / 2)) * 20;
            int slownessLevel = Math.min(3, (15 + (skillLevel * 5)) / 15); // Tỷ lệ làm chậm ~ Cấp độ 1->3

            int killCounter = 0;
            Set<LivingEntity> trackedEnemies = new HashSet<>();

            @Override
            public void run() {
                if (ticks >= durationTicks || !player.isOnline()) {
                    center.getWorld().playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1.2f);
                    this.cancel();
                    return;
                }

                // VISUAL: Vẽ Vòng bo Kết Giới
                if (ticks % 10 == 0) {
                    for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        Location pLoc = center.clone().add(x, 0.1, z);
                        center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, pLoc, 1, 0, 0, 0, 0);
                        center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, pLoc, 1, 0, 0, 0, 0);
                    }
                }

                // LOGIC: Xử lý mỗi giây (20 ticks)
                if (ticks % 20 == 0) {

                    // 1. Kiểm tra tiến độ Kills để Thăng cấp kết giới
                    for (LivingEntity ent : trackedEnemies) {
                        if (ent.isDead() || !ent.isValid()) {
                            killCounter++;
                        }
                    }
                    if (killCounter >= 5) {
                        int levelUps = killCounter / 5;
                        killCounter = killCounter % 5;
                        radius += (0.5 * levelUps); // Tăng 1m bán kính
                        durationTicks += (20 * levelUps); // Cộng thêm 2 giây
                        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                        center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center.clone().add(0, 1, 0), 50, radius, 0.5, radius, 0.1);
                        player.sendMessage("§a§l[Kết Giới] §fĐã hấp thụ sinh mệnh! Mở rộng & Kéo dài thời gian.");
                    }
                    trackedEnemies.clear();

                    // 2. Tác dụng lên Entity trong vùng
                    for (org.bukkit.entity.Entity ent : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                        if (ent instanceof LivingEntity) {
                            LivingEntity target = (LivingEntity) ent;

                            if (CombatManager.canTarget(player, target)) {
                                // [ĐÃ FIX TOÁN HỌC]: Cơ bản 15% + (Level * 5%)
                                double slowPercent = 0.15 + (skillLevel * 0.05);
                                // Quái vật: Chuyển đổi % Slow ra Amplifier của Potion (15% = Amp 0, 30% = Amp 1, 45% = Amp 2)
                                int mobAmplifier = Math.max(0, (int)(slowPercent / 0.15) - 1);
                                StatusEffectManager.applySlow(target, 30, slowPercent, mobAmplifier);
                            } else {
                                // Đồng minh / Bản thân: Hồi 0.5% Máu tối đa
                                if (!StatusEffectManager.isHealingBlocked(target)) {
                                    double maxHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                                    double healAmount = maxHp * 0.005;

                                    if (target instanceof Player) {
                                        RPGPlayer tRpg = RPGCore.getInstance().getPlayerManager().getRPGPlayer(target.getUniqueId());
                                        if (tRpg != null) tRpg.heal(healAmount); // Gọi hàm chuẩn của bạn
                                    } else {
                                        // Dành cho Pet/Đồng minh là Quái
                                        double newHp = Math.min(maxHp, target.getHealth() + healAmount);
                                        target.setHealth(newHp);
                                    }
                                    target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 2, 0), 1, 0.2, 0.2, 0.2, 0);
                                }
                            }
                        }
                    }

                    // 3. Nội tại ẩn: Gia tốc sinh trưởng Nông sản
                    // TODO: Tích hợp với Job Nông Dân (Farmer). Sau này lấy cấp độ nghề của người chơi
                    // để tăng tỷ lệ (hiện tại là 25%) hoặc buff thêm sản lượng khi thu hoạch trong kết giới.
                    int blockRadius = (int) radius;
                    for (int x = -blockRadius; x <= blockRadius; x++) {
                        for (int y = -2; y <= 2; y++) {
                            for (int z = -blockRadius; z <= blockRadius; z++) {
                                Block b = center.clone().add(x, y, z).getBlock();
                                if (b.getBlockData() instanceof Ageable) {
                                    Ageable ageable = (Ageable) b.getBlockData();
                                    if (ageable.getAge() < ageable.getMaximumAge() && Math.random() < 0.5) { // 50% tỷ lệ lớn lên mỗi giây
                                        ageable.setAge(ageable.getAge() + 1);
                                        b.setBlockData(ageable);
                                        b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0);
                                    }
                                }
                            }
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(RPGCore.getInstance(), 0L, 1L);
    }
}