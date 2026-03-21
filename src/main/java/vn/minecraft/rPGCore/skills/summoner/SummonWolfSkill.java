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

public class SummonWolfSkill implements ISkill {

    private final JavaPlugin plugin;

    // Thêm Constructor nhận plugin
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
        return 30.0 + (level * 5.0); // Cấp càng cao tốn càng nhiều Mana
    }

    @Override
    public long getCooldown(int level) {
        return 15000L; // 15 giây (tính bằng milliseconds)
    }

    @Override
    public int getUpgradeCost(int currentLevel) {
        return 1; // Tốn 1 SP để nâng cấp
    }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return new HashMap<>(); // Không yêu cầu kỹ năng tiền đề
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
        double manaCost = getManaCost(level);

        // 1. Kiểm tra Năng lượng
        if (rpgPlayer.getCurrentResource() < manaCost) {
            player.sendActionBar("§c§l✖ Không đủ Năng lượng!");
            return false;
        }

        // 2. Kiểm tra Cooldown
        if (player.hasMetadata("CD_Summon_Wolf")) {
            player.sendActionBar("§c§l✖ Kỹ năng đang hồi phục!");
            return false;
        }

        // 3. Trừ Mana & Khóa Cooldown
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - manaCost);
        player.setMetadata("CD_Summon_Wolf", new FixedMetadataValue(RPGCore.getInstance(), true));

        long cooldownTicks = getCooldown(level) / 50L; // Chuyển từ ms sang ticks (1 tick = 50ms)
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (player.hasMetadata("CD_Summon_Wolf")) {
                    player.removeMetadata("CD_Summon_Wolf", RPGCore.getInstance());
                    player.sendMessage("§a§l[!] §fKỹ năng §eLinh Lang §fđã sẵn sàng!");
                }
            }
        }.runTaskLater(RPGCore.getInstance(), cooldownTicks);

        // 4. TRIỆU HỒI MYTHICMOB VÀ NHÚNG DỮ LIỆU
        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(1.5));

        try {
            // Gọi con quái có tên nội bộ là "SpiritWolf"
            ActiveMob wolf = MythicBukkit.inst().getMobManager().spawnMob("SpiritWolf", spawnLoc);

            if (wolf != null) {
                // A. Kêu MythicMobs nhận diện chủ nhân (Cơ chế setowner của Wiki)
                wolf.setOwnerUUID(player.getUniqueId());

                Entity bukkitEntity = wolf.getEntity().getBukkitEntity();

                // B. Fix riêng cho các loài có thể "Ngồi" trong Vanilla (Chó, Mèo, Vẹt)
                if (bukkitEntity instanceof org.bukkit.entity.Tameable) {
                    ((org.bukkit.entity.Tameable) bukkitEntity).setTamed(true);
                    ((org.bukkit.entity.Tameable) bukkitEntity).setOwner(player);
                }

                // CẤM NGỒI (Chỉ áp dụng nếu là Sói/Mèo/Vẹt)
                if (bukkitEntity instanceof org.bukkit.entity.Sittable) {
                    ((org.bukkit.entity.Sittable) bukkitEntity).setSitting(false);
                }

                bukkitEntity.setMetadata("Summoner_Pet", new FixedMetadataValue(RPGCore.getInstance(), player.getUniqueId().toString()));

                // Nhúng thêm Level của skill để sau này CombatManager dùng để scale sức mạnh nếu cần
                bukkitEntity.setMetadata("Summon_Level", new FixedMetadataValue(RPGCore.getInstance(), level));

                // C. Hiệu ứng Visual cực ngầu
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