package vn.minecraft.rPGCore.skills.reaper;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.AbstractEnhancementSkill;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BloodDemonSkill extends AbstractEnhancementSkill {

    public BloodDemonSkill(JavaPlugin plugin) {
        super(plugin);
    }

    @Override public String getId() { return "reaper_blood_demon"; }
    @Override public String getName() { return "Huyết Quỷ"; }
    @Override public String getRequiredClass() { return "reaper"; }
    @Override public Material getIcon() { return Material.NETHER_WART; }

    // Đã nâng lên 6 cấp theo yêu cầu
    @Override public int getMaxLevel() { return 6; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) { return 30.0; }

    @Override
    public long getCooldown(int level) {
        double cd = 36.0 - level;
        return (long) (cd * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLvl = Math.max(1, level);

        // HỆ SỐ MỚI
        int meleeBuff = 42 + (displayLvl * 8); // Lv1: 50% -> Lv6: 90%
        int skillBuff = 15 + (displayLvl * 5); // Lv1: 20% -> Lv6: 45%
        double burnRate = 3.0 + (displayLvl * 1.0); // Lv1: 4% -> Lv6: 9%

        return List.of(
                "§7Đánh thức dòng máu ác quỷ chảy trong",
                "§7huyết quản, hóa thân thành Huyết Quỷ.",
                "",
                "§e[Kích Hoạt - Ác Quỷ Thức Tỉnh]",
                "§f- Yêu cầu tối thiểu §a80% Máu Tối Đa§f.",
                "§f- Khi kích hoạt, trạng thái duy trì liên tục",
                "§f  cho đến khi sinh lực chạm ngưỡng §c33%§f.",
                "",
                "§4[Nội Tại - Lời Nguyền Thiêu Đốt]",
                "§f- Thiêu đốt §c" + burnRate + "% Máu Tối Đa§f mỗi giây.",
                "",
                "§a[Nội Tại - Quyền Năng Huyết Quỷ]",
                "§f- Sát thương Đánh thường tăng §c" + meleeBuff + "%§f.",
                "§f- Sát thương Mọi Kỹ năng tăng §c" + skillBuff + "%§f."
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        String cdKey = "CD_" + getId();

        if (player.hasMetadata("BloodDemon_Active")) {
            player.sendMessage("§c§l[!] §fHuyết Quỷ đang hoạt động, không thể ngắt!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        if (player.hasMetadata(cdKey)) {
            if (System.currentTimeMillis() < player.getMetadata(cdKey).get(0).asLong()) {
                double timeLeft = (player.getMetadata(cdKey).get(0).asLong() - System.currentTimeMillis()) / 1000.0;
                player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
                return false;
            }
        }

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực!");
            return false;
        }

        double maxHp = rpgPlayer.getTotalMaxHealth();
        if (player.getHealth() < maxHp * 0.80) {
            player.sendMessage("§c§l[!] §fCần tối thiểu §a80% Máu Tối Đa§f để Hóa Quỷ!");
            player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.5f);
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);

        // ==========================================
        // GẮN CỜ TRẠNG THÁI (STATE FLAG) ĐỘC LẬP
        // Không dùng applyWeaponEnhancement để tránh bị ghi đè bởi các chiêu Charge!
        // ==========================================
        double burnRate = 0.03 + (level * 0.01);

        // Gắn cờ thủ công, tách biệt hoàn toàn với hệ thống Đòn đánh tay
        player.setMetadata("Reaper_BloodDemon_Level", new FixedMetadataValue(plugin, level));
        player.setMetadata("BloodDemon_Active", new FixedMetadataValue(plugin, true));

        // Tự gửi thông báo
        player.sendMessage("§4§l[HUYẾT QUỶ] §fĐã đánh thức sức mạnh ác quỷ!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 2);

        startDemonTask(player, rpgPlayer, level, maxHp, burnRate);
        return true;
    }

    private void startDemonTask(Player player, RPGPlayer rpgPlayer, int level, double maxHp, double burnRate) {
        long taskId = System.currentTimeMillis();
        player.setMetadata("BloodDemon_Task", new FixedMetadataValue(plugin, taskId));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() ||
                        (player.hasMetadata("BloodDemon_Task") && player.getMetadata("BloodDemon_Task").get(0).asLong() != taskId)) {
                    endDemonState(player, rpgPlayer, level);
                    this.cancel();
                    return;
                }

                // CƠ CHẾ MỚI: TỰ NGẮT KHI MÁU CHẠM NGƯỠNG 33%
                double currentHp = player.getHealth();
                double safeFloor = maxHp * 0.33;

                if (currentHp <= safeFloor) {
                    endDemonState(player, rpgPlayer, level);
                    this.cancel();
                    return;
                }

                // Thiêu đốt máu mỗi giây
                if (ticks % 20 == 0) {
                    double burnAmount = maxHp * burnRate;
                    double newHp = Math.max(safeFloor, currentHp - burnAmount);
                    player.setHealth(newHp);

                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, 0.5f, 1.5f);
                    player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1.2, 0), 3, 0.3, 0.3, 0.3, 0.1);
                }

                // VFX
                if (ticks % 2 == 0) {
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add((Math.random()-0.5), 1.0 + Math.random(), (Math.random()-0.5)), 1, 0,0,0,0, new Particle.DustOptions(Color.BLACK, 1.0f));
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add((Math.random()-0.5), 1.0 + Math.random(), (Math.random()-0.5)), 1, 0,0,0,0, new Particle.DustOptions(Color.MAROON, 1.2f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void endDemonState(Player player, RPGPlayer rpgPlayer, int level) {
        // Chỉ xóa cờ của riêng Huyết Quỷ
        if (player.hasMetadata("BloodDemon_Active")) player.removeMetadata("BloodDemon_Active", plugin);
        if (player.hasMetadata("Reaper_BloodDemon_Level")) player.removeMetadata("Reaper_BloodDemon_Level", plugin);
        if (player.hasMetadata("BloodDemon_Task")) player.removeMetadata("BloodDemon_Task", plugin);

        // KHÔNG CÓ DÒNG NÀY: player.removeMetadata("Active_Weapon_Enhancement", plugin);

        if (player.isOnline() && !player.isDead()) {
            player.sendMessage("§8§o(Dòng máu ác quỷ đã ngủ yên, bắt đầu tính thời gian hồi chiêu...)");
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        }

        // Chạy Cooldown
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        String cdKey = "CD_" + getId();
        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));

        if (player.isOnline()) {
            startCooldownNotifier(player, finalCd);
        }
    }
}