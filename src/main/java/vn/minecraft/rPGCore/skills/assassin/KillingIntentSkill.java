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
    public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> req = new HashMap<>();
        req.put("assassin_fatal_blow", 4);
        return req;
    }

    @Override
    public int getRequiredPlayerLevel() {
        return 18; // Yêu cầu Level 18
    }

    @Override
    public double getManaCost(int level) { return 40.0; } // Tăng Mana từ 30 -> 40

    @Override
    public long getCooldown(int level) {
        return 22000; // 22s Hồi chiêu
    }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        double ratePerStack = 0.5 + (level * 0.4); // Lv1: 0.9%, Lv5: 2.5%
        double dmgPerStack = 1.0 + (level * 1.0);  // Lv1: 2.0%, Lv5: 6.0%
        int maxDur = 10 + level; // Lv1: 11s, Lv5: 15s

        lore.add("§7Giải phóng sát ý, yêu cầu người chơi phải");
        lore.add("§7đang trong §cTrạng thái Chiến đấu§7.");
        lore.add("");
        lore.add("§8[Nội tại - Cuồng bạo]:");
        lore.add("§7Mỗi 3 đòn đánh/1 kỹ năng tích §e1 Stack§7.");
        lore.add("§7Mỗi 1 Stack cung cấp:");
        lore.add("§7- §a+" + String.format("%.1f", ratePerStack) + "% §7Tỉ lệ Chí mạng.");
        lore.add("§7- §c+" + String.format("%.1f", dmgPerStack) + "% §7Sát thương Chí mạng.");
        lore.add("§7Khi nhận Stack sẽ §alàm mới§7 lại thời");
        lore.add("§7gian tồn tại về §e7 giây§7.");
        lore.add("");
        lore.add("§8[Nội tại - Sát ý tối đa]:");
        lore.add("§7Đạt mốc tối đa sẽ không thể làm mới nữa.");
        lore.add("§7Sát ý sẽ khóa cứng và duy trì trạng thái");
        lore.add("§7Cuồng Nộ trong §e" + maxDur + " giây§7.");

        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        if (!rpgPlayer.isInCombat()) {
            player.sendMessage("§cChỉ có thể kích hoạt khi đang trong Trạng thái Chiến đấu!");
            return false;
        }

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

        // Khởi tạo ở 0 SubStacks và đúng 7 giây
        player.setMetadata("Assassin_KillingIntent_SubStacks", new FixedMetadataValue(plugin, 0));
        player.setMetadata("Assassin_KillingIntent_Active", new FixedMetadataValue(plugin, System.currentTimeMillis() + 7000));
        player.setMetadata("Assassin_KillingIntent_Level", new FixedMetadataValue(plugin, level));

        player.sendMessage("§4§l[!] SÁT Ý LĨNH VỰC KÍCH HOẠT!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_ROAR, 0.5f, 1.5f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, new Particle.DustOptions(org.bukkit.Color.MAROON, 1.5f));

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
                        player.removeMetadata("Assassin_KillingIntent_SubStacks", plugin);
                        player.sendMessage("§8§o(Sát ý đã dần tản đi, nhịp độ trở lại bình thường...)");
                        this.cancel();
                    }
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 10L);

        return true;
    }
}