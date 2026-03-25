package vn.minecraft.rPGCore.skills.summoner;

import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;
import vn.minecraft.rPGCore.skills.summoner.pets.CopperGolemProfile;

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

    @Override public String getId() { return "summoner_copper_golem"; }
    @Override public String getName() { return "Triệu Hồi: Golem Đồng"; }
    @Override public String getRequiredClass() { return "summoner"; }
    @Override public Material getIcon() { return Material.COPPER_BLOCK; }
    @Override public int getMaxLevel() { return 10; }
    @Override public double getManaCost(int level) { return 60.0 + (level * 10.0); }

    @Override public long getCooldown(int level) {
        if (level >= 10) return 20000L;
        if (level >= 7) return 22000L;
        if (level >= 4) return 24000L;
        return 25000L;
    }

    @Override public int getUpgradeCost(int currentLevel) { return 1; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }

    @Override
    public List<String> getDescription(int level) {
        return Arrays.asList(
                "§7Lấy quặng đồng dưới lòng đất dung hợp",
                "§7cùng ma thuật để tạo ra cỗ máy bảo vệ.",
                "",
                "§8▪ §fHệ phái: §6Tiên Phong (Vanguard)",
                "§8▪ §fThuộc tính: §aĐại Địa (Earth)",
                "",
                "§e[Cơ Chế Bức Tường]",
                "§f- Máu: §aGia tăng theo Sát Thương Vũ Khí",
                "§f- Đứng im: §7Bất động như một bức tượng",
                "§f- Khiêu khích: §cThu hút quái, chịu 130% Sát thương",
                "§f- Tích nộ: §6Chịu 60 lần trừ máu sẽ tự bật Khiên",
                "",
                "§c[!] Tái kích hoạt: §fGọi Golem về sát bên, tốn §b50% Mana§f,",
                "§fhồi 20% HP và lập tức kích hoạt khiên Phản sát thương!"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        boolean isRecall = false;
        Entity existingGolem = null;

        // 1. KIỂM TRA TỒN TẠI (TÁI KÍCH HOẠT)
        if (player.hasMetadata("Active_Copper_Golem")) {
            String uuidStr = player.getMetadata("Active_Copper_Golem").get(0).asString();
            existingGolem = Bukkit.getEntity(UUID.fromString(uuidStr));
            if (existingGolem != null && existingGolem.isValid() && !existingGolem.isDead()) {
                isRecall = true;
            }
        }

        // 2. TÍNH TOÁN MANA (Recall chỉ tốn 50%)
        double finalCost = isRecall ? (getManaCost(level) / 2.0) : getManaCost(level);

        if (rpgPlayer.getCurrentResource() < finalCost) {
            player.sendMessage("§cKhông đủ Năng lượng (Cần " + finalCost + " Mana)!");
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

        // ==========================================
        // 3A. NẾU LÀ GỌI VỀ
        // ==========================================
        if (isRecall) {
            rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - finalCost);
            cooldowns.put(player.getUniqueId(), currentTime);
            startCooldownNotifier(player, finalCd);

            existingGolem.teleport(spawnLoc);
            LivingEntity le = (LivingEntity) existingGolem;
            double maxHp = le.getAttribute(Attribute.MAX_HEALTH).getValue();
            le.setHealth(Math.min(maxHp, le.getHealth() + (maxHp * 0.2)));

            // Ép bật Khiên Phản Sát Thương ngay lập tức
            CopperGolemProfile.activateReflect(le);

            player.sendMessage("§6§l[Đồng Nhân] §fĐã được gọi về, hồi §a20% HP §fvà bật §cPhản Sát Thương§f!");
            player.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            return true;
        }

        // ==========================================
        // 3B. NẾU LÀ TRIỆU HỒI MỚI (SPAWN)
        // ==========================================
        try {
            ActiveMob golem = MythicBukkit.inst().getMobManager().spawnMob("EarthCopperGolem", spawnLoc);

            if (golem != null) {
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - finalCost);
                cooldowns.put(player.getUniqueId(), currentTime);
                startCooldownNotifier(player, finalCd);

                golem.setLevel(level);
                golem.setOwnerUUID(player.getUniqueId());
                Entity bukkitEntity = golem.getEntity().getBukkitEntity();

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

                bukkitEntity.setMetadata("Summoner_Pet", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
                bukkitEntity.setMetadata("Pet_Type", new FixedMetadataValue(plugin, "CopperGolem"));
                bukkitEntity.setMetadata("Summon_Level", new FixedMetadataValue(plugin, level));

                // Lưu ID để làm Tái kích hoạt
                player.setMetadata("Active_Copper_Golem", new FixedMetadataValue(plugin, bukkitEntity.getUniqueId().toString()));

                if (rpgPlayer.getCurrentClass() instanceof vn.minecraft.rPGCore.classes.impl.SummonerClass) {
                    ((vn.minecraft.rPGCore.classes.impl.SummonerClass) rpgPlayer.getCurrentClass())
                            .registerPet(bukkitEntity.getUniqueId(), finalCost, rpgPlayer, "Golem Đồng");
                }

                player.getWorld().spawnParticle(Particle.BLOCK, spawnLoc.add(0, 1.5, 0), 50, 0.8, 0.8, 0.8, 0.1, Material.COPPER_BLOCK.createBlockData());
                player.getWorld().playSound(spawnLoc, Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
                player.sendMessage("§6§l[Triệu Hồi] §fMột Golem Đồng đã được chế tạo thành công!");
                return true;
            } else {
                player.sendMessage("§c[!] Lỗi Hệ Thống: Không tìm thấy Thú trong MythicMobs!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}