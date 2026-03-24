package vn.minecraft.rPGCore.skills.summoner;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import vn.minecraft.rPGCore.classes.impl.SummonerClass;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;
import vn.minecraft.rPGCore.skills.summoner.pets.IPetProfile;
import vn.minecraft.rPGCore.skills.summoner.pets.SummonPetManager;

import java.util.*;

public class SummonFusionSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public SummonFusionSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "summoner_fusion"; }
    @Override public String getName() { return "Nghi Thức Dung Hợp"; }
    @Override public String getRequiredClass() { return "summoner"; }
    @Override public Material getIcon() { return Material.NETHER_STAR; }
    @Override public int getMaxLevel() { return 5; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public Map<String, Integer> getRequiredSkills() { return new HashMap<>(); }
    @Override public int getRequiredPlayerLevel() { return 28; }

    @Override public double getManaCost(int level) { return 110.0 + (level * 10.0); }
    @Override public long getCooldown(int level) { return 60000L; }

    @Override
    public List<String> getDescription(int level) {
        // Lấy sát thương nổ diện rộng từ logic code bên dưới
        double explosionDmg = 50.0 + (level * 10.0);

        return Arrays.asList(
                "§7Khởi động vòng tròn ma thuật, hiến tế các Linh Thú",
                "§7cùng loại trên sân để triệu hồi Thực Thể Tiến Cấp.",
                "",
                "§e[Dung Hợp: Hiến tế]",
                "§f- Yêu cầu: §aTối thiểu 2 Linh Thú cùng loại",
                "§f- Bùng nổ: §7Gây §c" + explosionDmg + " Sát thương §7và §eChoáng 1s",
                "§7  cho toàn bộ kẻ địch đứng gần các tế phẩm.",
                "",
                "§d[Dung Hợp: Tái sinh]",
                "§f- Cấp độ Bonus: §a+1 Lv §f(Cho mỗi thú hiến tế dư ra)",
                "§f- Sức mạnh: §cKế thừa Máu & Sát thương tế phẩm",
                "§f- Sinh mệnh: §6Có thời hạn §f(Tăng theo Máu tế phẩm)"
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
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", (finalCd - (currentTime - lastUsed)) / 1000.0) + "s)");
            return false;
        }

        // ==========================================
        // 1. QUÉT & PHÂN LOẠI THÚ TRÊN SÂN (UNIVERSAL SCAN)
        // ==========================================
        Map<String, List<Entity>> fusableGroups = new HashMap<>();

        for (Entity e : player.getNearbyEntities(25, 10, 25)) {
            if (e.hasMetadata("Summoner_Pet") && e instanceof LivingEntity) {
                if (e.getMetadata("Summoner_Pet").get(0).asString().equals(player.getUniqueId().toString())) {
                    String petType = e.hasMetadata("Pet_Type") ? e.getMetadata("Pet_Type").get(0).asString() : "";
                    IPetProfile profile = SummonPetManager.getProfile(petType);

                    // Nếu thú này có dạng tiến hóa -> Thêm vào nhóm chờ dung hợp
                    if (profile != null && profile.getFusionTargetId() != null) {
                        fusableGroups.computeIfAbsent(petType, k -> new ArrayList<>()).add(e);
                    }
                }
            }
        }

        // ==========================================
        // 2. CHỌN NHÓM ĐÔNG NHẤT ĐỂ DUNG HỢP
        // ==========================================
        String bestGroupType = null;
        int maxCount = 0;
        for (Map.Entry<String, List<Entity>> entry : fusableGroups.entrySet()) {
            if (entry.getValue().size() > maxCount) {
                maxCount = entry.getValue().size();
                bestGroupType = entry.getKey();
            }
        }

        if (bestGroupType == null || maxCount < 2) {
            player.sendMessage("§c§l[!] §fBạn cần ít nhất 2 Linh Thú cùng loại (chưa tiến hóa) để dung hợp!");
            return false;
        }

        List<Entity> validPets = fusableGroups.get(bestGroupType);
        IPetProfile baseProfile = SummonPetManager.getProfile(bestGroupType);

        final String targetFusionId = baseProfile.getFusionTargetId();
        final String targetFusionName = baseProfile.getFusionTargetName();

        // 3. TÍNH BỘI SỐ VÀ BONUS LEVEL
        double totalSacrificeHp = 0.0;
        List<Double> petDamages = new ArrayList<>();

        for (Entity pet : validPets) {
            LivingEntity lp = (LivingEntity) pet;
            if (lp.getAttribute(Attribute.MAX_HEALTH) != null) {
                totalSacrificeHp += lp.getAttribute(Attribute.MAX_HEALTH).getValue();
            }
            // Lấy dummy stat 10.0 để tính trọng số dung hợp
            petDamages.add(10.0);
        }

        petDamages.sort(Collections.reverseOrder());
        double fusionMultiplier = 0.0;
        for (int i = 0; i < petDamages.size(); i++) {
            double weight = Math.max(0.1, 1.0 - (i * 0.1));
            fusionMultiplier += weight;
        }

        final int petsConsumed = validPets.size();
        final int finalFusionLevel = Math.min(level + (petsConsumed - 2), 20); // Tối đa Lv 20
        final double finalFusionMultiplier = fusionMultiplier;
        final double finalHpBonus = totalSacrificeHp;

        cooldowns.put(player.getUniqueId(), currentTime);
        startCooldownNotifier(player, finalCd);
        SummonerClass sClass = (SummonerClass) rpgPlayer.getCurrentClass();

        // 4. THU HỒI TẾ PHẨM VÀ TÍNH MANA TRẢ LẠI
        double reservedBefore = rpgPlayer.getReservedResource();

        for (Entity pet : validPets) {
            Location loc = pet.getLocation();
            loc.getWorld().strikeLightningEffect(loc);

            for (Entity e : loc.getWorld().getNearbyEntities(loc, 4, 3, 4)) {
                if (e instanceof LivingEntity && e != player && !e.hasMetadata("Summoner_Pet")) {
                    LivingEntity target = (LivingEntity) e;
                    target.damage(50.0 + (level * 10), player);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 5));
                }
            }
            sClass.unregisterPet(pet.getUniqueId(), rpgPlayer);
            pet.remove();
        }

        double reservedAfter = rpgPlayer.getReservedResource();
        final double exactManaFreed = reservedBefore - reservedAfter;
        final double totalRequiredMana = cost + exactManaFreed;

        player.sendMessage("§e§l[!] Nghi thức bắt đầu! §c" + petsConsumed + " linh thú §eđã dung hợp...");
        player.sendMessage("§b✦ Cấp độ " + targetFusionName + " dự kiến: §eLv " + finalFusionLevel);

        final Location ritualLoc = player.getLocation().clone();

        // 5. NGHI THỨC TRIỆU HỒI (UNIVERSAL)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks < 40) {
                    double radius = 3.0;
                    double x = Math.cos(ticks * (Math.PI / 10)) * radius;
                    double z = Math.sin(ticks * (Math.PI / 10)) * radius;
                    ritualLoc.getWorld().spawnParticle(Particle.FLAME, ritualLoc.clone().add(x, 0.1, z), 2, 0, 0, 0, 0);
                    ritualLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, ritualLoc.clone().add(-x, 0.1, -z), 2, 0, 0, 0, 0);
                    ticks++;
                    return;
                }
                this.cancel();

                if (rpgPlayer.getCurrentResource() < totalRequiredMana) {
                    player.sendMessage("§c§l[!] NGHI THỨC SỤP ĐỔ! §fBạn cần " + (int)totalRequiredMana + " Mana để duy trì thực thể.");
                    ritualLoc.getWorld().playSound(ritualLoc, Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                    return;
                }
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - totalRequiredMana);

                ritualLoc.getWorld().strikeLightningEffect(ritualLoc);
                ritualLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, ritualLoc.clone().add(0, 1, 0), 1);

                try {
                    // TRIỆU HỒI DỰA TRÊN ID TARGET CỦA PROFILE
                    ActiveMob fusionMob = MythicBukkit.inst().getMobManager().spawnMob(targetFusionId, ritualLoc);
                    if (fusionMob != null) {
                        fusionMob.setLevel(finalFusionLevel);
                        fusionMob.setOwnerUUID(player.getUniqueId());
                        Entity fusionEnt = fusionMob.getEntity().getBukkitEntity();

                        if (fusionEnt instanceof org.bukkit.entity.Tameable) {
                            ((org.bukkit.entity.Tameable) fusionEnt).setTamed(true);
                            ((org.bukkit.entity.Tameable) fusionEnt).setOwner(player);
                        }
                        if (fusionEnt instanceof org.bukkit.entity.Wolf) {
                            ((org.bukkit.entity.Wolf) fusionEnt).setAngry(true);
                            ((org.bukkit.entity.Wolf) fusionEnt).setCollarColor(DyeColor.WHITE);
                        }

                        // GẮN DATA TRƯỚC KHI GỌI PROFILE
                        fusionEnt.setMetadata("Summoner_Pet", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
                        fusionEnt.setMetadata("Pet_Type", new FixedMetadataValue(plugin, targetFusionId));
                        fusionEnt.setMetadata("Summon_Level", new FixedMetadataValue(plugin, finalFusionLevel));
                        fusionEnt.setMetadata("Fusion_Multiplier", new FixedMetadataValue(plugin, finalFusionMultiplier));

                        if (fusionEnt instanceof LivingEntity) {
                            LivingEntity livingFusion = (LivingEntity) fusionEnt;

                            IPetProfile fusionProfile = SummonPetManager.getProfile(targetFusionId);
                            if (fusionProfile != null) {
                                fusionProfile.onSpawn(player, livingFusion, finalFusionLevel, (double)petsConsumed, finalHpBonus);
                            }

                            long baseDurSec = Math.min(180, 60 + ((finalFusionLevel - 1) * 30));
                            long bonusDurSec = (long) (finalHpBonus / 100.0);
                            long totalDurationTicks = (baseDurSec + bonusDurSec) * 20L;

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (livingFusion.isValid() && !livingFusion.isDead()) {
                                        livingFusion.setHealth(0);
                                    }
                                }
                            }.runTaskLater(plugin, totalDurationTicks);
                        }

                        sClass.registerPet(fusionEnt.getUniqueId(), exactManaFreed, rpgPlayer, targetFusionName);
                        player.sendMessage("§4§l[Dung Hợp] §f" + targetFusionName + " đã giáng thế!");
                    }
                } catch (Exception e) {}
            }
        }.runTaskTimer(plugin, 0L, 1L);
        return true;
    }
}