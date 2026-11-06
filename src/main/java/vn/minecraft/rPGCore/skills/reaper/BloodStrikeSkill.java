package vn.minecraft.rPGCore.skills.reaper;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.AbstractEnhancementSkill;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BloodStrikeSkill extends AbstractEnhancementSkill {

    public BloodStrikeSkill(JavaPlugin plugin) { super(plugin); }

    @Override public String getId() { return "reaper_blood_strike"; }
    @Override public String getName() { return "Cường Hóa Máu"; }
    @Override public String getRequiredClass() { return "reaper"; }
    @Override public Material getIcon() { return Material.NETHERITE_HOE; }
    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) {
        double cost = 20.0;
        if (level >= 3) cost += 5.0;
        if (level >= 6) cost += 5.0;
        if (level >= 9) cost += 10.0;
        return cost;
    }

    @Override
    public long getCooldown(int level) { return 5000L; } // GCD Luôn luôn là 5s

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLvl = Math.max(1, level);
        double dmg1 = 170 + ((displayLvl - 1) * 20);
        double trueDmg = 1 + ((displayLvl - 1) * 1);

        return List.of(
                "§7Hiến tế §c10% Máu hiện tại§7 nạp vào vũ khí.",
                "",
                "§e[Tích Tụ: Chờ 8 Giây]",
                "§f- §cĐợt 1 & 2: §fVũ khí rực đỏ, chém gây §c" + (int)dmg1 + "% ST",
                "§f  kèm §e" + (int)trueDmg + "% ST Chuẩn §f(Từ tổng máu hiến tế).",
                "§f- Đợt 2 gây sát thương Diện rộng 3x3.",
                "",
                "§4[Cuồng Huyết: Đợt 3]",
                "§f- Ép xung cơ thể trong §c12s cố định§f.",
                "§f- Chém gây ST diện rộng, đồng thời bản thân",
                "§f  tăng §c5% Sát thương§f & §c20% Hút máu§f.",
                "",
                "§8§o* Khi Cuồng Huyết hết 12s, trạng thái Đợt 3",
                "§8§osẽ bị KHÓA (Suy kiệt) trong 8s tiếp theo."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            long expire = player.getMetadata(cdKey).get(0).asLong();
            if (System.currentTimeMillis() < expire) {
                double timeLeft = (expire - System.currentTimeMillis()) / 1000.0;
                player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
                return false;
            }
        }

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực!");
            return false;
        }

        double currentHp = player.getHealth();
        double hpCost = currentHp * 0.10;
        if (currentHp - hpCost <= 1.0) {
            player.sendMessage("§cKhông đủ Sinh lực để hiến tế!");
            return false;
        }

        // ==========================================
        // 1. THANH TOÁN TÀI NGUYÊN (TRẢ MÁU LẤY SÁT THƯƠNG)
        // ==========================================
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        player.setHealth(currentHp - hpCost);

        double drawnBlood = hpCost;
        if (player.hasMetadata("Reaper_BS_Drawn_HP")) {
            drawnBlood += player.getMetadata("Reaper_BS_Drawn_HP").get(0).asDouble();
        }
        player.setMetadata("Reaper_BS_Drawn_HP", new FixedMetadataValue(plugin, drawnBlood));

        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        // ==========================================
        // 2. ĐIỀU PHỐI STAGE (LIÊN HOÀN 3 NHỊP)
        // ==========================================
        int currentStage = player.hasMetadata("Reaper_BS_Stage") ? player.getMetadata("Reaper_BS_Stage").get(0).asInt() : 0;

        // Kiểm tra xem Đợt 3 có đang bị KHÓA (Suy kiệt) không?
        boolean isFatigued = false;
        if (player.hasMetadata("Reaper_BS_Fatigue")) {
            if (System.currentTimeMillis() < player.getMetadata("Reaper_BS_Fatigue").get(0).asLong()) {
                isFatigued = true;
            } else {
                player.removeMetadata("Reaper_BS_Fatigue", plugin);
            }
        }

        // XỬ LÝ LÊN NHỊP:
        if (currentStage == 0) {
            player.setMetadata("Reaper_BS_Stage", new FixedMetadataValue(plugin, 1));
            applyWeaponEnhancement(player, "Reaper_BS_OnHit", 1, 160L, "§c§l[Cường Hóa Máu I] §fSẵn Sàng!", null);
            startTimeoutClear(player, 160L); // Hết 8s tự động reset Stage 1 về 0 (Không phạt)
        }
        else if (currentStage == 1) {
            player.setMetadata("Reaper_BS_Stage", new FixedMetadataValue(plugin, 2));
            applyWeaponEnhancement(player, "Reaper_BS_OnHit", 2, 160L, "§c§l[Cường Hóa Máu II] §fSẵn Sàng!", null);
            startTimeoutClear(player, 160L); // Hết 8s tự động reset Stage 2 về 0 (Không phạt)
        }
        else if (currentStage >= 2) {
            // [CƠ CHẾ FATIGUE: Đang Suy Kiệt -> ÉP VỀ GIAI ĐOẠN 2]
            if (isFatigued) {
                player.setMetadata("Reaper_BS_Stage", new FixedMetadataValue(plugin, 2)); // Ép thành 2, không cho lên 3
                applyWeaponEnhancement(player, "Reaper_BS_OnHit", 2, 160L, "§8§l[SUY KIỆT] §fVũ khí rực đỏ (Giai đoạn 2).", null);
                startTimeoutClear(player, 160L);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.5f);
            }
            // KHÔNG Suy Kiệt -> CHẠY GIAI ĐOẠN 3 (CUỒNG HUYẾT)
            else {
                player.setMetadata("Reaper_BS_Stage", new FixedMetadataValue(plugin, 3));
                applyWeaponEnhancement(player, "Reaper_BS_OnHit", 3, 100L, "§4§l[CUỒNG HUYẾT] §fVũ khí khát máu!", null);

                // Kích hoạt đồng hồ 12s Cuồng Huyết
                if (!player.hasMetadata("Reaper_Bloodlust_End")) {
                    player.sendMessage("§4§l[CUỒNG HUYẾT] §fBùng nổ sức mạnh trong 12s!");
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
                    startBloodlustTimer(player, 240L); // Đồng hồ 12s
                }
            }
        }

        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.8f);
        return true;
    }

    // ==========================================
    // CƠ CHẾ XÓA BUFF: HẾT 8S MÀ KHÔNG CHÉM -> TỰ ĐỘNG RESET, KHÔNG PHẠT
    // ==========================================
    private void startTimeoutClear(Player player, long delayTicks) {
        long taskId = System.currentTimeMillis();
        player.setMetadata("Reaper_BS_Task", new FixedMetadataValue(plugin, taskId));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                // Chỉ reset chuỗi về 0, KHÔNG gán án phạt Fatigue
                if (player.hasMetadata("Reaper_BS_Task") && player.getMetadata("Reaper_BS_Task").get(0).asLong() == taskId) {
                    player.removeMetadata("Reaper_BS_Stage", plugin);
                    player.removeMetadata("Reaper_BS_Task", plugin);
                    player.removeMetadata("Reaper_BS_Drawn_HP", plugin);
                    player.removeMetadata("Reaper_BS_OnHit", plugin);
                    player.sendMessage("§8§o(Huyết Kích tiêu tán...)");
                }
            }
        }.runTaskLater(plugin, delayTicks);
    }

    // ==========================================
    // CƠ CHẾ FATIGUE: KẾT THÚC 12S CUỒNG HUYẾT -> BỊ KHÓA ĐỢT 3 TRONG 8S
    // ==========================================
    private void startBloodlustTimer(Player player, long delayTicks) {
        long bloodlustId = System.currentTimeMillis();
        player.setMetadata("Reaper_Bloodlust_End", new FixedMetadataValue(plugin, bloodlustId));
        player.removeMetadata("Reaper_BS_Task", plugin); // Hủy đếm 8s của Giai đoạn 2

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                if (player.hasMetadata("Reaper_Bloodlust_End") && player.getMetadata("Reaper_Bloodlust_End").get(0).asLong() == bloodlustId) {

                    // Dọn dẹp Huyết Ấn
                    player.removeMetadata("Reaper_BS_Stage", plugin);
                    player.removeMetadata("Reaper_Bloodlust_End", plugin);
                    player.removeMetadata("Reaper_BS_Drawn_HP", plugin);
                    player.removeMetadata("Reaper_BS_OnHit", plugin);

                    // KHÓA GIAI ĐOẠN 3 (CUỒNG HUYẾT) TRONG 8 GIÂY TỚI
                    player.setMetadata("Reaper_BS_Fatigue", new FixedMetadataValue(plugin, System.currentTimeMillis() + 8000L));

                    player.sendMessage("§c§l[!] §4Cơ thể quá tải! Giai đoạn 3 bị khóa trong 8 giây.");
                    player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.5f);
                }
            }
        }.runTaskLater(plugin, delayTicks);
    }

    // ==========================================
    // XỬ LÝ ĐÒN ĐÁNH TAY TỪ REAPER CLASS (Đã được chặn lỗi 1 DMG ở ReaperClass.java)
    // ==========================================
    public void processOnHit(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, int level, int stageBuff) {
        double drawnBlood = attacker.hasMetadata("Reaper_BS_Drawn_HP") ? attacker.getMetadata("Reaper_BS_Drawn_HP").get(0).asDouble() : 0.0;

        double baseDmgMulti = 1.7 + ((level - 1) * 0.2);
        double trueDmgPct = 0.01 + ((level - 1) * 0.01);
        double trueDmgAmount = drawnBlood * trueDmgPct;

        attacker.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);

        if (stageBuff == 1) {
            double finalDmg = CombatManager.calculateDamage(attacker, rpgAttacker, target, baseDmgMulti, 0.0, false, false);

            attacker.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
            CombatManager.applySkillDamage(attacker, target, finalDmg + trueDmgAmount);
            attacker.removeMetadata("Ghost_L_Blocker", plugin);

            attacker.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);

            clearComboSafely(attacker);
        }
        else if (stageBuff == 2 || stageBuff == 3) {
            double centerDmgMulti = baseDmgMulti * 1.8;
            double aoeDmgMulti = centerDmgMulti * 0.8;

            attacker.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8f, 1.5f);
            attacker.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation().add(0, 1, 0), 1);

            double centerFinalDmg = CombatManager.calculateDamage(attacker, rpgAttacker, target, centerDmgMulti, 0.0, false, false);
            attacker.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
            CombatManager.applySkillDamage(attacker, target, centerFinalDmg + trueDmgAmount);
            attacker.removeMetadata("Ghost_L_Blocker", plugin);

            // Lan Sát thương - Sử dụng Lọc Đồng Minh theo tiêu chuẩn V2
            for (Entity e : target.getNearbyEntities(3, 2, 3)) {
                if (e instanceof LivingEntity && !e.equals(target) && CombatManager.canTarget(attacker, e)) {
                    LivingEntity aoeTarget = (LivingEntity) e;
                    double aoeFinalDmg = CombatManager.calculateDamage(attacker, rpgAttacker, aoeTarget, aoeDmgMulti, 0.0, false, false);

                    attacker.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                    CombatManager.applySkillDamage(attacker, aoeTarget, aoeFinalDmg + trueDmgAmount);
                    attacker.removeMetadata("Ghost_L_Blocker", plugin);

                    aoeTarget.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, aoeTarget.getLocation().add(0, 1, 0), 10, Material.REDSTONE_BLOCK.createBlockData());
                }
            }

            // [ĐÃ SỬA]: Xóa bỏ dòng IF.
            // Cứ chém trúng mục tiêu (dù ở Stage 2 hay Stage 3) là reset Chuỗi Combo về 0.
            // (Đồng hồ 12s Cuồng Huyết vẫn sẽ tiếp tục chạy ngầm vì clearComboSafely không động tới nó).
            clearComboSafely(attacker);
        }
    }

    private void clearComboSafely(Player attacker) {
        attacker.removeMetadata("Reaper_BS_Stage", plugin);
        attacker.removeMetadata("Reaper_BS_Task", plugin); // Vô hiệu hóa đếm giờ tự xóa của Giai đoạn 1/2
        attacker.removeMetadata("Reaper_BS_Drawn_HP", plugin);
    }
}