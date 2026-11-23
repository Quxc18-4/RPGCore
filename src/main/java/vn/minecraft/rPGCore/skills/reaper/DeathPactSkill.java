package vn.minecraft.rPGCore.skills.reaper;

import org.bukkit.EntityEffect;
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
    public String getId() { return "reaper_deaths_pact"; }

    @Override
    public String getName() { return "Khế Ước Máu"; }

    @Override
    public String getRequiredClass() { return "reaper"; }

    @Override
    public Material getIcon() { return Material.WRITABLE_BOOK; }

    @Override
    public int getMaxLevel() { return 6; }

    @Override
    public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) { return 40.0 + (level * 10.0); }

    @Override
    public long getCooldown(int level) { return (22 - (level - 1)) * 1000L; }

    @Override
    public List<String> getDescription(int level) {
        return getDescription(null, level);
    }

    public List<String> getDescription(Player player, int level) {
        List<String> lore = new ArrayList<>();
        int l = Math.min(Math.max(1, level), 6);

        int cost = (int) costPcts[l - 1];
        int heal = (int) healPcts[l - 1];

        lore.add("§7Vượt qua ranh giới sinh tử, tự rút máu");
        lore.add("§7bản thân để đổi lấy khả năng phục hồi mãnh liệt.");
        lore.add("");
        lore.add("[Thi Triển - Khế Ước Máu]");
        lore.add("§f- Lập tức hi sinh §4" + cost + "§f% Máu Tối Đa");
        lore.add("§f- Trong 8s kế tiếp, hồi phục: §2" + heal + "§f% Máu Tối Đa.");
        lore.add("");
        lore.add("§8§o* Không gì có thể cản trở khế ước máu hồi phục");

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
        // 1. KIỂM TRA NGƯỠNG MÁU THẾ CHẤP
        // ==========================================
        double maxHp = rpgPlayer.getTotalMaxHealth();
        double hpCost = maxHp * (costPcts[level - 1] / 100.0);
        double currentHp = player.getHealth();

        if (currentHp <= hpCost + 3.0) {
            player.sendMessage("§c§l[!] §cSinh lực quá thấp, không đủ để thế chấp Khế Ước!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        // ==========================================
        // 2. THANH TOÁN TÀI NGUYÊN & HIỆU ỨNG RÚT MÁU
        // ==========================================
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - manaCost);
        player.setHealth(currentHp - hpCost);

        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));
        player.setMetadata(cdKey, new FixedMetadataValue(plugin, System.currentTimeMillis() + finalCd));
        startCooldownNotifier(player, finalCd);

        // Hiệu ứng hình ảnh khi hiến máu
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);

        // ==========================================
        // 3. ĐẶT CỜ "TRUE HEAL" BỎ QUA ANTI-HEAL TRONG 8 GIÂY
        // ==========================================
        // Dựa vào logic chúng ta đã thống nhất để vượt qua StatusEffectManager một cách hợp pháp
        player.setMetadata("RPG_True_Heal_Priority", new FixedMetadataValue(plugin, System.currentTimeMillis() + 8000L));

        // ==========================================
        // 4. BUKKIT RUNNABLE: HỒI MÁU CHẬM TRONG 8 GIÂY
        // ==========================================
        // 8 giây chia cho mỗi nhịp 0.5 giây (10 ticks) -> Có tổng cộng 16 nhịp hồi máu
        double totalHeal = maxHp * (healPcts[level - 1] / 100.0);
        double healPerTick = totalHeal / 16.0;

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                // Kiểm tra trạng thái an toàn trước khi hồi
                if (!player.isOnline() || player.isDead() || ticks >= 16) {
                    this.cancel();
                    return;
                }

                // Lấy Max HP trực tiếp từ Bukkit để tương thích hoàn hảo với player.setHealth
                double dynamicMaxHp = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                double dynamicCurrentHp = player.getHealth();

                if (dynamicCurrentHp < dynamicMaxHp) {
                    // Ép máu an toàn, đảm bảo không bao giờ vượt qua Max HP
                    player.setHealth(Math.min(dynamicMaxHp, dynamicCurrentHp + healPerTick));

                    // Hiệu ứng bong bóng máu mỗi 0.5s
                    player.getWorld().spawnParticle(Particle.CRIMSON_SPORE, player.getLocation().add(0, 2, 0), 5, 0.3, 0.2, 0.3, 0);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 10L); // Chạy mỗi 0.5s (10 ticks)

        return true;
    }
}