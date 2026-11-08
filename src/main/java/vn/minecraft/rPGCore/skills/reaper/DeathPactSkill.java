package vn.minecraft.rPGCore.skills.reaper;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class DeathPactSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // Các mốc chỉ số theo thiết kế: Cấp 1 -> Cấp 6
    private final double[] costPcts = {10.0, 15.0, 20.0, 25.0, 30.0, 35.0};
    private final double[] healPcts = {15.0, 20.0, 25.0, 32.0, 40.0, 50.0};

    public DeathPactSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "reaper_deaths_pact";
    }

    @Override
    public String getName() {
        return "Khế Ước Tử Thần";
    }

    @Override
    public String getRequiredClass() {
        return "reaper";
    }

    @Override
    public Material getIcon() {
        return Material.WRITABLE_BOOK;
    }

    @Override
    public int getMaxLevel() {
        return 6; // Tối đa 6 cấp theo yêu cầu
    }

    @Override
    public int getUpgradeCost(int currentLevel) {
        return 1; // Mỗi lần nâng tốn 1 SP
    }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return Collections.emptyMap();
    }

    @Override
    public double getManaCost(int level) {
        return 40.0 + (level * 10.0);
    }

    @Override
    public long getCooldown(int level) {
        // Hồi chiêu từ 24s giảm dần xuống 19s
        return (22 - (level - 1)) * 1000L;
    }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int l = Math.min(Math.max(1, level), 6);

        int cost = (int) costPcts[l - 1];
        int heal = (int) healPcts[l - 1];

        lore.add("§7Vượt qua ranh giới sinh tử, tự rút máu");
        lore.add("§7bản thân để đổi lấy khả năng phục hồi mãnh liệt.");
        lore.add("");
        lore.add("§c[-] Thế chấp: §4" + cost + "% Máu Tối Đa");
        lore.add("§a[+] Thu về: §2" + heal + "% Máu Tối Đa §7(Hồi trong 6s)");
        lore.add("");
        lore.add("§8§o* Khế ước này ép buộc cơ thể phục hồi,");
        lore.add("§8§obỏ qua mọi trạng thái Cấm Hồi Phục (Anti-heal).");

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0 || level > 6) return false;

        String cdKey = "CD_" + getId();
        if (player.hasMetadata(cdKey)) {
            long expire = player.getMetadata(cdKey).get(0).asLong();
            if (System.currentTimeMillis() < expire) {
                double timeLeft = (expire - System.currentTimeMillis()) / 1000.0;
                player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
                return false;
            }
        }

        double manaCost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < manaCost) {
            player.sendMessage("§cKhông đủ Thể lực!");
            return false;
        }

        // ==========================================
        // 1. KIỂM TRA NGƯỠNG MÁU THẾ CHẤP (HARD THRESHOLD)
        // ==========================================
        double maxHp = rpgPlayer.getTotalMaxHealth();
        double hpCost = maxHp * (costPcts[level - 1] / 100.0);
        double currentHp = player.getHealth();

        // Bắt buộc máu hiện tại phải lớn hơn lượng máu thế chấp (Cộng dư 1.0 để không bị chết oan)
        if (currentHp <= hpCost + 1.0) {
            player.sendMessage("§c§l[!] §cSinh lực quá thấp, không đủ để thế chấp Khế Ước!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        // ==========================================
        // 2. THANH TOÁN TÀI NGUYÊN
        // ==========================================
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - manaCost);
        player.setHealth(currentHp - hpCost);

        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        // Hiệu ứng hình ảnh khi hiến máu
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
        //player.sendMessage("§4§l[KHẾ ƯỚC] §cThế chấp " + (int)hpCost + " Sinh lực!");

        // ==========================================
        // 3. CƠ CHẾ HỒI MÁU ĐẶC QUYỀN (6 GIÂY - BYPASS ANTI-HEAL)
        // ==========================================
        double totalHeal = maxHp * (healPcts[level - 1] / 100.0);
        double healPerTick = totalHeal / 12.0; // Chia làm 12 nhịp, mỗi nhịp 0.5s -> Tổng 6s

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }
                if (ticks >= 12) {
                    this.cancel();
                    return;
                }

                double dynamicCurrentHp = player.getHealth();
                double dynamicMaxHp = rpgPlayer.getTotalMaxHealth();

                // Ép máu trực tiếp lên Player Bukkit, tuyệt đối không dùng rpgPlayer.heal()
                // Cơ chế này lách qua lớp phòng ngự của StatusEffectManager (Cấm Heal)
                if (dynamicCurrentHp < dynamicMaxHp) {
                    player.setHealth(Math.min(dynamicMaxHp, dynamicCurrentHp + healPerTick));

                    // Particle phun tim đỏ thẫm đặc trưng của Khế Ước
                    player.getWorld().spawnParticle(Particle.CRIMSON_SPORE, player.getLocation().add(0, 2, 0), 5, 0.3, 0.2, 0.3, 0);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 10L); // Chạy mỗi 10 ticks (0.5 giây)

        return true;
    }
}