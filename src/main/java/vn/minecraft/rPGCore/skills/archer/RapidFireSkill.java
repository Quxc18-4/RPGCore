package vn.minecraft.rPGCore.skills.archer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.minecraft.rPGCore.player.RPGPlayer;
import vn.minecraft.rPGCore.skills.ISkill;

import java.util.*;

public class RapidFireSkill implements ISkill {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public RapidFireSkill(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "archer_rapid_fire"; }
    @Override public String getName() { return "Liên Hoàn Tên"; }
    @Override public String getRequiredClass() { return "archer"; }
    @Override public Material getIcon() { return Material.REPEATER; } // Dùng Repeater cho ngầu
    @Override public int getMaxLevel() { return 6; }
    @Override public int getUpgradeCost(int currentLevel) { return 1; }

    @Override
    public Map<String, Integer> getRequiredSkills() {
        return Collections.singletonMap("archer_double_shot", 10); // Cần Nhị Tiễn lv 10
    }

    @Override
    public double getManaCost(int level) {
        if (level >= 6) return 70.0;
        if (level >= 3) return 65.0;
        return 60.0;
    }

    @Override
    public long getCooldown(int level) {
        double cd = 18.0;
        if (level >= 6) cd = 15.0;
        else if (level >= 3) cd = 16.5;
        return (long) (cd * 1000);
    }

    private int getArrowCount(int level) {
        if (level >= 6) return 28;
        if (level >= 3) return 20;
        return 16;
    }

    private float getSpeedBonus(int level) {
        return (level >= 6) ? 0.18f : 0.15f;
    }

    @Override
    public List<String> getDescription(int level) {
        List<String> lore = new ArrayList<>();
        int displayLevel = Math.max(1, level);

        lore.add("§7Vận chiêu, xả liên tục một cơn mưa tên.");
        lore.add("§7Không thể dùng chiêu khác trong lúc xả.");

        // BẠN TỰ SỬA CÔNG THỨC SÁT THƯƠNG Ở ĐÂY (Đang để tạm Base 15% + 2%)
        double dmg = 15 + (displayLevel - 1) * 2;

        lore.add("§7Số mũi tên: §a" + getArrowCount(displayLevel) + " mũi");
        lore.add("§7Sát thương: §c" + (int) dmg + "% §7mỗi mũi");
        lore.add("§7Tốc chạy tăng: §b" + (int)(getSpeedBonus(displayLevel) * 100) + "%");

        if (level < 6) {
            lore.add("");
            lore.add("§8§o(Kỹ năng đột phá ở cấp 3, 6)");
        }
        return lore;
    }

    @Override
    public boolean execute(Player player, RPGPlayer rpgPlayer, int level) {
        if (level <= 0) return false;

        double cost = getManaCost(level);
        if (rpgPlayer.getCurrentResource() < cost) {
            player.sendMessage("§cKhông đủ Mana!");
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);

        long baseCd = getCooldown(level); // Lấy thời gian gốc
        long finalCd = getFinalCooldown(rpgPlayer, baseCd); // Đưa qua máy ép CDR

        if (currentTime - lastUsed < finalCd) {
            double timeLeft = (finalCd - (currentTime - lastUsed)) / 1000.0;
            player.sendMessage("§cKỹ năng đang hồi chiêu (" + String.format("%.1f", timeLeft) + "s)");
            return false;
        }
        rpgPlayer.setCurrentResource(rpgPlayer.getCurrentResource() - cost);
        cooldowns.put(player.getUniqueId(), currentTime);

        startCooldownNotifier(player, finalCd);

        player.sendMessage("§a§l[!] LIÊN HOÀN TÊN!");

        // 1. KHÓA DÙNG CHIÊU VÀ TĂNG TỐC CHẠY
        player.setMetadata("Is_Casting", new FixedMetadataValue(plugin, true));
        float baseSpeed = 0.2f; // Tốc chạy mặc định của Minecraft
        player.setWalkSpeed(baseSpeed * (1.0f + getSpeedBonus(level)));

        int totalArrows = getArrowCount(level);

        // 2. LOOP BẮN LIÊN TỤC (Mỗi 2 ticks = 0.1s xả 1 mũi)
        new BukkitRunnable() {
            int shotsFired = 0;

            @Override
            public void run() {
                // Hủy chiêu nếu người chơi chết, offline, hoặc đã bắn đủ số lượng
                if (!player.isOnline() || player.isDead() || shotsFired >= totalArrows) {
                    player.removeMetadata("Is_Casting", plugin);
                    player.setWalkSpeed(baseSpeed); // Trả lại tốc chạy
                    this.cancel();
                    return;
                }

                Location eyeLoc = player.getEyeLocation();
                Vector dir = eyeLoc.getDirection();
                fireRapidArrow(player, eyeLoc, dir, level);
                player.playSound(eyeLoc, org.bukkit.Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.5f);

                shotsFired++;
            }
        }.runTaskTimer(plugin, 0L, 2L); // 0L là delay ban đầu, 2L là chu kỳ lặp

        return true;
    }

    private void fireRapidArrow(Player player, Location spawnLoc, Vector velocity, int skillLevel) {
        Arrow arrow = player.getWorld().spawnArrow(spawnLoc, velocity, 3.5f, 0f);
        arrow.setShooter(player);

        // BẠN TỰ SỬA CÔNG THỨC SÁT THƯƠNG Ở ĐÂY (Khớp với phần Description)
        double dmgMultiplier = 0.15 + (skillLevel - 1) * 0.02;
        double ignoreChance = 0.10; // Cố định 10% xuyên giáp

        arrow.setMetadata("Is_Custom_Skill", new FixedMetadataValue(plugin, true));
        arrow.setMetadata("Skill_DmgMulti", new FixedMetadataValue(plugin, dmgMultiplier));
        arrow.setMetadata("Skill_ArmorPen", new FixedMetadataValue(plugin, ignoreChance));

        // [TÍCH HỢP XUYÊN THẤU]: Bơm thẳng mác Xuyên thấu vào đạn kỹ năng
        if (player.hasMetadata("Archer_Pierce_Buff")) {
            arrow.setMetadata("Is_Piercing_Arrow", new FixedMetadataValue(plugin, true));
            arrow.setMetadata("Pierce_Hit_Count", new FixedMetadataValue(plugin, 0));
            arrow.setMetadata("Pierced_Entities", new FixedMetadataValue(plugin, new java.util.ArrayList<java.util.UUID>()));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isOnGround()) {
                    arrow.remove();
                    this.cancel();
                    return;
                }
                arrow.getWorld().spawnParticle(Particle.CRIT, arrow.getLocation(), 1, 0.0, 0.0, 0.0, 0.0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}