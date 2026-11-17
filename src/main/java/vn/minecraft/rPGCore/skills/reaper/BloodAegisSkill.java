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

public class BloodAegisSkill extends AbstractEnhancementSkill {

    public BloodAegisSkill(JavaPlugin plugin) {
        super(plugin);
    }

    @Override public String getId() { return "reaper_blood_aegis"; }
    @Override public String getName() { return "Chuyển Hóa Sinh Mệnh"; }
    @Override public String getRequiredClass() { return "reaper"; }
    @Override public Material getIcon() { return Material.RED_DYE; }

    @Override public int getMaxLevel() { return 6; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) { return 50.0 + (level * 10.0); }

    @Override
    public long getCooldown(int level) {
        double cd = 26.0 - level;
        return (long) (cd * 1000);
    }

    @Override
    public List<String> getDescription(int level) { return getDescription(null, level); }

    @Override
    public List<String> getDescription(Player player, int level) {
        int displayLvl = Math.max(1, level);

        int hpCostRatio = 20 + (displayLvl * 5);
        int shieldRatio = 12 + (displayLvl * 3);
        int physMulti = 220 + (displayLvl * 30);
        int trueMulti = 20 + (displayLvl * 10);

        return List.of(
                "§7Thiêu đốt máu thịt để tạo ra",
                "§7một lớp khiên máu bảo hộ.",
                "",
                "§e[Kích Hoạt - Chuyển Hóa Sinh Mệnh]",
                "§f- Hiến tế §c" + hpCostRatio + "% Máu tối đa§f của bản thân,",
                "§f  chuyển hóa chúng thành §e" + shieldRatio + "% Giáp Ảo§f.",
                "§f- Giáp Ảo tồn tại trong tối đa §a10 giây§f.",
                "",
                "§4[Nội Tại - Bùng Nổ Máu]",
                "§f- Nếu lớp Giáp Ảo này bị đánh vỡ hoàn toàn",
                "§f  trong §c5 giây đầu tiên§f, nó sẽ phát nổ và gây ST.",
                "§f- Sát thương nổ: §c" + physMulti + "% Sát Thương",
                "§f  kèm §e" + trueMulti + "% ST Chuẩn §f(Lượng giáp đã nhận)"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        String cdKey = "CD_" + getId();
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

        // ==========================================
        // 1. TÍNH TOÁN HIẾN TẾ MÁU
        // ==========================================
        double maxHp = rpgPlayer.getTotalMaxHealth();
        double hpCostRatio = 0.20 + (level * 0.05);
        double requiredHp = maxHp * hpCostRatio;

        if (player.getHealth() <= requiredHp) {
            player.sendMessage("§c§l[!] §fKhông đủ Sinh lực để hiến tế cho Chuyển Hóa Sinh Mệnh!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        player.setHealth(player.getHealth() - requiredHp);

        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        // ==========================================
        // 2. TÍNH TOÁN & NẠP GIÁP ẢO
        // ==========================================
        double shieldRatio = 0.12 + (level * 0.03);
        double rawShieldGain = maxHp * shieldRatio;

        double currentShield = player.hasMetadata("RPG_Absorption_Shield") ? player.getMetadata("RPG_Absorption_Shield").get(0).asDouble() : 0.0;
        double globalCap = maxHp * 1.0; // Trần 100% HP toàn server

        double newShield = Math.min(globalCap, currentShield + rawShieldGain);

        // KHAI BÁO BIẾN LƯỢNG GIÁP THỰC NHẬN (Được dùng làm đạn nổ)
        double actualShieldGained = newShield - currentShield;

        // Bảo vệ giáp không bị tan biến 5%/s trong suốt thời gian hiệu lực
        player.setMetadata("Hard_Shield_Active", new FixedMetadataValue(plugin, true));
        player.setMetadata("RPG_Absorption_Shield", new FixedMetadataValue(plugin, newShield));

        // ÉP CLIENT RENDER UI NGAY LẬP TỨC
        if (!player.hasPotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION)) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, Integer.MAX_VALUE, 4, false, false, false));
        }
        double uiAmount = (newShield / maxHp) * 20.0;
        player.setAbsorptionAmount(Math.max(1.0, Math.min(20.0, uiAmount)));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.5f);
        player.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, player.getLocation().add(0, 1.5, 0), 50, 0.5, 0.8, 0.5, Material.REDSTONE_BLOCK.createBlockData());
        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5, 0.2);

        player.sendMessage("§4§l[CHUYỂN HÓA] §fHiến tế máu, tạo thành công §e" + (int)actualShieldGained + " Giáp Ảo§f!");

        // ==========================================
        // 3. CƠ CHẾ PHẢN CÔNG & TỰ HỦY GIÁP
        // ==========================================
        startRetaliationTask(player, rpgPlayer, level, actualShieldGained);

        return true;
    }

    private void startRetaliationTask(Player player, RPGPlayer rpgPlayer, int level, double shieldGained) {
        long taskId = System.currentTimeMillis();
        player.setMetadata("Reaper_Aegis_Task", new FixedMetadataValue(plugin, taskId));

        new BukkitRunnable() {
            int ticks = 0;
            final int explosionWindow = 100; // 5 giây đầu (Cho phép nổ)
            final int maxTicks = 200;        // Tối đa 10 giây (Tự xóa giáp)

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    removeTaskState(player);
                    this.cancel();
                    return;
                }

                if (player.hasMetadata("Reaper_Aegis_Task") && player.getMetadata("Reaper_Aegis_Task").get(0).asLong() != taskId) {
                    this.cancel(); // Đã bị override bởi chiêu mới
                    return;
                }

                double currentShield = player.hasMetadata("RPG_Absorption_Shield") ? player.getMetadata("RPG_Absorption_Shield").get(0).asDouble() : 0.0;

                // TRƯỜNG HỢP 1: BỊ ĐÁNH VỠ KHIÊN
                if (currentShield <= 0) {
                    if (ticks <= explosionWindow) {
                        // Vỡ trong 5 giây đầu -> KÍCH NỔ!
                        explode(player, rpgPlayer, level, shieldGained);
                    } else {
                        // Vỡ sau 5 giây -> Chỉ thông báo, không nổ
                        player.sendMessage("§8§o(Chuyển Hóa Sinh Mệnh đã ổn định, không còn khả năng bùng nổ...)");
                    }
                    removeTaskState(player);
                    this.cancel();
                    return;
                }

                // TRƯỜNG HỢP 2: HẾT 10 GIÂY -> TỰ XÓA BỎ GIÁP
                if (ticks >= maxTicks) {
                    player.sendMessage("§8§o(Chuyển Hóa Sinh Mệnh đã hết thời gian và tiêu tán...)");

                    // Xóa triệt để giáp và render lại UI
                    player.removeMetadata("RPG_Absorption_Shield", plugin);
                    player.setAbsorptionAmount(0.0);
                    player.removePotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION);

                    removeTaskState(player);
                    this.cancel();
                    return;
                }

                // Visual vòng tròn: 5s đầu (Màu Đỏ Sẫm - Đang gồng nổ), 5s sau (Màu Xám - Mất tác dụng nổ)
                if (ticks % 5 == 0) {
                    double angle = (ticks * Math.PI / 10);
                    double x = 1.2 * Math.cos(angle);
                    double z = 1.2 * Math.sin(angle);

                    org.bukkit.Color particleColor = ticks <= explosionWindow ? org.bukkit.Color.MAROON : org.bukkit.Color.GRAY;
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(x, 1.0, z), 1, 0,0,0,0, new Particle.DustOptions(particleColor, 1.5f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void explode(Player player, RPGPlayer rpgPlayer, int level, double shieldGained) {
        player.sendMessage("§c§l[!] BÙNG NỔ MÁU!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 3);

        double baseDmgMulti = 2.2 + (level * 0.30);
        double trueDmgRatio = 0.20 + (level * 0.10);

        double trueDmgAmount = shieldGained * trueDmgRatio;

        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 2.5, 2.5, 2.5)) {
            if (e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) {
                LivingEntity victim = (LivingEntity) e;

                double finalPhysDmg = CombatManager.calculateDamage(player, rpgPlayer, victim, baseDmgMulti, 0.0, false, false);
                double totalDmg = finalPhysDmg + trueDmgAmount;

                player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                CombatManager.applySkillDamage(player, victim, totalDmg);
                player.removeMetadata("Ghost_L_Blocker", plugin);
            }
        }
    }

    // Hàm dọn dẹp cờ bảo vệ giáp khi hết 10 giây hoặc sau khi khiên vỡ
    private void removeTaskState(Player player) {
        player.removeMetadata("Reaper_Aegis_Task", plugin);
        player.removeMetadata("Hard_Shield_Active", plugin);
    }
}