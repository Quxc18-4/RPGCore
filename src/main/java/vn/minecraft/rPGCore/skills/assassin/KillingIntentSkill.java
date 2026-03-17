package vn.minecraft.rPGCore.skills.assassin;

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

public class KillingIntentSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public KillingIntentSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "assassin_killing_intent"; }
    @Override public String getName() { return "Sát Ý Lĩnh Vực"; }
    @Override public String getRequiredClass() { return "assassin"; }
    @Override public Material getIcon() { return Material.CRIMSON_ROOTS; }

    @Override public int getMaxLevel() { return 5; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public Map<String, Integer> getRequiredSkills() { return Collections.emptyMap(); }

    @Override
    public double getManaCost(int level) {
        return 30.0; // Mana rất rẻ để mở combat
    }

    @Override
    public long getCooldown(int level) {
        return 30000; // 30s Hồi chiêu
    }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        double ratePerStack = 1.0 + (level * 0.5); // Lv1: +1.5%, Lv5: +3.5%
        double dmgPerStack = 2.0 + (level * 1.0);  // Lv1: +3.0%, Lv5: +7.0%

        lore.add("§7Giải phóng sát ý, tiến vào trạng thái");
        lore.add("§7Cuồng Nộ trong vòng §e15 giây§7.");
        lore.add("");
        lore.add("§8[Nội tại - Tích Lũy Sát Ý]:");
        lore.add("§7Mỗi khi gây sát thương lên kẻ địch (Đánh");
        lore.add("§7thường hoặc Kỹ năng), nhận 1 Stack Sát Ý.");
        lore.add("§7Mỗi Stack cung cấp:");
        lore.add("§7- §a+" + String.format("%.1f", ratePerStack) + "% §7Tỉ lệ Chí mạng.");
        lore.add("§7- §c+" + String.format("%.1f", dmgPerStack) + "% §7Sát thương Chí mạng.");
        lore.add("");
        lore.add("§7Tối đa tích lũy: §e10 Stacks§7.");
        lore.add("§7Mỗi lần nhận Stack sẽ §alàm mới§7 lại");
        lore.add("§7thời gian 15 giây tồn tại của Sát Ý.");

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Thể lực (Stamina)!");
            return false;
        }

        long now = System.currentTimeMillis();
        long cd = getCooldown(level);
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < cd) {
            double timeLeft = (cd - (now - cooldowns.get(player.getUniqueId()))) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), now);

        // Đặt Metadata để CombatManager bắt đầu tính Stack
        player.setMetadata("Assassin_KillingIntent_Active", new FixedMetadataValue(plugin, System.currentTimeMillis() + 15000));
        player.setMetadata("Assassin_KillingIntent_Stacks", new FixedMetadataValue(plugin, 0)); // Bắt đầu ở 0 Stack
        player.setMetadata("Assassin_KillingIntent_Level", new FixedMetadataValue(plugin, level)); // Lưu Level để tính chỉ số

        player.sendMessage("§4§l[!] SÁT Ý LĨNH VỰC KÍCH HOẠT!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_ROAR, 0.5f, 1.5f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, new Particle.DustOptions(org.bukkit.Color.MAROON, 1.5f));
        // Vòng lặp kiểm tra hết hạn
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                if (player.hasMetadata("Assassin_KillingIntent_Active")) {
                    long expire = player.getMetadata("Assassin_KillingIntent_Active").get(0).asLong();
                    if (System.currentTimeMillis() > expire) {
                        player.removeMetadata("Assassin_KillingIntent_Active", plugin);
                        player.removeMetadata("Assassin_KillingIntent_Stacks", plugin);
                        player.sendMessage("§8§o(Sát ý đã dần tản đi, nhịp độ trở lại bình thường...)");
                        this.cancel();
                    }
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 10L); // Check mỗi nửa giây

        return true;
    }
}