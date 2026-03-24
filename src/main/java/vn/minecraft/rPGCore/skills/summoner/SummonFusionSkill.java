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
    @Override public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> req = new HashMap<>();
        req.put("summoner_wolf", 3);
        return req;
    }

    @Override public double getManaCost(int level) { return 150.0 + (level * 10.0); }
    @Override public long getCooldown(int level) { return 45000L; }

    @Override
    public List<String> getDescription(int level) {
        return Arrays.asList(
                "§7Hiến tế toàn bộ Linh Thú trên sân.",
                "§7Sấm sét giáng xuống gây sát thương diện rộng",
                "§7và bắt đầu 2s nghi thức triệu hồi.",
                "",
                "§f⚡ §c§lTINH LANG VƯƠNG GIÁNG THẾ:",
                "§f- Yêu cầu: §eÍt nhất 2 Linh Lang thường",
                "§f- Cấp độ Bonus: §a+1 Lv cho mỗi thú dư ra",
                "§f- Máu & Sát thương: §cGộp từ các tế phẩm",
                "",
                "§fTiêu hao: §b" + getManaCost(level) + " Mana",
                "§fThời gian hồi: §e" + (getCooldown(level) / 1000.0) + "s"
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

        // 1. TÌM KIẾM TẾ PHẨM HỢP LỆ (Chỉ lấy SpiritWolf)
        List<Entity> validPets = new ArrayList<>();
        double totalSacrificeHp = 0.0;
        List<Double> petDamages = new ArrayList<>();

        for (Entity e : player.getNearbyEntities(25, 10, 25)) {
            if (e.hasMetadata("Summoner_Pet") && e instanceof LivingEntity) {
                if (e.getMetadata("Summoner_Pet").get(0).asString().equals(player.getUniqueId().toString())) {
                    String petType = e.hasMetadata("Pet_Type") ? e.getMetadata("Pet_Type").get(0).asString() : "";

                    if (petType.equalsIgnoreCase("SpiritWolf")) {
                        validPets.add(e);
                        LivingEntity lp = (LivingEntity) e;
                        if (lp.getAttribute(Attribute.MAX_HEALTH) != null) {
                            totalSacrificeHp += lp.getAttribute(Attribute.MAX_HEALTH).getValue();
                        }
                        // Lấy dummy stat để tính fusion multiplier
                        petDamages.add(10.0);
                    }
                }
            }
        }

        // 2. KIỂM TRA ĐIỀU KIỆN
        if (validPets.size() < 2) {
            player.sendMessage("§c§l[!] §fBạn cần ít nhất 2 Linh Lang thường trên sân để thực hiện Dung Hợp!");
            return false;
        }

        // 3. TÍNH BỘI SỐ VÀ BONUS LEVEL
        petDamages.sort(Collections.reverseOrder());
        double fusionMultiplier = 0.0;
        for (int i = 0; i < petDamages.size(); i++) {
            double weight = Math.max(0.1, 1.0 - (i * 0.1));
            fusionMultiplier += weight;
        }

        final int petsConsumed = validPets.size();
        final int finalAlphaLevel = Math.min(level + (petsConsumed - 2), 20); // Tối đa Lv 20
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
        final double totalRequiredMana = cost + exactManaFreed; // Phải đủ trả lại tiền thế chấp cho Sói Vương

        player.sendMessage("§e§l[!] Nghi thức bắt đầu! §c" + petsConsumed + " linh thú §eđã dung hợp...");
        player.sendMessage("§b✦ Cấp độ Tinh Lang Vương dự kiến: §eLv " + finalAlphaLevel);

        final Location ritualLoc = player.getLocation().clone();

        // 5. NGHI THỨC TRIỆU HỒI SÓI VƯƠNG (Delay 2 giây)
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

                // Kiểm tra lại Mana trước khi xuất hiện
                if (rpgPlayer.getCurrentResource() < totalRequiredMana) {
                    player.sendMessage("§c§l[!] NGHI THỨC SỤP ĐỔ! §fBạn cần " + (int)totalRequiredMana + " Mana để duy trì Sói Vương.");
                    ritualLoc.getWorld().playSound(ritualLoc, Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                    return;
                }
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - totalRequiredMana);

                ritualLoc.getWorld().strikeLightningEffect(ritualLoc);
                ritualLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, ritualLoc.clone().add(0, 1, 0), 1);

                try {
                    ActiveMob alpha = MythicBukkit.inst().getMobManager().spawnMob("AlphaSpiritWolf", ritualLoc);
                    if (alpha != null) {
                        alpha.setLevel(finalAlphaLevel);
                        alpha.setOwnerUUID(player.getUniqueId());
                        Entity alphaEnt = alpha.getEntity().getBukkitEntity();

                        if (alphaEnt instanceof org.bukkit.entity.Wolf) {
                            org.bukkit.entity.Wolf wolf = (org.bukkit.entity.Wolf) alphaEnt;
                            wolf.setTamed(true);
                            wolf.setOwner(player);
                            wolf.setAngry(true);
                            wolf.setCollarColor(DyeColor.WHITE);
                        }

                        // GẮN DATA TRƯỚC KHI GỌI PROFILE
                        alphaEnt.setMetadata("Summoner_Pet", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
                        alphaEnt.setMetadata("Pet_Type", new FixedMetadataValue(plugin, "AlphaSpiritWolf"));
                        alphaEnt.setMetadata("Summon_Level", new FixedMetadataValue(plugin, finalAlphaLevel));
                        alphaEnt.setMetadata("Fusion_Multiplier", new FixedMetadataValue(plugin, finalFusionMultiplier));

                        if (alphaEnt instanceof LivingEntity) {
                            LivingEntity livingAlpha = (LivingEntity) alphaEnt;

                            vn.minecraft.rPGCore.skills.summoner.pets.IPetProfile profile = vn.minecraft.rPGCore.skills.summoner.pets.SummonPetManager.getProfile("AlphaSpiritWolf");
                            if (profile != null) {
                                profile.onSpawn(player, livingAlpha, finalAlphaLevel, (double)petsConsumed, finalHpBonus);
                            }

                            long baseDurSec = Math.min(180, 60 + ((finalAlphaLevel - 1) * 30));
                            long bonusDurSec = (long) (finalHpBonus / 100.0);
                            long totalDurationTicks = (baseDurSec + bonusDurSec) * 20L;

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (livingAlpha.isValid() && !livingAlpha.isDead()) {
                                        livingAlpha.setHealth(0);
                                    }
                                }
                            }.runTaskLater(plugin, totalDurationTicks);
                        }

                        sClass.registerPet(alphaEnt.getUniqueId(), exactManaFreed, rpgPlayer, "Tinh Lang Vương");
                        player.sendMessage("§4§l[Dung Hợp] §fTinh Lang Vương đã giáng thế!");
                    }
                } catch (Exception e) {}
            }
        }.runTaskTimer(plugin, 0L, 1L);
        return true;
    }
}