package vn.minecraft.rPGCore.skills.summoner;

import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
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

public class SummonWindSpiritSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public SummonWindSpiritSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "summoner_wind_spirit"; }
    @Override public String getName() { return "Triệu Hồi: Tinh Linh Gió"; }
    @Override public String getRequiredClass() { return "summoner"; }
    @Override public Material getIcon() { return Material.BREEZE_ROD; }
    @Override public int getMaxLevel() { return 10; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override
    public double getManaCost(int level) {
        return 60.0 + (level * 10.0);
    }

    @Override
    public long getCooldown(int level) {
        if (level >= 10) return 15000L;
        if (level >= 7) return 16000L;
        if (level >= 4) return 17000L;
        return 18000L;
    }

    @Override
    public List<String> getDescription(int level) {
        double flatDamage = 12.0 + ((level - 1) * 6.0);
        double scaling = 0.8 + (level * 0.25);
        int scalePercent = (int) (scaling * 100);

        int atkSpeedTick = Math.max(8, 20 - (int)((level - 1) * 1.33));
        double atkSpeedSec = atkSpeedTick / 20.0;

        int duration = (level == 10) ? 90 : 40 + ((level - 1) * 5);

        return Arrays.asList(
                "§7Hô gọi ngọn gió lốc hình thành nên Tinh linh,",
                "§7bậc thầy tấn công tầm xa và khống chế kẻ địch.",
                "",
                "§8▪ §fHệ phái: §bNhanh Nhẹn (Swift)",
                "§8▪ §fThuộc tính: §aGió (Wind)",
                "",
                "§e[Nội tại: Cuồng Phong]",
                "§f- Đạn gió: §c" + flatDamage + " §f(+§e" + scalePercent + "% ST Tổng§f)",
                "§f- Tốc độ bắn: §e" + atkSpeedSec + "s / viên",
                "§f- Thời gian tồn tại: §6" + duration + " giây",
                "§f- Đòn cường hóa: Bắn khí nén gây §c140% ST AOE§f,",
                "§f gây hiệu ứng Đẩy lùi/ Hất tung khi bộc phát.",
                "",
                "§d[Hiệu ứng Cộng Hưởng]",
                "§f- §b+12% §fTốc độ di chuyển",
                "§f- §a+10% §fKháng hiệu ứng bất lợi"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Năng lượng (Cần " + cost + " Mana)!");
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

        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(1.5)).add(0, 0.5, 0);

        try {
            ActiveMob windSpirit = MythicBukkit.inst().getMobManager().spawnMob("WindSpirit", spawnLoc);

            if (windSpirit != null) {
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
                cooldowns.put(player.getUniqueId(), currentTime);
                startCooldownNotifier(player, finalCd);

                windSpirit.setLevel(level);
                windSpirit.setOwnerUUID(player.getUniqueId());
                Entity bukkitEntity = windSpirit.getEntity().getBukkitEntity();

                if (bukkitEntity instanceof LivingEntity) {
                    vn.minecraft.rPGCore.skills.summoner.pets.IPetProfile profile = vn.minecraft.rPGCore.skills.summoner.pets.SummonPetManager.getProfile("WindSpirit");
                    if (profile != null) {
                        profile.onSpawn(player, (LivingEntity) bukkitEntity, level);
                    }
                }

                if (bukkitEntity instanceof org.bukkit.entity.Tameable) {
                    ((org.bukkit.entity.Tameable) bukkitEntity).setTamed(true);
                    ((org.bukkit.entity.Tameable) bukkitEntity).setOwner(player);
                }

                bukkitEntity.setMetadata("Summoner_Pet", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
                bukkitEntity.setMetadata("Pet_Type", new FixedMetadataValue(plugin, "WindSpirit"));
                bukkitEntity.setMetadata("Summon_Level", new FixedMetadataValue(plugin, level));

                if (rpgPlayer.getCurrentClass() instanceof vn.minecraft.rPGCore.classes.impl.SummonerClass) {
                    ((vn.minecraft.rPGCore.classes.impl.SummonerClass) rpgPlayer.getCurrentClass())
                            .registerPet(bukkitEntity.getUniqueId(), cost, rpgPlayer, "Tinh Linh Gió");
                }

                player.getWorld().spawnParticle(Particle.CLOUD, spawnLoc, 50, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().spawnParticle(Particle.GUST, spawnLoc.add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(spawnLoc, Sound.ENTITY_BREEZE_IDLE_AIR, 1.0f, 1.2f);

                player.sendMessage("§b§l[Triệu Hồi] §fTinh Linh Gió đã xuất hiện!");
                return true;
            } else {
                player.sendMessage("§c[!] Lỗi Hệ Thống: MythicMobs không tìm thấy Thú 'WindSpirit'.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}