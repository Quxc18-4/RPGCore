package vn.minecraft.rPGCore.skills.archer;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PiercingBuffSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PiercingBuffSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "archer_pierce_buff"; }
    @Override public String getName() { return "Phong Tiễn Xuyên Thấu"; }
    @Override public String getRequiredClass() { return "archer"; }
    @Override public Material getIcon() { return Material.FEATHER; }
    @Override public int getMaxLevel() { return 5; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    // [CẬP NHẬT]: Yêu cầu cấp độ 22
    @Override public int getRequiredPlayerLevel() { return 22; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override
    public double getManaCost(int level) {
        return 40.0 + (level * 5.0);
    }

    @Override
    public long getCooldown(int level) {
        return 25000L;
    }

    @Override
    public List<String> getDescription(int level) {
        int duration = 8 + (level * 2);
        return Arrays.asList(
                "§7Yểm linh khí gió vào mũi tên, giúp chúng",
                "§7có khả năng xuyên thấu mọi mục tiêu.",
                "",
                "§f- Thời gian: §a" + duration + " giây",
                "§f- Giảm ST: §c-8% §fST mỗi lần xuyên qua",
                "§f- Tối thiểu: §eGiữ lại 30% ST gốc",
                "§8(Hiệu quả với cả đòn bắn thường và kỹ năng)"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        double cost = getManaCost(level);

        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Năng lượng!");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));

        if (currentTime - lastUsed < finalCd) {
            double timeLeft = (finalCd - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);
        startCooldownNotifier(player, finalCd);

        int durationTicks = (8 + (level * 2)) * 20;

        // ==========================================
        // [FIX BUG]: Kiểm tra an toàn List rỗng trước khi get(0)
        // ==========================================
        if (player.hasMetadata("Archer_Pierce_Task")) {
            java.util.List<org.bukkit.metadata.MetadataValue> metaList = player.getMetadata("Archer_Pierce_Task");
            if (metaList != null && !metaList.isEmpty()) {
                Object task = metaList.get(0).value();
                if (task instanceof BukkitRunnable) {
                    ((BukkitRunnable) task).cancel();
                }
            }
        }

        player.setMetadata("Archer_Pierce_Buff", new FixedMetadataValue(plugin, true));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 2.0f);
        player.sendMessage("§b§l[!] Mọi mũi tên bắn ra giờ đây đều mang sức mạnh Xuyên Thấu!");

        BukkitRunnable buffTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.hasMetadata("Archer_Pierce_Buff")) {
                    player.removeMetadata("Archer_Pierce_Buff", plugin);
                    player.sendMessage("§7§o(Sức mạnh Xuyên Thấu đã hết...)");
                }
            }
        };
        buffTask.runTaskLater(plugin, durationTicks);

        // Gắn Metadata lưu lại Task mới
        player.setMetadata("Archer_Pierce_Task", new FixedMetadataValue(plugin, buffTask));

        return true;
    }
}