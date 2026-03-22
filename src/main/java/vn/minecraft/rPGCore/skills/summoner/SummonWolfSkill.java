package vn.minecraft.rPGCore.skills.summoner;

import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.RPGCore;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SummonWolfSkill implements ISkill {

    private final JavaPlugin plugin;

    // [ĐỒNG BỘ]: Dùng Map để lưu thời gian chính xác giống Assassin
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public SummonWolfSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() { return "summoner_wolf"; }

    @Override
    public String getName() { return "Triệu Hồi: Linh Lang"; }

    @Override
    public String getRequiredClass() { return "summoner"; }

    @Override
    public Material getIcon() { return Material.BONE; }

    @Override
    public int getMaxLevel() { return 5; }

    @Override
    public double getManaCost(int level) {
        return 30.0 + (level * 5.0);
    }

    @Override
    public long getCooldown(int level) {
        return 15000L; // 15 giây
    }

    @Override
    public int getUpgradeCost(int currentLevel) {
        return 1;
    }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return new HashMap<>();
    }

    @Override
    public List<String> getDescription(int level) {
        return Arrays.asList(
                "§7Triệu hồi một Linh Lang chiến đấu bên cạnh.",
                "§7Linh Lang tự động tấn công mục tiêu của bạn.",
                "§7Sát thương và máu của Linh Lang kế thừa",
                "§7từ chỉ số Phép Thuật của chủ nhân.",
                "",
                "§fMana tiêu hao: §b" + getManaCost(level),
                "§fThời gian hồi: §e" + (getCooldown(level) / 1000.0) + "s"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        double cost = getManaCost(level);

        // 1. Kiểm tra Năng lượng (Đồng bộ Chat)
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Năng lượng (Mana)!");
            return false;
        }

        // 2. KIỂM TRA HỒI CHIÊU (Áp dụng ý tưởng của bạn)
        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);

        long baseCd = getCooldown(level); // Lấy thời gian gốc
        long finalCd = getFinalCooldown(rpgPlayer, baseCd); // Đưa qua máy ép CDR

        if (currentTime - lastUsed < finalCd) {
            double timeLeft = (finalCd - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        // 3. TRỪ NĂNG LƯỢNG VÀ LƯU COOLDOWN
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        startCooldownNotifier(player, finalCd);

        // ==========================================
        // 4. TRIỆU HỒI MYTHICMOB (FIX TỌA ĐỘ Y)
        // ==========================================
        // Dịch lên 0.5 block để Sói không bị chôn chân dưới đất
        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(1.5)).add(0, 0.5, 0);

        try {
            ActiveMob wolf = MythicBukkit.inst().getMobManager().spawnMob("SpiritWolf", spawnLoc);

            if (wolf != null) {
                wolf.setOwnerUUID(player.getUniqueId());
                Entity bukkitEntity = wolf.getEntity().getBukkitEntity();

                if (bukkitEntity instanceof org.bukkit.entity.Tameable) {
                    ((org.bukkit.entity.Tameable) bukkitEntity).setTamed(true);
                    ((org.bukkit.entity.Tameable) bukkitEntity).setOwner(player);
                }

                if (bukkitEntity instanceof org.bukkit.entity.Sittable) {
                    ((org.bukkit.entity.Sittable) bukkitEntity).setSitting(false);
                }

                bukkitEntity.setMetadata("Summoner_Pet", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
                bukkitEntity.setMetadata("Summon_Level", new FixedMetadataValue(plugin, level));

                player.getWorld().spawnParticle(Particle.SOUL, spawnLoc, 50, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().spawnParticle(Particle.ENCHANT, spawnLoc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(spawnLoc, Sound.ENTITY_WOLF_GROWL, 1.0f, 0.8f);

                player.sendMessage("§d§l[Triệu Hồi] §fMột Linh Lang đã đáp lại lời gọi của bạn!");
                return true;
            }
        } catch (Exception e) {
            player.sendMessage("§c[!] Lỗi: Không tìm thấy quái vật SpiritWolf trong thư mục MythicMobs!");
            e.printStackTrace();
        }

        return false;
    }
}