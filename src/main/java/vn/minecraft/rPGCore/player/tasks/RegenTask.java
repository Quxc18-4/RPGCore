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
            // ==========================================
            if (player.hasMetadata("Lifesteal_Pool")) {
                double pool = player.getMetadata("Lifesteal_Pool").get(0).asDouble();

                if (pool > 0) {
                    // [REAPER - NỘI TẠI 2]: Tăng tốc độ rút từ Pool lên 50% (Từ 20% -> 30% Max HP)
                    double drainMultiplier = player.hasMetadata("REAPER_FAST_POOL") ? 0.30 : 0.20;
                    double drainAmount = Math.min(pool, maxHp * drainMultiplier);

                    // ==========================================
                    // [DEBUG LOGGING] - THEO DÕI POOL & DRAIN
                    // ==========================================
                    RPGCore.getInstance().getLogger().info("[DEBUG-REGEN] " + player.getName() + " | Pool Hiện Tại: " + pool + " | Mức xả tick này: " + drainAmount);

                    pool -= drainAmount;
                    player.setMetadata("Lifesteal_Pool", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), pool));

                    double currentHp = player.getHealth();
                    double excessHealing = 0.0;

                    if (currentHp < maxHp) {
                        double healAmount = Math.min(maxHp - currentHp, drainAmount);
                        player.setHealth(currentHp + healAmount);
                        excessHealing = drainAmount - healAmount;

                        if (player.getHealth() >= maxHp) {
                            player.setMetadata("Lifesteal_Full_Timer", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                        } else {
                            player.removeMetadata("Lifesteal_Full_Timer", RPGCore.getInstance());
                        }
                        player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 2, 0), 1, 0.3, 0.3, 0.3, 0);

                        RPGCore.getInstance().getLogger().info("[DEBUG-REGEN] " + player.getName() + " | Đã hồi máu: " + healAmount + " | Máu Dư thừa (Excess): " + excessHealing);
                    } else {
                        excessHealing = drainAmount;
                        if (player.hasMetadata("Lifesteal_Full_Timer")) {
                            RPGCore.getInstance().getLogger().warning("[DEBUG-REGEN] " + player.getName() + " | Đã bị xóa Pool do Full máu quá 1s. Máu lãng phí: " + pool);
                            player.removeMetadata("Lifesteal_Pool", RPGCore.getInstance());
                            player.removeMetadata("Lifesteal_Full_Timer", RPGCore.getInstance());
                        } else {
                            player.setMetadata("Lifesteal_Full_Timer", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));
                            RPGCore.getInstance().getLogger().info("[DEBUG-REGEN] " + player.getName() + " | Vừa đầy máu, cắm cờ chờ xóa Pool ở tick sau.");
                        }
                    }

                    // ==========================================
                    // SHIELD LOGIC: SET VAMPIRIC & REAPER
                    // ==========================================
                    if (excessHealing > 0) {
                        double currentShield = player.hasMetadata("RPG_Absorption_Shield") ? player.getMetadata("RPG_Absorption_Shield").get(0).asDouble() : 0.0;
                        double shieldToAdd = 0.0;

                        // [REAPER - NỘI TẠI 3]: Chuyển 30% máu dư thành Khiên
                        if (player.hasMetadata("REAPER_OVERHEAL_SHIELD")) {
                            shieldToAdd = excessHealing * 0.30;
                        }
                        // Set 4 Vampiric: 100% máu dư thành Khiên
                        else if (rpgPlayer.hasSetBonus("vampiric", 4)) {
                            shieldToAdd = excessHealing;
                        }

                        if (shieldToAdd > 0) {
                            double maxShieldAllowed = maxHp * 0.50; // [FIX]: Giới hạn tối đa 50% HP
                            if (currentShield < maxShieldAllowed) {
                                double newShield = Math.min(maxShieldAllowed, currentShield + shieldToAdd);
                                player.setMetadata("RPG_Absorption_Shield", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), newShield));

                                RPGCore.getInstance().getLogger().info("[DEBUG-REGEN] " + player.getName() + " | Chuyển đổi Khiên: " + currentShield + " -> " + newShield + " (Giới hạn: " + maxShieldAllowed + ")");
                            }
                        }
                    }

                } else {
                    player.removeMetadata("Lifesteal_Pool", RPGCore.getInstance());
                    player.removeMetadata("Lifesteal_Full_Timer", RPGCore.getInstance());
                }
            }

            // ==========================================
            // [MỚI]: MÁY QUÉT GIÁP ẢO CHUẨN V2 (CÓ PHÂN LOẠI)
            // ==========================================
            if (player.hasMetadata("RPG_Absorption_Shield")) {
                double currentShield = player.getMetadata("RPG_Absorption_Shield").get(0).asDouble();

                // 1. NGƯỜI GÁC CỔNG: ÉP TRẦN MÁY CHỦ
                // Reaper nội tại cap 50%, nhưng Cuồng Huyết của Warrior cap tới 160%
                // Để hệ thống không xung đột, RegenTask sẽ nới trần tổng lên 200% Max HP (Giới hạn tối đa tuyệt đối)
                double absoluteMaxShield = maxHp * 2.0;
                if (currentShield > absoluteMaxShield) {
                    currentShield = absoluteMaxShield;
                    player.setMetadata("RPG_Absorption_Shield", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), currentShield));
                }

                // 2. CƠ CHẾ TAN BIẾN (CHỈ ÁP DỤNG CHO GIÁP MỀM)
                // Nếu người chơi KHÔNG CÓ thẻ "Hard_Shield_Active" -> Bị trừ 5%/s ngoài combat
                if (!inCombat && currentShield > 0 && !player.hasMetadata("Hard_Shield_Active")) {
                    double decayAmount = maxHp * 0.05;
                    currentShield = Math.max(0.0, currentShield - decayAmount);
                    player.setMetadata("RPG_Absorption_Shield", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), currentShield));
                }

                // 3. RENDER TIM VÀNG (UI TỰ ĐỘNG ĐIỀU CHỈNH)
                if (currentShield > 0) {
                    if (!player.hasPotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION)) {
                        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.ABSORPTION,
                                Integer.MAX_VALUE, 4, false, false, false));
                    }

                    // Vẽ UI dựa trên mốc 50% Máu (Người chơi có 50% máu = 10 tim vàng)
                    // Nếu Warrior bật Cuồng huyết (160% Máu), tim vàng sẽ kẹt ở mức tối đa 20.0 (10 tim) cho đến khi giáp rớt xuống dưới 50%
                    double shieldPercentage = currentShield / (maxHp * 0.50);
                    double uiAbsorptionAmount = shieldPercentage * 20.0;

                    player.setAbsorptionAmount(Math.max(1.0, Math.min(20.0, uiAbsorptionAmount)));
                } else {
                    player.removeMetadata("RPG_Absorption_Shield", RPGCore.getInstance());
                    player.setAbsorptionAmount(0.0);
                    player.removePotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION);
                }
            }
            // ==========================================
            // 1. CƠ CHẾ TỤT THANH ĐÓI (ĐÃ NERF MẠNH)
            // ==========================================
            if (!hasFullnessBuff) {
                if (foodLevel > 10) {
                    player.setExhaustion(player.getExhaustion() + 0.01f); // Giảm từ 0.2 xuống 0.05
                }

                boolean isHealingNeeded = (player.getHealth() < maxHp) || (currentRes < maxRes);
                if (isHealingNeeded && foodLevel > 0) {
                    player.setExhaustion(player.getExhaustion() + 0.05f); // Giảm từ 1.4 xuống 0.2 (Rất chậm)
                }
            }

            if (foodLevel == 0) {
                player.damage(maxHp * 0.03);
            }

            // ==========================================
            // 3. CÔNG THỨC HỒI MÁU & MANA CƠ BẢN
            // ==========================================
            double hpRegen = 0.0;
            double resourceRegen = 0.0;
            double baseHpRegen = 20.0 + (5.0 * playerLevel);

            if (foodLevel >= 18) {
                hpRegen = baseHpRegen + (maxHp * (inCombat ? 0.002 : 0.02));
                resourceRegen = maxRes * 0.12;
            }
            else if (foodLevel >= 12) {
                hpRegen = baseHpRegen;
                resourceRegen = maxRes * 0.1;
            }
            else if (foodLevel >= 4) {
                hpRegen = baseHpRegen * 0.5;
                resourceRegen = maxRes * 0.05;
            }

            // ==========================================
            // 4. KÍCH HOẠT CHỈ SỐ HỒI PHỤC TỪ TRANG BỊ & NỘI TẠI
            // ==========================================
            Map<String, Double> stats = rpgPlayer.getTotalStatsMap();
            double bonusHpRegenPct = stats.getOrDefault("Tỉ lệ hồi phục", 0.0);
            double bonusManaRegenPct = stats.getOrDefault("Hồi Năng lượng", 0.0);

            // [REAPER - NỘI TẠI 4]: Cộng thêm % Hồi phục dựa trên Máu đã mất
            if (rpgPlayer.getCurrentClass() != null) {
                bonusHpRegenPct += rpgPlayer.getCurrentClass().getDynamicHpRegenRate(player, rpgPlayer);
            }

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

            // ==========================================
            // [TRINKET]: HỘ THỂ TUẦN HOÀN (MỖI 15 GIÂY)
            // ==========================================
            double periodicShieldPct = stats.getOrDefault("Hộ Thể Tuần Hoàn", 0.0);
            if (periodicShieldPct > 0 && !player.isDead()) {
                int shieldTimer = player.hasMetadata("Trinket_Shield_Timer") ? player.getMetadata("Trinket_Shield_Timer").get(0).asInt() : 15;
                shieldTimer--;

                if (shieldTimer <= 0) {
                    shieldTimer = 15; // Reset đồng hồ
                    double shieldAmount = maxHp * (periodicShieldPct / 100.0);

                    // [CHUẨN RPGCORE V2]: Cộng dồn giáp và để luồng quét Giáp ở cuối RegenTask tự tính toán UI
                    double currentShield = player.hasMetadata("RPG_Absorption_Shield") ? player.getMetadata("RPG_Absorption_Shield").get(0).asDouble() : 0.0;
                    player.setMetadata("RPG_Absorption_Shield", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), currentShield + shieldAmount));

                    // Cấp thẻ Hard Shield nếu bạn KHÔNG muốn lớp khiên trang sức này bị tan biến 5%/s khi thoát combat
                    // (Tùy chọn: Nếu muốn nó bị tan biến như khiên hút máu thì xóa dòng dưới đi)
                    player.setMetadata("Hard_Shield_Active", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), true));

                    // Đã xóa dòng RPG_Absorption_Shield_Max
                    // Đã xóa lệnh setAbsorptionAmount(20.0) ép UI

                    player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_GOLD, 0.5f, 1.5f);
                }
                player.setMetadata("Trinket_Shield_Timer", new org.bukkit.metadata.FixedMetadataValue(RPGCore.getInstance(), shieldTimer));
            } else if (player.hasMetadata("Trinket_Shield_Timer")) {
                player.removeMetadata("Trinket_Shield_Timer", RPGCore.getInstance()); // Dọn rác nếu tháo nhẫn
                player.removeMetadata("Hard_Shield_Active", RPGCore.getInstance()); // Dọn thẻ nếu có
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