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

public class SummonFireSpiritSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public SummonFireSpiritSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() { return "summoner_fire_spirit"; }

    @Override
    public String getName() { return "Triệu Hồi: Tinh Linh Lửa"; }

    @Override
    public String getRequiredClass() { return "summoner"; }

    @Override
    public Material getIcon() { return Material.BLAZE_POWDER; }

    @Override
    public int getMaxLevel() {
        return 10;
    }

    @Override
    public double getManaCost(int level) {
        // Tốn nhiều Mana hơn Sói một chút vì là Caster tầm xa
        return 65.0 + (level * 5.0);
    }

    @Override
    public long getCooldown(int level) {
        // Hồi chiêu: Cấp 1-3: 18s | Cấp 4-6: 17s | Cấp 7-9: 16s | Cấp 10: 15s
        if (level >= 10) return 15000L;
        if (level >= 7) return 16000L;
        if (level >= 4) return 17000L;
        return 18000L;
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
        // Đồng bộ công thức từ FireSpiritProfile
        double flatDamage = 15.0 + (level * 2.0);
        double scaling = 0.5 + (level * 0.05);
        int scalePercent = (int) (scaling * 100);

        return Arrays.asList(
                "§7Triệu hồi một Tinh Linh Lửa đúc kết từ",
                "§7ngọn lửa nguyên thủy để thiêu rụi kẻ thù.",
                "",
                "§8▪ §fHệ phái: §6Pháp Sư (Caster)",
                "§8▪ §fThuộc tính: §cHỏa (Fire)",
                "",
                "§e[Chỉ số Kế thừa]",
                "§f- Đạn Lửa: §c" + flatDamage + " §f(+§e" + scalePercent + "% ST Vũ khí§f)",
                "§f- Thiêu Đốt: §c75 ST/giây §f(Hiệu ứng Vanilla)",
                "",
                "§d[Hiệu ứng Cộng Hưởng]",
                "§f- §b+5% §fXuyên giáp phép",
                "§f- §6+5% §fTỉ lệ đòn đánh gây Thiêu Đốt"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        double cost = getManaCost(level);

        // 1. Kiểm tra tài chính (Mana)
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Năng lượng (Cần " + cost + " Mana)!");
            return false;
        }

        // 2. Kiểm tra hồi chiêu
        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long finalCd = getFinalCooldown(rpgPlayer, getCooldown(level));

        if (currentTime - lastUsed < finalCd) {
            double timeLeft = (finalCd - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(1.5)).add(0, 0.5, 0);

        try {
            // [QUAN TRỌNG]: Gọi đúng tên Internal Name của bù nhìn MythicMobs
            ActiveMob fireSpirit = MythicBukkit.inst().getMobManager().spawnMob("FireSpirit", spawnLoc);

            if (fireSpirit != null) {
                // Trừ Mana & Bắt đầu hồi chiêu
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
                cooldowns.put(player.getUniqueId(), currentTime);
                startCooldownNotifier(player, finalCd);

                fireSpirit.setLevel(level);
                fireSpirit.setOwnerUUID(player.getUniqueId());
                Entity bukkitEntity = fireSpirit.getEntity().getBukkitEntity();

                if (bukkitEntity instanceof org.bukkit.entity.LivingEntity) {
                    // Gọi Profile nạp não bộ
                    vn.minecraft.rPGCore.skills.summoner.pets.IPetProfile profile = vn.minecraft.rPGCore.skills.summoner.pets.SummonPetManager.getProfile("FireSpirit");
                    if (profile != null) {
                        profile.onSpawn(player, (org.bukkit.entity.LivingEntity) bukkitEntity, level);
                    }
                }

                // Đóng dấu kiểm định Metadata
                bukkitEntity.setMetadata("Summoner_Pet", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
                bukkitEntity.setMetadata("Pet_Type", new FixedMetadataValue(plugin, "FireSpirit"));
                bukkitEntity.setMetadata("Summon_Level", new FixedMetadataValue(plugin, level));

                // Lưu vào sổ nợ (Thế chấp Mana)
                if (rpgPlayer.getCurrentClass() instanceof vn.minecraft.rPGCore.classes.impl.SummonerClass) {
                    ((vn.minecraft.rPGCore.classes.impl.SummonerClass) rpgPlayer.getCurrentClass())
                            .registerPet(bukkitEntity.getUniqueId(), cost, rpgPlayer, "Tinh Linh Lửa");
                }

                // Hiệu ứng triệu hồi rực lửa
                player.getWorld().spawnParticle(Particle.FLAME, spawnLoc, 50, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().spawnParticle(Particle.LAVA, spawnLoc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(spawnLoc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);

                player.sendMessage("§c§l[Triệu Hồi] §fMột Tinh Linh Lửa đã đáp lại lời gọi của bạn!");
                return true;
            } else {
                player.sendMessage("§c[!] Lỗi Hệ Thống: MythicMobs không tìm thấy Thú 'FireSpirit'. Vui lòng kiểm tra lại file cấu hình!");
            }
        } catch (Exception e) {
            player.sendMessage("§c[!] Lỗi Ngoại lệ: Quá trình tạo quái bị gián đoạn!");
            e.printStackTrace();
        }

        return false;
    }
}