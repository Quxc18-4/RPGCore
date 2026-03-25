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
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SummonCopperGolemSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public SummonCopperGolemSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() { return "summoner_copper_golem"; }

    @Override
    public String getName() { return "Triệu Hồi: Golem Đồng"; }

    @Override
    public String getRequiredClass() { return "summoner"; }

    @Override
    public Material getIcon() { return Material.COPPER_BLOCK; } // Biểu tượng Khối Đồng

    @Override
    public int getMaxLevel() { return 10; }

    @Override
    public double getManaCost(int level) {
        // Tốn nhiều Mana hơn Sói (Lv1: 70 | Lv10: 160)
        return 60.0 + (level * 10.0);
    }

    @Override
    public long getCooldown(int level) {
        // Hồi chiêu lâu hơn Sói vì là Tanker
        if (level >= 10) return 20000L; // 20s
        if (level >= 7) return 22000L;
        if (level >= 4) return 24000L;
        return 25000L; // 25s
    }

    @Override
    public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override
    public List<String> getDescription(int level) {
        double flatDamage = 30.0 + ((level - 1) * 10.0);
        double scaling = 0.8 + (level * 0.2);
        int scalePercent = (int) (scaling * 100);

        return Arrays.asList(
                "§7Lấy quặng đồng dưới lòng đất dung hợp",
                "§7cùng ma thuật để tạo ra cỗ máy bảo vệ.",
                "",
                "§8▪ §fHệ phái: §6Tiên Phong (Vanguard)",
                "§8▪ §fThuộc tính: §aĐại Địa (Earth)",
                "",
                "§e[Chỉ số Kế thừa]",
                "§f- Sát thương: §c" + flatDamage + " §f(+§e" + scalePercent + "% Sát thương vũ khí§f)",
                "§f- Kỹ năng: §6Khiêu Khích",
                "",
                "§d[Hiệu ứng Hấp thụ]",
                "§f- §c-4% §fSát thương nhận vào",
                "§f- §6Nhận Lớp khiên §f= §a15% Máu Golem"
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
            // Triệu hồi Golem từ cấu hình MythicMobs
            ActiveMob golem = MythicBukkit.inst().getMobManager().spawnMob("EarchCopperGolem", spawnLoc);

            if (golem != null) {
                // Trừ Mana & kích hoạt Hồi chiêu
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
                cooldowns.put(player.getUniqueId(), currentTime);
                startCooldownNotifier(player, finalCd);

                golem.setLevel(level);
                golem.setOwnerUUID(player.getUniqueId());
                Entity bukkitEntity = golem.getEntity().getBukkitEntity();

                // Kích hoạt Profile Java để bơm máu, sát thương và thu nhỏ kích thước
                if (bukkitEntity instanceof org.bukkit.entity.LivingEntity) {
                    vn.minecraft.rPGCore.skills.summoner.pets.IPetProfile profile = vn.minecraft.rPGCore.skills.summoner.pets.SummonPetManager.getProfile("CopperGolem");
                    if (profile != null) {
                        profile.onSpawn(player, (org.bukkit.entity.LivingEntity) bukkitEntity, level);
                    }
                }

                if (bukkitEntity instanceof org.bukkit.entity.Tameable) {
                    ((org.bukkit.entity.Tameable) bukkitEntity).setTamed(true);
                    ((org.bukkit.entity.Tameable) bukkitEntity).setOwner(player);
                }

                // Gắn Metadata nhận diện Thú Triệu Hồi
                bukkitEntity.setMetadata("Summoner_Pet", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
                bukkitEntity.setMetadata("Pet_Type", new FixedMetadataValue(plugin, "EarchCopperGolem"));
                bukkitEntity.setMetadata("Summon_Level", new FixedMetadataValue(plugin, level));

                // Lưu vào sổ nợ (Thế chấp Mana)
                if (rpgPlayer.getCurrentClass() instanceof vn.minecraft.rPGCore.classes.impl.SummonerClass) {
                    ((vn.minecraft.rPGCore.classes.impl.SummonerClass) rpgPlayer.getCurrentClass())
                            .registerPet(bukkitEntity.getUniqueId(), cost, rpgPlayer, "Golem Đồng");
                }

                // Hiệu ứng hạt bụi đồng (Copper) và tiếng rèn kim loại
                player.getWorld().spawnParticle(Particle.BLOCK, spawnLoc.add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1, Material.COPPER_BLOCK.createBlockData());
                player.getWorld().spawnParticle(Particle.LAVA, spawnLoc, 10, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(spawnLoc, Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);

                player.sendMessage("§6§l[Triệu Hồi] §fMột Golem Đồng đã được chế tạo thành công!");
                return true;
            } else {
                player.sendMessage("§c[!] Lỗi Hệ Thống: MythicMobs không tìm thấy Thú 'EarchCopperGolem'!");
            }
        } catch (Exception e) {
            player.sendMessage("§c[!] Lỗi Ngoại lệ: Quá trình tạo Golem bị gián đoạn!");
            e.printStackTrace();
        }

        return false;
    }
}