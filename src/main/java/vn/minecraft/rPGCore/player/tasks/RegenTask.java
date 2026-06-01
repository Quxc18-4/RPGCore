package vn.minecraft.rPGCore.player.tasks;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.listeners.FoodListener;
import vn.minecraft.rPGCore.managers.FoodBuffManager;
import vn.minecraft.rPGCore.player.PlayerManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.ui.BossBarManager;

import java.util.Map;
import java.util.UUID;

public class RegenTask extends BukkitRunnable {

    private final PlayerManager playerManager;
    private final BossBarManager bossBarManager;

    public RegenTask(PlayerManager playerManager, BossBarManager bossBarManager) {
        this.playerManager = playerManager;
        this.bossBarManager = bossBarManager;
    }

    @Override
    public void run() {
        for (UUID uuid : playerManager.getAllPlayerUUIDs()) {
            RPGPlayer rpgPlayer = playerManager.getRPGPlayer(uuid);
            if (rpgPlayer == null) continue;

            Player player = rpgPlayer.getBukkitPlayer();
            if (player == null || !player.isOnline() || player.isDead()) continue;

            rpgPlayer.updateMaxStats();
            rpgPlayer.updateMoveSpeed();

            boolean hasFullnessBuff = FoodListener.fullnessTimers.containsKey(uuid) &&
                    FoodListener.fullnessTimers.get(uuid) > System.currentTimeMillis();

            int foodLevel = player.getFoodLevel();
            int playerLevel = player.getLevel();
            boolean inCombat = rpgPlayer.isInCombat();

            double maxHp = rpgPlayer.getTotalMaxHealth();
            double maxRes = rpgPlayer.getTotalMaxResource();
            double currentRes = rpgPlayer.getCurrentResource();

            // ==========================================
            // [MỚI]: HỆ THỐNG XẢ HỒ MÁU (LIFESTEAL POOL DRAINER)
            // (Chạy độc lập, không bị ảnh hưởng bởi In-Combat Debuff)
            // ==========================================
            if (player.hasMetadata("Lifesteal_Pool")) {
                double pool = player.getMetadata("Lifesteal_Pool").get(0).asDouble();

                if (pool > 0) {
                    // RÚT CỐ ĐỊNH: 20% Max HP từ Pool mỗi giây (Chống abuse hồi máu vụn vặt)
                    double drainAmount = Math.min(pool, maxHp * 0.20);
                    pool -= drainAmount;
                    player.setMetadata("Lifesteal_Pool", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), pool));

                    double currentHp = player.getHealth();
                    double excessHealing = 0.0; // [MỚI]: Biến hứng máu dư

                    if (currentHp < maxHp) {
                        double healAmount = Math.min(maxHp - currentHp, drainAmount);
                        player.setHealth(currentHp + healAmount);
                        excessHealing = drainAmount - healAmount; // [MỚI]: Máu bị tràn

                        // Nếu heal xong mà đầy bình -> Set cờ
                        if (player.getHealth() >= maxHp) {
                            player.setMetadata("Lifesteal_Full_Timer", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                        } else {
                            player.removeMetadata("Lifesteal_Full_Timer", RPGCore.getInstance());
                        }
                        player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 2, 0), 1, 0.3, 0.3, 0.3, 0);
                    } else {
                        excessHealing = drainAmount; // [MỚI]: Đang full máu thì toàn bộ là máu tràn
                        if (player.hasMetadata("Lifesteal_Full_Timer")) {
                            // Đã trôi qua 1s đầy máu -> XÓA SẠCH POOL
                            player.removeMetadata("Lifesteal_Pool", RPGCore.getInstance());
                            player.removeMetadata("Lifesteal_Full_Timer", RPGCore.getInstance());
                        } else {
                            // Mới đầy máu giây này, cho 1s ân hạn
                            player.setMetadata("Lifesteal_Full_Timer", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                        }
                    }

                    // ==========================================
                    // [SET 4] VAMPIRIC: CHUYỂN MÁU DƯ THÀNH KHIÊN ẢO
                    // ==========================================
                    if (excessHealing > 0 && rpgPlayer.hasSetBonus("vampiric", 4)) {
                        double currentShield = player.hasMetadata("RPG_Absorption_Shield") ? player.getMetadata("RPG_Absorption_Shield").get(0).asDouble() : 0.0;
                        double maxShieldBonus = maxHp * 0.20; // Tối đa 20% Max HP

                        if (currentShield < maxShieldBonus) {
                            double newShield = Math.min(maxShieldBonus, currentShield + excessHealing);
                            player.setMetadata("RPG_Absorption_Shield", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), newShield));
                            player.setMetadata("RPG_Absorption_Shield_Max", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), Math.max(maxShieldBonus, player.hasMetadata("RPG_Absorption_Shield_Max") ? player.getMetadata("RPG_Absorption_Shield_Max").get(0).asDouble() : 0.0)));
                            player.setAbsorptionAmount(20.0); // Hiển thị phun tim vàng Vanilla
                        }
                    }

                } else {
                    player.removeMetadata("Lifesteal_Pool", RPGCore.getInstance());
                    player.removeMetadata("Lifesteal_Full_Timer", RPGCore.getInstance());
                }
            }
            // ==========================================
            // 1. CƠ CHẾ TỤT THANH ĐÓI (ĐÃ NERF MẠNH)
            // ==========================================
            if (!hasFullnessBuff) {
                if (foodLevel > 10) {
                    player.setExhaustion(player.getExhaustion() + 0.05f); // Giảm từ 0.2 xuống 0.05
                }

                boolean isHealingNeeded = (player.getHealth() < maxHp) || (currentRes < maxRes);
                if (isHealingNeeded && foodLevel > 0) {
                    player.setExhaustion(player.getExhaustion() + 0.2f); // Giảm từ 1.4 xuống 0.2 (Rất chậm)
                }
            }

            if (foodLevel == 0) {
                player.damage(maxHp * 0.05);
            }

            // ==========================================
            // 3. CÔNG THỨC HỒI MÁU & MANA CƠ BẢN
            // ==========================================
            double hpRegen = 0.0;
            double resourceRegen = 0.0;
            double baseHpRegen = 20.0 + (2.0 * playerLevel);

            if (foodLevel >= 18) {
                hpRegen = baseHpRegen + (maxHp * (inCombat ? 0.005 : 0.015));
                resourceRegen = maxRes * 0.06; // [ĐÃ SỬA]: 8% -> 6%
            }
            else if (foodLevel >= 12) {
                hpRegen = baseHpRegen;
                resourceRegen = maxRes * 0.04; // [ĐÃ SỬA]: 5% -> 4%
            }
            else if (foodLevel >= 4) {
                hpRegen = baseHpRegen * 0.5;
                resourceRegen = maxRes * 0.02; // [ĐÃ SỬA]: 2% -> 2%
            }

            // ==========================================
            // 4. KÍCH HOẠT CHỈ SỐ HỒI PHỤC TỪ TRANG BỊ & NỘI TẠI
            // ==========================================
            Map<String, Double> stats = rpgPlayer.getTotalStatsMap();
            double bonusHpRegenPct = stats.getOrDefault("Tỉ lệ hồi phục", 0.0);
            double bonusManaRegenPct = stats.getOrDefault("Hồi Năng lượng", 0.0);

            hpRegen *= (1.0 + (bonusHpRegenPct / 100.0));
            resourceRegen *= (1.0 + (bonusManaRegenPct / 100.0));

            // [TRINKET]: CỘNG THẲNG NĂNG LƯỢNG TĨNH
            double staticManaRegen = stats.getOrDefault("Hồi Năng lượng cố định", 0.0);
            resourceRegen += staticManaRegen;

            // ==========================================
            // [SET 4] GIANT: THEO DÕI VỊ TRÍ & HỒI MÁU
            // ==========================================
            org.bukkit.Location currentLoc = player.getLocation();
            if (rpgPlayer.hasSetBonus("giant", 4)) {
                String locKey = currentLoc.getWorld().getName() + "_" + currentLoc.getBlockX() + "_" + currentLoc.getBlockY() + "_" + currentLoc.getBlockZ();
                String lastLocStr = player.hasMetadata("Giant_Last_Loc") ? player.getMetadata("Giant_Last_Loc").get(0).asString() : "";

                if (locKey.equals(lastLocStr)) {
                    int standTicks = player.hasMetadata("Giant_Stand_Ticks") ? player.getMetadata("Giant_Stand_Ticks").get(0).asInt() : 0;
                    standTicks += 20; // Task chạy mỗi 20 ticks (1s)
                    player.setMetadata("Giant_Stand_Ticks", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), standTicks));

                    if (standTicks >= 60) { // Đã đứng yên 3s
                        double giantBonusRegen = maxHp * 0.02; // Tính ra 2% Máu tối đa
                        hpRegen += giantBonusRegen;            // [ĐÂY LÀ CHỖ CỘNG VÀO HPREGEN]
                        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 2, 0), 2, 0.3, 0.1, 0.3, 0);
                    }
                } else {
                    player.setMetadata("Giant_Last_Loc", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), locKey));
                    player.setMetadata("Giant_Stand_Ticks", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), 0));
                }
            } else if (player.hasMetadata("Giant_Last_Loc")) {
                player.removeMetadata("Giant_Last_Loc", RPGCore.getInstance());
                player.removeMetadata("Giant_Stand_Ticks", RPGCore.getInstance());
            }

            // [TRINKET]: HỘ THỂ TUẦN HOÀN (MỖI 15 GIÂY)
            double periodicShieldPct = stats.getOrDefault("Hộ Thể Tuần Hoàn", 0.0);
            if (periodicShieldPct > 0 && !player.isDead()) {
                int shieldTimer = player.hasMetadata("Trinket_Shield_Timer") ? player.getMetadata("Trinket_Shield_Timer").get(0).asInt() : 15;
                shieldTimer--;

                if (shieldTimer <= 0) {
                    shieldTimer = 15; // Reset đồng hồ
                    double shieldAmount = maxHp * (periodicShieldPct / 100.0);

                    // Ghi đè (Overwrite) lớp Khiên Cũ
                    player.setMetadata("RPG_Absorption_Shield", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), shieldAmount));
                    player.setMetadata("RPG_Absorption_Shield_Max", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), shieldAmount));
                    player.setAbsorptionAmount(20.0); // Hiển thị phun tim vàng
                    player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_GOLD, 0.5f, 1.5f);
                }
                player.setMetadata("Trinket_Shield_Timer", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), shieldTimer));
            } else if (player.hasMetadata("Trinket_Shield_Timer")) {
                player.removeMetadata("Trinket_Shield_Timer", RPGCore.getInstance()); // Dọn rác nếu tháo nhẫn
            }
            // ==========================================
            // 5. HÌNH PHẠT GIAO TRANH
            // ==========================================
            if (inCombat) {
                resourceRegen *= 0.5;
            }

            // ==========================================
            // 6. FOOD BUFF
            // ==========================================
            double foodRegenBonus = RPGCore.getInstance().getFoodBuffManager().getBonus(uuid, FoodBuffManager.StatType.REGEN);
            if (foodRegenBonus != 0) {
                double multiplier = 1.0 + (foodRegenBonus / 100.0);
                hpRegen *= multiplier;
                resourceRegen *= multiplier;
            }

            // ==========================================
            // 6.5. CƠ CHẾ ĐỘC QUYỀN CỦA NỘ KHÍ (RAGE)
            // ==========================================
            if (rpgPlayer.getCurrentClass() != null && rpgPlayer.getCurrentClass().getResourceType() == vn.minecraft.rPGCore.classes.ResourceType.RAGE) {
                // Khóa van: Không cho tự hồi Nộ từ đồ ăn hay dòng chỉ số "Hồi Năng lượng"
                resourceRegen = 0.0;

                // [YÊU CẦU MỚI]: Hồi 1 nộ/giây TRONG combat, không bị tụt ngoài combat
                if (inCombat && currentRes < maxRes) {
                    resourceRegen = 1.0;
                }
            }

            // ==========================================
            // 7. CẬP NHẬT UI
            // ==========================================
            if (resourceRegen > 0 && currentRes < maxRes) {
                rpgPlayer.setCurrentResource(currentRes + resourceRegen);
            }

            if (hpRegen > 0 && player.getHealth() < maxHp) {
                rpgPlayer.heal(hpRegen);
            }

            bossBarManager.updateBars(player, rpgPlayer);
        }
    }
}