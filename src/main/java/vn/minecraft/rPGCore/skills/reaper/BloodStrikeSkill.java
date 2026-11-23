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
    public long getCooldown(int level) { return 5000L; }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLvl = Math.max(1, level);

        double dmg1 = 130 + ((displayLvl - 1) * 15);
        double trueDmg = 1 + ((displayLvl - 1) * 1);

        double dmg2Center = dmg1 * 1.6;
        double dmg2Aoe = dmg2Center * 0.8;

        return List.of(
                "§7Hiến tế Máu của bản thân để Cường hóa vũ khí.",
                "",
                "§e[Thi Triển - Cường Hóa Máu]",
                "§f- §cKích hoạt: §fĐòn đánh tiếp theo gây §c" + (int)dmg1 + "% ST",
                "§f  kèm §e" + (int)trueDmg + "% ST Chuẩn §ftương đương 10% máu.",
                "§f- §cTái kích họạt lần 2: §fGây §c" + (int)dmg2Center + "% ST§f lên mục tiêu",
                "§f  và §c" + (int)dmg2Aoe + "% ST§f cho kẻ địch xung quanh.",
                "§8§o(Duy trì 8s, nếu không tấn công sẽ tự động tiêu tán)",
                "",
                "§4[Nội Tại - Cuồng Huyết]",
                "§f- Tái kích hoạt lần 3. Kích hoạt Cuồng Huyết trong §c12s§f.",
                "§f- Giúp tăng §c5% Sát thương§f và §c20% Hút máu§f.",
                "§8§o(Hết Cuồng huyết, rơi vào Suy kiệt trong 8s.)"
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

        int currentStage = player.hasMetadata("Reaper_BS_Stage") ? player.getMetadata("Reaper_BS_Stage").get(0).asInt() : 0;

        boolean isFatigued = false;
        if (player.hasMetadata("Reaper_BS_Fatigue")) {
            if (System.currentTimeMillis() < player.getMetadata("Reaper_BS_Fatigue").get(0).asLong()) {
                isFatigued = true;
            } else {
                player.removeMetadata("Reaper_BS_Fatigue", plugin);
            }
        }

        if (currentStage == 0) {
            player.setMetadata("Reaper_BS_Stage", new FixedMetadataValue(plugin, 1));
            applyWeaponEnhancement(player, "Reaper_BS_OnHit", 1, 160L, "§c§l[Cường Hóa Máu I] §fSẵn Sàng!", null);
            startTimeoutClear(player, 160L);
        }
        else if (currentStage == 1) {
            player.setMetadata("Reaper_BS_Stage", new FixedMetadataValue(plugin, 2));
            applyWeaponEnhancement(player, "Reaper_BS_OnHit", 2, 160L, "§c§l[Cường Hóa Máu II] §fSẵn Sàng!", null);
            startTimeoutClear(player, 160L);
        }
        else if (currentStage >= 2) {
            if (isFatigued) {
                player.setMetadata("Reaper_BS_Stage", new FixedMetadataValue(plugin, 2));
                applyWeaponEnhancement(player, "Reaper_BS_OnHit", 2, 160L, "§8§l[SUY KIỆT] §fChưa thể vào trạng thái §4§l[CUỒNG HUYẾT] lúc này.", null);
                startTimeoutClear(player, 160L);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.5f);
            }
            else {
                player.setMetadata("Reaper_BS_Stage", new FixedMetadataValue(plugin, 3));
                applyWeaponEnhancement(player, "Reaper_BS_OnHit", 3, 100L, "§4§l[CUỒNG HUYẾT] §fTrạng thái duy trì trong 12s!", null);

                if (!player.hasMetadata("Reaper_Bloodlust_End")) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
                    startBloodlustTimer(player, 240L);
                }
            }
        }

        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.8f);
        return true;
    }

    private void startTimeoutClear(Player player, long delayTicks) {
        long taskId = System.currentTimeMillis();
        player.setMetadata("Reaper_BS_Task", new FixedMetadataValue(plugin, taskId));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                if (player.hasMetadata("Reaper_BS_Task") && player.getMetadata("Reaper_BS_Task").get(0).asLong() == taskId) {
                    player.removeMetadata("Reaper_BS_Stage", plugin);
                    player.removeMetadata("Reaper_BS_Task", plugin);
                    player.removeMetadata("Reaper_BS_Drawn_HP", plugin);
                    player.removeMetadata("Reaper_BS_OnHit", plugin);
                    player.sendMessage("§8§o(Cường Hóa hết hiệu lực...)");
                }
            }
        }.runTaskLater(plugin, delayTicks);
    }

    private void startBloodlustTimer(Player player, long delayTicks) {
        long bloodlustId = System.currentTimeMillis();
        player.setMetadata("Reaper_Bloodlust_End", new FixedMetadataValue(plugin, bloodlustId));
        player.removeMetadata("Reaper_BS_Task", plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                if (player.hasMetadata("Reaper_Bloodlust_End") && player.getMetadata("Reaper_Bloodlust_End").get(0).asLong() == bloodlustId) {

                    // [LÕI CỦA BẢN FIX]: Kiểm tra Stage hiện tại trước khi xóa!
                    int currentStage = player.hasMetadata("Reaper_BS_Stage") ? player.getMetadata("Reaper_BS_Stage").get(0).asInt() : 0;

                    // NẾU người chơi VẪN ĐANG ở Cuồng huyết (Stage 3) hoặc đã lụi tàn tự nhiên (0) thì mới dọn dẹp.
                    // CÒN NẾU họ đang ở Stage 1 hoặc 2 (build chuỗi mới sau khi chém) thì TUYỆT ĐỐI KHÔNG XÓA STAGE của họ!
                    if (currentStage >= 3 || currentStage == 0) {
                        player.removeMetadata("Reaper_BS_Stage", plugin);
                        player.removeMetadata("Reaper_BS_Drawn_HP", plugin);
                        player.removeMetadata("Reaper_BS_OnHit", plugin);
                    }

                    // Xóa cờ End
                    player.removeMetadata("Reaper_Bloodlust_End", plugin);

                    // KHÓA CUỒNG HUYẾT TRONG 8 GIÂY TỚI
                    player.setMetadata("Reaper_BS_Fatigue", new FixedMetadataValue(plugin, System.currentTimeMillis() + 8000L));

                    player.sendMessage("§c§l[!] §4Cơ thể suy kiệt! Không thể kích hoạt Cuồng Huyết trong 8s tới.");
                    player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.5f);
                }
            }
        }.runTaskLater(plugin, delayTicks);
    }

    public void processOnHit(Player attacker, RPGPlayer rpgAttacker, LivingEntity target, int level, int stageBuff) {
        double drawnBlood = attacker.hasMetadata("Reaper_BS_Drawn_HP") ? attacker.getMetadata("Reaper_BS_Drawn_HP").get(0).asDouble() : 0.0;
        double baseDmgMulti = 1.3 + ((level - 1) * 0.15);
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
            // [CÂN BẰNG]: Giảm hệ số cường hóa từ 1.8 xuống 1.6
            double centerDmgMulti = baseDmgMulti * 1.6;
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

            // Gọn gàng nhất: Dù chém ở Stage 2 hay 3, đều dọn dẹp sạch sẽ để nhường chỗ cho chuỗi mới.
            clearComboSafely(attacker);
        }
    }

    private void clearComboSafely(Player attacker) {
        attacker.removeMetadata("Reaper_BS_Stage", plugin);
        attacker.removeMetadata("Reaper_BS_Task", plugin);
        attacker.removeMetadata("Reaper_BS_Drawn_HP", plugin);
    }
}