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
import vn.minecraft.rPGCore.managers.CombatManager;
import vn.minecraft.rPGCore.managers.StatusEffectManager;
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

    @Override
    public double getManaCost(int level) {
        return 100.0 + (level * 10.0);
    }

    @Override public long getCooldown(int level) { return 60000L; }

    @Override
    public List<String> getDescription(int level) {
        double explosionDmg = 50.0 + (level * 10.0);

        return Arrays.asList(
                "§7Khởi động vòng tròn ma thuật, hiến tế các Linh Thú",
                "§7trên sân để triệu hồi Thực Thể Tiến Cấp.",
                "",
                "§e[Dung Hợp: Cơ Bản]",
                "§f- Yêu cầu: §aTối thiểu 3 Linh Thú cùng loại",
                "§f- Bùng nổ: §7Gây §c" + explosionDmg + " Sát thương §7và §eChoáng 1s",
                "§7  cho toàn bộ kẻ địch đứng gần các tế phẩm.",
                "",
                "§d[Dung Hợp: Tái sinh]",
                "§f- Cấp độ Bonus: §a+1 Lv §f(Mỗi 2 thú hiến tế dư ra)",
                "§f- Sức mạnh: §cKế thừa Máu, Sát thương & Kích thước",
                "§f- Sinh mệnh: §6Có thời hạn §f(Tăng theo Máu tế phẩm)",
                "",
                "§6[Dung Hợp Lai (Yêu cầu Nội tại)]",
                "§f- §c2 Tinh Linh Lửa §f+ §61 Golem Đồng §f= §4Golem Nham Thạch"
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

        SummonerClass sClass = (SummonerClass) rpgPlayer.getCurrentClass();
        if (sClass == null) return false;

        // ==========================================
        // QUÉT TOÀN BỘ ĐỆ TỬ CỦA NGƯỜI CHƠI TRÊN SÂN
        // ==========================================
        List<Entity> fireSpirits = new ArrayList<>();
        List<Entity> copperGolems = new ArrayList<>();
        Map<String, List<Entity>> fusableGroups = new HashMap<>();

        for (Entity e : player.getNearbyEntities(25, 10, 25)) {
            if (e.hasMetadata("Summoner_Pet") && e instanceof LivingEntity) {
                if (e.getMetadata("Summoner_Pet").get(0).asString().equals(player.getUniqueId().toString())) {
                    String petType = e.hasMetadata("Pet_Type") ? e.getMetadata("Pet_Type").get(0).asString() : "";

                    // Lọc cho Dung hợp Lai
                    if (petType.equals("FireSpirit")) fireSpirits.add(e);
                    if (petType.equals("CopperGolem")) copperGolems.add(e);

                    // Lọc cho Dung hợp Cơ bản
                    IPetProfile profile = SummonPetManager.getProfile(petType);
                    if (profile != null && profile.getFusionTargetId() != null) {
                        fusableGroups.computeIfAbsent(petType, k -> new ArrayList<>()).add(e);
                    }
                }
            }
        }

        // ==========================================
        // LUỒNG 1: DUNG HỢP LAI (HYBRID FUSION) - Golem Nham Thạch
        // ==========================================

        // [ĐÃ SỬA LỖI]: Gọi getSkillData() trước khi lấy level
        int advancedFusionLevel = rpgPlayer.getSkillData().getSkillLevel("summoner_advanced_fusion");

        if (advancedFusionLevel > 0 && fireSpirits.size() >= 2 && copperGolems.size() >= 1) {

            List<Entity> hybridSacrifices = new ArrayList<>();
            hybridSacrifices.add(fireSpirits.get(0));
            hybridSacrifices.add(fireSpirits.get(1));
            hybridSacrifices.add(fireSpirits.get(2));
            hybridSacrifices.add(fireSpirits.get(3));
            hybridSacrifices.add(copperGolems.get(0));

            double totalHpBonus = 0;
            for (Entity sac : hybridSacrifices) {
                LivingEntity le = (LivingEntity) sac;
                if (le.getAttribute(Attribute.MAX_HEALTH) != null) {
                    totalHpBonus += le.getAttribute(Attribute.MAX_HEALTH).getValue();
                }
            }

            // Golem Nham Thạch là kết tinh lai tạp hoàn hảo, nhận multiplier mặc định là 1.5
            final double hybridMultiplier = 4.0;

            // Xử lý nổ và xóa xác chết
            double reservedBefore = rpgPlayer.getReservedResource();
            for (Entity pet : hybridSacrifices) {
                Location loc = pet.getLocation();
                loc.getWorld().strikeLightningEffect(loc);

                for (Entity e : loc.getWorld().getNearbyEntities(loc, 4, 3, 4)) {
                    if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                        LivingEntity target = (LivingEntity) e;
                        target.damage(50.0 + (level * 10), player);
                        StatusEffectManager.applySlow(target, 30, 1.0, 5); // Khống chế cứng 1.5s
                    }
                }
                sClass.unregisterPet(pet.getUniqueId(), rpgPlayer);
                pet.remove();
            }
            double reservedAfter = rpgPlayer.getReservedResource();
            final double exactManaFreed = reservedBefore - reservedAfter;
            final double totalRequiredMana = cost + exactManaFreed;

            // Đưa vào nghi thức tiến hóa
            startRitual(player, rpgPlayer, level, "MagmaGolem", "Golem Nham Thạch", 3, hybridMultiplier, totalHpBonus, totalRequiredMana, exactManaFreed, currentTime, finalCd);
            return true;
        }

        // ==========================================
        // LUỒNG 2: DUNG HỢP CƠ BẢN (TRUYỀN THỐNG)
        // ==========================================
        String bestGroupType = null;
        int maxCount = 0;
        for (Map.Entry<String, List<Entity>> entry : fusableGroups.entrySet()) {
            if (entry.getValue().size() > maxCount) {
                maxCount = entry.getValue().size();
                bestGroupType = entry.getKey();
            }
        }

        if (bestGroupType == null || maxCount < 3) {
            player.sendMessage("§c§l[!] §fBạn cần ít nhất 3 Linh Thú cùng loại (hoặc đúng công thức Dung hợp lai) để thực hiện Nghi thức!");
            return false;
        }

        List<Entity> validPets = fusableGroups.get(bestGroupType);
        IPetProfile baseProfile = SummonPetManager.getProfile(bestGroupType);

        final String targetFusionId = baseProfile.getFusionTargetId();
        final String targetFusionName = baseProfile.getFusionTargetName();

        double totalSacrificeHp = 0.0;
        List<Double> petDamages = new ArrayList<>();

        for (Entity pet : validPets) {
            LivingEntity lp = (LivingEntity) pet;
            if (lp.getAttribute(Attribute.MAX_HEALTH) != null) {
                totalSacrificeHp += lp.getAttribute(Attribute.MAX_HEALTH).getValue();
            }
            petDamages.add(10.0);
        }

        petDamages.sort(Collections.reverseOrder());
        double fusionMultiplier = 0.0;
        for (int i = 0; i < petDamages.size(); i++) {
            double weight = Math.max(0.1, 1.0 - (i * 0.1));
            fusionMultiplier += weight;
        }

        double reservedBefore = rpgPlayer.getReservedResource();
        for (Entity pet : validPets) {
            Location loc = pet.getLocation();
            loc.getWorld().strikeLightningEffect(loc);

            for (Entity e : loc.getWorld().getNearbyEntities(loc, 4, 3, 4)) {
                if (e instanceof LivingEntity && CombatManager.canTarget(player, e)) {
                    LivingEntity target = (LivingEntity) e;
                    target.damage(50.0 + (level * 10), player);
                    StatusEffectManager.applySlow(target, 30, 1.0, 5); // Khống chế cứng 1.5s
                }
            }
            sClass.unregisterPet(pet.getUniqueId(), rpgPlayer);
            pet.remove();
        }
        double reservedAfter = rpgPlayer.getReservedResource();
        final double exactManaFreed = reservedBefore - reservedAfter;
        final double totalRequiredMana = cost + exactManaFreed;

        startRitual(player, rpgPlayer, level, targetFusionId, targetFusionName, validPets.size(), fusionMultiplier, totalSacrificeHp, totalRequiredMana, exactManaFreed, currentTime, finalCd);
        return true;
    }

    // ==========================================
    // HÀM CHUNG: THỰC THI ANIMATION VÀ GỌI THÚ TIẾN HÓA RA SÂN
    // ==========================================
    private void startRitual(Player player, RPGPlayer rpgPlayer, int skillLevel, String targetFusionId, String targetFusionName, int petsConsumed, double finalFusionMultiplier, double finalHpBonus, double totalRequiredMana, double exactManaFreed, long currentTime, long finalCd) {

        final int finalFusionLevel = Math.min(skillLevel + ((petsConsumed - 3) / 2), 20);

        cooldowns.put(player.getUniqueId(), currentTime);
        startCooldownNotifier(player, finalCd);
        SummonerClass sClass = (SummonerClass) rpgPlayer.getCurrentClass();

        player.sendMessage("§e§l[!] Nghi thức bắt đầu! §c" + petsConsumed + " linh thú §eđã dung hợp...");
        player.sendMessage("§b✦ Cấp độ " + targetFusionName + " dự kiến: §eLv " + finalFusionLevel);

        final Location ritualLoc = player.getLocation().clone();

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
                    ActiveMob fusionMob = MythicBukkit.inst().getMobManager().spawnMob(targetFusionId, ritualLoc);
                    if (fusionMob != null) {
                        fusionMob.setLevel(finalFusionLevel);
                        fusionMob.setOwnerUUID(player.getUniqueId());
                        Entity fusionEnt = fusionMob.getEntity().getBukkitEntity();

                        if (fusionEnt instanceof org.bukkit.entity.Tameable) {
                            ((org.bukkit.entity.Tameable) fusionEnt).setTamed(true);
                            ((org.bukkit.entity.Tameable) fusionEnt).setOwner(player);
                        }

                        // [FIX]: Tránh lỗi ép kiểu nếu Golem Nham Thạch (Iron Golem) không phải là Sói
                        if (fusionEnt instanceof org.bukkit.entity.Wolf) {
                            ((org.bukkit.entity.Wolf) fusionEnt).setAngry(true);
                            ((org.bukkit.entity.Wolf) fusionEnt).setCollarColor(DyeColor.WHITE);
                        }

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

                            // Tính thời gian tồn tại
                            long baseDurSec = Math.min(180, 60 + ((finalFusionLevel - 1) * 30));
                            long bonusDurSec = (long) (finalHpBonus / 100.0);
                            long totalDurationTicks = (baseDurSec + bonusDurSec) * 20L;

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (livingFusion.isValid() && !livingFusion.isDead()) {
                                        livingFusion.setHealth(0); // Hết hạn là chết tự nhiên (gọi onDeath)
                                    }
                                }
                            }.runTaskLater(plugin, totalDurationTicks);
                        }

                        sClass.registerPet(fusionEnt.getUniqueId(), exactManaFreed, rpgPlayer, targetFusionName);

                        if (targetFusionId.equals("MagmaGolem")) {
                            player.sendMessage("§4§l[Dung Hợp Lai] §f" + targetFusionName + " đã giáng thế!");
                        } else {
                            player.sendMessage("§4§l[Dung Hợp] §f" + targetFusionName + " đã giáng thế!");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}