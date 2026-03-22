package vn.minecraft.rPGCore.skills.summoner;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
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
    @Override public Material getIcon() { return Material.WITHER_SKELETON_SKULL; }
    @Override public int getMaxLevel() { return 5; }
    @Override public int getUpgradeCost(int currentLevel) { return 2; }
    @Override public Map<String, Integer> getRequiredSkills() {
        Map<String, Integer> req = new HashMap<>();
        req.put("summoner_wolf", 3);
        return req;
    }

    @Override
    public double getManaCost(int level) {
        return 150.0 + (level * 10.0); // Chi phí thi triển cố định
    }

    @Override
    public long getCooldown(int level) {
        return 45000L;
    }

    @Override
    public List<String> getDescription(int level) {
        long baseDur = Math.min(180, 60 + ((level - 1) * 30));
        return Arrays.asList(
                "§7Hiến tế toàn bộ Linh Thú trên sân.",
                "§7Sấm sét giáng xuống gây sát thương diện rộng",
                "§7và bắt đầu 2s nghi thức triệu hồi.",
                "",
                "§f⚡ §c§lTINH LANG VƯƠNG GIÁNG THẾ:",
                "§f- Thời gian: §e" + baseDur + "s + (1s / 100 Máu tế)",
                "§f- Kích cỡ: §eCơ bản + (0.2 x Số lượng tế)",
                "§f- Máu: §a2500 + 80% Tổng Máu tế",
                "§f- Sát thương: §cDồn DMG tế (Giảm 10% mỗi con)",
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

        List<Entity> myPets = new ArrayList<>();
        double totalSacrificeHp = 0.0;
        List<Double> petDamages = new ArrayList<>();

        for (Entity e : player.getNearbyEntities(25, 10, 25)) {
            if (e.hasMetadata("Summoner_Pet") && e instanceof LivingEntity) {
                if (e.getMetadata("Summoner_Pet").get(0).asString().equals(player.getUniqueId().toString())) {
                    myPets.add(e);
                    totalSacrificeHp += ((LivingEntity) e).getAttribute(Attribute.MAX_HEALTH).getValue();
                    try {
                        ActiveMob am = MythicBukkit.inst().getMobManager().getActiveMob(e.getUniqueId()).orElse(null);
                        if (am != null) petDamages.add(am.getDamage());
                    } catch (Exception ignored) {}
                }
            }
        }

        if (myPets.isEmpty()) {
            player.sendMessage("§c§l[!] §fKhông có Thú triệu hồi nào ở gần!");
            return false;
        }

        petDamages.sort(Collections.reverseOrder());
        double totalSacrificeDmg = 0.0;
        for (int i = 0; i < petDamages.size(); i++) {
            double multiplier = Math.max(0.1, 1.0 - (i * 0.1));
            totalSacrificeDmg += petDamages.get(i) * multiplier;
        }

        cooldowns.put(player.getUniqueId(), currentTime);
        startCooldownNotifier(player, finalCd);
        SummonerClass sClass = (SummonerClass) rpgPlayer.getCurrentClass();

        // ==========================================
        // [FIX CHUẨN OOP]: Lấy chính xác lượng Mana Thế chấp thông qua RPGPlayer
        // ==========================================
        double reservedBefore = rpgPlayer.getReservedResource();

        // GIAI ĐOẠN 1: GIÁNG SÉT & GIẢI TÁN THÚ CŨ
        for (Entity pet : myPets) {
            Location loc = pet.getLocation();
            loc.getWorld().strikeLightningEffect(loc);

            for (Entity e : loc.getWorld().getNearbyEntities(loc, 4, 3, 4)) {
                if (e instanceof LivingEntity && e != player && !e.hasMetadata("Summoner_Pet")) {
                    LivingEntity target = (LivingEntity) e;
                    target.damage(50.0 + (level * 10), player);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 5));
                }
            }
            sClass.unregisterPet(pet.getUniqueId(), rpgPlayer); // Lệnh này sẽ tự hoàn Mana vào CurrentResource và trừ ở ReservedResource
            pet.remove();
        }

        // Đo chính xác số lượng Mana đã được giải phóng
        double reservedAfter = rpgPlayer.getReservedResource();
        final double exactManaFreed = reservedBefore - reservedAfter;
        final double totalRequiredMana = cost + exactManaFreed;

        player.sendMessage("§e§l[!] Nghi thức bắt đầu! §c" + myPets.size() + " linh thú §eđã dung hợp...");

        final Location ritualLoc = player.getLocation().clone();
        final int petCount = myPets.size();
        final double finalHpBonus = totalSacrificeHp;
        final double finalDmgBonus = totalSacrificeDmg;

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

                // Trừ phí Thi triển + Phí thế chấp Sói
                if (rpgPlayer.getCurrentResource() < totalRequiredMana) {
                    player.sendMessage("§c§l[!] NGHI THỨC SỤP ĐỔ! §fBạn cần " + (int)totalRequiredMana + " Mana (Phí thi triển + Phí duy trì) để hoàn thành.");
                    ritualLoc.getWorld().playSound(ritualLoc, Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                    return;
                }
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - totalRequiredMana);

                ritualLoc.getWorld().strikeLightningEffect(ritualLoc);
                ritualLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, ritualLoc.clone().add(0, 1, 0), 1);

                try {
                    ActiveMob alpha = MythicBukkit.inst().getMobManager().spawnMob("AlphaSpiritWolf", ritualLoc);
                    if (alpha != null) {
                        alpha.setLevel(level);
                        alpha.setOwnerUUID(player.getUniqueId());
                        Entity alphaEnt = alpha.getEntity().getBukkitEntity();

                        if (alphaEnt instanceof org.bukkit.entity.Wolf) {
                            org.bukkit.entity.Wolf wolf = (org.bukkit.entity.Wolf) alphaEnt;
                            wolf.setTamed(true);
                            wolf.setOwner(player);
                            wolf.setAngry(true);
                            wolf.setCollarColor(org.bukkit.DyeColor.RED);
                        }

                        if (alphaEnt instanceof LivingEntity) {
                            LivingEntity livingAlpha = (LivingEntity) alphaEnt;

                            livingAlpha.getAttribute(Attribute.SCALE).setBaseValue(1.5 + (petCount * 0.2));

                            double newMaxHp = 2500 + (level * 300) + (finalHpBonus * 0.8);
                            livingAlpha.getAttribute(Attribute.MAX_HEALTH).setBaseValue(newMaxHp);
                            livingAlpha.setHealth(newMaxHp);

                            // BƠM THẲNG SÁT THƯƠNG DUNG HỢP VÀO VANILLA
                            double baseMmDmg = 180 + (level * 30);
                            double newBaseDmg = baseMmDmg + finalDmgBonus;

                            if (livingAlpha.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                                livingAlpha.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(newBaseDmg);
                            }

                            long baseDurSec = Math.min(180, 60 + ((level - 1) * 30));
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

                        // Gắn thẻ căn cước
                        alphaEnt.setMetadata("Fusion_Damage_Bonus", new FixedMetadataValue(plugin, finalDmgBonus));
                        alphaEnt.setMetadata("Summoner_Pet", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

                        // [FIX]: Chuyển chính xác số Mana bị khóa lúc nãy sang cho con Sói Vương!
                        sClass.registerPet(alphaEnt.getUniqueId(), exactManaFreed, rpgPlayer, "Tinh Lang Vương");

                        player.sendMessage("§4§l[Dung Hợp] §fTinh Lang Vương đã giáng thế!");
                    }
                } catch (Exception e) {}
            }
        }.runTaskTimer(plugin, 0L, 1L);
        return true;
    }
}