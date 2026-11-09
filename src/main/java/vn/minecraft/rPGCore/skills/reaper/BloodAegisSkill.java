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
    @Override public String getName() { return "Huyết Thuẫn"; }
    @Override public String getRequiredClass() { return "reaper"; }
    @Override public Material getIcon() { return Material.RED_DYE; }

    // Yêu cầu mới: Max Level 6
    @Override public int getMaxLevel() { return 6; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    // Resource: 60/70/80/90/100/110
    @Override
    public double getManaCost(int level) { return 50.0 + (level * 10.0); }

    // CD: 25s (Lv1) -> 20s (Lv6)
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

        int hpCostRatio = 20 + (displayLvl * 5); // 25% -> 50%
        int shieldRatio = 12 + (displayLvl * 3); // 15% -> 30%
        int physMulti = 220 + (displayLvl * 30); // 250% -> 400%
        int trueMulti = 20 + (displayLvl * 10);  // 30% -> 80%

        return List.of(
                "§7Thiêu đốt máu thịt để tạo ra",
                "§7một lớp khiên máu bảo hộ.",
                "",
                "§e[Kích Hoạt - Huyết Thuẫn]",
                "§f- Hiến tế §c" + hpCostRatio + "% Máu tối đa§f của bản thân,",
                "§f  chuyển hóa chúng thành §e" + shieldRatio + "% Giáp Ảo§f.",
                "",
                "§4[Nội Tại - Bùng Nổ Máu]",
                "§f- Nếu lớp Giáp Ảo này bị đánh vỡ hoàn toàn",
                "§f  trong vòng §c5 giây§f, nó sẽ phát nổ và gây ST.",
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
        // 1. TÍNH TOÁN HIẾN TẾ MÁU (BLOOD SACRIFICE)
        // ==========================================
        double maxHp = rpgPlayer.getTotalMaxHealth();
        double hpCostRatio = 0.20 + (level * 0.05); // 25% -> 50%
        double requiredHp = maxHp * hpCostRatio;

        // Cấm tự sát: Nếu máu hiện tại thấp hơn hoặc bằng lượng máu cần hiến tế thì cấm cast
        if (player.getHealth() <= requiredHp) {
            player.sendMessage("§c§l[!] §fKhông đủ Sinh lực để hiến tế cho Huyết Thuẫn!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        // Trừ Thể lực & Trừ thẳng Máu
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        player.setHealth(player.getHealth() - requiredHp);

        // Xử lý Cooldown chuẩn RPGCore
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        // ==========================================
        // 2. TÍNH TOÁN & NẠP GIÁP ẢO (SHIELD GENERATION)
        // ==========================================
        double shieldRatio = 0.12 + (level * 0.03); // 15% -> 30%
        double rawShieldGain = maxHp * shieldRatio;

        double currentShield = player.hasMetadata("RPG_Absorption_Shield") ? player.getMetadata("RPG_Absorption_Shield").get(0).asDouble() : 0.0;
        double maxShieldAllowed = maxHp * 0.50; // Giới hạn 50% Max HP của server

        // Tính Giáp thực nhận để không vượt trần, dùng làm đạn sát thương nổ
        double newShield = Math.min(maxShieldAllowed, currentShield + rawShieldGain);
        double actualShieldGained = newShield - currentShield;

        // Đẩy thẳng lượng giáp vào Metadata, Gatekeeper của Server sẽ tự động render UI Tim Vàng
        player.setMetadata("RPG_Absorption_Shield", new FixedMetadataValue(plugin, newShield));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.5f);
        player.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, player.getLocation().add(0, 1.5, 0), 50, 0.5, 0.8, 0.5, Material.REDSTONE_BLOCK.createBlockData());
        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5, 0.2);

        player.sendMessage("§4§l[HUYẾT THUẪN] §fHiến tế máu, tạo ra §e" + (int)actualShieldGained + " Giáp Ảo§f!");

        // ==========================================
        // 3. CƠ CHẾ PHẢN CÔNG (RETALIATION TASK)
        // ==========================================
        startRetaliationTask(player, rpgPlayer, level, actualShieldGained);

        return true;
    }

    private void startRetaliationTask(Player player, RPGPlayer rpgPlayer, int level, double shieldGained) {
        long taskId = System.currentTimeMillis();
        player.setMetadata("Reaper_Aegis_Task", new FixedMetadataValue(plugin, taskId));

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100; // 5 giây = 100 ticks

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                if (player.hasMetadata("Reaper_Aegis_Task") && player.getMetadata("Reaper_Aegis_Task").get(0).asLong() != taskId) {
                    this.cancel();
                    return;
                }

                if (ticks >= maxTicks) {
                    player.sendMessage("§8§o(Huyết Thuẫn mất trạng thái phản công...)");
                    player.removeMetadata("Reaper_Aegis_Task", plugin);
                    this.cancel();
                    return;
                }

                double currentShield = player.hasMetadata("RPG_Absorption_Shield") ? player.getMetadata("RPG_Absorption_Shield").get(0).asDouble() : 0.0;

                if (currentShield <= 0) {
                    // GIÁP VỠ! KÍCH NỔ!
                    explode(player, rpgPlayer, level, shieldGained);
                    player.removeMetadata("Reaper_Aegis_Task", plugin);
                    this.cancel();
                    return;
                }

                // Vẽ hạt Particle bao quanh người trong 5 giây
                if (ticks % 5 == 0) {
                    double angle = (ticks * Math.PI / 10);
                    double x = 1.2 * Math.cos(angle);
                    double z = 1.2 * Math.sin(angle);
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(x, 1.0, z), 1, 0,0,0,0, new Particle.DustOptions(org.bukkit.Color.MAROON, 1.5f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void explode(Player player, RPGPlayer rpgPlayer, int level, double shieldGained) {
        player.sendMessage("§c§l[!] HUYẾT ÁP BÙNG NỔ!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 3);

        double baseDmgMulti = 2.2 + (level * 0.30); // 250% -> 400%
        double trueDmgRatio = 0.20 + (level * 0.10); // 30% -> 80%

        // Game Balancing: ST Chuẩn quy đổi từ lượng Giáp Thực tế (đã áp trần 50% HP)
        double trueDmgAmount = shieldGained * trueDmgRatio;

        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 2.5, 2.5, 2.5)) {
            if (e instanceof LivingEntity && !e.equals(player) && CombatManager.canTarget(player, e)) { // Lọc đồng minh/đệ tử[cite: 7, 8]
                LivingEntity victim = (LivingEntity) e;

                // Tính toán sát thương vật lý qua CombatManager[cite: 7, 8]
                double finalPhysDmg = CombatManager.calculateDamage(player, rpgPlayer, victim, baseDmgMulti, 0.0, false, false);
                double totalDmg = finalPhysDmg + trueDmgAmount;

                // Khóa lặp sát thương đệ quy[cite: 7, 8]
                player.setMetadata("Ghost_L_Blocker", new FixedMetadataValue(plugin, true));
                CombatManager.applySkillDamage(player, victim, totalDmg); // Gây sát thương & kích hoạt Hút Máu[cite: 7, 8]
                player.removeMetadata("Ghost_L_Blocker", plugin);
            }
        }
    }
}