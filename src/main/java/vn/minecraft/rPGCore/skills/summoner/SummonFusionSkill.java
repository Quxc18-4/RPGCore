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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.RPGCore;
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
        req.put("summoner_wolf", 3); // Cần Skill Sói Lv3 để mở khóa
        return req;
    }

    @Override
    public double getManaCost(int level) {
        return 100.0 + (level * 20.0); // Cấp càng cao tốn càng nhiều Mana để vẽ vòng
    }

    @Override
    public long getCooldown(int level) {
        return 45000L; // 45 giây
    }

    @Override
    public List<String> getDescription(int level) {
        return Arrays.asList(
                "§7Hiến tế toàn bộ Linh Thú hiện tại trên sân.",
                "§7Sau 2 giây nghi thức, giáng sấm sét và triệu hồi",
                "§7một §c§lHuyết Lang Vương §7khổng lồ.",
                "",
                "§cCẢNH BÁO: §7Nếu khi kết thúc nghi thức bạn",
                "§7không đủ Mana, nghi thức sẽ sụp đổ và mất trắng đệ tử!",
                "",
                "§fMana tiêu hao: §b" + getManaCost(level),
                "§fThời gian hồi: §e" + (getCooldown(level) / 1000.0) + "s"
        );
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long baseCd = getCooldown(level);
        long finalCd = getFinalCooldown(rpgPlayer, baseCd);

        if (currentTime - lastUsed < finalCd) {
            double timeLeft = (finalCd - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }

        List<Entity> myPets = new ArrayList<>();
        double totalSacrificeHp = 0.0;
        double totalSacrificeDmg = 0.0;
        String dominantSpecies = "AlphaSpiritWolf"; // Mặc định (Có thể mở rộng switch-case đọc tên loài)

        // 1. TÌM KIẾM VÀ TÍNH TOÁN CHỈ SỐ TẾ PHẨM
        for (Entity e : player.getNearbyEntities(25, 10, 25)) {
            if (e.hasMetadata("Summoner_Pet") && e instanceof LivingEntity) {
                String ownerId = e.getMetadata("Summoner_Pet").get(0).asString();
                if (ownerId.equals(player.getUniqueId().toString())) {
                    myPets.add(e);

                    // Lấy máu và sát thương lưu vào bộ nhớ tạm
                    LivingEntity livingPet = (LivingEntity) e;
                    totalSacrificeHp += livingPet.getAttribute(Attribute.MAX_HEALTH).getValue();

                    try {
                        ActiveMob am = MythicBukkit.inst().getMobManager().getActiveMob(e.getUniqueId()).orElse(null);
                        if (am != null) totalSacrificeDmg += am.getDamage();
                    } catch (Exception ignored) {}
                }
            }
        }

        if (myPets.isEmpty()) {
            player.sendMessage("§c§l[!] §fKhông có Thú triệu hồi nào ở gần để tiến hành dung hợp!");
            return false;
        }

        // Bắt đầu Hồi chiêu
        cooldowns.put(player.getUniqueId(), currentTime);
        startCooldownNotifier(player, finalCd);
        double cost = getManaCost(level);
        SummonerClass sClass = (SummonerClass) rpgPlayer.getCurrentClass();

        // 2. GIAI ĐOẠN 1: GIÁNG SÉT TỊCH THU ĐỆ TỬ
        for (Entity pet : myPets) {
            Location loc = pet.getLocation();
            loc.getWorld().strikeLightningEffect(loc);
            loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);

            // Xóa sổ và Hoàn Mana ngay lập tức
            sClass.unregisterPet(pet.getUniqueId(), rpgPlayer);
            pet.remove();
        }
        player.sendMessage("§e§l[!] Nghi thức bắt đầu! Vòng xoáy năng lượng đang hội tụ...");

        // Final các biến để dùng trong Task
        final double finalHpBonus = totalSacrificeHp;
        final double finalDmgBonus = totalSacrificeDmg;
        final int petCount = myPets.size();
        final Location ritualLoc = player.getLocation().clone();

        // 3. GIAI ĐOẠN 2 & 3: VẼ VÒNG TRÒN VÀ PHÁN XÉT (Trễ 2s = 40 ticks)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                // VẼ VÒNG TRÒN (Hiệu ứng Visual trong 40 ticks)
                if (ticks < 40) {
                    double radius = 3.0;
                    double angle = ticks * (Math.PI / 10);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    ritualLoc.getWorld().spawnParticle(Particle.FLAME, ritualLoc.clone().add(x, 0.1, z), 2, 0, 0, 0, 0);
                    ritualLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, ritualLoc.clone().add(-x, 0.1, -z), 2, 0, 0, 0, 0);
                    if (ticks % 10 == 0) ritualLoc.getWorld().playSound(ritualLoc, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.5f);
                    ticks++;
                    return;
                }

                this.cancel();

                // GIAI ĐOẠN 3: PHÁN XÉT LÒNG THAM
                if (rpgPlayer.getCurrentResource() < cost) {
                    player.sendMessage("§c§l[!] NGHI THỨC SỤP ĐỔ! §fBạn không đủ " + cost + " Mana để dung hợp.");
                    ritualLoc.getWorld().playSound(ritualLoc, Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
                    ritualLoc.getWorld().spawnParticle(Particle.SMOKE, ritualLoc.clone().add(0, 1, 0), 50, 1, 1, 1, 0.1);
                    return;
                }

                // Trừ Mana Dung Hợp
                rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);

                // GIAI ĐOẠN 4: GIÁNG THẾ!
                ritualLoc.getWorld().strikeLightningEffect(ritualLoc);
                ritualLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, ritualLoc.clone().add(0, 1, 0), 1);
                ritualLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, ritualLoc.clone().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

                try {
                    ActiveMob alpha = MythicBukkit.inst().getMobManager().spawnMob(dominantSpecies, ritualLoc);
                    if (alpha != null) {
                        alpha.setLevel(level);
                        alpha.setOwnerUUID(player.getUniqueId());
                        Entity alphaEnt = alpha.getEntity().getBukkitEntity();

                        // Tính Toán Kích Thước (Scale 1.5 mặc định + 0.3 mỗi con tế)
                        double scaleBase = 1.5 + (petCount * 0.3);
                        if (alphaEnt instanceof LivingEntity) {
                            LivingEntity livingAlpha = (LivingEntity) alphaEnt;
                            livingAlpha.getAttribute(Attribute.SCALE).setBaseValue(scaleBase);

                            // Ghi đè máu (Cộng dồn máu từ thú hiến tế)
                            double newMaxHp = livingAlpha.getAttribute(Attribute.MAX_HEALTH).getValue() + (finalHpBonus * 0.8);
                            livingAlpha.getAttribute(Attribute.MAX_HEALTH).setBaseValue(newMaxHp);
                            livingAlpha.setHealth(newMaxHp);
                        }

                        // Lưu Sát Thương Hiến Tế vào Meta để MM đọc được
                        alphaEnt.setMetadata("Fusion_Damage_Bonus", new FixedMetadataValue(plugin, finalDmgBonus));
                        alphaEnt.setMetadata("Summoner_Pet", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

                        // Đăng ký Sổ Nam Tào (Thế chấp lượng Mana của chính chiêu này)
                        sClass.registerPet(alphaEnt.getUniqueId(), cost, rpgPlayer, "Huyết Lang Vương");

                        player.sendMessage("§4§l[Dung Hợp] §fSự phẫn nộ của tự nhiên đã thành hình!");
                        ritualLoc.getWorld().playSound(ritualLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.8f);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }
}